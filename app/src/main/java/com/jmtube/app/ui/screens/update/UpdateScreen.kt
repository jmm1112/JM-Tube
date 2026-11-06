package com.jmtube.app.ui.screens.update

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jmtube.app.BuildConfig
import com.jmtube.app.R
import com.jmtube.app.data.local.LocalDataManager
import com.jmtube.app.data.local.PlayerPreferences
import com.jmtube.app.data.update.AppRelease
import com.jmtube.app.data.update.UpdateRepository
import com.jmtube.app.ui.components.layout.topbar.FlowTopBar
import com.jmtube.app.ui.components.shared.FlowEmptyState
import com.jmtube.app.ui.components.shared.FlowLoadingIndicator
import com.jmtube.app.ui.components.shared.FlowMaxContentWidth
import com.jmtube.app.ui.screens.home.SmartAppHeaderLogoIcon
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

private val ContentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp)
private val HeroSize = 96.dp
private val HeroLogoWidth = 44.dp
private val HeaderSpacing = 12.dp
private val FactSpacing = 8.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateScreen(
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val preferences = remember(context) { PlayerPreferences(context) }
    val selectedAppIcon by preferences.selectedAppIcon.collectAsStateWithLifecycle(initialValue = null)

    var isChecking by remember { mutableStateOf(true) }
    var release by remember { mutableStateOf<AppRelease?>(null) }
    var stage by remember { mutableStateOf<UpdateStage>(UpdateStage.Available) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val updates = UpdateRepository(OkHttpClient(), LocalDataManager(context))
        val result = runCatching { updates.fetch() }.getOrNull()
        if (result != null) {
            release = result
            stage = UpdateStage.Available
        } else {
            release = null
        }
        isChecking = false
    }

    val currentRelease = release

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            FlowTopBar(
                title = "",
                onBack = onClose,
                actions = {
                    if (currentRelease != null) {
                        UpdateMenu(
                            onSkip = onClose,
                            onOpenGitHub = { uriHandler.openUri(currentRelease.downloadUrl) },
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (currentRelease != null) {
                UpdateActionBar(
                    stage = stage,
                    onPrimary = {
                        when (stage) {
                            UpdateStage.Available -> {
                                scope.launch {
                                    for (p in 1..100) {
                                        stage = UpdateStage.Downloading(p / 100f)
                                        kotlinx.coroutines.delay(30)
                                    }
                                    stage = UpdateStage.Ready
                                }
                            }
                            UpdateStage.Ready, UpdateStage.NeedsPermission -> {
                                runCatching { uriHandler.openUri(currentRelease.downloadUrl) }
                            }
                            else -> {
                                runCatching { uriHandler.openUri(currentRelease.downloadUrl) }
                            }
                        }
                    },
                    onSecondary = onClose,
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            when {
                isChecking -> {
                    FlowLoadingIndicator()
                }

                currentRelease == null -> {
                    FlowEmptyState(title = stringResource(R.string.flow_is_up_to_date), icon = Icons.Outlined.Verified)
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.widthIn(max = FlowMaxContentWidth).fillMaxWidth(),
                        contentPadding = ContentPadding,
                    ) {
                        item(key = "header") {
                            ReleaseHeader(currentRelease, selectedAppIcon)
                        }
                        if (currentRelease.releaseNotes.isNotBlank()) {
                            val notesList = currentRelease.releaseNotes.lines().filter { it.isNotBlank() }
                            releaseNotes(notesList)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReleaseHeader(
    release: AppRelease,
    selectedAppIcon: String?,
) {
    val context = LocalContext.current
    val formattedDate = remember(release.publishedAt) {
        if (release.publishedAt.isBlank()) "" else {
            runCatching {
                val instant = java.time.Instant.parse(release.publishedAt)
                val formatter = java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy")
                instant.atZone(java.time.ZoneId.systemDefault()).format(formatter)
            }.getOrDefault(release.publishedAt.substringBefore("T"))
        }
    }
    val sizeText = remember(release.apkSizeBytes) {
        if (release.apkSizeBytes > 0L) Formatter.formatShortFileSize(context, release.apkSizeBytes) else ""
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(HeaderSpacing),
        modifier = Modifier.padding(bottom = HeaderSpacing),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(HeroSize),
        ) {
            Box(contentAlignment = Alignment.Center) {
                SmartAppHeaderLogoIcon(
                    iconSuffix = selectedAppIcon,
                    modifier = Modifier.size(HeroLogoWidth),
                )
            }
        }
        Text(
            text = "JM Tube v${release.versionName} is here",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(FactSpacing),
            verticalArrangement = Arrangement.spacedBy(FactSpacing),
        ) {
            ReleaseFact(
                Icons.Outlined.SyncAlt,
                "v${BuildConfig.VERSION_NAME} → v${release.versionName}",
            )
            if (sizeText.isNotBlank()) {
                ReleaseFact(
                    Icons.Outlined.Download,
                    sizeText,
                )
            }
            if (formattedDate.isNotBlank()) {
                ReleaseFact(
                    Icons.Outlined.CalendarToday,
                    formattedDate,
                )
            }
        }
    }
}

@Composable
private fun UpdateMenu(
    onSkip: () -> Unit,
    onOpenGitHub: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.more_options))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("Skip this version") },
                leadingIcon = { Icon(Icons.Outlined.NotificationsOff, contentDescription = null) },
                onClick = {
                    open = false
                    onSkip()
                },
            )
            DropdownMenuItem(
                text = { Text("Open in GitHub") },
                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null) },
                onClick = {
                    open = false
                    onOpenGitHub()
                },
            )
        }
    }
}
