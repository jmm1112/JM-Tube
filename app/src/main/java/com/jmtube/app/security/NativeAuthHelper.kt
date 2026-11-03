package com.jmtube.app.security

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Invokes Android's native BiometricPrompt (API 28+) and KeyguardManager device credential
 * confirmation so the user can unlock JM Tube with Fingerprint, Face Unlock, PIN, Pattern, or Password.
 */
object NativeAuthHelper {
    /** Returns true if the device has a PIN, pattern, password, or biometric lock set up. */
    fun isDeviceSecure(context: Context): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return keyguardManager?.isDeviceSecure == true
    }

    /** Returns true if the device has biometric hardware and enrolled biometrics. */
    fun isBiometricAvailable(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val biometricManager = context.getSystemService(BiometricManager::class.java)
            if (biometricManager != null) {
                val result =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        biometricManager.canAuthenticate(
                            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                BiometricManager.Authenticators.BIOMETRIC_WEAK,
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        biometricManager.canAuthenticate()
                    }
                return result == BiometricManager.BIOMETRIC_SUCCESS
            }
        }
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
    }

    /** Creates an Intent to open Android's system security / screen lock settings. */
    fun securitySettingsIntent(): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(
                    Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                )
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }

    /**
     * Triggers Android's native authentication flow.
     *
     * @param activity Foreground Activity hosting the prompt.
     * @param title Dialog title shown in the system prompt.
     * @param subtitle Dialog subtitle shown in the system prompt.
     * @param preferBiometric Whether to offer biometric unlock first (with screen lock fallback),
     *   or go straight to Android's screen lock (PIN / Pattern / Password).
     * @param launchCredentialIntent Launcher for KeyguardManager confirm device credential fallback.
     * @param onSuccess Invoked on the main thread when the user authenticates.
     * @param onFailureOrCancel Invoked on the main thread when cancelled or failed.
     */
    fun authenticate(
        activity: Activity,
        title: String,
        subtitle: String,
        preferBiometric: Boolean,
        launchCredentialIntent: (Intent) -> Unit,
        onSuccess: () -> Unit,
        onFailureOrCancel: () -> Unit = {},
    ) {
        if (!isDeviceSecure(activity)) {
            onSuccess()
            return
        }

        // If user specifically requested PIN/Pattern/Password only or device is below API 28,
        // or on API 28-29 when biometrics aren't preferred, use KeyguardManager or DEVICE_CREDENTIAL.
        if (!preferBiometric) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                authenticateWithBiometricPromptApi30(
                    activity = activity,
                    title = title,
                    subtitle = subtitle,
                    authenticators = BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                    fallbackToKeyguard = {
                        launchKeyguardCredential(activity, title, subtitle, launchCredentialIntent, onFailureOrCancel)
                    },
                    onSuccess = onSuccess,
                    onFailureOrCancel = onFailureOrCancel,
                )
            } else {
                launchKeyguardCredential(activity, title, subtitle, launchCredentialIntent, onFailureOrCancel)
            }
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            authenticateWithBiometricPromptApi30(
                activity = activity,
                title = title,
                subtitle = subtitle,
                authenticators =
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                fallbackToKeyguard = {
                    launchKeyguardCredential(activity, title, subtitle, launchCredentialIntent, onFailureOrCancel)
                },
                onSuccess = onSuccess,
                onFailureOrCancel = onFailureOrCancel,
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            authenticateWithBiometricPromptApi29(
                activity = activity,
                title = title,
                subtitle = subtitle,
                fallbackToKeyguard = {
                    launchKeyguardCredential(activity, title, subtitle, launchCredentialIntent, onFailureOrCancel)
                },
                onSuccess = onSuccess,
                onFailureOrCancel = onFailureOrCancel,
            )
        } else {
            launchKeyguardCredential(activity, title, subtitle, launchCredentialIntent, onFailureOrCancel)
        }
    }

    @Suppress("NewApi")
    private fun authenticateWithBiometricPromptApi30(
        activity: Activity,
        title: String,
        subtitle: String,
        authenticators: Int,
        fallbackToKeyguard: () -> Unit,
        onSuccess: () -> Unit,
        onFailureOrCancel: () -> Unit,
    ) {
        try {
            val prompt =
                BiometricPrompt
                    .Builder(activity)
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setAllowedAuthenticators(authenticators)
                    .build()

            val cancellationSignal = CancellationSignal()
            val executor = ContextCompat.getMainExecutor(activity)
            prompt.authenticate(
                cancellationSignal,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        onSuccess()
                    }

                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence?,
                    ) {
                        if (
                            errorCode == BiometricPrompt.BIOMETRIC_ERROR_NO_BIOMETRICS ||
                            errorCode == BiometricPrompt.BIOMETRIC_ERROR_HW_NOT_PRESENT ||
                            errorCode == BiometricPrompt.BIOMETRIC_ERROR_HW_UNAVAILABLE
                        ) {
                            fallbackToKeyguard()
                        } else {
                            onFailureOrCancel()
                        }
                    }
                },
            )
        } catch (_: Throwable) {
            fallbackToKeyguard()
        }
    }

    @Suppress("NewApi", "DEPRECATION")
    private fun authenticateWithBiometricPromptApi29(
        activity: Activity,
        title: String,
        subtitle: String,
        fallbackToKeyguard: () -> Unit,
        onSuccess: () -> Unit,
        onFailureOrCancel: () -> Unit,
    ) {
        try {
            val prompt =
                BiometricPrompt
                    .Builder(activity)
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setDeviceCredentialAllowed(true)
                    .build()

            val cancellationSignal = CancellationSignal()
            val executor = ContextCompat.getMainExecutor(activity)
            prompt.authenticate(
                cancellationSignal,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        onSuccess()
                    }

                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence?,
                    ) {
                        if (
                            errorCode == BiometricPrompt.BIOMETRIC_ERROR_NO_BIOMETRICS ||
                            errorCode == BiometricPrompt.BIOMETRIC_ERROR_HW_NOT_PRESENT ||
                            errorCode == BiometricPrompt.BIOMETRIC_ERROR_HW_UNAVAILABLE
                        ) {
                            fallbackToKeyguard()
                        } else {
                            onFailureOrCancel()
                        }
                    }
                },
            )
        } catch (_: Throwable) {
            fallbackToKeyguard()
        }
    }

    @Suppress("DEPRECATION")
    private fun launchKeyguardCredential(
        activity: Activity,
        title: String,
        subtitle: String,
        launchCredentialIntent: (Intent) -> Unit,
        onFailureOrCancel: () -> Unit,
    ) {
        val keyguardManager = activity.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val intent = keyguardManager?.createConfirmDeviceCredentialIntent(title, subtitle)
        if (intent != null) {
            runCatching { launchCredentialIntent(intent) }
                .onFailure { onFailureOrCancel() }
        } else {
            onFailureOrCancel()
        }
    }
}
