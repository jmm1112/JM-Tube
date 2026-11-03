package com.jmtube.app.ui.startup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.navigation.compose.hiltViewModel
import com.jmtube.app.data.local.AppUiModePreferences
import com.jmtube.app.data.recommendation.FlowNeuroEngine
import com.jmtube.app.data.repository.DeArrowRepository
import com.jmtube.app.data.repository.OriginalTitleRepository
import com.jmtube.app.platform.AppIconController
import com.jmtube.app.platform.AppUiMode
import com.jmtube.app.platform.AppUiRoot
import com.jmtube.app.platform.DeviceFormFactorDetector
import com.jmtube.app.security.AppLockPreferences
import com.jmtube.app.ui.FlowApp
import com.jmtube.app.ui.IncomingLinks
import com.jmtube.app.ui.components.profiles.ProfileSwitchingOverlay
import com.jmtube.app.ui.components.security.AppLockOverlay
import com.jmtube.app.ui.components.shared.ProvideChannelGroupLabels
import com.jmtube.app.ui.components.shared.ProvideDateDisplaySettings
import com.jmtube.app.ui.components.shared.card.ProvideVideoCardState
import com.jmtube.app.ui.screens.crash.CrashReportScreen
import com.jmtube.app.ui.screens.playon.LocalPlayOn
import com.jmtube.app.ui.screens.playon.PlayOnViewModel
import com.jmtube.app.ui.theme.FlowFontFamily
import com.jmtube.app.ui.tv.FlowTvApp
import com.jmtube.app.ui.tv.screens.TvProfileGate
import com.jmtube.app.ui.utils.ProvideWindowSizeClass
import com.jmtube.app.utils.FlowCrashHandler
import kotlinx.coroutines.flow.StateFlow

/**
 * Everything MainActivity shows: the theme gate, the phone or TV app, a crash report from the last session, and the
 * providers every screen reads. [onUiRoot] reports which of phone or TV is showing.
 */
