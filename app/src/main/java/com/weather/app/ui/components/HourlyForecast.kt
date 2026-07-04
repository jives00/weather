package com.weather.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.weather.app.domain.model.HourlyWeather
import com.weather.app.domain.model.Units
import com.weather.app.ui.theme.OnWeatherSurface
import com.weather.app.ui.theme.OnWeatherSurfaceDim
import com.weather.app.ui.theme.WeatherCardBackground
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFmt = DateTimeFormatter.ofPattern("h a").withZone(ZoneId.systemDefault())

@Composable
fun HourlyForecastSection(
    hourly: List<HourlyWeather>,
    units: Units,
    modifier: Modifier = Modifier
) {
    val nowEpoch = remember { System.currentTimeMillis() / 1000 }
    val items = remember(hourly) { hourly.filter { it.time >= nowEpoch - 1800 }.take(24) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Hourly forecast",
            style = MaterialTheme.typography.titleMedium,
            color = OnWeatherSurface,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        LazyRow(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(WeatherCardBackground)
                .padding(vertical = 12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(items) { item ->
                HourlyItem(item = item, units = units, isNow = item.time <= nowEpoch + 1800)
            }
        }
    }
}

@Composable
private fun HourlyItem(item: HourlyWeather, units: Units, isNow: Boolean) {
    Column(
        modifier = Modifier
            .width(56.dp)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "${item.temperature.toInt()}${units.temperatureSymbol}",
            style = MaterialTheme.typography.labelLarge,
            color = OnWeatherSurface
        )
        Spacer(Modifier.height(14.dp))
        Icon(
            imageVector = WeatherIcon.forCondition(item.condition),
            contentDescription = null,
            tint = WeatherIcon.colorForCondition(item.condition),
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = if (isNow) "Now" else timeFmt.format(Instant.ofEpochSecond(item.time)).lowercase(),
            style = MaterialTheme.typography.labelMedium,
            color = if (isNow) OnWeatherSurface else OnWeatherSurfaceDim
        )
    }
}

