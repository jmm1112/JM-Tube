package com.jmtube.app.sync2.linking

import com.jmtube.app.sync.crypto.SyncBytes
import com.jmtube.app.sync2.crypto.ProtocolException
import com.jmtube.app.sync2.engine.LinkedException
import com.jmtube.app.sync2.engine.Session
import com.jmtube.app.sync2.roster.Entry
import com.jmtube.app.sync2.roster.RosterState
import com.jmtube.app.sync2.roster.applyEntry
import com.jmtube.app.sync2.roster.openDeviceMeta

/**
 * Normalizes device names across sync and display, ensuring JM Tube branding is used
 * in place of any legacy "Flow" names.
 */
fun sanitizeDeviceName(name: String): String =
    when {
        name.startsWith("Flow (") -> "JM Tube (" + name.removePrefix("Flow (")
        name == "Flow (Android)" -> "JM Tube (Android)"
        name == "Flow" -> "JM Tube"
        else -> name
    }

/**
 * Device names and platforms, as the latest roster entry naming each device says. Names are only
 * shown, so one that can't be read is left out instead of stopping sync.
 */
suspend fun deviceNames(session: Session): Map<String, DeviceMeta> = deviceNames(session.entries, session.group, session::epochKey)

internal suspend fun deviceNames(
    entries: List<Entry>,
    group: ByteArray,
    epochKey: suspend (Int) -> ByteArray,
): Map<String, DeviceMeta> {
    val names = HashMap<String, DeviceMeta>()
    var state: RosterState? = null
    for (entry in entries) {
        val after = applyEntry(state, entry)
        for ((did, sealed) in entry.meta.orEmpty()) {
            openName(group, epochKey, after.epoch, entry.seq, did, sealed)?.let {
                val clean = sanitizeDeviceName(it.name)
                names[did] = if (clean != it.name) it.copy(name = clean) else it
            }
        }
        state = after
    }
    return names
}

private suspend fun openName(
    group: ByteArray,
    epochKey: suspend (Int) -> ByteArray,
    epoch: Int,
    seq: Long,
    did: String,
    sealed: String,
): DeviceMeta? =
    try {
        val content =
            openDeviceMeta(
                epochKey(epoch),
                group,
                epoch,
                seq,
                SyncBytes.fromHex(did),
                SyncBytes.b64urlDecode(sealed),
            )
        val meta = pairingJson.decodeFromString(DeviceMeta.serializer(), content)
        val clean = sanitizeDeviceName(meta.name)
        if (clean != meta.name) meta.copy(name = clean) else meta
    } catch (_: ProtocolException) {
        null
    } catch (_: LinkedException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
