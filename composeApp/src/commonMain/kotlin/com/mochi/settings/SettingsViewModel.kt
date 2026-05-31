package com.mochi.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Holds the user preferences (theme + daily new-card limit) and persists changes. */
class SettingsViewModel(private val store: SettingsStore) : ViewModel() {

    private val _themeMode = MutableStateFlow(store.themeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _newCardLimit = MutableStateFlow(store.newCardLimit())
    val newCardLimit: StateFlow<Int> = _newCardLimit.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        store.setThemeMode(mode)
        _themeMode.value = mode
    }

    fun setNewCardLimit(limit: Int) {
        store.setNewCardLimit(limit)
        _newCardLimit.value = limit
    }
}
