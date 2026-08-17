package com.zdon.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Static palette used when dynamic color is unavailable (API 26-30) or disabled.
 *
 * Minimal design: neutral, near-monochrome surfaces carry all layout weight and a
 * single blue accent ([primaryLight]/[primaryDark]) is reserved for interactive
 * elements and active state. Secondary and tertiary roles are mapped to neutral
 * grays so no decorative color noise appears in lists, chips or icons.
 */
internal object ZdonColors {

    // Light scheme
    val primaryLight = Color(0xFF00639B)
    val onPrimaryLight = Color(0xFFFFFFFF)
    val primaryContainerLight = Color(0xFFCEE5FF)
    val onPrimaryContainerLight = Color(0xFF001D33)
    val secondaryLight = Color(0xFF4F5C6B)
    val onSecondaryLight = Color(0xFFFFFFFF)
    val secondaryContainerLight = Color(0xFFE7ECF3)
    val onSecondaryContainerLight = Color(0xFF0D1D2A)
    val tertiaryLight = Color(0xFF4F5C6B)
    val onTertiaryLight = Color(0xFFFFFFFF)
    val tertiaryContainerLight = Color(0xFFE7ECF3)
    val onTertiaryContainerLight = Color(0xFF0D1D2A)
    val errorLight = Color(0xFFBA1A1A)
    val onErrorLight = Color(0xFFFFFFFF)
    val errorContainerLight = Color(0xFFFFDAD6)
    val onErrorContainerLight = Color(0xFF410002)
    val backgroundLight = Color(0xFFFFFFFF)
    val onBackgroundLight = Color(0xFF1A1C1E)
    val surfaceLight = Color(0xFFFFFFFF)
    val onSurfaceLight = Color(0xFF1A1C1E)
    val surfaceVariantLight = Color(0xFFF1F2F5)
    val onSurfaceVariantLight = Color(0xFF42474E)
    val outlineLight = Color(0xFF71757D)
    val outlineVariantLight = Color(0xFFE1E1E6)
    val inverseSurfaceLight = Color(0xFF2F3033)
    val inverseOnSurfaceLight = Color(0xFFF0F0F4)
    val inversePrimaryLight = Color(0xFF9ECDFF)
    val surfaceTintLight = primaryLight
    val scrimLight = Color(0xFF000000)

    // Dark scheme
    val primaryDark = Color(0xFF9ECDFF)
    val onPrimaryDark = Color(0xFF003354)
    val primaryContainerDark = Color(0xFF004A76)
    val onPrimaryContainerDark = Color(0xFFCEE5FF)
    val secondaryDark = Color(0xFFBEC6CF)
    val onSecondaryDark = Color(0xFF28323C)
    val secondaryContainerDark = Color(0xFF3E4850)
    val onSecondaryContainerDark = Color(0xFFD7E3ED)
    val tertiaryDark = Color(0xFFBEC6CF)
    val onTertiaryDark = Color(0xFF28323C)
    val tertiaryContainerDark = Color(0xFF3E4850)
    val onTertiaryContainerDark = Color(0xFFD7E3ED)
    val errorDark = Color(0xFFFFB4AB)
    val onErrorDark = Color(0xFF690005)
    val errorContainerDark = Color(0xFF93000A)
    val onErrorContainerDark = Color(0xFFFFDAD6)
    val backgroundDark = Color(0xFF16161A)
    val onBackgroundDark = Color(0xFFE4E2E8)
    val surfaceDark = Color(0xFF16161A)
    val onSurfaceDark = Color(0xFFE4E2E8)
    val surfaceVariantDark = Color(0xFF2A2B30)
    val onSurfaceVariantDark = Color(0xFFC7C7D0)
    val outlineDark = Color(0xFF909199)
    val outlineVariantDark = Color(0xFF34353B)
    val inverseSurfaceDark = Color(0xFFE4E2E8)
    val inverseOnSurfaceDark = Color(0xFF2F3033)
    val inversePrimaryDark = Color(0xFF00639B)
    val surfaceTintDark = primaryDark
    val scrimDark = Color(0xFF000000)
}