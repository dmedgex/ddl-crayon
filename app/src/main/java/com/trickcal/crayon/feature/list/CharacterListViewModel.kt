package com.trickcal.crayon.feature.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trickcal.crayon.domain.filter.CharacterFilterMatcher
import com.trickcal.crayon.model.AttributeType
import com.trickcal.crayon.model.BoardTier
import com.trickcal.crayon.model.BrowseMode
import com.trickcal.crayon.model.CharacterDisplayMode
import com.trickcal.crayon.model.CharacterFilter
import com.trickcal.crayon.model.CharacterListPreferences
import com.trickcal.crayon.model.CharacterProfile
import com.trickcal.crayon.model.PersonalityType
import com.trickcal.crayon.repository.CrayonRepository
import com.trickcal.crayon.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LayerSummaryUiModel(
    val tier: BoardTier,
    val slots: List<LayerSlotUiModel>,
)

data class LayerSlotUiModel(
    val slotId: String,
    val attributeType: AttributeType,
    val isLit: Boolean,
)

data class CharacterCardUiModel(
    val id: String,
    val name: String,
    val avatarKey: String,
    val litCount: Int,
    val totalCount: Int,
    val layerSummaries: List<LayerSummaryUiModel>,
    val isCompactDimmed: Boolean,
)

data class CharacterListUiState(
    val filter: CharacterFilter = CharacterFilter(),
    val browseMode: BrowseMode = BrowseMode.DETAIL,
    val displayMode: CharacterDisplayMode = CharacterDisplayMode.DETAIL,
    val characters: List<CharacterCardUiModel> = emptyList(),
    val selectedCharacter: CharacterProfile? = null,
    val litSlots: Set<String> = emptySet(),
    val batchPaintDialog: BatchPaintDialogUiState = BatchPaintDialogUiState(),
)

