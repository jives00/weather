package com.weather.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.weather.app.domain.model.HourlyWeather
import com.weather.app.domain.model.Units
import com.weather.app.domain.model.WeatherCondition
import com.weather.app.ui.theme.OnWeatherSurface
import com.weather.app.ui.theme.OnWeatherSurfaceDim
import com.weather.app.ui.theme.WeatherCardBackground
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFmt = DateTimeFormatter.ofPattern("h:mm a").withZone(ZoneId.systemDefault())
private val hourFmt = DateTimeFormatter.ofPattern("h a").withZone(ZoneId.systemDefault())

@Composable
fun PrecipitationForecastCard(
    hourly: List<HourlyWeather>,
    units: Units,
    modifier: Modifier = Modifier
) {
    val nowEpoch = System.currentTimeMillis() / 1000
    val nextHours = remember(hourly) { hourly.filter { it.time >= nowEpoch }.take(12) }

    val precipHours = nextHours.filter { it.precipitation > 0.05 || it.precipitationProbability > 25 }
    if (precipHours.isEmpty()) return

    val isSnow = precipHours.first().condition in listOf(WeatherCondition.SNOW, WeatherCondition.SNOW_SHOWERS)
    val precipType = if (isSnow) "Snow" else "Rain"

    val currentlyPrecip = nextHours.firstOrNull()
        ?.let { it.precipitation > 0.1 || it.precipitationProbability > 50 } == true

    val headerText: String
    val subText: String
    if (currentlyPrecip) {
        val endHour = precipHours.last()
        headerText = "$precipType continuing through ${timeFmt.format(Instant.ofEpochSecond(endHour.time))}"
        subText = "Tapering off later this hour"
    } else {
        val startHour = precipHours.first()
        val endHour = precipHours.last()
        val startStr = timeFmt.format(Instant.ofEpochSecond(startHour.time))
        val endStr = timeFmt.format(Instant.ofEpochSecond(endHour.time))
        headerText = "$precipType expected around $startStr"
        subText = "Continuing through $endStr"
    }

    val maxProbability = precipHours.maxOf { it.precipitationProbability }
    val totalAmount = precipHours.sumOf { it.precipitation }
    val amountText = formatPrecipAmount(totalAmount, units)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(WeatherCardBackground),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(headerText, style = MaterialTheme.typography.titleMedium, color = OnWeatherSurface)
            Text(subText, style = MaterialTheme.typography.bodyMedium, color = OnWeatherSurfaceDim)
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("$maxProbability% chance", style = MaterialTheme.typography.bodyMedium, color = OnWeatherSurface)
                Text("$amountText expected", style = MaterialTheme.typography.bodyMedium, color = OnWeatherSurface)
            }
        }
        PrecipBarChart(hours = nextHours, precipType = precipType, units = units)
    }
}

private fun formatPrecipAmount(amount: Double, units: Units): String {
    val decimals = if (units == Units.IMPERIAL) 2 else 1
    return "%.${decimals}f%s".format(amount, units.precipitationUnit)
}

private fun formatHourlyAmount(amount: Double, units: Units): String {
    val decimals = if (units == Units.IMPERIAL) 2 else 1
    return "%.${decimals}f".format(amount)
}

private val barAreaHeight = 56.dp

@Composable
private fun PrecipBarChart(hours: List<HourlyWeather>, precipType: String, units: Units) {
    val barColor = if (precipType == "Snow") Color(0xFFB0BEC5) else Color(0xFF4FC3F7)
    val maxVal = hours.maxOfOrNull { maxOf(it.precipitation, it.precipitationProbability / 100.0) }?.coerceAtLeast(0.1) ?: 1.0

    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(hours) { hour ->
            val value = maxOf(hour.precipitation, hour.precipitationProbability / 100.0)
            val normalized = (value / maxVal).toFloat().coerceIn(0f, 1f)

            Column(
                modifier = Modifier.width(56.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${hour.precipitationProbability}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnWeatherSurfaceDim
                )
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .height(barAreaHeight)
                        .fillMaxWidth(0.5f),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (normalized > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(normalized.coerceAtLeast(0.03f))
                                .clip(RoundedCornerShape(4.dp))
                                .background(barColor.copy(alpha = 0.8f))
                        )
                    }
                }
                Text(
                    text = formatHourlyAmount(hour.precipitation, units),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnWeatherSurfaceDim,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = hourFmt.format(Instant.ofEpochSecond(hour.time)).lowercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnWeatherSurfaceDim,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
