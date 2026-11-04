package com.jmtube.app.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.SwitchAccount
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jmtube.app.R
import com.jmtube.app.data.model.Video
import com.jmtube.app.data.music.DownloadedTrack
import com.jmtube.app.data.music.model.MusicTrack
import com.jmtube.app.data.profile.displayName
import com.jmtube.app.data.stats.RecapPeriod
import com.jmtube.app.data.video.DownloadedVideo
import com.jmtube.app.ui.OnTabReselected
import com.jmtube.app.ui.components.layout.flowBottomContentPadding
import com.jmtube.app.ui.components.layout.navigation.FlowTab
import com.jmtube.app.ui.components.layout.topbar.FlowTopBar
import com.jmtube.app.ui.components.library.LibraryNavigationRow
import com.jmtube.app.ui.components.library.LibrarySection
import com.jmtube.app.ui.components.profiles.ProfileAvatar
import com.jmtube.app.ui.components.profiles.ProfileSwitchSheet
import com.jmtube.app.ui.components.profiles.ProfileSwitchViewModel
import com.jmtube.app.ui.components.settings.SettingsDestination
import com.jmtube.app.ui.components.settings.SettingsTarget
import com.jmtube.app.ui.components.shared.FlowEmptyState
import com.jmtube.app.ui.components.shared.FlowMaxContentWidth
import com.jmtube.app.ui.components.shared.MediaKind
import com.jmtube.app.ui.components.stats.RecapEntryCard
import java.time.format.TextStyle

private val ListVerticalPadding = 12.dp
private val ShelfSpacing = 24.dp
private val RecapCardPadding = 16.dp

