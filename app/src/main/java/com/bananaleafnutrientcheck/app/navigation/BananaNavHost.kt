package com.bananaleafnutrientcheck.app.navigation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bananaleafnutrientcheck.app.R
import com.bananaleafnutrientcheck.app.presentation.AboutScreen
import com.bananaleafnutrientcheck.app.presentation.HomeScreen
import com.bananaleafnutrientcheck.app.presentation.ScanScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BananaNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppDestination.Home.route

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            DestinationTabs(
                currentRoute = currentRoute,
                onDestinationSelected = { destination ->
                    navController.navigateSingleTopTo(destination.route)
                },
            )

            NavHost(
                navController = navController,
                startDestination = AppDestination.Home.route,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(AppDestination.Home.route) {
                    HomeScreen(
                        onStartScan = {
                            navController.navigateSingleTopTo(AppDestination.Scan.route)
                        },
                        onOpenAbout = {
                            navController.navigateSingleTopTo(AppDestination.About.route)
                        },
                    )
                }
                composable(AppDestination.Scan.route) {
                    ScanScreen()
                }
                composable(AppDestination.About.route) {
                    AboutScreen()
                }
            }
        }
    }
}

@Composable
private fun DestinationTabs(
    currentRoute: String,
    onDestinationSelected: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppDestination.entries.forEach { destination ->
            FilterChip(
                selected = currentRoute == destination.route,
                onClick = { onDestinationSelected(destination) },
                label = {
                    Text(text = stringResource(destination.labelResId))
                },
            )
        }
    }
}

private fun NavHostController.navigateSingleTopTo(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
