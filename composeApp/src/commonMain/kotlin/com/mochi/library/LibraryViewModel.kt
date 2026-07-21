package com.mochi.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Exposes the Library grid as a self-refreshing StateFlow. */
class LibraryViewModel(store: LibraryStore) : ViewModel() {
    val units: StateFlow<List<UnitSummary>> =
        store.units().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptyList(),
        )
}
