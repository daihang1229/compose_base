package com.rainbow.compose.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val DramaLightColorScheme = lightColorScheme(
    primary          = DramaPrimaryMain,
    onPrimary        = DramaBackgroundDefault,
    secondary        = DramaPrimarySecondary,
    background       = DramaBackgroundDefault,
    surface          = DramaBackgroundDefault,
    onBackground     = DramaTextPrimary,
    onSurface        = DramaTextPrimary,
    surfaceVariant   = DramaBackgroundSecondary,
    onSurfaceVariant = DramaTextSecondary,
    error            = DramaStatusError,
    outline          = DramaTextDisabled
)

private val DramaDarkColorScheme = darkColorScheme(
    primary          = DramaPrimaryMain,
    onPrimary        = DramaBackgroundDefault,
    secondary        = DramaPrimarySecondary,
    background       = DramaBackgroundDefaultDark,
    surface          = DramaBackgroundSecondaryDark,
    onBackground     = DramaTextPrimaryDark,
    onSurface        = DramaTextPrimaryDark,
    surfaceVariant   = DramaBackgroundTertiaryDark,
    onSurfaceVariant = DramaTextSecondaryDark,
    error            = DramaStatusError,
    outline          = DramaTextDisabledDark
)

/**
 * 项目统一 Compose 主题。
 *
 * @param darkTheme 是否深色模式，默认跟随系统 [isSystemInDarkTheme]。
 */
@Composable
fun DramaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DramaDarkColorScheme else DramaLightColorScheme
    val dramaColors = if (darkTheme) DarkDramaColors else LightDramaColors

    CompositionLocalProvider(LocalDramaColors provides dramaColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = DramaTypography,
            content     = content
        )
    }
}

val dramaColors: DramaColors
    @Composable get() = LocalDramaColors.current

@Composable
fun isDarkTheme(): Boolean = dramaColors.isDark
