package com.weather.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.weather.app.domain.model.WeatherLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.locationDataStore: DataStore<Preferences> by preferencesDataStore(name = "locations")

class LocationDataStore(private val context: Context) {
    private val gson = Gson()
    private val LOCATIONS_KEY = stringPreferencesKey("saved_locations")
    private val CURRENT_LOCATION_KEY = stringPreferencesKey("current_location")
    private val RECENT_SEARCHES_KEY = stringPreferencesKey("recent_searches")

    val currentLocation: Flow<WeatherLocation?> = context.locationDataStore.data.map { prefs ->
        val json = prefs[CURRENT_LOCATION_KEY] ?: return@map null
        runCatching { gson.fromJson(json, WeatherLocation::class.java) }.getOrNull()
    }

    val locations: Flow<List<WeatherLocation>> = context.locationDataStore.data.map { readLocations(it) }

    /** Previewed-but-not-saved cities, most recent first. */
    val recentSearches: Flow<List<WeatherLocation>> = context.locationDataStore.data.map { readList(it, RECENT_SEARCHES_KEY) }

    suspend fun addRecentSearch(location: WeatherLocation) {
        context.locationDataStore.edit { prefs ->
            val updated = listOf(location) + readList(prefs, RECENT_SEARCHES_KEY).filterNot { it.isSamePlaceAs(location) }
            prefs[RECENT_SEARCHES_KEY] = gson.toJson(updated.take(MAX_RECENTS))
        }
    }

    suspend fun removeRecentSearch(location: WeatherLocation) {
        context.locationDataStore.edit { prefs ->
            prefs[RECENT_SEARCHES_KEY] = gson.toJson(readList(prefs, RECENT_SEARCHES_KEY).filterNot { it.isSamePlaceAs(location) })
        }
    }

    suspend fun saveCurrentLocation(location: WeatherLocation) {
        context.locationDataStore.edit { prefs ->
            prefs[CURRENT_LOCATION_KEY] = gson.toJson(location)
        }
    }

    suspend fun saveLocation(location: WeatherLocation) {
        context.locationDataStore.edit { prefs ->
            val current = readLocations(prefs).toMutableList()
            if (current.none { it.id == location.id }) current.add(location)
            prefs[LOCATIONS_KEY] = gson.toJson(current)
        }
    }

    suspend fun removeLocation(locationId: String) {
        context.locationDataStore.edit { prefs ->
            val current = readLocations(prefs).filterNot { it.id == locationId }
            prefs[LOCATIONS_KEY] = gson.toJson(current)
        }
    }

    suspend fun reorderLocations(locations: List<WeatherLocation>) {
        context.locationDataStore.edit { prefs ->
            prefs[LOCATIONS_KEY] = gson.toJson(locations)
        }
    }

    private fun readLocations(prefs: Preferences): List<WeatherLocation> = readList(prefs, LOCATIONS_KEY)

    private fun readList(prefs: Preferences, key: Preferences.Key<String>): List<WeatherLocation> {
        val json = prefs[key] ?: return emptyList()
        val type = object : TypeToken<List<WeatherLocation>>() {}.type
        return gson.fromJson<List<WeatherLocation>>(json, type) ?: emptyList()
    }

    private companion object {
        const val MAX_RECENTS = 5
    }
}
