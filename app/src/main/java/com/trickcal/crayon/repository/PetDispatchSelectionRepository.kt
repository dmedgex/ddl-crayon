package com.trickcal.crayon.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.trickcal.crayon.model.PetDispatchSelectionTab
import com.trickcal.crayon.model.PetDispatchSelectionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.petDispatchDataStore by preferencesDataStore(name = "pet_dispatch")

class PetDispatchSelectionRepository(
    private val context: Context,
) {
    fun observeSelectionState(): Flow<PetDispatchSelectionState> =
        context.petDispatchDataStore.data.map { preferences ->
            PetDispatchSelectionStorageCodec.decode(
                StoredPetDispatchSelectionState(
                    selectedRegionName = preferences[Keys.selectedRegionName],
                    selectedTaskCount = preferences[Keys.selectedTaskCount],
                    selectedOwnedPetIds = preferences[Keys.selectedOwnedPetIds],
                    selectedFarmPetIds = preferences[Keys.selectedFarmPetIds],
                    selectedTab = preferences[Keys.selectedTab],
                ),
            )
        }

    suspend fun saveSelectionState(state: PetDispatchSelectionState) {
        val encoded = PetDispatchSelectionStorageCodec.encode(state)
        context.petDispatchDataStore.edit { preferences ->
            preferences[Keys.selectedRegionName] = encoded.selectedRegionName.orEmpty()
            preferences[Keys.selectedTaskCount] = encoded.selectedTaskCount ?: 1
            preferences[Keys.selectedOwnedPetIds] = encoded.selectedOwnedPetIds.orEmpty()
            preferences[Keys.selectedFarmPetIds] = encoded.selectedFarmPetIds.orEmpty()
            preferences[Keys.selectedTab] = encoded.selectedTab.orEmpty()
        }
    }

    private object Keys {
        val selectedRegionName = stringPreferencesKey("selected_region_name")
        val selectedTaskCount = intPreferencesKey("selected_task_count")
        val selectedOwnedPetIds = stringSetPreferencesKey("selected_owned_pet_ids")
        val selectedFarmPetIds = stringSetPreferencesKey("selected_farm_pet_ids")
        val selectedTab = stringPreferencesKey("selected_tab")
    }
}

internal data class StoredPetDispatchSelectionState(
    val selectedRegionName: String? = null,
    val selectedTaskCount: Int? = null,
    val selectedOwnedPetIds: Set<String>? = null,
    val selectedFarmPetIds: Set<String>? = null,
    val selectedTab: String? = null,
)

internal object PetDispatchSelectionStorageCodec {
    fun decode(stored: StoredPetDispatchSelectionState): PetDispatchSelectionState =
        PetDispatchSelectionState(
            selectedRegionName = stored.selectedRegionName?.takeIf { it.isNotBlank() },
            selectedTaskCount = stored.selectedTaskCount ?: 1,
            selectedOwnedPetIds = stored.selectedOwnedPetIds.toIntSet(),
            selectedFarmPetIds = stored.selectedFarmPetIds.toIntSet(),
            selectedTab = PetDispatchSelectionTab.fromStorageValue(stored.selectedTab),
        )

    fun encode(state: PetDispatchSelectionState): StoredPetDispatchSelectionState =
        StoredPetDispatchSelectionState(
            selectedRegionName = state.selectedRegionName,
            selectedTaskCount = state.selectedTaskCount,
            selectedOwnedPetIds = state.selectedOwnedPetIds.toStringSet(),
            selectedFarmPetIds = state.selectedFarmPetIds.toStringSet(),
            selectedTab = state.selectedTab.name,
        )
}

private fun Set<String>?.toIntSet(): Set<Int> =
    this.orEmpty().mapNotNull { value -> value.toIntOrNull() }.toSet()

private fun Set<Int>.toStringSet(): Set<String> =
    map(Int::toString).toSet()
