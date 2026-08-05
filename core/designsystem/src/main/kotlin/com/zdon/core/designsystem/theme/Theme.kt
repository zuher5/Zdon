package com.zdon.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.zdon.core.model.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = ZdonColors.primaryLight,
    onPrimary = ZdonColors.onPrimaryLight,
    primaryContainer = ZdonColors.primaryContainerLight,
    onPrimaryContainer = ZdonColors.onPrimaryContainerLight,
    secondary = ZdonColors.secondaryLight,
    onSecondary = ZdonColors.onSecondaryLight,
    secondaryContainer = ZdonColors.secondaryContainerLight,
    onSecondaryContainer = ZdonColors.onSecondaryContainerLight,
    tertiary = ZdonColors.tertiaryLight,
    onTertiary = ZdonColors.onTertiaryLight,
    tertiaryContainer = ZdonColors.tertiaryContainerLight,
    onTertiaryContainer = ZdonColors.onTertiaryContainerLight,
    error = ZdonColors.errorLight,
    onError = ZdonColors.onErrorLight,
    errorContainer = ZdonColors.errorContainerLight,
    onErrorContainer = ZdonColors.onErrorContainerLight,
    background = ZdonColors.backgroundLight,
    onBackground = ZdonColors.onBackgroundLight,
    surface = ZdonColors.surfaceLight,
    onSurface = ZdonColors.onSurfaceLight,
    surfaceVariant = ZdonColors.surfaceVariantLight,
    onSurfaceVariant = ZdonColors.onSurfaceVariantLight,
    outline = ZdonColors.outlineLight,
    outlineVariant = ZdonColors.outlineVariantLight,
    inverseSurface = ZdonColors.inverseSurfaceLight,
    inverseOnSurface = ZdonColors.inverseOnSurfaceLight,
    inversePrimary = ZdonColors.inversePrimaryLight,
    surfaceTint = ZdonColors.surfaceTintLight,
    scrim = ZdonColors.scrimLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = ZdonColors.primaryDark,
    onPrimary = ZdonColors.onPrimaryDark,
    primaryContainer = ZdonColors.primaryContainerDark,
    onPrimaryContainer = ZdonColors.onPrimaryContainerDark,
    secondary = ZdonColors.secondaryDark,
    onSecondary = ZdonColors.onSecondaryDark,
    secondaryContainer = ZdonColors.secondaryContainerDark,
    onSecondaryContainer = ZdonColors.onSecondaryContainerDark,
    tertiary = ZdonColors.tertiaryDark,
    onTertiary = ZdonColors.onTertiaryDark,
    tertiaryContainer = ZdonColors.tertiaryContainerDark,
    onTertiaryContainer = ZdonColors.onTertiaryContainerDark,
    error = ZdonColors.errorDark,
    onError = ZdonColors.onErrorDark,
    errorContainer = ZdonColors.errorContainerDark,
    onErrorContainer = ZdonColors.onErrorContainerDark,
    background = ZdonColors.backgroundDark,
    onBackground = ZdonColors.onBackgroundDark,
    surface = ZdonColors.surfaceDark,
    onSurface = ZdonColors.onSurfaceDark,
    surfaceVariant = ZdonColors.surfaceVariantDark,
    onSurfaceVariant = ZdonColors.onSurfaceVariantDark,
    outline = ZdonColors.outlineDark,
    outlineVariant = ZdonColors.outlineVariantDark,
    inverseSurface = ZdonColors.inverseSurfaceDark,
    inverseOnSurface = ZdonColors.inverseOnSurfaceDark,
    inversePrimary = ZdonColors.inversePrimaryDark,
    surfaceTint = ZdonColors.surfaceTintDark,
    scrim = ZdonColors.scrimDark,
)

/**
 * Root theme.
 *
 * Dynamic color is applied on Android 12+ when the user opts in; older releases
 * fall back to the static palette. The theme takes an explicit [ThemeMode] so the
 * setting is honoured without reading preferences from inside the composable.
 */
@Composable
fun ZdonTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    useDynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val context = LocalContext.current

    val colorScheme = when {
        useDynamicColor && supportsDynamicColor && darkTheme -> dynamicDarkColorScheme(context)
        useDynamicColor && supportsDynamicColor -> dynamicLightColorScheme(context)
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ZdonTypography,
        shapes = ZdonShapes,
        content = content,
    )
}
