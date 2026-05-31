package com.mochi.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Holds the current theme preference and persists changes. */
class SettingsViewModel(private val store: SettingsStore) : ViewModel() {

    private val _themeMode = MutableStateFlow(store.themeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        store.setThemeMode(mode)
        _themeMode.value = mode
    }
}