class CharacterListViewModel(
    private val repository: CrayonRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val filterState = MutableStateFlow(CharacterFilter())
    private val browseModeState = MutableStateFlow(BrowseMode.DETAIL)
    private val displayModeState = MutableStateFlow(CharacterDisplayMode.DETAIL)
    private val sortingLitSlotsState = MutableStateFlow<Set<String>?>(null)
    private val selectedCharacterId = MutableStateFlow<String?>(null)
    private val batchPaintDialogVisible = MutableStateFlow(false)
    private val batchPaintSelectionState = MutableStateFlow(BatchPaintSelection())
    private var latestLitSlots: Set<String> = emptySet()
    private var hasLoadedLitSlots = false
    private val batchDialogState =
        combine(batchPaintDialogVisible, batchPaintSelectionState) { isVisible, selection ->
            BatchDialogState(
                isVisible = isVisible,
                selection = selection,
            )
        }
    private val repositoryListState =
        combine(
            repository.observeCharacters(),
            repository.observeLitSlots(),
            filterState,
            browseModeState,
            selectedCharacterId,
        ) { characters, litSlots, filter, browseMode, selectedId ->
            latestLitSlots = litSlots
            if (!hasLoadedLitSlots || sortingLitSlotsState.value == null) {
                sortingLitSlotsState.value = litSlots
                hasLoadedLitSlots = true
            }
            BaseListState(
                characters = characters,
                litSlots = litSlots,
                filter = filter,
                browseMode = browseMode,
                displayMode = CharacterDisplayMode.DETAIL,
                sortingLitSlots = null,
                selectedCharacterId = selectedId,
            )
        }
    private val baseListState =
        combine(repositoryListState, displayModeState, sortingLitSlotsState) { baseState, displayMode, sortingLitSlots ->
            baseState.copy(
                displayMode = displayMode,
                sortingLitSlots = sortingLitSlots,
            )
        }

    init {
        viewModelScope.launch {
            settingsRepository.observeCharacterListPreferences().collect { preferences ->
                applyCharacterListPreferences(preferences)
            }
        }
    }

    val uiState: StateFlow<CharacterListUiState> =
        combine(
            baseListState,
            batchDialogState,
        ) { baseState, batchState ->
            val filteredCharacters = baseState.characters.filter { character ->
                CharacterFilterMatcher.matches(character, baseState.filter)
            }
            val cardItems = filteredCharacters.map { character ->
                character.toCardUi(
                    litSlots = baseState.litSlots,
                    filter = baseState.filter,
                )
            }.sortedForDisplay(
                displayMode = baseState.displayMode,
                sortDimmedIds = filteredCharacters
                    .filter { character ->
                        character.shouldDimCompactCard(
                            filter = baseState.filter,
                            litSlots = baseState.sortingLitSlots ?: baseState.litSlots,
                        )
                    }
                    .mapTo(linkedSetOf()) { character -> character.id },
            )
            val batchPaintPreview = BatchPaintPlanner.buildPreview(
                characters = baseState.characters,
                litSlots = baseState.litSlots,
                selection = batchState.selection,
            )
            CharacterListUiState(
                filter = baseState.filter,
                browseMode = baseState.browseMode,
                displayMode = baseState.displayMode,
                characters = cardItems,
                selectedCharacter = baseState.characters.firstOrNull { it.id == baseState.selectedCharacterId },
                litSlots = baseState.litSlots,
                batchPaintDialog = BatchPaintDialogUiState(
                    isVisible = batchState.isVisible,
                    selection = batchState.selection,
                    preview = batchPaintPreview,
                ),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CharacterListUiState(),
        )

    fun toggleTier(tier: BoardTier) {
        updateCharacterListPreferences { preferences ->
            preferences.copy(
                filter = preferences.filter.copy(
                    selectedTiers = preferences.filter.selectedTiers.toggleSingleItem(tier),
                ),
            )
        }
    }

    fun toggleAttribute(attributeType: AttributeType) {
        updateCharacterListPreferences { preferences ->
            preferences.copy(
                filter = preferences.filter.copy(
                    selectedAttributes = preferences.filter.selectedAttributes.toggleItem(attributeType),
                ),
            )
        }
    }

    fun setNameQuery(query: String) {
        updateCharacterListPreferences { preferences ->
            preferences.copy(
                filter = preferences.filter.copy(nameQuery = query),
            )
        }
    }

    fun togglePersonality(personality: PersonalityType) {
        updateCharacterListPreferences { preferences ->
            preferences.copy(
                filter = preferences.filter.copy(
                    selectedPersonality = if (preferences.filter.selectedPersonality == personality) {
                        null
                    } else {
                        personality
                    },
                ),
            )
        }
    }

    fun clearFilters() {
        updateCharacterListPreferences { preferences ->
            preferences.copy(
                filter = CharacterFilter(matchMode = preferences.filter.matchMode),
            )
        }
    }

    fun setBrowseMode(mode: BrowseMode) {
        updateCharacterListPreferences { preferences ->
            preferences.copy(browseMode = mode)
        }
        if (mode == BrowseMode.DETAIL) {
            dismissPaintSheet()
        }
    }

    fun toggleDisplayMode() {
        updateCharacterListPreferences { preferences ->
            preferences.copy(
                displayMode = if (preferences.displayMode == CharacterDisplayMode.DETAIL) {
                    CharacterDisplayMode.COMPACT
                } else {
                    CharacterDisplayMode.DETAIL
                },
            )
        }
    }

    fun refreshCardSort() {
        if (hasLoadedLitSlots) {
            sortingLitSlotsState.value = latestLitSlots
        }
    }

    fun openPaintSheet(characterId: String) {
        batchPaintDialogVisible.value = false
        selectedCharacterId.value = characterId
    }

    fun dismissPaintSheet() {
        selectedCharacterId.value = null
    }

    fun toggleSlot(slotId: String) {
        viewModelScope.launch {
            repository.toggleSlot(slotId)
        }
    }

    fun quickPaintCharacter(characterId: String) {
        val character = repository.getCharacter(characterId) ?: return
        val quickPaintTarget = character.resolveQuickPaintTarget(
            filter = filterState.value,
            litSlots = uiState.value.litSlots,
        ) ?: return

        viewModelScope.launch {
            repository.setSlotsLit(
                slotIds = quickPaintTarget.slotIds,
                isLit = quickPaintTarget.shouldUnlock,
            )
        }
    }

    fun openBatchPaintDialog() {
        dismissPaintSheet()
        batchPaintDialogVisible.value = true
    }

    fun dismissBatchPaintDialog() {
        batchPaintDialogVisible.value = false
        batchPaintSelectionState.value = BatchPaintSelection()
    }

    fun toggleBatchTier(tier: BoardTier) {
        batchPaintSelectionState.update { selection ->
            selection.copy(selectedTiers = selection.selectedTiers.toggleSingleItem(tier))
        }
    }

    fun toggleBatchAttribute(attributeType: AttributeType) {
        batchPaintSelectionState.update { selection ->
            selection.copy(selectedAttributes = selection.selectedAttributes.toggleItem(attributeType))
        }
    }

    fun setBatchPaintAction(action: BatchPaintAction) {
        batchPaintSelectionState.update { selection ->
            selection.copy(action = action)
        }
    }

    fun applyBatchPaint() {
        val preview = uiState.value.batchPaintDialog.preview
        val action = uiState.value.batchPaintDialog.selection.action
        if (preview.slotIds.isEmpty()) {
            return
        }
        viewModelScope.launch {
            repository.setSlotsLit(
                slotIds = preview.slotIds,
                isLit = action == BatchPaintAction.UNLOCK,
            )
            dismissBatchPaintDialog()
        }
    }

    private fun updateCharacterListPreferences(
        transform: (CharacterListPreferences) -> CharacterListPreferences,
    ) {
        val updated = transform(currentCharacterListPreferences())
        applyCharacterListPreferences(updated)
        refreshCardSort()
        viewModelScope.launch {
            settingsRepository.setCharacterListPreferences(updated)
        }
    }

    private fun currentCharacterListPreferences(): CharacterListPreferences =
        CharacterListPreferences(
            filter = filterState.value,
            browseMode = browseModeState.value,
            displayMode = displayModeState.value,
        )

    private fun applyCharacterListPreferences(preferences: CharacterListPreferences) {
        if (filterState.value != preferences.filter) {
            filterState.value = preferences.filter
        }
        if (browseModeState.value != preferences.browseMode) {
            browseModeState.value = preferences.browseMode
        }
        if (displayModeState.value != preferences.displayMode) {
            displayModeState.value = preferences.displayMode
        }
    }
}

internal fun CharacterProfile.toCardUi(
    litSlots: Set<String>,
    filter: CharacterFilter = CharacterFilter(),
): CharacterCardUiModel =
    CharacterCardUiModel(
        id = id,
        name = name,
        avatarKey = avatarKey,
        litCount = litSlotCount(litSlots),
        totalCount = allAttributeSlots().size,
        layerSummaries = layers.map { layer ->
            LayerSummaryUiModel(
                tier = layer.tier,
                slots = layer.attributeSlots.map { slot ->
                    LayerSlotUiModel(
                        slotId = slot.id,
                        attributeType = slot.attributeType,
                        isLit = slot.id in litSlots,
                    )
                },
            )
        },
        isCompactDimmed = shouldDimCompactCard(filter = filter, litSlots = litSlots),
    )

internal fun List<CharacterCardUiModel>.sortedForDisplay(
    displayMode: CharacterDisplayMode,
    sortDimmedIds: Set<String>? = null,
): List<CharacterCardUiModel> =
    if (displayMode == CharacterDisplayMode.COMPACT) {
        val (dimmed, normal) = partition { item ->
            sortDimmedIds?.contains(item.id) ?: item.isCompactDimmed
        }
        dimmed + normal
    } else {
        this
    }

private fun <T> Set<T>.toggleItem(item: T): Set<T> =
    if (item in this) this - item else this + item

private fun <T> Set<T>.toggleSingleItem(item: T): Set<T> =
    if (item in this) {
        emptySet()
    } else {
        setOf(item)
    }

internal fun CharacterProfile.shouldDimCompactCard(
    filter: CharacterFilter,
    litSlots: Set<String>,
): Boolean = resolveQuickPaintTarget(filter = filter, litSlots = litSlots)?.shouldUnlock == true

internal data class QuickPaintTarget(
    val slotIds: Set<String>,
    val shouldUnlock: Boolean,
)

private data class SingleTierAttributeSelection(
    val tier: BoardTier,
    val attributeType: AttributeType,
)

private fun CharacterFilter.singleTierAttributeSelectionOrNull(): SingleTierAttributeSelection? {
    val tier = selectedTiers.singleOrNull() ?: return null
    val attributeType = selectedAttributes.singleOrNull() ?: return null
    return SingleTierAttributeSelection(
        tier = tier,
        attributeType = attributeType,
    )
}

internal fun CharacterProfile.resolveQuickPaintTarget(
    filter: CharacterFilter,
    litSlots: Set<String>,
): QuickPaintTarget? {
    val selection = filter.singleTierAttributeSelectionOrNull() ?: return null
    val slotIds = layerForTier(selection.tier)
        ?.attributeSlots
        ?.filter { slot -> slot.attributeType == selection.attributeType }
        ?.mapTo(linkedSetOf()) { slot -> slot.id }
        .orEmpty()
    if (slotIds.isEmpty()) {
        return null
    }
    return QuickPaintTarget(
        slotIds = slotIds,
        shouldUnlock = slotIds.none { slotId -> slotId in litSlots },
    )
}

private data class BaseListState(
    val characters: List<CharacterProfile>,
    val litSlots: Set<String>,
    val filter: CharacterFilter,
    val browseMode: BrowseMode,
    val displayMode: CharacterDisplayMode,
    val sortingLitSlots: Set<String>?,
    val selectedCharacterId: String?,
)

private data class BatchDialogState(
    val isVisible: Boolean,
    val selection: BatchPaintSelection,
)
