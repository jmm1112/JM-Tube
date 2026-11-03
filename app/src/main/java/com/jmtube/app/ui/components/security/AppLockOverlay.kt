package com.jmtube.app.ui.components.security

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jmtube.app.R
import com.jmtube.app.security.AppLockPreferences
import com.jmtube.app.security.NativeAuthHelper
import com.jmtube.app.ui.screens.home.YouTubeLogoIcon

/**
 * Full-screen App Lock gate rendered at the root of JM Tube when App Lock is active.
 * Automatically triggers Android's native Biometric or Screen Lock prompt.
 */
@Composable
fun AppLockOverlay(appLockPreferences: AppLockPreferences) {
    val context = LocalContext.current
    val activity = context as? Activity
    val isLocked by appLockPreferences.isLocked.collectAsStateWithLifecycle()
    val enabled by appLockPreferences.enabled.collectAsStateWithLifecycle(initialValue = false)
    val biometricPreferred by appLockPreferences.biometricPreferred.collectAsStateWithLifecycle(initialValue = true)
    val hideInRecents by appLockPreferences.hideInRecents.collectAsStateWithLifecycle(initialValue = true)

    val promptTitle = stringResource(R.string.app_lock_prompt_title)
    val promptSubtitle = stringResource(R.string.app_lock_prompt_subtitle)

    var isAuthenticating by remember { mutableStateOf(false) }

    val credentialLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            isAuthenticating = false
            appLockPreferences.setSuppressNextBackgroundLock(false)
            if (result.resultCode == Activity.RESULT_OK) {
                appLockPreferences.unlock()
            }
        }

    fun triggerAuth(useBiometric: Boolean) {
        val currentActivity = activity ?: return
        if (isAuthenticating) return
        isAuthenticating = true
        NativeAuthHelper.authenticate(
            activity = currentActivity,
            title = promptTitle,
            subtitle = promptSubtitle,
            preferBiometric = useBiometric,
            launchCredentialIntent = { intent ->
                appLockPreferences.setSuppressNextBackgroundLock(true)
                credentialLauncher.launch(intent)
            },
            onSuccess = {
                isAuthenticating = false
                appLockPreferences.unlock()
            },
            onFailureOrCancel = {
                isAuthenticating = false
            },
        )
    }

    // Apply FLAG_SECURE when locked or when hideInRecents is active with App Lock enabled
    DisposableEffect(activity, isLocked, enabled, hideInRecents) {
        val window = activity?.window
        val secureRequired = isLocked || (enabled && hideInRecents)
        if (window != null) {
            if (secureRequired) {
                window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
        onDispose {}
    }

    // Automatically prompt when the lock overlay appears
    LaunchedEffect(isLocked) {
        if (isLocked) {
            triggerAuth(useBiometric = biometricPreferred)
        } else {
            isAuthenticating = false
        }
    }

    BackHandler(enabled = isLocked) {
        activity?.moveTaskToBack(true)
    }

    AnimatedVisibility(
        visible = isLocked,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        val colors = MaterialTheme.colorScheme
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    colors.surface,
                                    colors.surfaceContainerLowest,
                                ),
                        ),
                    ).pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent()
                            }
                        }
                    }.windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 24.dp, vertical = 32.dp)
                    .testTag("app_lock_overlay"),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Surface(
                    shape = CircleShape,
                    color = colors.primaryContainer.copy(alpha = 0.85f),
                    tonalElevation = 6.dp,
                    modifier = Modifier.size(96.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = stringResource(R.string.settings_app_lock_title),
                            tint = colors.onPrimaryContainer,
                            modifier = Modifier.size(44.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                YouTubeLogoIcon(modifier = Modifier.size(36.dp))

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.app_lock_overlay_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.app_lock_overlay_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(36.dp))

                Button(
                    onClick = { triggerAuth(useBiometric = biometricPreferred) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("app_lock_unlock_button"),
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Icon(
                        imageVector = if (biometricPreferred) Icons.Outlined.Fingerprint else Icons.Filled.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                    Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                    Text(
                        text = stringResource(R.string.app_lock_unlock_button),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                if (biometricPreferred) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { triggerAuth(useBiometric = false) },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("app_lock_screen_lock_button"),
                        shape = MaterialTheme.shapes.extraLarge,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Password,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                        Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                        Text(
                            text = stringResource(R.string.app_lock_use_screen_lock_button),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}
