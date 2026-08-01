package com.weather.app.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.weather.app.domain.model.WeatherCondition
import com.weather.app.domain.model.WeatherForecast
import com.weather.app.domain.model.WeatherLocation
import com.weather.app.ui.components.*
import com.weather.app.ui.theme.OnWeatherSurface
import com.weather.app.ui.theme.OnWeatherSurfaceDim
import java.util.Calendar

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val locations by viewModel.locations.collectAsStateWithLifecycle()
    val forecasts by viewModel.forecasts.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()

    val locationPermission = rememberPermissionState(android.Manifest.permission.ACCESS_COARSE_LOCATION) { granted ->
        if (granted) viewModel.onLocationPermissionGranted()
    }
    LaunchedEffect(locationPermission.status.isGranted) {
        if (locationPermission.status.isGranted) viewModel.onLocationPermissionGranted()
        else locationPermission.launchPermissionRequest()
    }

    var showAddDialog by remember { mutableStateOf(false) }
    val pageCount = if (locations.isEmpty()) 1 else locations.size
    val pagerState = rememberPagerState(pageCount = { pageCount })

    val currentLocation = locations.getOrNull(pagerState.currentPage)
    val currentForecast = (currentLocation?.let { forecasts[it.id] } as? WeatherUiState.Success)?.forecast
    val hourOfDay = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Animated gradient background
        AnimatedBackgroundWithSize(
            condition = currentForecast?.current?.condition ?: WeatherCondition.CLEAR_DAY,
            hourOfDay = hourOfDay,
            modifier = Modifier.fillMaxSize()
        )

        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val location = locations.getOrNull(page)
            val state = location?.let { forecasts[it.id] }
            when {
                location == null -> EmptyState(onAddLocation = { showAddDialog = true })
                state == null || state is WeatherUiState.Loading -> LoadingPage()
                state is WeatherUiState.Error -> ErrorPage(state.message) { viewModel.fetchForecast(location) }
                state is WeatherUiState.Success -> WeatherPage(forecast = state.forecast, hourOfDay = hourOfDay)
            }
        }

        // Status bar scrim — prevents scrolled content from showing through the notification bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(androidx.compose.foundation.layout.WindowInsets.statusBars)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.88f), Color.Transparent)
                    )
                )
                .align(Alignment.TopCenter)
        )

        // Top bar + update banner stacked
        Column(modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding()) {
            TopBar(
                locationName = currentLocation?.name ?: "",
                isGps = currentLocation?.isCurrentLocation == true,
                onSettings = onNavigateToSettings,
                onAdd = { showAddDialog = true }
            )
            UpdateBanner(
                state = updateState,
                context = context,
                onUpdate = { viewModel.startUpdate(context) },
                onInstall = { viewModel.installUpdate(context) },
                onDismiss = { viewModel.dismissUpdate() },
                onRetry = { viewModel.checkForUpdate() }
            )
        }

        // Page dots
        if (locations.size > 1) {
            PageIndicator(
                pageCount = locations.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 16.dp)
            )
        }
    }

    if (showAddDialog) {
        AddLocationDialog(
            onDismiss = { showAddDialog = false },
            onSearch = { viewModel.searchLocations(it) },
            onSelect = { viewModel.addLocation(it); showAddDialog = false }
        )
    }
}

