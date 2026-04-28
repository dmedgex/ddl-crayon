package com.trickcal.crayon.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.trickcal.crayon.model.CharacterProfile
import com.trickcal.crayon.repository.CrayonRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class CharacterDetailUiState(
    val character: CharacterProfile? = null,
    val litSlots: Set<String> = emptySet(),
)

class CharacterDetailViewModel(
    private val repository: CrayonRepository,
    private val characterId: String,
) : ViewModel() {
    val uiState: StateFlow<CharacterDetailUiState> =
        combine(
            repository.observeCharacter(characterId),
            repository.observeLitSlots(),
        ) { character, litSlots ->
            CharacterDetailUiState(
                character = character,
                litSlots = litSlots,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CharacterDetailUiState(),
        )

    companion object {
        fun factory(
            repository: CrayonRepository,
            characterId: String,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(CharacterDetailViewModel::class.java)) {
                        "Unsupported ViewModel class: ${modelClass.name}"
                    }
                    return CharacterDetailViewModel(
                        repository = repository,
                        characterId = characterId,
                    ) as T
                }
            }
    }
}
