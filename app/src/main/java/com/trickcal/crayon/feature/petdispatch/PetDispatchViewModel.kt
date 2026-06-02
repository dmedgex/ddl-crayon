package com.trickcal.crayon.feature.petdispatch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trickcal.crayon.domain.petdispatch.PetDispatchSolver
import com.trickcal.crayon.model.PetDispatchPet
import com.trickcal.crayon.model.PetDispatchRegion
import com.trickcal.crayon.model.PetDispatchResult
import com.trickcal.crayon.model.PetDispatchSelectionState
import com.trickcal.crayon.model.PetDispatchSelectionTab
import com.trickcal.crayon.repository.PetDispatchRepository
import com.trickcal.crayon.repository.PetDispatchSelectionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PetDispatchUiState(
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val pets: List<PetDispatchPet> = emptyList(),
    val regions: List<PetDispatchRegion> = emptyList(),
    val selectedRegionName: String = "",
    val selectedTaskCount: Int = 1,
    val selectedOwnedPetIds: Set<Int> = emptySet(),
    val selectedFarmPetIds: Set<Int> = emptySet(),
    val selectedTab: PetDispatchSelectionTab = PetDispatchSelectionTab.OWNED,
    val isCalculating: Boolean = false,
    val result: PetDispatchResult? = null,
) {
    val selectedRegion: PetDispatchRegion?
        get() = regions.firstOrNull { it.name == selectedRegionName }
}

