package com.trickcal.crayon

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.room.Room
import com.trickcal.crayon.data.local.CharacterBoardAssetLoader
import com.trickcal.crayon.data.local.DemoCharacterCatalog
import com.trickcal.crayon.data.room.CrayonDatabase
import com.trickcal.crayon.feature.battlepower.BattlePowerCalculatorViewModel
import com.trickcal.crayon.feature.damage.DamageCalculatorViewModel
import com.trickcal.crayon.feature.detail.CharacterDetailViewModel
import com.trickcal.crayon.feature.list.CharacterListViewModel
import com.trickcal.crayon.feature.petdispatch.PetDispatchViewModel
import com.trickcal.crayon.feature.settings.SettingsViewModel
import com.trickcal.crayon.feature.statistics.StatisticsViewModel
import com.trickcal.crayon.repository.AppUpdateRepository
import com.trickcal.crayon.repository.AssetBattlePowerCharacterRepository
import com.trickcal.crayon.repository.BattlePowerCharacterRepository
import com.trickcal.crayon.repository.CatalogRepository
import com.trickcal.crayon.repository.CrayonRepository
import com.trickcal.crayon.repository.PaintProgressRepository
import com.trickcal.crayon.repository.PetDispatchRepository
import com.trickcal.crayon.repository.PetDispatchSelectionRepository
import com.trickcal.crayon.repository.SettingsRepository

class AppContainer(context: Context) {
    companion object {
        private const val TAG = "AppContainer"
    }

    private val database: CrayonDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            CrayonDatabase::class.java,
            "trickcal_crayon.db",
        ).build()
    }

    private val catalogRepository: CatalogRepository by lazy {
        val catalog = runCatching {
            CharacterBoardAssetLoader.load(context.applicationContext)
        }.getOrElse {
            Log.e(TAG, "Failed to load character_boards.json. Falling back to demo catalog.", it)
            DemoCharacterCatalog.characters
        }
        CatalogRepository(catalog)
    }

    private val paintProgressRepository: PaintProgressRepository by lazy {
        PaintProgressRepository(database.paintProgressDao())
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context.applicationContext)
    }

    val crayonRepository: CrayonRepository by lazy {
        CrayonRepository(
            catalogRepository = catalogRepository,
            paintProgressRepository = paintProgressRepository,
        )
    }

    val appUpdateRepository: AppUpdateRepository by lazy {
        AppUpdateRepository()
    }

    val petDispatchRepository: PetDispatchRepository by lazy {
        PetDispatchRepository(context.applicationContext)
    }

    val petDispatchSelectionRepository: PetDispatchSelectionRepository by lazy {
        PetDispatchSelectionRepository(context.applicationContext)
    }

    val battlePowerCharacterRepository: BattlePowerCharacterRepository by lazy {
        AssetBattlePowerCharacterRepository(context.applicationContext)
    }
}

class CrayonViewModelFactory(
    private val appContainer: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>,
        extras: CreationExtras,
    ): T {
        return when {
            modelClass.isAssignableFrom(CharacterListViewModel::class.java) -> {
                CharacterListViewModel(
                    repository = appContainer.crayonRepository,
                    settingsRepository = appContainer.settingsRepository,
                ) as T
            }

            modelClass.isAssignableFrom(CharacterDetailViewModel::class.java) -> {
                error("CharacterDetailViewModel should be created with an explicit characterId factory.")
            }

            modelClass.isAssignableFrom(StatisticsViewModel::class.java) -> {
                StatisticsViewModel(appContainer.crayonRepository) as T
            }

            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(
                    settingsRepository = appContainer.settingsRepository,
                    crayonRepository = appContainer.crayonRepository,
                    appUpdateRepository = appContainer.appUpdateRepository,
                ) as T
            }

            modelClass.isAssignableFrom(PetDispatchViewModel::class.java) -> {
                PetDispatchViewModel(
                    petDispatchRepository = appContainer.petDispatchRepository,
                    selectionRepository = appContainer.petDispatchSelectionRepository,
                ) as T
            }

            modelClass.isAssignableFrom(DamageCalculatorViewModel::class.java) -> {
                DamageCalculatorViewModel() as T
            }

            modelClass.isAssignableFrom(BattlePowerCalculatorViewModel::class.java) -> {
                BattlePowerCalculatorViewModel(appContainer.battlePowerCharacterRepository) as T
            }

            else -> error("Unsupported ViewModel class: ${modelClass.name}")
        }
    }
}
