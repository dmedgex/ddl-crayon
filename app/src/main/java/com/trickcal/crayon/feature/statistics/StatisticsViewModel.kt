package com.trickcal.crayon.feature.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trickcal.crayon.model.StatisticsSnapshot
import com.trickcal.crayon.repository.CrayonRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class StatisticsUiState(
    val snapshot: StatisticsSnapshot = StatisticsSnapshot.empty(),
)

class StatisticsViewModel(
    repository: CrayonRepository,
) : ViewModel() {
    val uiState: StateFlow<StatisticsUiState> =
        repository.observeStatistics()
            .map { snapshot -> StatisticsUiState(snapshot = snapshot) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = StatisticsUiState(),
            )
}
