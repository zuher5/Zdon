package com.zdon.core.model

/** Light/dark selection. [SYSTEM] follows the platform setting. */
enum class ThemeMode(val label: String) {
    SYSTEM("System default"),
    LIGHT("Light"),
    DARK("Dark"),
    ;

    companion object {
        fun fromNameOrDefault(name: String?, fallback: ThemeMode = SYSTEM): ThemeMode =
            entries.firstOrNull { it.name == name } ?: fallback
    }
}
