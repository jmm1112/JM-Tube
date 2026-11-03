package com.jmtube.app.ui.startup

import android.animation.ObjectAnimator
import android.app.Activity
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen

private const val MAX_HOLD_MS = 1_500L

/**
 * The platform splash, held only until Compose draws the first frame of [YouTubeStartupAnimation],
 * so cold start transitions seamlessly from the native window into the full YouTube opening
 * animation (Red Pill -> Expanding Timeline/Seek Bar -> YouTube Logo Lockup -> Main App).
 */
class SplashController(
    private val activity: Activity,
) {
    @Volatile
    var contentReady: Boolean = false

    private val startedAt = SystemClock.uptimeMillis()

    fun install(splash: SplashScreen) {
        splash.setKeepOnScreenCondition { !contentReady && SystemClock.uptimeMillis() - startedAt < MAX_HOLD_MS }

        splash.setOnExitAnimationListener { splashScreenViewProvider ->
            val splashView = splashScreenViewProvider.view
            ObjectAnimator.ofFloat(splashView, View.ALPHA, 1.0f, 0.0f).apply {
                duration = 120L
                interpolator = LinearInterpolator()
                doOnEnd {
                    splashScreenViewProvider.remove()
                }
                start()
            }
        }
    }

    /** Opens the next launch on [tone] with the art of [iconSuffix]; Android 13 and later only. */
    fun rememberTheme(
        tone: SplashTone,
        iconSuffix: String,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.splashScreen.setSplashScreenTheme(splashThemeFor(iconSuffix, tone))
        }
    }
}
