package com.jmtube.app.sync2.linking

import com.jmtube.app.sync.crypto.SyncBytes
import com.jmtube.app.sync.crypto.SyncCrypto
import com.jmtube.app.sync.protocol.SyncSerialization
import com.jmtube.app.sync2.crypto.MemberSecrets
import com.jmtube.app.sync2.crypto.recoveryKeys
import com.jmtube.app.sync2.crypto.recoveryWords
import com.jmtube.app.sync2.crypto.wrapEpochKey
import com.jmtube.app.sync2.data.GroupRecord
import com.jmtube.app.sync2.data.LAN_STORE
import com.jmtube.app.sync2.data.LinkedDatabase
import com.jmtube.app.sync2.data.WEBDAV_STORE
import com.jmtube.app.sync2.data.addEntries
import com.jmtube.app.sync2.engine.LinkedException
import com.jmtube.app.sync2.roster.Draft
import com.jmtube.app.sync2.roster.Entry
import com.jmtube.app.sync2.roster.Op
import com.jmtube.app.sync2.roster.Role
import com.jmtube.app.sync2.roster.buildEntry
import com.jmtube.app.sync2.roster.entryHash
import com.jmtube.app.sync2.roster.epochDraft
import com.jmtube.app.sync2.roster.memberSubject
import com.jmtube.app.sync2.roster.sealDeviceMeta
import com.jmtube.app.sync2.store.Appended
import com.jmtube.app.sync2.store.FileLayout
import com.jmtube.app.sync2.store.LocalStore
import com.jmtube.app.sync2.store.ServerStore
import com.jmtube.app.sync2.store.WebDavStore
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val STORE_JSON = "store.json"

/** The genesis of a new group with this device and a new recovery key, and the recovery words to show once. */
private class Genesis(
    device: MemberSecrets,
    meta: DeviceMeta,
) {
    val group: ByteArray = SyncCrypto.randomBytes(16)
    val entry: Entry
    val wraps: Map<String, String>
    val words: String

    init {
        val epochKey = SyncCrypto.randomBytes(32)
        val recoverySecret = SyncCrypto.randomBytes(32)
        val recovery = recoveryKeys(recoverySecret)
        val members = listOf(device, recovery)
        val sealedMeta = sealDeviceMeta(epochKey, group, 0, 0, device.did, metaJson(meta))
        val draft =
            Draft(
                op = Op.GENESIS,
                signer = SyncBytes.toHex(device.did),
                add =
                    listOf(
                        memberSubject(device.sigPublic, device.kemPublic, Role.DEVICE),
                        memberSubject(recovery.sigPublic, recovery.kemPublic, Role.RECOVERY),
                    ),
                epoch = epochDraft(epochKey, group, 0, members.map { SyncBytes.toHex(it.did) }),
                meta = mapOf(SyncBytes.toHex(device.did) to SyncBytes.b64urlEncode(sealedMeta)),
            )
        entry = buildEntry(group, null, draft, members)
        wraps =
            members.associate { SyncBytes.toHex(it.did) to SyncBytes.b64urlEncode(wrapEpochKey(it.kemPublic, group, 0, it.did, epochKey)) }
        words = recoveryWords(recoverySecret)
        recovery.wipe()
        recoverySecret.fill(0)
        epochKey.fill(0)
    }
}

/**
 * Creates the group, claims the server with the operator's admin token, and returns the recovery
 * words to show once.
 */
suspend fun createGroup(
    db: LinkedDatabase,
    storeUrl: String,
    adminToken: String,
    device: MemberSecrets,
    meta: DeviceMeta,
    link: String,
): String {
    val genesis = Genesis(device, meta)
    val store = ServerStore.connect(storeUrl, device, null, SyncBytes.toHex(genesis.group))
    store.claim(adminToken, genesis.entry, genesis.wraps)
    val roster = store.rosterAfter(-1)
    if (roster.map(::entryHash) != listOf(entryHash(genesis.entry))) throw LinkedException.Unexpected()
    db.groups().addEntries(roster)
    db.profiles().saveGroup(GroupRecord(SyncBytes.toHex(genesis.group), storeUrl, store.identity().first, link), profile = link)
    return genesis.words
}

/**
 * Creates the group in an empty WebDAV folder: probes it, marks it with `store.json`, then writes the
 * genesis with its wraps (spec/store.md, Layout). Returns the recovery words to show once, and
 * whether the server honours `If-None-Match`.
 */
suspend fun createWebDavGroup(
    db: LinkedDatabase,
    files: WebDavStore,
    device: MemberSecrets,
    meta: DeviceMeta,
    link: String,
): Pair<String, Boolean> {
    val conditional = files.probe()
    if (files.get(STORE_JSON) != null) throw LinkedException.ServerHasGroup()
    val genesis = Genesis(device, meta)
    val group = SyncBytes.toHex(genesis.group)
    files.put(
        STORE_JSON,
        SyncSerialization
            .canonicalJson(
                buildJsonObject {
                    put("format", "FLOW-SYNC/2")
                    put("group", group)
                },
            ).toByteArray(),
    )
    val layout = FileLayout(files, SyncBytes.toHex(device.did))
    if (layout.appendRoster("", genesis.entry, genesis.wraps, null) != Appended.Done) throw LinkedException.ServerHasGroup()
    db.groups().addEntries(listOf(genesis.entry))
    db.profiles().saveGroup(GroupRecord(group, WEBDAV_STORE + files.location, null, link), profile = link)
    return genesis.words to conditional
}

/** Creates a group that lives on its devices alone (spec/live.md, LAN groups): nothing is claimed. */
suspend fun createLanGroup(
    db: LinkedDatabase,
    local: LocalStore,
    device: MemberSecrets,
    meta: DeviceMeta,
    link: String,
): String {
    val genesis = Genesis(device, meta)
    local.hold(genesis.entry, 0, genesis.wraps.mapValues { (_, wrap) -> SyncBytes.b64urlDecode(wrap) }, null)
    db.groups().addEntries(listOf(genesis.entry))
    db.profiles().saveGroup(GroupRecord(SyncBytes.toHex(genesis.group), LAN_STORE, null, link), profile = link)
    return genesis.words
}