@Composable
fun LibraryScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToPlaylists: (MediaKind?) -> Unit,
    onNavigateToLikedVideos: () -> Unit,
    onNavigateToLikedMusic: () -> Unit,
    onNavigateToWatchLater: () -> Unit,
    onNavigateToSavedShorts: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToLocalMedia: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onManageData: () -> Unit,
    onOpenSettings: (SettingsTarget) -> Unit = { target -> onManageData() },
    onOpenRecap: (RecapPeriod?) -> Unit,
    onVideoClick: (Video) -> Unit,
    onMusicClick: (MusicTrack, List<MusicTrack>, String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onMusicPlaylistClick: (String) -> Unit,
    onDownloadedVideoClick: (List<DownloadedVideo>, Int) -> Unit,
    onDownloadedMusicClick: (List<DownloadedTrack>, Int) -> Unit,
    onSavedShortClick: (Video) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val shortsEnabled by viewModel.shortsEnabled.collectAsStateWithLifecycle()
    val shelfPreviewsEnabled by viewModel.shelfPreviewsEnabled.collectAsStateWithLifecycle()
    val separatePlaylistKinds by viewModel.separatePlaylistKinds.collectAsStateWithLifecycle()
    val isLibraryEmpty by viewModel.isLibraryEmpty.collectAsStateWithLifecycle()
    val recapReady by viewModel.recapReady.collectAsStateWithLifecycle()
    val notesCount by viewModel.notesCount.collectAsStateWithLifecycle()
    val hiddenSections by viewModel.hiddenSections.collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0]
    val listState = rememberLazyListState()
    OnTabReselected(FlowTab.Library.route) { listState.animateScrollToItem(0) }
    val openSection: (LibrarySection) -> Unit = { section ->
        when (section) {
            LibrarySection.HISTORY -> onNavigateToHistory()
            LibrarySection.PLAYLISTS -> onNavigateToPlaylists(null)
            LibrarySection.VIDEO_PLAYLISTS -> onNavigateToPlaylists(MediaKind.Videos)
            LibrarySection.MUSIC_PLAYLISTS -> onNavigateToPlaylists(MediaKind.Music)
            LibrarySection.WATCH_LATER -> onNavigateToWatchLater()
            LibrarySection.LIKED_VIDEOS -> onNavigateToLikedVideos()
            LibrarySection.LIKED_MUSIC -> onNavigateToLikedMusic()
            LibrarySection.DOWNLOADS -> onNavigateToDownloads()
            LibrarySection.SAVED_SHORTS -> onNavigateToSavedShorts()
            LibrarySection.LOCAL_MEDIA -> onNavigateToLocalMedia()
            LibrarySection.NOTES -> onNavigateToNotes()
            LibrarySection.SETTINGS -> onManageData()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = { FlowTopBar(title = stringResource(R.string.nav_you)) },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(top = ListVerticalPadding, bottom = flowBottomContentPadding(ListVerticalPadding)),
            verticalArrangement = Arrangement.spacedBy(ShelfSpacing),
        ) {
            item(key = "profile_header", contentType = "profile") {
                YouProfileHeaderCard(
                    onOpenSettings = onOpenSettings,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item(key = "recap", contentType = "recap") {
                RecapEntryCard(
                    readyLabel =
                        when (val ready = recapReady) {
                            is RecapPeriod.Month -> "${ready.month.month.getDisplayName(
                                TextStyle.FULL_STANDALONE,
                                locale,
                            )} ${ready.month.year}"

                            is RecapPeriod.Year -> ready.year.toString()

                            else -> null
                        },
                    onOpen = {
                        val period = recapReady
                        viewModel.onRecapHandled()
                        onOpenRecap(period)
                    },
                    onDismiss = viewModel::onRecapHandled,
                    modifier = Modifier.padding(horizontal = RecapCardPadding),
                )
            }
            val hidden = hiddenSections ?: return@LazyColumn
            val contentSections = visibleContentSections(hidden, separatePlaylistKinds, shortsEnabled)
            val settingsSections = visibleSettingsSections(hidden, notesEnabled = notesCount != null)
            if (shelfPreviewsEnabled && isLibraryEmpty) {
                item(key = "library-empty", contentType = "empty") {
                    FlowEmptyState(
                        title = stringResource(R.string.library_empty_title),
                        subtitle = stringResource(R.string.library_empty_body),
                        icon = Icons.Outlined.VideoLibrary,
                    )
                }
            } else if (shelfPreviewsEnabled) {
                libraryShelves(
                    viewModel = viewModel,
                    sections = contentSections,
                    onOpenSection = openSection,
                    onVideoClick = onVideoClick,
                    onMusicClick = onMusicClick,
                    onPlaylistClick = onPlaylistClick,
                    onMusicPlaylistClick = onMusicPlaylistClick,
                    onDownloadedVideoClick = onDownloadedVideoClick,
                    onDownloadedMusicClick = onDownloadedMusicClick,
                    onSavedShortClick = onSavedShortClick,
                )
            } else if (contentSections.isNotEmpty()) {
                item(key = "sections", contentType = "navigation-section") {
                    val counts by viewModel.counts.collectAsStateWithLifecycle()
                    LibrarySectionList(sections = contentSections, counts = counts, onOpenSection = openSection)
                }
            }

            // An empty library has no shelves, but downloads from an earlier install can still be found.
            val emptyLibraryDownloads = shelfPreviewsEnabled && isLibraryEmpty && LibrarySection.DOWNLOADS in contentSections
            if (emptyLibraryDownloads || settingsSections.isNotEmpty()) {
                item(key = "settings-data", contentType = "navigation-section") {
                    Column(modifier = Modifier.widthIn(max = FlowMaxContentWidth).padding(horizontal = 16.dp)) {
                        LibrarySectionHeader(stringResource(R.string.library_settings_data_header))
                        if (emptyLibraryDownloads) {
                            LibraryNavigationRow(
                                icon = LibrarySection.DOWNLOADS.icon,
                                title = LibrarySection.DOWNLOADS.title,
                                subtitle = stringResource(R.string.empty_downloads),
                                onClick = onNavigateToDownloads,
                            )
                        }
                        settingsSections.forEach { section ->
                            val notes = notesCount
                            if (section == LibrarySection.NOTES && notes != null && notes > 0) {
                                LibraryNavigationRow(
                                    icon = section.icon,
                                    title = section.title,
                                    subtitle = pluralStringResource(R.plurals.notes_count, notes, notes),
                                    onClick = onNavigateToNotes,
                                )
                            } else {
                                LibrarySectionRow(section = section, counts = null, onClick = { openSection(section) })
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

internal fun LazyListScope.libraryShelves(
    viewModel: LibraryViewModel,
    sections: List<LibrarySection>,
    onOpenSection: (LibrarySection) -> Unit,
    onVideoClick: (Video) -> Unit,
    onMusicClick: (MusicTrack, List<MusicTrack>, String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onMusicPlaylistClick: (String) -> Unit,
    onDownloadedVideoClick: (List<DownloadedVideo>, Int) -> Unit,
    onDownloadedMusicClick: (List<DownloadedTrack>, Int) -> Unit,
    onSavedShortClick: (Video) -> Unit,
) {
    var showLibraryHeading = true

    sections.forEach { section ->
        val onTitleClick = { onOpenSection(section) }
        when (section) {
            LibrarySection.HISTORY -> {
                item(key = section.name, contentType = "media-shelf") {
                    LibraryMediaShelfRoute(
                        section = section,
                        itemsFlow = viewModel.history,
                        onTitleClick = onTitleClick,
                        onVideoClick = onVideoClick,
                        onMusicClick = onMusicClick,
                        onDownloadedVideoClick = onDownloadedVideoClick,
                        onDownloadedMusicClick = onDownloadedMusicClick,
                    )
                }
            }

            LibrarySection.LIKED_MUSIC -> {
                val shouldPrependHeading = showLibraryHeading
                if (shouldPrependHeading) showLibraryHeading = false
                item(key = section.name, contentType = "media-shelf") {
                    Column {
                        if (shouldPrependHeading) {
                            LibraryMainHeading()
                        }
                        LibraryMediaShelfRoute(
                            section = section,
                            itemsFlow = viewModel.likedMusic,
                            onTitleClick = onTitleClick,
                            onVideoClick = onVideoClick,
                            onMusicClick = onMusicClick,
                            onDownloadedVideoClick = onDownloadedVideoClick,
                            onDownloadedMusicClick = onDownloadedMusicClick,
                        )
                    }
                }
            }

            LibrarySection.PLAYLISTS, LibrarySection.VIDEO_PLAYLISTS, LibrarySection.MUSIC_PLAYLISTS -> {
                val shouldPrependHeading = showLibraryHeading
                if (shouldPrependHeading) showLibraryHeading = false
                item(key = section.name, contentType = "playlist-shelf") {
                    Column {
                        if (shouldPrependHeading) {
                            LibraryMainHeading()
                        }
                        LibraryPlaylistsShelf(
                            section = section,
                            videoPlaylistsFlow = viewModel.playlists.takeIf { section != LibrarySection.MUSIC_PLAYLISTS },
                            musicPlaylistsFlow = viewModel.musicPlaylists.takeIf { section != LibrarySection.VIDEO_PLAYLISTS },
                            onTitleClick = onTitleClick,
                            onVideoPlaylistClick = onPlaylistClick,
                            onMusicPlaylistClick = onMusicPlaylistClick,
                        )
                    }
                }
            }

            LibrarySection.WATCH_LATER, LibrarySection.LIKED_VIDEOS -> {
                val shouldPrependHeading = showLibraryHeading
                if (shouldPrependHeading) showLibraryHeading = false
                item(key = section.name, contentType = "video-shelf") {
                    Column {
                        if (shouldPrependHeading) {
                            LibraryMainHeading()
                        }
                        LibraryVideoShelf(
                            section = section,
                            videosFlow = if (section == LibrarySection.WATCH_LATER) viewModel.watchLater else viewModel.likedVideos,
                            onTitleClick = onTitleClick,
                            onVideoClick = onVideoClick,
                        )
                    }
                }
            }

            LibrarySection.DOWNLOADS -> {
                val shouldPrependHeading = showLibraryHeading
                if (shouldPrependHeading) showLibraryHeading = false
                item(key = section.name, contentType = "media-shelf") {
                    Column {
                        if (shouldPrependHeading) {
                            LibraryMainHeading()
                        }
                        val activeDownloads by viewModel.activeDownloadCount.collectAsStateWithLifecycle()
                        LibraryMediaShelfRoute(
                            section = section,
                            itemsFlow = viewModel.downloads,
                            onTitleClick = onTitleClick,
                            onVideoClick = onVideoClick,
                            onMusicClick = onMusicClick,
                            onDownloadedVideoClick = onDownloadedVideoClick,
                            onDownloadedMusicClick = onDownloadedMusicClick,
                            emptyMessage =
                                stringResource(
                                    if ((activeDownloads ?: 0) > 0) R.string.downloads_section_in_progress else R.string.empty_downloads,
                                ),
                        )
                    }
                }
            }

            LibrarySection.SAVED_SHORTS -> {
                val shouldPrependHeading = showLibraryHeading
                if (shouldPrependHeading) showLibraryHeading = false
                item(key = section.name, contentType = "shorts-shelf") {
                    Column {
                        if (shouldPrependHeading) {
                            LibraryMainHeading()
                        }
                        LibraryShortsShelfRoute(
                            section = section,
                            shortsFlow = viewModel.savedShorts,
                            onTitleClick = onTitleClick,
                            onShortClick = onSavedShortClick,
                        )
                    }
                }
            }

            LibrarySection.LOCAL_MEDIA, LibrarySection.NOTES, LibrarySection.SETTINGS -> {
                Unit
            }
        }
    }
}

@Composable
private fun LibraryMainHeading() {
    Text(
        text = stringResource(R.string.library),
        style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp,
        ),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .widthIn(max = FlowMaxContentWidth)
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp, bottom = 2.dp),
    )
}

@Composable
fun YouProfileHeaderCard(
    onOpenSettings: (SettingsTarget) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileSwitchViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val running = state?.profiles?.firstOrNull { it.id == viewModel.runningId }
    var openSheet by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { openSheet = true },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (running != null) {
                ProfileAvatar(profile = running, size = 56.dp)
            } else {
                Icon(
                    Icons.Outlined.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = running?.displayName(context) ?: stringResource(R.string.profile_default_name),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.profiles_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Icon(
                        Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Surface(
                onClick = { openSheet = true },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Outlined.SwitchAccount,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.profiles_switch),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }

    if (openSheet) {
        ProfileSwitchSheet(
            profiles = state?.profiles.orEmpty(),
            runningId = viewModel.runningId,
            onSwitch = viewModel::switchTo,
            onAdd = { onOpenSettings(SettingsTarget(SettingsDestination.PROFILE_EDIT)) },
            onManage = { onOpenSettings(SettingsTarget(SettingsDestination.PROFILES)) },
            onDismiss = { openSheet = false },
        )
    }
}
