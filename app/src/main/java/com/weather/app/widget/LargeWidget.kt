package com.weather.app.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import android.appwidget.AppWidgetManager
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.weather.app.MainActivity
import com.weather.app.data.datastore.WidgetDataStore
import com.weather.app.domain.model.WeatherCondition
import com.weather.app.work.WeatherRefreshWorker

class LargeWeatherWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetDataStore.getPrimary(context)
        provideContent {
            val bgTop = if (data != null) WidgetColors.gradientTopForCondition(data.condition, data.isDay) else Color(0xFF1565C0)
            Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(bgTop)
                        .padding(16.dp)
                        .clickable(actionStartActivity<MainActivity>())
                ) {
                    if (data == null) {
                        Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("—", style = TextStyle(color = ColorProvider(Color.White)))
                        }
                    } else {
                        Text(
                            text = data.locationName,
                            style = TextStyle(color = ColorProvider(Color.White.copy(alpha = 0.85f)), fontSize = 14.sp),
                            maxLines = 1
                        )

                        Spacer(GlanceModifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                provider = ImageProvider(WeatherDrawableMap.drawableFor(data.condition)),
                                contentDescription = data.conditionDescription,
                                modifier = GlanceModifier.size(52.dp)
                            )
                            Spacer(GlanceModifier.width(8.dp))
                            Text(
                                text = "${data.temperature}${data.temperatureSymbol}",
                                style = TextStyle(color = ColorProvider(Color.White), fontSize = 52.sp, fontWeight = FontWeight.Bold)
                            )
                            Spacer(GlanceModifier.width(12.dp))
                            Column {
                                Text(
                                    text = data.conditionDescription,
                                    style = TextStyle(color = ColorProvider(Color.White), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "H: ${data.highTemp}°  L: ${data.lowTemp}°",
                                    style = TextStyle(color = ColorProvider(Color.White.copy(alpha = 0.7f)), fontSize = 12.sp)
                                )
                                Text(
                                    text = "Feels ${data.feelsLike}${data.temperatureSymbol}",
                                    style = TextStyle(color = ColorProvider(Color.White.copy(alpha = 0.7f)), fontSize = 12.sp)
                                )
                            }
                        }

                        Spacer(GlanceModifier.height(12.dp))

                        if (data.hourlyTemps.isNotEmpty()) {
                            Row(modifier = GlanceModifier.fillMaxWidth()) {
                                data.hourlyTemps.take(4).forEachIndexed { i, temp ->
                                    Column(
                                        modifier = GlanceModifier.defaultWeight().padding(4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "${i + 1}h",
                                            style = TextStyle(color = ColorProvider(Color.White.copy(alpha = 0.6f)), fontSize = 10.sp)
                                        )
                                        Text(
                                            text = "$temp°",
                                            style = TextStyle(color = ColorProvider(Color.White), fontSize = 14.sp)
                                        )
                                    }
                                }
                            }
                        }
                    }
            }
        }
    }
}

class LargeWeatherWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = LargeWeatherWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.d("WeatherWidget", "LargeWeatherWidgetReceiver.onUpdate() triggered for ${appWidgetIds.size} widget(s)")
        WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<WeatherRefreshWorker>().build())
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }
}
