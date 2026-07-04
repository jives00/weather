package com.weather.app.data.repository

import com.weather.app.data.api.WeatherApiClient
import com.weather.app.domain.mapper.OpenWeatherMapMapper
import com.weather.app.domain.mapper.WeatherMapper
import com.weather.app.domain.model.Units
import com.weather.app.domain.model.WeatherForecast
import com.weather.app.domain.model.WeatherLocation

class WeatherRepository(
    private val weatherApi: com.weather.app.data.api.WeatherApi = WeatherApiClient.weatherApi,
    private val airQualityApi: com.weather.app.data.api.AirQualityApi = WeatherApiClient.airQualityApi,
    private val openWeatherMapApi: com.weather.app.data.api.OpenWeatherMapApi = WeatherApiClient.openWeatherMapApi
) {
    suspend fun getForecast(location: WeatherLocation, units: Units): Result<WeatherForecast> {
        val aqi = runCatching {
            airQualityApi.getAirQuality(location.latitude, location.longitude).current.usAqi
        }.getOrNull()

        val primary = runCatching {
            val response = weatherApi.getForecast(
                latitude = location.latitude,
                longitude = location.longitude,
                temperatureUnit = units.apiTemperatureUnit,
                windSpeedUnit = units.apiWindSpeedUnit,
                precipitationUnit = units.apiPrecipitationUnit
            )
            WeatherMapper.mapToForecast(response, location, units, aqi)
        }
        if (primary.isSuccess) return primary

        return runCatching {
            val current = openWeatherMapApi.getCurrentWeather(location.latitude, location.longitude)
            val forecast = openWeatherMapApi.getForecast(location.latitude, location.longitude)
            OpenWeatherMapMapper.mapToForecast(current, forecast, location, units, aqi)
        }.recoverCatching { throw primary.exceptionOrNull() ?: it }
    }
}