class PetDispatchViewModel(
    private val petDispatchRepository: PetDispatchRepository,
    private val selectionRepository: PetDispatchSelectionRepository,
) : ViewModel() {
    private val catalogState = MutableStateFlow(PetDispatchCatalogState())
    private val isCalculatingState = MutableStateFlow(false)
    private val resultState = MutableStateFlow<PetDispatchResult?>(null)
    private val selectionState = MutableStateFlow(PetDispatchSelectionState())
    private var hasLoadedPersistedSelection = false

    val uiState: StateFlow<PetDispatchUiState> =
        combine(
            catalogState,
            selectionState,
            isCalculatingState,
            resultState,
        ) { catalogState, selection, isCalculating, result ->
            val catalog = catalogState.catalog
            val pets = catalog?.pets.orEmpty()
            val regions = catalog?.regions.orEmpty()
            val sanitizedSelection = PetDispatchSelectionStateSanitizer.sanitize(
                regions = regions,
                pets = pets,
                selection = selection,
            )

            PetDispatchUiState(
                isLoading = catalog == null && catalogState.errorMessage == null,
                loadError = catalogState.errorMessage,
                pets = pets,
                regions = regions,
                selectedRegionName = sanitizedSelection.selectedRegionName.orEmpty(),
                selectedTaskCount = sanitizedSelection.selectedTaskCount,
                selectedOwnedPetIds = sanitizedSelection.selectedOwnedPetIds,
                selectedFarmPetIds = sanitizedSelection.selectedFarmPetIds,
                selectedTab = sanitizedSelection.selectedTab,
                isCalculating = isCalculating,
                result = result,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PetDispatchUiState(),
        )

    init {
        observePersistedSelection()
        loadCatalog()
    }

    fun retryLoad() {
        loadCatalog()
    }

    fun setSelectedRegion(regionName: String) {
        persistSelection(
            selectionState.value.copy(
                selectedRegionName = regionName,
            ),
        )
    }

    fun setSelectedTaskCount(taskCount: Int) {
        persistSelection(
            selectionState.value.copy(
                selectedTaskCount = taskCount,
            ),
        )
    }

    fun toggleOwnedPet(petId: Int) {
        val current = selectionState.value
        val nextIds =
            if (petId in current.selectedOwnedPetIds) {
                current.selectedOwnedPetIds - petId
            } else {
                current.selectedOwnedPetIds + petId
            }
        persistSelection(current.copy(selectedOwnedPetIds = nextIds))
    }

    fun toggleFarmPet(petId: Int) {
        val current = selectionState.value
        val nextIds =
            if (petId in current.selectedFarmPetIds) {
                current.selectedFarmPetIds - petId
            } else {
                current.selectedFarmPetIds + petId
            }
        persistSelection(current.copy(selectedFarmPetIds = nextIds))
    }

    fun setSelectedTab(tab: PetDispatchSelectionTab) {
        val current = selectionState.value
        if (current.selectedTab == tab) {
            return
        }
        persistSelection(current.copy(selectedTab = tab))
    }

    fun dismissResult() {
        resultState.value = null
    }

    fun calculate() {
        val state = uiState.value
        val region = state.selectedRegion
        if (region == null) {
            resultState.value = PetDispatchResult(
                isSuccess = false,
                errorMessage = "请选择派遣区域。",
                textReport = "请选择派遣区域。",
            )
            return
        }
        if (state.selectedOwnedPetIds.isEmpty()) {
            resultState.value = PetDispatchResult(
                isSuccess = false,
                errorMessage = "请选择至少一只拥有的宠物。",
                textReport = "请选择至少一只拥有的宠物。",
            )
            return
        }

        val ownedPets = state.pets.filter { pet -> pet.id in state.selectedOwnedPetIds }
        val farmPets = state.pets.filter { pet -> pet.id in state.selectedFarmPetIds }

        viewModelScope.launch {
            isCalculatingState.value = true
            try {
                val startedAtNanos = System.nanoTime()
                val rawResult = withContext(Dispatchers.Default) {
                    PetDispatchSolver.calculate(
                        region = region,
                        taskCount = state.selectedTaskCount,
                        ownedPets = ownedPets,
                        farmPets = farmPets,
                    )
                }
                val elapsedMs = (System.nanoTime() - startedAtNanos) / 1_000_000
                val timedResult = rawResult.copy(calculationTimeMs = elapsedMs)
                resultState.value =
                    if (timedResult.isSuccess) {
                        timedResult.copy(
                            textReport = runCatching {
                                PetDispatchSolver.buildTextReport(region.name, timedResult)
                            }.getOrElse { error ->
                                "生成文本报告失败：${error.message ?: error::class.simpleName.orEmpty()}"
                            },
                        )
                    } else {
                        timedResult.copy(textReport = timedResult.errorMessage.orEmpty())
                    }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                resultState.value = PetDispatchResult(
                    isSuccess = false,
                    errorMessage = "计算失败：${error.message ?: error::class.simpleName.orEmpty()}",
                    textReport = "计算失败：${error.message ?: error::class.simpleName.orEmpty()}",
                )
            } finally {
                isCalculatingState.value = false
            }
        }
    }

    private fun loadCatalog() {
        viewModelScope.launch {
            catalogState.value = PetDispatchCatalogState(catalog = null, errorMessage = null)
            runCatching {
                petDispatchRepository.loadCatalog()
            }.onSuccess { catalog ->
                catalogState.value = PetDispatchCatalogState(catalog = catalog)
                ensureSelectionDefaults(
                    regions = catalog.regions,
                    pets = catalog.pets,
                )
            }.onFailure { error ->
                catalogState.value = PetDispatchCatalogState(
                    errorMessage = error.message ?: "加载宠物派遣数据失败。",
                )
            }
        }
    }

    private fun observePersistedSelection() {
        viewModelScope.launch {
            selectionRepository.observeSelectionState().collect { persistedSelection ->
                if (!hasLoadedPersistedSelection) {
                    selectionState.value = persistedSelection
                    hasLoadedPersistedSelection = true
                }
            }
        }
    }

    private suspend fun ensureSelectionDefaults(
        regions: List<PetDispatchRegion>,
        pets: List<PetDispatchPet>,
    ) {
        val currentSelection = selectionState.value
        val sanitizedSelection = PetDispatchSelectionStateSanitizer.sanitize(
            regions = regions,
            pets = pets,
            selection = currentSelection,
        )
        if (sanitizedSelection != currentSelection) {
            selectionState.value = sanitizedSelection
            selectionRepository.saveSelectionState(sanitizedSelection)
        }
    }

    private fun persistSelection(selection: PetDispatchSelectionState) {
        selectionState.value = selection
        viewModelScope.launch {
            selectionRepository.saveSelectionState(selection)
        }
    }

    private data class PetDispatchCatalogState(
        val catalog: com.trickcal.crayon.model.PetDispatchCatalog? = null,
        val errorMessage: String? = null,
    )
}

internal object PetDispatchSelectionStateSanitizer {
    fun sanitize(
        regions: List<PetDispatchRegion>,
        pets: List<PetDispatchPet>,
        selection: PetDispatchSelectionState,
    ): PetDispatchSelectionState {
        val validRegionNames = regions.map(PetDispatchRegion::name).toSet()
        val validPetIds = pets.map(PetDispatchPet::id).toSet()
        val sanitizedRegionName =
            selection.selectedRegionName
                ?.takeIf { regionName -> regionName.isNotBlank() && regionName in validRegionNames }
                ?: regions.firstOrNull()?.name

        return selection.copy(
            selectedRegionName = sanitizedRegionName,
            selectedTaskCount = selection.selectedTaskCount.coerceIn(1, 5),
            selectedOwnedPetIds = selection.selectedOwnedPetIds.filterTo(linkedSetOf()) { it in validPetIds },
            selectedFarmPetIds = selection.selectedFarmPetIds.filterTo(linkedSetOf()) { it in validPetIds },
        )
    }
}
