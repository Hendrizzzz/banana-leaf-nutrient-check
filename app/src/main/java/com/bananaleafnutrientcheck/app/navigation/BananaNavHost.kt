package com.bananaleafnutrientcheck.app.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.bananaleafnutrientcheck.app.presentation.ScanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BananaNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppDestination.Home.route
    val isHome = currentRoute == AppDestination.Home.route

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (!isHome) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                navController.navigateSingleTopTo(AppDestination.Home.route)
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back_24),
                                contentDescription = stringResource(R.string.navigation_back_home),
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
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
                    val context = LocalContext.current
                    val scanViewModel: ScanViewModel = viewModel(
                        factory = ScanViewModel.factory(context.applicationContext),
                    )
                    val scanUiState by scanViewModel.uiState.collectAsStateWithLifecycle()

                    ScanScreen(
                        uiState = scanUiState,
                        onImageSelected = scanViewModel::onPhotoPickerResult,
                        onCameraImageCaptured = scanViewModel::onCameraCaptureResult,
                        onClearImage = scanViewModel::clearSelectedImage,
                        onAnalyzeImage = scanViewModel::analyzeSelectedImage,
                    )
                }
                composable(AppDestination.About.route) {
                    AboutScreen()
                }
            }
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
