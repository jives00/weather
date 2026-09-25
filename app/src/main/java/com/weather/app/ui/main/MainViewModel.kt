package com.weather.app.ui.main

import android.app.Application
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.weather.app.data.datastore.LocationDataStore
import com.weather.app.data.datastore.SettingsDataStore
import com.weather.app.data.datastore.WidgetDataStore
import com.weather.app.data.repository.DownloadProgress
import com.weather.app.data.repository.UpdateRepository
import com.weather.app.data.repository.WeatherRepository
import com.weather.app.domain.model.Units
import com.weather.app.domain.model.WeatherForecast
import com.weather.app.domain.model.WeatherLocation
import androidx.glance.appwidget.updateAll
import com.weather.app.widget.BarWeatherWidget
import com.weather.app.widget.LargeWeatherWidget
import com.weather.app.widget.MediumWeatherWidget
import com.weather.app.widget.SmallWeatherWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.util.Locale

sealed class UpdateState {
    data object None : UpdateState()
    data class Available(val tag: String, val apkUrl: String) : UpdateState()
    data class Downloading(val progress: Float) : UpdateState()
    data class ReadyToInstall(val uri: Uri?) : UpdateState()
    data class Error(val message: String) : UpdateState()
    data object Dismissed : UpdateState()
}

/** [query] is the (trimmed) text these results answer — lags the field while a search is in flight. */
data class SearchResults(val query: String, val locations: List<WeatherLocation>)

data class PreviewState(
    val location: WeatherLocation,
    val forecast: WeatherUiState,
    val isSaved: Boolean = false
)

