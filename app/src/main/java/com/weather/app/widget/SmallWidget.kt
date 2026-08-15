package com.weather.app.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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

class SmallWeatherWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetDataStore.getPrimary(context)
        provideContent {
            val bgTop = if (data != null) WidgetColors.gradientTopForCondition(data.condition, data.isDay) else Color(0xFF1565C0)
            Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(bgTop)
                        .clickable(actionStartActivity<MainActivity>()),
                    contentAlignment = Alignment.Center
                ) {
                    if (data == null) {
                        Text("—", style = TextStyle(color = ColorProvider(Color.White), fontSize = 22.sp))
                    } else {
                        val updatedTime = if (data.lastRefreshedAt > 0L)
                            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(data.lastRefreshedAt))
                        else "—"
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                provider = ImageProvider(WeatherDrawableMap.drawableFor(data.condition)),
                                contentDescription = null,
                                modifier = GlanceModifier.size(28.dp)
                            )
                            Text(
                                text = "${data.temperature}${data.temperatureSymbol}",
                                style = TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = updatedTime,
                                style = TextStyle(
                                    color = ColorProvider(Color.White.copy(alpha = 0.6f)),
                                    fontSize = 9.sp
                                )
                            )
                        }
                    }
            }
        }
    }
}

class SmallWeatherWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = SmallWeatherWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.d("WeatherWidget", "SmallWeatherWidgetReceiver.onUpdate() triggered for ${appWidgetIds.size} widget(s)")
        WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<WeatherRefreshWorker>().build())
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }
}