@Composable
private fun WeatherPage(forecast: WeatherForecast, hourOfDay: Int) {
    val nowEpoch = remember { System.currentTimeMillis() / 1000 }
    val upcomingHourly = remember(forecast) { forecast.hourly.filter { it.time >= nowEpoch - 1800 }.take(24) }
    val isPrecipCondition = forecast.current.condition in listOf(
        WeatherCondition.RAIN, WeatherCondition.DRIZZLE, WeatherCondition.RAIN_SHOWERS,
        WeatherCondition.FREEZING_RAIN, WeatherCondition.SNOW, WeatherCondition.SNOW_SHOWERS,
        WeatherCondition.THUNDERSTORM
    )
    val hasPrecipForecast = remember(forecast) {
        forecast.hourly.filter { it.time >= nowEpoch }.take(12)
            .any { it.precipitation > forecast.units.fromMm(0.05) || it.precipitationProbability > 25 }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        // Hero: ~40% of screen height
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillParentMaxHeight(0.42f)
            ) {
                // Illustrated scene (hills + sun/moon/clouds)
                WeatherScene(
                    condition = forecast.current.condition,
                    isDay = forecast.current.isDay,
                    modifier = Modifier.fillMaxSize()
                )
                // Temperature and condition overlay
                HeroContent(forecast = forecast, modifier = Modifier.fillMaxSize())
            }
        }

        // Precipitation chart (only when raining or rain expected)
        if (isPrecipCondition || hasPrecipForecast) {
            item {
                PrecipitationForecastCard(
                    hourly = forecast.hourly,
                    units = forecast.units,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp).padding(bottom = 12.dp)
                )
            }
        }

        // Hourly forecast
        item {
            HourlyForecastSection(
                hourly = upcomingHourly,
                units = forecast.units,
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = if (!isPrecipCondition && !hasPrecipForecast) 16.dp else 0.dp).padding(bottom = 12.dp)
            )
        }

        // 10-day forecast (expandable)
        item {
            DailyForecastSection(
                daily = forecast.daily,
                hourly = forecast.hourly,
                units = forecast.units,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)
            )
        }

        // Detail cards
        item {
            DetailCardsGrid(
                forecast = forecast,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)
            )
        }
    }
}

private val heroShadow = androidx.compose.ui.graphics.Shadow(
    color = Color.Black.copy(alpha = 0.6f),
    offset = androidx.compose.ui.geometry.Offset(0f, 1f),
    blurRadius = 6f
)

@Composable
private fun HeroContent(forecast: WeatherForecast, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        // Dark scrim — stronger at top (protects the bar buttons) fading out toward center
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)
                    )
                )
        )

        // All text stacked on the left, anchored toward the bottom of the hero
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Now",
                style = MaterialTheme.typography.labelLarge.copy(shadow = heroShadow),
                color = OnWeatherSurfaceDim
            )
            Text(
                text = "${forecast.current.temperature.toInt()}°",
                style = MaterialTheme.typography.displayLarge.copy(shadow = heroShadow),
                color = OnWeatherSurface
            )
            Text(
                text = "H: ${forecast.highToday.toInt()}°  ·  L: ${forecast.lowToday.toInt()}°",
                style = MaterialTheme.typography.titleMedium.copy(shadow = heroShadow),
                color = OnWeatherSurfaceDim
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = forecast.current.conditionDescription,
                style = MaterialTheme.typography.titleMedium.copy(shadow = heroShadow),
                color = OnWeatherSurface
            )
            Text(
                text = "Feels like ${forecast.current.feelsLike.toInt()}°",
                style = MaterialTheme.typography.bodyMedium.copy(shadow = heroShadow),
                color = OnWeatherSurfaceDim
            )
        }
    }
}

@Composable
private fun TopBar(locationName: String, isGps: Boolean, onSettings: () -> Unit, onAdd: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onAdd) {
            Icon(Icons.Filled.Add, contentDescription = "Add location", tint = OnWeatherSurface)
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            if (isGps) {
                Icon(Icons.Filled.MyLocation, null, tint = OnWeatherSurface, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(locationName, style = MaterialTheme.typography.titleMedium, color = OnWeatherSurface)
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = OnWeatherSurface)
        }
    }
}

@Composable
private fun PageIndicator(pageCount: Int, currentPage: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(pageCount) { i ->
            Box(
                modifier = Modifier.size(if (i == currentPage) 8.dp else 6.dp).clip(CircleShape)
                    .background(if (i == currentPage) OnWeatherSurface else OnWeatherSurface.copy(alpha = 0.4f))
            )
        }
    }
}

@Composable
private fun LoadingPage() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = OnWeatherSurface)
    }
}

@Composable
private fun ErrorPage(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(top = 120.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(message, color = OnWeatherSurface, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun EmptyState(onAddLocation: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(top = 120.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("No location set", color = OnWeatherSurface, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onAddLocation) { Text("Add Location") }
    }
}

@Composable
private fun AddLocationDialog(onDismiss: () -> Unit, onSearch: (String) -> List<WeatherLocation>, onSelect: (WeatherLocation) -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<WeatherLocation>()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Location") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("City name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(onClick = { results = onSearch(query) }, modifier = Modifier.fillMaxWidth()) { Text("Search") }
                results.forEach { loc ->
                    TextButton(onClick = { onSelect(loc) }, modifier = Modifier.fillMaxWidth()) { Text(loc.name) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
