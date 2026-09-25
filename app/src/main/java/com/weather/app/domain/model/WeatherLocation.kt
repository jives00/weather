package com.weather.app.domain.model

import java.util.UUID
import kotlin.math.abs

data class WeatherLocation(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val isCurrentLocation: Boolean = false,
    // "Illinois, United States" — shown under search results to tell same-named cities apart
    val region: String? = null
) {
    /** Geocoder IDs are random per search, so identity across searches is by coordinates (~1 km). */
    fun isSamePlaceAs(other: WeatherLocation): Boolean =
        abs(latitude - other.latitude) < 0.01 && abs(longitude - other.longitude) < 0.01
}
