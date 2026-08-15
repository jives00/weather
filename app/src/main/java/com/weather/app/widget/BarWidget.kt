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
import com.weather.app.work.WeatherRefreshWorker

class BarWeatherWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetDataStore.getPrimary(context)
        provideContent {
            // No GlanceTheme — avoids Material You dynamic color overriding explicit colors
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 2.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (data == null) {
                    Text("—", style = TextStyle(color = ColorProvider(Color.White), fontSize = 14.sp))
                } else {
                    // Left: weather emoji icon
                    Image(
                        provider = ImageProvider(WeatherDrawableMap.drawableFor(data.condition)),
                        contentDescription = data.conditionDescription,
                        modifier = GlanceModifier.size(30.dp)
                    )

                    Spacer(GlanceModifier.width(8.dp))

                    // Center: temp + location
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = "${data.temperature}${data.temperatureSymbol}",
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = data.locationName,
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                    }

                    Spacer(GlanceModifier.width(6.dp))

                    // Right: condition + H/L
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = data.conditionDescription,
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = "H: ${data.highTemp}°  L: ${data.lowTemp}°",
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

class BarWeatherWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = BarWeatherWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.d("WeatherWidget", "BarWeatherWidgetReceiver.onUpdate() triggered for ${appWidgetIds.size} widget(s)")
        WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<WeatherRefreshWorker>().build())
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }
}
