package com.zdon.feature.settings

import com.zdon.core.model.EngineStatus
import com.zdon.core.model.UserPreferences

/** State of the settings screen. */
data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val engineStatus: EngineStatus = EngineStatus.Unknown,
    val isUpdatingBinary: Boolean = false,
    val notificationsBlockedBySystem: Boolean = false,
)

/** One-shot events surfaced as snackbars. */
sealed interface SettingsEvent {
    data class ShowMessage(val messageRes: Int) : SettingsEvent
    data class ShowRawMessage(val message: String) : SettingsEvent
}