@Composable
internal fun FlowRoot(
    storedTheme: StateFlow<ThemeSettings?>,
    links: IncomingLinks,
    splashController: SplashController,
    originalTitles: OriginalTitleRepository,
    deArrow: DeArrowRepository,
    appIconController: AppIconController,
    appLockPreferences: AppLockPreferences? = null,
    onUiRoot: (AppUiRoot) -> Unit,
) {
    // Nothing is composed until the theme is known: the splash covers the wait, and the app
    // composes once in the right theme instead of twice.
    val theme = storedTheme.collectAsState().value ?: return

    val context = LocalContext.current
    val applicationContext = context.applicationContext
    val configuration = LocalConfiguration.current
    val uiPreferences = remember { AppUiModePreferences(applicationContext) }
    val appUiMode by uiPreferences.mode.collectAsState(initial = AppUiMode.AUTOMATIC)
    val deviceFormFactor =
        remember(configuration.uiMode, context) {
            DeviceFormFactorDetector.detect(context)
        }
    val appUiRoot = appUiMode.resolve(deviceFormFactor)
    SideEffect { onUiRoot(appUiRoot) }

    // Check for a crash that happened last session.
    // If found, show the CrashReportScreen instead of the normal UI. It is cleared as soon
    // as it is read, so leaving the screen any way shows it once, not on every launch.
    var pendingCrashLog by remember {
        mutableStateOf(
            FlowCrashHandler.getLastCrash(applicationContext)?.also {
                FlowCrashHandler.clearLastCrash(applicationContext)
            },
        )
    }

    if (pendingCrashLog != null) {
        SideEffect { splashController.contentReady = true }
        FlowTheme(theme.copy(fontFamily = FlowFontFamily)) {
            CrashReportScreen(
                report = pendingCrashLog!!,
                onContinue = { pendingCrashLog = null },
            )
        }
        return
    }

    // Initialize Flow Neuro Engine
    LaunchedEffect(Unit) {
        FlowNeuroEngine.initialize(applicationContext)
    }

    var startupAnimationDone by rememberSaveable { mutableStateOf(false) }

    FlowTheme(theme) {
        SideEffect { splashController.contentReady = true }
        val splashTone = splashTone(MaterialTheme.colorScheme.background)
        val selectedIcon by appIconController.selected.collectAsState(initial = null)
        var activeIconSuffix by remember { mutableStateOf(com.jmtube.app.util.AppIcons.DEFAULT_SUFFIX) }
        LaunchedEffect(splashTone, selectedIcon) {
            val isDark = splashTone != SplashTone.LIGHT
            val currentSuffix = selectedIcon ?: appIconController.activeSuffix()
            val effectiveSuffix = com.jmtube.app.util.AppIcons.resolveSmartSuffix(currentSuffix, isDark)
            activeIconSuffix = effectiveSuffix
            if (appIconController.activeSuffix() != effectiveSuffix) {
                appIconController.apply(effectiveSuffix)
            }
            splashController.rememberTheme(splashTone, effectiveSuffix)
        }

        // Date preferences: five DataStore flows used to be opened per video card,
        // metadata line, info section, description sheet and info dialog.
        ProvideWindowSizeClass {
            ProvideDateDisplaySettings {
                // Card preferences and watch progress are collected once here. Cards used to
                // collect them individually, so a feed of ten opened ten Room observers and
                // fifty DataStore collectors.
                ProvideVideoCardState(originalTitles, deArrow) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .semantics { testTagsAsResourceId = true },
                    ) {
                        // 1. MAIN APP (Home/NavHost)
                        // This loads *behind* the splash screen immediately.
                        // By the time splash fades, this is ready.
                        val deeplinkVideoId by links.videoId
                        val isDeeplinkShort by links.isShort
                        val isDeeplinkShared by links.isShared
                        val openMusicPlayerRequest by links.musicPlayerRequest
                        val pendingRoute by links.route
                        val downloadVideoId by links.downloadVideoId

                        ProvideChannelGroupLabels {
                            // One "Play on" for every player and both shells.
                            CompositionLocalProvider(LocalPlayOn provides hiltViewModel<PlayOnViewModel>()) {
                                if (appUiRoot == AppUiRoot.TV) {
                                    SideEffect { splashController.contentReady = true }
                                    TvProfileGate(hasLink = deeplinkVideoId != null) { startSettings ->
                                        FlowTvApp(
                                            deeplinkVideoId = deeplinkVideoId,
                                            isShort = isDeeplinkShort,
                                            onDeeplinkConsumed = links::consumeVideo,
                                            startSettings = startSettings,
                                        )
                                    }
                                } else {
                                    FlowApp(
                                        currentTheme = theme.themeMode,
                                        themeVariant = theme.themeVariant,
                                        systemLightThemeMode = theme.systemLightThemeMode,
                                        systemDarkThemeMode = theme.systemDarkThemeMode,
                                        deeplinkVideoId = deeplinkVideoId,
                                        isShort = isDeeplinkShort,
                                        isSharedLink = isDeeplinkShared,
                                        openMusicPlayerRequest = openMusicPlayerRequest,
                                        onDeeplinkConsumed = links::consumeVideo,
                                        pendingRoute = pendingRoute,
                                        onPendingRouteConsumed = links::consumeRoute,
                                        downloadVideoId = downloadVideoId,
                                        onDownloadConsumed = links::consumeDownload,
                                        onStartDestinationKnown = { splashController.contentReady = true },
                                    )
                                }
                            }
                        }
                        ProfileSwitchingOverlay(
                            currentTone = splashTone.name,
                            onSplashTone = { name ->
                                SplashTone.entries.firstOrNull { it.name == name }?.let {
                                    splashController.rememberTheme(
                                        it,
                                        appIconController.activeSuffix(),
                                    )
                                }
                            },
                        )
                        val isAppLocked = appLockPreferences?.isLocked?.collectAsState()?.value ?: false
                        if (!startupAnimationDone && appUiRoot != AppUiRoot.TV && !isAppLocked) {
                            YouTubeStartupAnimation(
                                onFinished = { startupAnimationDone = true },
                                iconSuffix = selectedIcon ?: activeIconSuffix,
                                backgroundColor = MaterialTheme.colorScheme.background,
                                contentColor = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        if (appUiRoot != AppUiRoot.TV && appLockPreferences != null) {
                            AppLockOverlay(appLockPreferences = appLockPreferences)
                        }
                    }
                }
            }
        }
    }
}