sealed class WeatherUiState {
    data object Loading : WeatherUiState()
    data class Success(val forecast: WeatherForecast) : WeatherUiState()
    data class Error(val message: String) : WeatherUiState()
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val locationDataStore = LocationDataStore(application)
    private val settingsDataStore = SettingsDataStore(application)
    private val repository = WeatherRepository()
    private val updateRepository = UpdateRepository()
    private val fusedLocation = LocationServices.getFusedLocationProviderClient(application)

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.None)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    init {
        checkForUpdate()
    }

    fun checkForUpdate() {
        viewModelScope.launch {
            updateRepository.checkForUpdate().fold(
                onSuccess = { info ->
                    if (info != null) _updateState.value = UpdateState.Available(info.tag, info.apkUrl)
                },
                onFailure = { e -> _updateState.value = UpdateState.Error(e.message ?: "Update check failed") }
            )
        }
    }

    fun startUpdate(context: Context) {
        val state = _updateState.value as? UpdateState.Available ?: return
        viewModelScope.launch {
            updateRepository.downloadApk(context, state.apkUrl, state.tag).collect { progress ->
                _updateState.value = when (progress) {
                    is DownloadProgress.InProgress -> UpdateState.Downloading(progress.fraction)
                    is DownloadProgress.Complete -> UpdateState.ReadyToInstall(progress.uri)
                    is DownloadProgress.Failed -> UpdateState.Error(progress.reason)
                }
            }
        }
    }

    fun installUpdate(context: Context) {
        val uri = (_updateState.value as? UpdateState.ReadyToInstall)?.uri ?: return
        if (!context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        )
    }

    fun dismissUpdate() { _updateState.value = UpdateState.Dismissed }

    val units: StateFlow<Units> = settingsDataStore.units
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Units.IMPERIAL)

    private val savedLocations: StateFlow<List<WeatherLocation>> = locationDataStore.locations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _gpsLocation = MutableStateFlow<WeatherLocation?>(null)

    val locations: StateFlow<List<WeatherLocation>> = combine(_gpsLocation, savedLocations) { gps, saved ->
        buildList {
            gps?.let { add(it) }
            addAll(saved)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _forecasts = MutableStateFlow<Map<String, WeatherUiState>>(emptyMap())
    val forecasts: StateFlow<Map<String, WeatherUiState>> = _forecasts.asStateFlow()

    private val _locationPermissionGranted = MutableStateFlow(false)

    fun onLocationPermissionGranted() {
        _locationPermissionGranted.value = true
        refreshGpsLocation()
    }

    fun refreshGpsLocation() {
        viewModelScope.launch {
            try {
                @Suppress("MissingPermission")
                val loc = fusedLocation.lastLocation.await() ?: return@launch
                val name = resolveLocationName(loc.latitude, loc.longitude)
                val gps = WeatherLocation(id = "gps", name = name, latitude = loc.latitude, longitude = loc.longitude, isCurrentLocation = true)
                _gpsLocation.value = gps
                locationDataStore.saveCurrentLocation(gps)
                fetchForecast(gps)
            } catch (_: Exception) {}
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            locations.value.forEach { fetchForecast(it) }
        }
    }

    fun fetchForecast(location: WeatherLocation) {
        viewModelScope.launch {
            _forecasts.update { it + (location.id to WeatherUiState.Loading) }
            val result = repository.getForecast(location, units.value)
            _forecasts.update { map ->
                map + (location.id to result.fold(
                    onSuccess = { forecast ->
                        WidgetDataStore.save(getApplication(), forecast)
                        WeatherUiState.Success(forecast)
                    },
                    onFailure = { WeatherUiState.Error(it.message ?: "Failed to load weather") }
                ))
            }
            if (result.isSuccess) {
                val app = getApplication<Application>()
                SmallWeatherWidget().updateAll(app)
                BarWeatherWidget().updateAll(app)
                MediumWeatherWidget().updateAll(app)
                LargeWeatherWidget().updateAll(app)
            }
        }
    }

    fun addLocation(location: WeatherLocation) {
        viewModelScope.launch {
            locationDataStore.saveLocation(location)
            fetchForecast(location)
        }
    }

    fun removeLocation(locationId: String) {
        viewModelScope.launch {
            locationDataStore.removeLocation(locationId)
            _forecasts.update { it - locationId }
        }
    }

    // ── Search & preview ─────────────────────────────────────────────────────

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<SearchResults> = _searchQuery
        .map { it.trim() }
        .distinctUntilChanged()
        .debounce(300)
        .mapLatest { query -> SearchResults(query, if (query.length < 2) emptyList() else geocode(query)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchResults("", emptyList()))

    val recentSearches: StateFlow<List<WeatherLocation>> = locationDataStore.recentSearches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _preview = MutableStateFlow<PreviewState?>(null)
    val preview: StateFlow<PreviewState?> = _preview.asStateFlow()

    /** Location the main pager should scroll to next time it's shown. */
    private val _pendingPageLocationId = MutableStateFlow<String?>(null)
    val pendingPageLocationId: StateFlow<String?> = _pendingPageLocationId.asStateFlow()

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }

    fun clearSearch() { _searchQuery.value = "" }

    fun jumpToPage(locationId: String) { _pendingPageLocationId.value = locationId }

    /** Back to page 1 — the GPS city when available. */
    fun jumpToFirstPage() { _pendingPageLocationId.value = locations.value.firstOrNull()?.id }

    fun onPageJumpHandled() { _pendingPageLocationId.value = null }

    /** Loads a city's forecast without saving it — no widget data, no background refresh. */
    fun startPreview(location: WeatherLocation) {
        _preview.value = PreviewState(location, WeatherUiState.Loading)
        viewModelScope.launch {
            locationDataStore.addRecentSearch(location)
            loadPreviewForecast(location)
        }
    }

    fun retryPreview() {
        val location = _preview.value?.location ?: return
        _preview.update { it?.copy(forecast = WeatherUiState.Loading) }
        viewModelScope.launch { loadPreviewForecast(location) }
    }

    private suspend fun loadPreviewForecast(location: WeatherLocation) {
        val state = repository.getForecast(location, units.value).fold(
            onSuccess = { WeatherUiState.Success(it) },
            onFailure = { WeatherUiState.Error(it.message ?: "Failed to load weather") }
        )
        // Ignore a late response if the user already moved on to another city
        _preview.update { if (it?.location?.id == location.id) it.copy(forecast = state) else it }
    }

    fun savePreview() {
        val preview = _preview.value ?: return
        if (preview.isSaved) return
        _preview.value = preview.copy(isSaved = true)
        viewModelScope.launch {
            locationDataStore.removeRecentSearch(preview.location)
            addLocation(preview.location)
        }
    }

    /** Undo [savePreview] — the city goes back to being a recent search. */
    fun unsavePreview() {
        val preview = _preview.value ?: return
        if (!preview.isSaved) return
        _preview.value = preview.copy(isSaved = false)
        viewModelScope.launch {
            locationDataStore.addRecentSearch(preview.location)
            removeLocation(preview.location.id)
        }
    }

    private suspend fun geocode(query: String): List<WeatherLocation> = withContext(Dispatchers.IO) {
        try {
            @Suppress("DEPRECATION")
            val addresses = Geocoder(getApplication(), Locale.getDefault())
                .getFromLocationName(query, 8) ?: emptyList()
            addresses.mapNotNull { addr ->
                val name = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: return@mapNotNull null
                val country = if (addr.countryCode != "US") ", ${addr.countryCode}" else ""
                val region = listOfNotNull(addr.adminArea?.takeIf { it != name }, addr.countryName)
                    .joinToString(", ").ifEmpty { null }
                WeatherLocation(
                    name = "$name$country",
                    latitude = addr.latitude,
                    longitude = addr.longitude,
                    region = region
                )
            }.distinctBy { it.name to it.region }
        } catch (_: Exception) { emptyList() }
    }

    private fun resolveLocationName(lat: Double, lon: Double): String {
        return try {
            @Suppress("DEPRECATION")
            val addresses = Geocoder(getApplication(), Locale.getDefault()).getFromLocation(lat, lon, 1)
            addresses?.firstOrNull()?.locality
                ?: addresses?.firstOrNull()?.subAdminArea
                ?: "Current Location"
        } catch (_: Exception) { "Current Location" }
    }
}
