package com.weather.app.domain.model

enum class WeatherCondition {
    CLEAR_DAY,
    CLEAR_NIGHT,
    PARTLY_CLOUDY_DAY,
    PARTLY_CLOUDY_NIGHT,
    OVERCAST,
    FOG,
    DRIZZLE,
    RAIN,
    FREEZING_RAIN,
    SNOW,
    SNOW_SHOWERS,
    RAIN_SHOWERS,
    THUNDERSTORM,
    UNKNOWN;

    companion object {
        fun fromWmoCode(code: Int, isDay: Boolean): WeatherCondition = when (code) {
            0, 1 -> if (isDay) CLEAR_DAY else CLEAR_NIGHT
            2 -> if (isDay) PARTLY_CLOUDY_DAY else PARTLY_CLOUDY_NIGHT
            3 -> OVERCAST
            45, 48 -> FOG
            51, 53, 55 -> DRIZZLE
            56, 57 -> FREEZING_RAIN
            61, 63, 65 -> RAIN
            66, 67 -> FREEZING_RAIN
            71, 73, 75, 77 -> SNOW
            80, 81, 82 -> RAIN_SHOWERS
            85, 86 -> SNOW_SHOWERS
            95, 96, 99 -> THUNDERSTORM
            else -> UNKNOWN
        }

        fun fromOwmCode(code: Int, isDay: Boolean): WeatherCondition = when (code) {
            in 200..232 -> THUNDERSTORM
            in 300..321 -> DRIZZLE
            511 -> FREEZING_RAIN
            in 500..504 -> RAIN
            in 520..531 -> RAIN_SHOWERS
            in 611..613 -> FREEZING_RAIN
            in 600..622 -> SNOW
            701, 711, 721, 731, 741, 751, 761, 762, 771, 781 -> FOG
            800 -> if (isDay) CLEAR_DAY else CLEAR_NIGHT
            801, 802 -> if (isDay) PARTLY_CLOUDY_DAY else PARTLY_CLOUDY_NIGHT
            803, 804 -> OVERCAST
            else -> UNKNOWN
        }

        fun descriptionFromWmoCode(code: Int): String = when (code) {
            0 -> "Clear sky"
            1 -> "Mainly clear"
            2 -> "Partly cloudy"
            3 -> "Overcast"
            45 -> "Foggy"
            48 -> "Icy fog"
            51 -> "Light drizzle"
            53 -> "Drizzle"
            55 -> "Heavy drizzle"
            56, 57 -> "Freezing drizzle"
            61 -> "Light rain"
            63 -> "Rain"
            65 -> "Heavy rain"
            66, 67 -> "Freezing rain"
            71 -> "Light snow"
            73 -> "Snow"
            75 -> "Heavy snow"
            77 -> "Snow grains"
            80 -> "Light showers"
            81 -> "Showers"
            82 -> "Heavy showers"
            85, 86 -> "Snow showers"
            95 -> "Thunderstorm"
            96, 99 -> "Thunderstorm with hail"
            else -> "Unknown"
        }
    }
}
