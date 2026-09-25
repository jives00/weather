package com.weather.app.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.weather.app.domain.model.WeatherCondition
import com.weather.app.ui.components.AnimatedBackgroundWithSize
import com.weather.app.ui.theme.OnWeatherSurface
import kotlinx.coroutines.launch
import java.util.Calendar

/** Full forecast for a searched city that isn't saved (yet). Lives outside the pager. */
@Composable
fun PreviewScreen(
    viewModel: MainViewModel,
    /** [saved] — the city was added, so back should skip search and land on the pager. */
    onBack: (saved: Boolean) -> Unit
) {
    val preview by viewModel.preview.collectAsStateWithLifecycle()
    val current = preview ?: return
    val hourOfDay = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val forecast = (current.forecast as? WeatherUiState.Success)?.forecast

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    BackHandler { onBack(current.isSaved) }

    Box(Modifier.fillMaxSize()) {
        AnimatedBackgroundWithSize(
            condition = forecast?.current?.condition ?: WeatherCondition.CLEAR_DAY,
            hourOfDay = hourOfDay,
            modifier = Modifier.fillMaxSize()
        )

        when (val state = current.forecast) {
            is WeatherUiState.Loading -> LoadingPage()
            is WeatherUiState.Error -> ErrorPage(state.message, onRetry = viewModel::retryPreview)
            is WeatherUiState.Success -> WeatherPage(forecast = state.forecast, hourOfDay = hourOfDay)
        }

        StatusBarScrim(Modifier.align(Alignment.TopCenter))

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onBack(current.isSaved) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OnWeatherSurface)
            }
            Text(
                current.location.name,
                style = MaterialTheme.typography.titleMedium,
                color = OnWeatherSurface,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            // Toggle: ＋ saves, ✓ unsaves
            IconButton(onClick = {
                val message = if (current.isSaved) {
                    viewModel.unsavePreview()
                    "${current.location.name} removed"
                } else {
                    viewModel.savePreview()
                    "${current.location.name} added"
                }
                scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(message)
                }
            }) {
                if (current.isSaved) {
                    Icon(Icons.Filled.Check, contentDescription = "Remove saved location", tint = OnWeatherSurface)
                } else {
                    Icon(Icons.Filled.Add, contentDescription = "Save location", tint = OnWeatherSurface)
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
        )
    }
}
