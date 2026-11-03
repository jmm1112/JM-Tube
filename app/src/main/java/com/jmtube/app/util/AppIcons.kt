package com.jmtube.app.util

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.jmtube.app.R

/** A launcher alias: its manifest suffix, the name the picker shows and the adaptive icon it installs. */
data class LauncherIcon(
    val suffix: String,
    @StringRes val nameRes: Int,
    @DrawableRes val iconRes: Int,
)

object AppIcons {
    /** Component name prefix shared by every alias (the application id namespace). */
    const val NAMESPACE = "com.jmtube.app"

    /** Dark mode default launcher alias. */
    const val DARK_SUFFIX = ".IconFlowRed"

    /** Light mode default launcher alias. */
    const val LIGHT_SUFFIX = ".IconFlowLight"

    /** Night Cookie default launcher alias. */
    const val NIGHT_COOKIE_SUFFIX = ".IconExpressiveCookie"

    /** The alias enabled by default in the manifest. Used as a safe fallback. */
    const val DEFAULT_SUFFIX = NIGHT_COOKIE_SUFFIX

    /**
     * Every launcher alias, in manifest declaration order.
     */
    val ALL: List<LauncherIcon> =
        listOf(
            LauncherIcon(NIGHT_COOKIE_SUFFIX, R.string.icon_name_expressive_cookie, R.mipmap.ic_launcher_expressive_cookie),
            LauncherIcon(DARK_SUFFIX, R.string.icon_name_flow_red, R.mipmap.ic_launcher),
            LauncherIcon(LIGHT_SUFFIX, R.string.icon_name_flow_light, R.mipmap.ic_launcher_flow_light),
            LauncherIcon(".IconFlowPlay", R.string.icon_name_flow_play, R.mipmap.ic_launcher_flow_play),
            LauncherIcon(".IconAmoled", R.string.icon_name_amoled, R.mipmap.ic_launcher_amoled),
            LauncherIcon(".IconMonochrome", R.string.icon_name_monochrome, R.mipmap.ic_launcher_monochrome),
            LauncherIcon(".IconGhost", R.string.icon_name_ghost, R.mipmap.ic_launcher_ghost),
            LauncherIcon(".IconDynamic", R.string.icon_name_dynamic, R.mipmap.ic_launcher_dynamic),
            LauncherIcon(".IconMaterialSky", R.string.icon_name_material_sky, R.mipmap.ic_launcher_material_sky),
            LauncherIcon(".IconMaterialMint", R.string.icon_name_material_mint, R.mipmap.ic_launcher_material_mint),
            LauncherIcon(".IconExpressiveSegmented", R.string.icon_name_expressive_segmented, R.mipmap.ic_launcher_expressive_segmented),
            LauncherIcon(".IconExpressivePlay", R.string.icon_name_expressive_play, R.mipmap.ic_launcher_expressive_play),
            LauncherIcon(".IconExpressiveSky", R.string.icon_name_expressive_sky, R.mipmap.ic_launcher_expressive_sky),
            LauncherIcon(".IconExpressiveMint", R.string.icon_name_expressive_mint, R.mipmap.ic_launcher_expressive_mint),
            LauncherIcon(".IconExpressiveOval", R.string.icon_name_expressive_oval, R.mipmap.ic_launcher_expressive_oval),
            LauncherIcon(".IconExpressivePill", R.string.icon_name_expressive_pill, R.mipmap.ic_launcher_expressive_pill),
        )

    val ALL_SUFFIXES: List<String> = ALL.map { it.suffix }

    fun isThemeAdaptive(suffix: String): Boolean = suffix == DARK_SUFFIX || suffix == LIGHT_SUFFIX

    fun smartSuffixFor(isDark: Boolean): String = if (isDark) DARK_SUFFIX else LIGHT_SUFFIX

    fun resolveSmartSuffix(
        suffix: String,
        isDark: Boolean,
    ): String = if (isThemeAdaptive(suffix)) smartSuffixFor(isDark) else suffix

    fun icon(suffix: String): LauncherIcon = ALL.firstOrNull { it.suffix == suffix } ?: ALL.first { it.suffix == DEFAULT_SUFFIX }

    @DrawableRes
    fun splashIconRes(suffix: String): Int =
        when (suffix) {
            NIGHT_COOKIE_SUFFIX -> R.drawable.ic_launcher_expressive_cookie_foreground
            ".IconExpressiveMint" -> R.drawable.ic_launcher_expressive_mint_foreground
            ".IconExpressiveOval" -> R.drawable.ic_launcher_expressive_oval_foreground
            ".IconExpressivePill" -> R.drawable.ic_launcher_expressive_pill_foreground
            ".IconExpressivePlay" -> R.drawable.ic_launcher_expressive_play_foreground
            ".IconExpressiveSky" -> R.drawable.ic_launcher_expressive_sky_foreground
            ".IconExpressiveSegmented" -> R.drawable.ic_launcher_expressive_segmented_foreground
            ".IconMaterialSky" -> R.drawable.ic_launcher_foreground
            ".IconMaterialMint" -> R.drawable.ic_launcher_foreground
            ".IconAmoled" -> R.drawable.splash_icon_amoled
            ".IconMonochrome" -> R.drawable.splash_icon_monochrome
            ".IconGhost" -> R.drawable.splash_icon_ghost
            ".IconFlowPlay" -> R.drawable.ic_fg_flow_play
            DARK_SUFFIX -> R.drawable.ic_splash_logo
            LIGHT_SUFFIX -> R.drawable.ic_splash_logo
            else -> icon(suffix).iconRes
        }
}
