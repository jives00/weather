package com.weather.app.data.api

import com.google.gson.annotations.SerializedName

data class OwmWeatherDescriptor(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String
)

data class OwmMain(
    val temp: Double,
    @SerializedName("feels_like") val feelsLike: Double,
    val humidity: Int,
    val pressure: Double
)

data class OwmWind(
    val speed: Double,
    val deg: Int
)

data class OwmSys(
    val sunrise: Long,
    val sunset: Long
)

data class OwmCurrentResponse(
    val dt: Long,
    val main: OwmMain,
    val weather: List<OwmWeatherDescriptor>,
    val wind: OwmWind,
    val visibility: Double,
    val sys: OwmSys
)

data class OwmPrecipVolume(
    @SerializedName("3h") val threeHour: Double? = null
)

data class OwmForecastEntry(
    val dt: Long,
    val main: OwmMain,
    val weather: List<OwmWeatherDescriptor>,
    val wind: OwmWind,
    val visibility: Double?,
    val pop: Double,
    val rain: OwmPrecipVolume? = null,
    val snow: OwmPrecipVolume? = null
)

data class OwmCity(
    val timezone: Int,
    val sunrise: Long,
    val sunset: Long
)

data class OwmForecastResponse(
    val list: List<OwmForecastEntry>,
    val city: OwmCity
)
