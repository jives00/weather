package com.weather.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.weather.app.ui.main.MainScreen
import com.weather.app.ui.main.MainViewModel
import com.weather.app.ui.main.PreviewScreen
import com.weather.app.ui.main.SearchScreen
import com.weather.app.ui.settings.SettingsScreen
import com.weather.app.ui.settings.SettingsViewModel
import com.weather.app.ui.theme.WeatherTheme
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestBatteryOptimizationExemption()
        setContent {
            WeatherTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "weather") {
                    composable("weather") {
                        MainScreen(
                            viewModel = mainViewModel,
                            onNavigateToSettings = { navController.navigate("settings") },
                            onNavigateToSearch = {
                                mainViewModel.clearSearch()
                                navController.navigate("search")
                            }
                        )
                    }
                    composable("search") {
                        SearchScreen(
                            viewModel = mainViewModel,
                            onBack = { navController.popBackStack() },
                            onSelect = { location, inPager ->
                                if (inPager != null) {
                                    mainViewModel.jumpToPage(inPager.id)
                                    navController.popBackStack("weather", inclusive = false)
                                } else {
                                    mainViewModel.startPreview(location)
                                    navController.navigate("preview")
                                }
                            }
                        )
                    }
                    composable("preview") {
                        PreviewScreen(
                            viewModel = mainViewModel,
                            onBack = { saved ->
                                // Once added, the city lives in the pager — skip back past search
                                if (saved) navController.popBackStack("weather", inclusive = false)
                                else navController.popBackStack()
                            }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mainViewModel.refreshAll()
    }

    private fun requestBatteryOptimizationExemption() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            })
        }
    }

}
