package com.trickcal.crayon.navigation

import com.trickcal.crayon.AppContainer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.trickcal.crayon.feature.detail.CharacterDetailScreen
import com.trickcal.crayon.feature.detail.CharacterDetailViewModel
import com.trickcal.crayon.feature.list.CharacterListScreen
import com.trickcal.crayon.feature.list.CharacterListViewModel
import com.trickcal.crayon.feature.settings.SettingsPage
import com.trickcal.crayon.feature.settings.SettingsViewModel
import com.trickcal.crayon.feature.statistics.StatisticsScreen
import com.trickcal.crayon.feature.statistics.StatisticsViewModel

@Composable
fun CrayonApp(
    appContainer: AppContainer,
    viewModelFactory: ViewModelProvider.Factory,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val topLevelDestinations = listOf(
        BottomDestination(
            route = TopLevelDestination.Characters.route,
            label = "使徒",
            icon = Icons.Filled.People,
        ),
        BottomDestination(
            route = TopLevelDestination.Statistics.route,
            label = "统计",
            icon = Icons.Filled.InsertChart,
        ),
        BottomDestination(
            route = SettingsDestination.route,
            label = SettingsDestination.label,
            icon = SettingsDestination.icon,
        ),
    )

    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.Characters.route,
    ) {
        composable(route = TopLevelDestination.Characters.route) {
            val viewModel: CharacterListViewModel = viewModel(factory = viewModelFactory)
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                viewModel.refreshCardSort()
            }
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            Scaffold(
                bottomBar = {
                    CrayonBottomBar(
                        destinations = topLevelDestinations,
                        currentDestinationRoute = currentRoute,
                        onNavigate = { destination ->
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                },
            ) { innerPadding ->
                CharacterListScreen(
                    modifier = Modifier.padding(innerPadding),
                    uiState = uiState,
                    onNameQueryChange = viewModel::setNameQuery,
                    onToggleTier = viewModel::toggleTier,
                    onToggleAttribute = viewModel::toggleAttribute,
                    onTogglePersonality = viewModel::togglePersonality,
                    onClearFilters = viewModel::clearFilters,
                    onModeSelected = viewModel::setBrowseMode,
                    onToggleDisplayMode = viewModel::toggleDisplayMode,
                    onOpenPaintSheet = viewModel::openPaintSheet,
                    onDismissPaintSheet = viewModel::dismissPaintSheet,
                    onOpenBatchPaintDialog = viewModel::openBatchPaintDialog,
                    onDismissBatchPaintDialog = viewModel::dismissBatchPaintDialog,
                    onToggleBatchTier = viewModel::toggleBatchTier,
                    onToggleBatchAttribute = viewModel::toggleBatchAttribute,
                    onSetBatchPaintAction = viewModel::setBatchPaintAction,
                    onApplyBatchPaint = viewModel::applyBatchPaint,
                    onToggleSlot = viewModel::toggleSlot,
                    onQuickPaintCharacter = viewModel::quickPaintCharacter,
                    onNavigateToDetail = { characterId ->
                        navController.navigate(CharacterDetailDestination.createRoute(characterId))
                    },
                )
            }
        }

        composable(route = TopLevelDestination.Statistics.route) {
            val viewModel: StatisticsViewModel = viewModel(factory = viewModelFactory)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            Scaffold(
                bottomBar = {
                    CrayonBottomBar(
                        destinations = topLevelDestinations,
                        currentDestinationRoute = currentRoute,
                        onNavigate = { destination ->
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                },
            ) { innerPadding ->
                StatisticsScreen(
                    modifier = Modifier.padding(innerPadding),
                    uiState = uiState,
                )
            }
        }

        composable(route = SettingsDestination.route) {
            val viewModel: SettingsViewModel = viewModel(factory = viewModelFactory)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }
            LaunchedEffect(viewModel) {
                viewModel.messages.collect { message ->
                    snackbarHostState.showSnackbar(message)
                }
            }
            Scaffold(
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState)
                },
                bottomBar = {
                    CrayonBottomBar(
                        destinations = topLevelDestinations,
                        currentDestinationRoute = currentRoute,
                        onNavigate = { destination ->
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                },
            ) { innerPadding ->
                SettingsPage(
                    modifier = Modifier.padding(innerPadding),
                    uiState = uiState,
                    onThemeModeSelected = viewModel::setThemeMode,
                    onCheckUpdateRequested = viewModel::checkForUpdate,
                    buildExportFileName = viewModel::buildExportFileName,
                    onExportRequested = { context, uri ->
                        viewModel.exportToStream(context.contentResolver.openOutputStream(uri))
                    },
                    onImportRequested = { context, uri ->
                        viewModel.prepareImport(context.contentResolver.openInputStream(uri))
                    },
                    onDismissImportConfirm = viewModel::dismissImportConfirm,
                    onConfirmImport = viewModel::confirmImport,
                    onDismissUpdateDialog = viewModel::dismissUpdateDialog,
                )
            }
        }

        composable(
            route = CharacterDetailDestination.route,
            arguments = listOf(
                navArgument(CharacterDetailDestination.ARG_CHARACTER_ID) {
                    type = NavType.StringType
                },
            ),
        ) { backStackEntry ->
            val characterId = backStackEntry.arguments
                ?.getString(CharacterDetailDestination.ARG_CHARACTER_ID)
                .orEmpty()
            var backInFlight by remember(backStackEntry.id) { mutableStateOf(false) }
            LaunchedEffect(currentRoute) {
                if (currentRoute != CharacterDetailDestination.route) {
                    backInFlight = false
                }
            }
            val viewModel: CharacterDetailViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = CharacterDetailViewModel.factory(
                    repository = appContainer.crayonRepository,
                    characterId = characterId,
                ),
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            Scaffold(
                topBar = {
                    DetailTopBar(
                        title = uiState.character?.name ?: "使徒详情",
                        backEnabled = !backInFlight,
                        onBack = {
                            if (backInFlight) {
                                return@DetailTopBar
                            }
                            backInFlight = true
                            val canPop = navController.currentBackStackEntry
                                ?.destination
                                ?.route == CharacterDetailDestination.route
                            if (canPop) {
                                val popped = navController.popBackStack()
                                if (!popped) {
                                    backInFlight = false
                                }
                            } else {
                                backInFlight = false
                            }
                        },
                    )
                },
            ) { innerPadding ->
                CharacterDetailScreen(
                    modifier = Modifier.padding(innerPadding),
                    uiState = uiState,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailTopBar(
    title: String,
    backEnabled: Boolean,
    onBack: () -> Unit,
) {
    CenterAlignedTopAppBar(
        title = {
            Text(text = title, fontWeight = FontWeight.SemiBold)
        },
        navigationIcon = {
            IconButton(
                onClick = onBack,
                enabled = backEnabled,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                )
            }
        },
    )
}

@Composable
private fun CrayonBottomBar(
    destinations: List<BottomDestination>,
    currentDestinationRoute: String?,
    onNavigate: (BottomDestination) -> Unit,
) {
    NavigationBar {
        destinations.forEach { destination ->
            NavigationBarItem(
                selected = currentDestinationRoute == destination.route,
                onClick = { onNavigate(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label,
                    )
                },
                label = { Text(destination.label) },
            )
        }
    }
}

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)
