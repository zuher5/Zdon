package com.zdon.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Static palette used when dynamic color is unavailable (API 26-30) or disabled.
 * Values are generated from the Zdon brand hue and satisfy the Material 3
 * contrast requirements for their respective roles.
 */
internal object ZdonColors {

    // Light scheme
    val primaryLight = Color(0xFF00639B)
    val onPrimaryLight = Color(0xFFFFFFFF)
    val primaryContainerLight = Color(0xFFCEE5FF)
    val onPrimaryContainerLight = Color(0xFF001D33)
    val secondaryLight = Color(0xFF51606F)
    val onSecondaryLight = Color(0xFFFFFFFF)
    val secondaryContainerLight = Color(0xFFD4E4F6)
    val onSecondaryContainerLight = Color(0xFF0D1D2A)
    val tertiaryLight = Color(0xFF67587A)
    val onTertiaryLight = Color(0xFFFFFFFF)
    val tertiaryContainerLight = Color(0xFFEDDCFF)
    val onTertiaryContainerLight = Color(0xFF221533)
    val errorLight = Color(0xFFBA1A1A)
    val onErrorLight = Color(0xFFFFFFFF)
    val errorContainerLight = Color(0xFFFFDAD6)
    val onErrorContainerLight = Color(0xFF410002)
    val backgroundLight = Color(0xFFFCFCFF)
    val onBackgroundLight = Color(0xFF1A1C1E)
    val surfaceLight = Color(0xFFFCFCFF)
    val onSurfaceLight = Color(0xFF1A1C1E)
    val surfaceVariantLight = Color(0xFFDEE3EB)
    val onSurfaceVariantLight = Color(0xFF42474E)
    val outlineLight = Color(0xFF72777F)
    val outlineVariantLight = Color(0xFFC2C7CF)
    val inverseSurfaceLight = Color(0xFF2F3033)
    val inverseOnSurfaceLight = Color(0xFFF0F0F4)
    val inversePrimaryLight = Color(0xFF97CBFF)
    val surfaceTintLight = primaryLight
    val scrimLight = Color(0xFF000000)

    // Dark scheme
    val primaryDark = Color(0xFF97CBFF)
    val onPrimaryDark = Color(0xFF003354)
    val primaryContainerDark = Color(0xFF004A76)
    val onPrimaryContainerDark = Color(0xFFCEE5FF)
    val secondaryDark = Color(0xFFB8C8DA)
    val onSecondaryDark = Color(0xFF233240)
    val secondaryContainerDark = Color(0xFF394857)
    val onSecondaryContainerDark = Color(0xFFD4E4F6)
    val tertiaryDark = Color(0xFFD2BFE7)
    val onTertiaryDark = Color(0xFF382A4A)
    val tertiaryContainerDark = Color(0xFF4F4061)
    val onTertiaryContainerDark = Color(0xFFEDDCFF)
    val errorDark = Color(0xFFFFB4AB)
    val onErrorDark = Color(0xFF690005)
    val errorContainerDark = Color(0xFF93000A)
    val onErrorContainerDark = Color(0xFFFFDAD6)
    val backgroundDark = Color(0xFF1A1C1E)
    val onBackgroundDark = Color(0xFFE2E2E6)
    val surfaceDark = Color(0xFF1A1C1E)
    val onSurfaceDark = Color(0xFFE2E2E6)
    val surfaceVariantDark = Color(0xFF42474E)
    val onSurfaceVariantDark = Color(0xFFC2C7CF)
    val outlineDark = Color(0xFF8C9199)
    val outlineVariantDark = Color(0xFF42474E)
    val inverseSurfaceDark = Color(0xFFE2E2E6)
    val inverseOnSurfaceDark = Color(0xFF2F3033)
    val inversePrimaryDark = Color(0xFF00639B)
    val surfaceTintDark = primaryDark
    val scrimDark = Color(0xFF000000)
}
