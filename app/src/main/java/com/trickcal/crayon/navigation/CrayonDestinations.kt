package com.trickcal.crayon.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    data object Characters : TopLevelDestination(
        route = "characters",
        label = "使徒",
        icon = Icons.Filled.People,
    )

    data object Statistics : TopLevelDestination(
        route = "statistics",
        label = "统计",
        icon = Icons.Filled.InsertChart,
    )
}

object CharacterDetailDestination {
    const val ARG_CHARACTER_ID = "characterId"
    const val route = "characters/detail/{$ARG_CHARACTER_ID}"

    fun createRoute(characterId: String): String = "characters/detail/${Uri.encode(characterId)}"
}
