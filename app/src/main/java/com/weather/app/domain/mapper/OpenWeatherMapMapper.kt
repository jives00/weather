package com.weather.app.domain.mapper

import com.weather.app.data.api.OwmCurrentResponse
import com.weather.app.data.api.OwmForecastEntry
import com.weather.app.data.api.OwmForecastResponse
import com.weather.app.domain.model.CurrentWeather
import com.weather.app.domain.model.DailyWeather
import com.weather.app.domain.model.HourlyWeather
import com.weather.app.domain.model.Units
import com.weather.app.domain.model.WeatherCondition
import com.weather.app.domain.model.WeatherForecast
import com.weather.app.domain.model.WeatherLocation
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

// OpenWeatherMap's free tier is a fallback for when Open-Meteo is unreachable. It lacks
// UV index and per-day sunrise/sunset, and only covers 5 days at 3-hour resolution (vs.
// Open-Meteo's 10 days hourly), so those fields are approximated or omitted below.
object OpenWeatherMapMapper {
    private val zone = ZoneId.systemDefault()

    // All OWM requests are made with units=imperial; convert to metric here so both
    // providers hand WeatherRepository values already in the caller's requested unit.
    private fun temp(fahrenheit: Double, units: Units): Double =
        if (units == Units.IMPERIAL) fahrenheit else (fahrenheit - 32) * 5 / 9

    private fun windSpeed(mph: Double, units: Units): Double =
        if (units == Units.IMPERIAL) mph else mph * 1.60934

    private fun precip(inches: Double, units: Units): Double =
        if (units == Units.IMPERIAL) inches else inches * 25.4

    private fun isDay(icon: String): Boolean = icon.endsWith("d")

    fun mapToForecast(
        current: OwmCurrentResponse,
        forecast: OwmForecastResponse,
        location: WeatherLocation,
        units: Units,
        aqi: Int?
    ): WeatherForecast {
        val currentWeather = mapCurrent(current, units)
        val hourly = mapHourly(forecast.list, units)
        val daily = mapDaily(forecast, units)

        return WeatherForecast(
            location = location,
            current = currentWeather,
            hourly = hourly,
            daily = daily,
            highToday = daily.firstOrNull()?.tempMax ?: currentWeather.temperature,
            lowToday = daily.firstOrNull()?.tempMin ?: currentWeather.temperature,
            aqi = aqi,
            units = units
        )
    }

    private fun mapCurrent(response: OwmCurrentResponse, units: Units): CurrentWeather {
        val descriptor = response.weather.firstOrNull()
        val dayNow = descriptor?.icon?.let(::isDay) ?: true
        val condition = WeatherCondition.fromOwmCode(descriptor?.id ?: -1, dayNow)
        return CurrentWeather(
            temperature = temp(response.main.temp, units),
            feelsLike = temp(response.main.feelsLike, units),
            humidity = response.main.humidity,
            condition = condition,
            conditionDescription = descriptor?.description?.replaceFirstChar { it.uppercase() } ?: "Unknown",
            windSpeed = windSpeed(response.wind.speed, units),
            windDirection = response.wind.deg,
            uvIndex = 0.0,
            visibility = response.visibility,
            pressure = response.main.pressure,
            isDay = dayNow,
            wmoCode = descriptor?.id ?: -1
        )
    }

    private fun mapHourly(entries: List<OwmForecastEntry>, units: Units): List<HourlyWeather> =
        entries.take(16).map { entry ->
            val descriptor = entry.weather.firstOrNull()
            val dayNow = descriptor?.icon?.let(::isDay) ?: true
            HourlyWeather(
                time = entry.dt,
                temperature = temp(entry.main.temp, units),
                precipitationProbability = (entry.pop * 100).roundToInt(),
                precipitation = precip((entry.rain?.threeHour ?: 0.0) + (entry.snow?.threeHour ?: 0.0), units),
                condition = WeatherCondition.fromOwmCode(descriptor?.id ?: -1, dayNow),
                wmoCode = descriptor?.id ?: -1,
                isDay = dayNow
            )
        }

    private fun mapDaily(forecast: OwmForecastResponse, units: Units): List<DailyWeather> {
        val zoneOffset = ZoneId.ofOffset("UTC", java.time.ZoneOffset.ofTotalSeconds(forecast.city.timezone))
        return forecast.list
            .groupBy { LocalDate.ofInstant(Instant.ofEpochSecond(it.dt), zoneOffset) }
            .entries
            .sortedBy { it.key }
            .map { (_, dayEntries) ->
                // Prefer the entry nearest local noon to represent the day's overall condition.
                val middayEntry = dayEntries.minByOrNull {
                    kotlin.math.abs(it.dt % 86400 - 12 * 3600)
                }!!
                val descriptor = middayEntry.weather.firstOrNull()
                val precipTotal = dayEntries.sumOf {
                    (it.rain?.threeHour ?: 0.0) + (it.snow?.threeHour ?: 0.0)
                }
                DailyWeather(
                    time = dayEntries.first().dt,
                    tempMax = temp(dayEntries.maxOf { it.main.temp }, units),
                    tempMin = temp(dayEntries.minOf { it.main.temp }, units),
                    condition = WeatherCondition.fromOwmCode(descriptor?.id ?: -1, isDay = true),
                    wmoCode = descriptor?.id ?: -1,
                    sunrise = forecast.city.sunrise,
                    sunset = forecast.city.sunset,
                    precipitationProbability = (dayEntries.maxOf { it.pop } * 100).roundToInt(),
                    precipitationSum = precip(precipTotal, units),
                    windSpeedMax = windSpeed(dayEntries.maxOf { it.wind.speed }, units),
                    uvIndexMax = 0.0
                )
            }
    }
}
