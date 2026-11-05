package com.jmtube.app.sync2.service

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.jmtube.app.data.profile.CreateResult
import com.jmtube.app.data.profile.ProfileRegistry
import com.jmtube.app.data.profile.ProfileRepository
import com.jmtube.app.data.profile.displayName
import com.jmtube.app.sync.identity.DeviceIdentity
import com.jmtube.app.sync.identity.Hlc
import com.jmtube.app.sync.protocol.CollectionWire
import com.jmtube.app.sync.protocol.SyncSerialization
import com.jmtube.app.sync2.changes.IncomingBatch
import com.jmtube.app.sync2.changes.PROFILES
import com.jmtube.app.sync2.data.DEFAULT_PROFILE
import com.jmtube.app.sync2.data.KeptBatches
import com.jmtube.app.sync2.data.LinkedDatabase
import com.jmtube.app.sync2.data.ProfileLinkRow
import com.jmtube.app.sync2.data.ProfileNameRow
import com.jmtube.app.sync2.engine.LinkedException
import com.jmtube.app.sync2.engine.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** A profile on this device, the link it syncs under when it does, and whether it is the one running. */
data class LocalProfile(
    val id: String,
    val name: String,
    val link: String?,
    val running: Boolean = false,
)

/** A synced profile no profile here syncs; [name] is null when no device named it, and blank for a default profile never renamed. */
data class OtherProfile(
    val link: String,
    val name: String?,
)

/** What Linked devices offers about profiles: this device's, the others synced, and room for a new one. */
data class ProfileChoices(
    val local: List<LocalProfile> = emptyList(),
    val others: List<OtherProfile> = emptyList(),
    val canCreate: Boolean = false,
)

/** A record of the `profiles` collection (spec/protocol.md, Profiles). */
@Serializable
private data class ProfileRecord(
    val deleted: Boolean,
    val hlc: String,
    val link: String,
    val name: String,
    val colorKey: String = "",
    val avatarKind: String = "initial",
    val avatarEmoji: String = "",
    val photoVersion: Long = 0L,
    val photoBase64: String? = null,
    val statusQuote: String = "",
    val bannerStyle: String = "sunset",
    val showMilestones: Boolean = true,
)

private val records = Json { ignoreUnknownKeys = true }

