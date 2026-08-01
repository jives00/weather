package com.weather.app.domain.model

enum class Units(val label: String) {
    IMPERIAL("Imperial (°F)"),
    METRIC("Metric (°C)");

    val temperatureSymbol: String get() = if (this == IMPERIAL) "°F" else "°C"
    val windSpeedUnit: String get() = if (this == IMPERIAL) "mph" else "km/h"
    val precipitationUnit: String get() = if (this == IMPERIAL) "in" else "mm"
    val visibilityUnit: String get() = if (this == IMPERIAL) "mi" else "km"
    val pressureUnit: String get() = "hPa"

    val apiTemperatureUnit: String get() = if (this == IMPERIAL) "fahrenheit" else "celsius"
    val apiWindSpeedUnit: String get() = if (this == IMPERIAL) "mph" else "kmh"
    val apiPrecipitationUnit: String get() = if (this == IMPERIAL) "inch" else "mm"

    // Forecast values arrive already converted to the user's display unit, but precipitation
    // thresholds in the UI are written in mm (where the magnitudes are legible). Convert the
    // threshold rather than the value so those call sites keep reading in a single unit.
    fun fromMm(mm: Double): Double = if (this == IMPERIAL) mm / 25.4 else mm
}
