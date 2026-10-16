package com.rainbow.compose.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val CommonLightColorScheme = lightColorScheme(
    primary          = CommonPrimaryMain,
    onPrimary        = CommonBackgroundDefault,
    secondary        = CommonPrimarySecondary,
    background       = CommonBackgroundDefault,
    surface          = CommonBackgroundDefault,
    onBackground     = CommonTextPrimary,
    onSurface        = CommonTextPrimary,
    surfaceVariant   = CommonBackgroundSecondary,
    onSurfaceVariant = CommonTextSecondary,
    error            = CommonStatusError,
    outline          = CommonTextDisabled
)

private val CommonDarkColorScheme = darkColorScheme(
    primary          = CommonPrimaryMain,
    onPrimary        = CommonBackgroundDefault,
    secondary        = CommonPrimarySecondary,
    background       = CommonBackgroundDefaultDark,
    surface          = CommonBackgroundSecondaryDark,
    onBackground     = CommonTextPrimaryDark,
    onSurface        = CommonTextPrimaryDark,
    surfaceVariant   = CommonBackgroundTertiaryDark,
    onSurfaceVariant = CommonTextSecondaryDark,
    error            = CommonStatusError,
    outline          = CommonTextDisabledDark
)

/**
 * 项目统一 Compose 主题。
 *
 * @param darkTheme 是否深色模式，默认跟随系统 [isSystemInDarkTheme]。
 */
@Composable
fun CommonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CommonDarkColorScheme else CommonLightColorScheme
    val commonColors = if (darkTheme) DarkCommonColors else LightCommonColors

    CompositionLocalProvider(LocalCommonColors provides commonColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = CommonTypography,
            content     = content
        )
    }
}

val commonColors: CommonColors
    @Composable get() = LocalCommonColors.current

@Composable
fun isDarkTheme(): Boolean = commonColors.isDark