/** Which of this device's profiles sync, and under which links (spec/protocol.md, Profiles). */
@Singleton
class LinkedProfiles
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val db: LinkedDatabase,
        private val profiles: ProfileRepository,
        private val identity: DeviceIdentity,
        private val kept: KeptBatches,
    ) {
        private val dao get() = db.profiles()

        suspend fun defaultId(): String = checkNotNull(profiles.state.first().defaultId)

        /** The link the running profile syncs under, or null when it doesn't sync. */
        suspend fun activeLink(): String? = dao.linkOf(profiles.running().id)

        /** Whether the running profile syncs and has a link still to read or batches kept for it: its data isn't all here. */
        suspend fun catchingUp(): Boolean {
            val row = dao.links().firstOrNull { it.profile == profiles.running().id } ?: return false
            return !row.caughtUp || kept.count(row.link) > 0 || dao.reads().any { it.link == row.link }
        }

        val choices: Flow<ProfileChoices> =
            combine(profiles.state, dao.observeLinks(), dao.observeNames(), dao.observeNamed()) { state, links, names, named ->
                val linked = links.associate { it.profile to it.link }
                val deleted = names.filter { it.deleted }.map { it.link }.toSet()
                val known = names.map { it.link } + named.mapNotNull { Json.decodeFromString(String.serializer().nullable, it) }
                val running = profiles.running().id
                ProfileChoices(
                    local = state.profiles.map { LocalProfile(it.id, it.displayName(context), linked[it.id], it.id == running) },
                    others =
                        known
                            .distinct()
                            .filter { it !in deleted && it !in linked.values }
                            .map { link -> OtherProfile(link, names.firstOrNull { it.link == link }?.name) },
                    canCreate = state.profiles.size < ProfileRegistry.MAX_PROFILES,
                )
            }

        /**
         * This device's links, or null while it can't tell yet. A restored device, whose group record names no
         * link, links Default once the logs name one link or none (spec/protocol.md, Recovery); with several, or
         * once every link here stopped, the user chooses.
         */
        suspend fun links(session: Session): List<ProfileLinkRow>? {
            dao.moveProfile(DEFAULT_PROFILE, defaultId())
            val present =
                profiles.state
                    .first()
                    .profiles
                    .map { it.id }
            dao.links().filter { it.profile !in present }.forEach { unlink(it.profile, it.link) }
            dao.links().takeIf { it.isNotEmpty() || session.record.link.isNotEmpty() }?.let { return it }
            val named = session.namedLinks() ?: return null
            if (named.size <= 1) {
                dao.choose(defaultId(), named.singleOrNull() ?: defaultId(), caughtUp = true)
            } else {
                // Reading every log again on each sync would repeat that first read: the user chooses from here on.
                dao.setGroupLink(named.min())
            }
            return dao.links()
        }

        /**
         * The running [profile] syncs under [link], its own id for a link of its own; its data merges with the link's. Only
         * the running profile's data is sent, so another profile is switched to before it links.
         */
        suspend fun link(
            profile: String,
            link: String,
        ) {
            check(profile == profiles.running().id)
            choose(profile, link)
        }

        private suspend fun choose(
            profile: String,
            link: String,
        ) {
            // A link this device never read needs no catching up.
            val read = link in dao.names().map { it.link } || link in choices.first().others.map { it.link }
            dao.choose(profile, link, caughtUp = !read)
        }

        /** Creates a profile here for [link], named as the link is, and links it; false at the profile limit. */
        suspend fun createFor(
            link: String,
            name: String,
        ): Boolean {
            val created = profiles.create(name) as? CreateResult.Created ?: return false
            choose(created.profile.id, link)
            return true
        }

        /** Stops syncing [profile] here; its data stays. A device in a group keeps at least one profile linked. */
        suspend fun stopHere(profile: String) {
            val link = dao.linkOf(profile) ?: return
            check(dao.links().size > 1)
            unlink(profile, link)
        }

        /** Stops syncing [profile] on every device: its link is deleted, and each device keeps its data. */
        suspend fun stopEverywhere(profile: String) {
            val link = dao.linkOf(profile) ?: return
            check(dao.links().size > 1)
            val currentProfile = profiles.state.first().profiles.firstOrNull { it.id == profile }
            if (currentProfile != null) {
                record(link, currentProfile, deleted = true)
            } else {
                val linkName = dao.names().firstOrNull { it.link == link }?.name ?: nameOf(profile).orEmpty()
                dao.saveNames(listOf(ProfileNameRow(link, linkName, Hlc(identity.editTime(), 0, identity.hlcNode()).encode(), deleted = true)))
            }
            unlink(profile, link)
        }

        private suspend fun unlink(
            profile: String,
            link: String,
        ) {
            dao.unlink(profile)
            kept.drop(link)
            dao.dropReads(link)
        }

        /**
         * The link a new device's Default joins: Default's when it syncs, else the first profile's here that does, whichever
         * profile runs. A device whose every link was deleted adds no device until the user links a profile again.
         */
        suspend fun welcomeLink(): String {
            val linked = dao.links().associate { it.profile to it.link }
            val state = profiles.state.first()
            return linked[state.defaultId]
                ?: state.profiles.firstNotNullOfOrNull { linked[it.id] }
                ?: throw LinkedException.NoProfileSynced()
        }

        private fun readProfilePhotoBase64(profileId: String): String? {
            return runCatching {
                val file = com.jmtube.app.data.profile.ProfileAvatarPhotos.fileFor(context, profileId)
                if (file.isFile && file.length() > 0) {
                    android.util.Base64.encodeToString(file.readBytes(), android.util.Base64.NO_WRAP)
                } else null
            }.getOrNull()
        }

        private fun saveProfilePhotoBase64(profileId: String, base64: String): Long {
            return runCatching {
                val bytes = android.util.Base64.decode(base64, android.util.Base64.NO_WRAP)
                val file = com.jmtube.app.data.profile.ProfileAvatarPhotos.fileFor(context, profileId)
                file.parentFile?.mkdirs()
                file.writeBytes(bytes)
                System.currentTimeMillis()
            }.getOrDefault(System.currentTimeMillis())
        }

        private val exportedSignatures = java.util.concurrent.ConcurrentHashMap<String, String>()

        /** This device's `profiles` records, after naming each link it syncs that has no record, or whose profile was renamed here. */
        suspend fun export(): Map<String, CollectionWire> {
            val names = dao.names().associateBy { it.link }
            val currentProfiles = profiles.state.first().profiles.associateBy { it.id }
            for (row in dao.links()) {
                val profile = currentProfiles[row.profile] ?: continue
                val current = names[row.link]
                val sig = "${profile.name}_${profile.colorKey}_${profile.avatar.kind}_${profile.avatar.emoji}_${profile.avatar.photoVersion}_${profile.statusQuote}_${profile.bannerStyle}_${profile.showMilestones}"
                val lastSig = exportedSignatures[row.link]
                if (current == null || (!current.deleted && (current.name != profile.name || sig != lastSig))) {
                    exportedSignatures[row.link] = sig
                    record(row.link, profile, deleted = false)
                }
            }
            val lines =
                dao.names().map { nameRow ->
                    val profile = dao.links().firstOrNull { it.link == nameRow.link }?.let { currentProfiles[it.profile] }
                    val pid = profile?.id ?: dao.links().firstOrNull { it.link == nameRow.link }?.profile
                    val photoBase64 = if (pid != null) readProfilePhotoBase64(pid) else null
                    SyncSerialization.canonicalJson(
                        records.encodeToJsonElement(
                            ProfileRecord.serializer(),
                            ProfileRecord(
                                deleted = nameRow.deleted,
                                hlc = nameRow.hlc,
                                link = nameRow.link,
                                name = profile?.name ?: nameRow.name,
                                colorKey = profile?.colorKey ?: "",
                                avatarKind = profile?.avatar?.kind ?: "initial",
                                avatarEmoji = profile?.avatar?.emoji ?: "",
                                photoVersion = profile?.avatar?.photoVersion ?: 0L,
                                photoBase64 = photoBase64,
                                statusQuote = profile?.statusQuote ?: "",
                                bannerStyle = profile?.bannerStyle ?: "sunset",
                                showMilestones = profile?.showMilestones ?: true,
                            ),
                        ),
                    )
                }
            return mapOf(PROFILES to CollectionWire(lines, lines.size, ""))
        }

        /**
         * Merges other devices' `profiles` records, last writer wins: a deleted link stops syncing here, even the
         * only one, and a renamed one renames its profile. A blank name is a default profile never renamed, which no
         * profile here takes.
         */
        suspend fun merge(batches: List<IncomingBatch>) {
            for (line in batches.flatMap { it.lines }) {
                val record = runCatching { records.decodeFromString(ProfileRecord.serializer(), line) }.getOrNull() ?: continue
                val current = dao.names().firstOrNull { it.link == record.link }
                if (current != null && Hlc.compareEncoded(record.hlc, current.hlc) <= 0) continue
                identity.observe(Hlc.decode(record.hlc).physicalMs)
                dao.saveNames(
                    listOf(
                        ProfileNameRow(
                            link = record.link,
                            name = record.name,
                            hlc = record.hlc,
                            deleted = record.deleted,
                        ),
                    ),
                )
                val profileId = dao.links().firstOrNull { it.link == record.link }?.profile ?: continue
                val existingProfile = profiles.state.first().profiles.firstOrNull { it.id == profileId }
                if (record.deleted) {
                    unlink(profileId, record.link)
                } else {
                    if (record.name.isNotBlank()) {
                        profiles.rename(profileId, record.name)
                    }
                    val avatar = when (record.avatarKind) {
                        com.jmtube.app.data.profile.ProfileAvatar.PHOTO -> {
                            var photoVersion = record.photoVersion
                            if (!record.photoBase64.isNullOrBlank()) {
                                photoVersion = saveProfilePhotoBase64(profileId, record.photoBase64)
                            } else if (existingProfile?.avatar?.kind == com.jmtube.app.data.profile.ProfileAvatar.PHOTO) {
                                photoVersion = existingProfile.avatar.photoVersion
                            }
                            com.jmtube.app.data.profile.ProfileAvatar(kind = com.jmtube.app.data.profile.ProfileAvatar.PHOTO, photoVersion = photoVersion)
                        }
                        com.jmtube.app.data.profile.ProfileAvatar.EMOJI -> {
                            com.jmtube.app.data.profile.ProfileAvatar(kind = com.jmtube.app.data.profile.ProfileAvatar.EMOJI, emoji = record.avatarEmoji)
                        }
                        else -> {
                            if (existingProfile?.avatar?.kind == com.jmtube.app.data.profile.ProfileAvatar.PHOTO && record.photoBase64.isNullOrBlank()) {
                                existingProfile.avatar
                            } else {
                                com.jmtube.app.data.profile.ProfileAvatar(kind = com.jmtube.app.data.profile.ProfileAvatar.INITIAL)
                            }
                        }
                    }
                    profiles.setAppearance(profileId, record.colorKey.ifBlank { existingProfile?.colorKey ?: "" }, avatar)
                    profiles.setCustomizations(profileId, record.statusQuote, record.bannerStyle, record.showMilestones)
                }
            }
        }

        private suspend fun record(
            link: String,
            profile: com.jmtube.app.data.profile.Profile,
            deleted: Boolean,
        ) = dao.saveNames(
            listOf(
                ProfileNameRow(
                    link = link,
                    name = profile.name,
                    hlc = Hlc(identity.editTime(), 0, identity.hlcNode()).encode(),
                    deleted = deleted,
                ),
            ),
        )

        /** The name a `profiles` record carries: the registry's own, blank for a default profile never renamed. */
        private suspend fun nameOf(profile: String): String? =
            profiles.state
                .first()
                .profiles
                .firstOrNull { it.id == profile }
                ?.name
    }
