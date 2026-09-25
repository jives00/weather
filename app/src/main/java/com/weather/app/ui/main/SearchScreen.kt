package com.weather.app.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.weather.app.domain.model.WeatherLocation
import com.weather.app.ui.theme.OnWeatherSurface
import com.weather.app.ui.theme.OnWeatherSurfaceDim
import com.weather.app.ui.theme.WeatherDivider

@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    /** [inPager] is the existing pager entry when the city is already saved (or is the GPS city). */
    onSelect: (location: WeatherLocation, inPager: WeatherLocation?) -> Unit
) {
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val results by viewModel.searchResults.collectAsStateWithLifecycle()
    val recents by viewModel.recentSearches.collectAsStateWithLifecycle()
    val pagerLocations by viewModel.locations.collectAsStateWithLifecycle()

    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    // Only grab focus on first open — not when returning from a preview
    var autoFocused by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!autoFocused) {
            focusRequester.requestFocus()
            autoFocused = true
        }
    }

    val trimmed = query.trim()
    val showingRecents = trimmed.length < 2
    val searching = !showingRecents && results.query != trimmed
    val rows = if (showingRecents) recents else results.locations

    fun select(location: WeatherLocation) {
        keyboard?.hide()
        onSelect(location, pagerLocations.firstOrNull { it.isSamePlaceAs(location) })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OnWeatherSurface)
            }
            TextField(
                value = query,
                onValueChange = viewModel::onSearchQueryChange,
                placeholder = { Text("Search for a city", color = OnWeatherSurfaceDim) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = viewModel::clearSearch) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear", tint = OnWeatherSurfaceDim)
                        }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = OnWeatherSurface,
                    unfocusedTextColor = OnWeatherSurface,
                    cursorColor = OnWeatherSurface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.weight(1f).focusRequester(focusRequester)
            )
        }
        HorizontalDivider(color = WeatherDivider)

        when {
            searching -> Box(Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OnWeatherSurface, modifier = Modifier.size(28.dp))
            }
            !showingRecents && rows.isEmpty() -> Text(
                "No matches for \"$trimmed\"",
                color = OnWeatherSurfaceDim,
                modifier = Modifier.padding(24.dp)
            )
            else -> LazyColumn(Modifier.fillMaxSize()) {
                if (showingRecents && rows.isNotEmpty()) {
                    item {
                        Text(
                            "RECENT",
                            style = MaterialTheme.typography.labelMedium,
                            color = OnWeatherSurfaceDim,
                            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp)
                        )
                    }
                }
                items(rows, key = { it.id }) { location ->
                    LocationResultRow(
                        location = location,
                        icon = if (showingRecents) Icons.Filled.History else Icons.Filled.Place,
                        isSaved = pagerLocations.any { it.isSamePlaceAs(location) },
                        onClick = { select(location) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationResultRow(location: WeatherLocation, icon: ImageVector, isSaved: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = OnWeatherSurfaceDim, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(location.name, style = MaterialTheme.typography.bodyLarge, color = OnWeatherSurface)
            location.region?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = OnWeatherSurfaceDim)
            }
        }
        if (isSaved) {
            Icon(Icons.Filled.Check, contentDescription = "Saved", tint = OnWeatherSurfaceDim, modifier = Modifier.size(20.dp))
        }
    }
}
