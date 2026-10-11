package com.deniscerri.ytdl.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.deniscerri.ytdl.MainActivity
import com.deniscerri.ytdl.R

/** Static shortcuts: no background polling, network or access to private download titles. */
class SolidWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { widgetId ->
            val views = RemoteViews(context.packageName, R.layout.solid_widget)
            listOf(R.id.widget_open to null, R.id.widget_library to "Downloads", R.id.widget_queue to "Queue")
                .forEachIndexed { index, (viewId, destination) ->
                    val intent = Intent(context, MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        destination?.let { putExtra("destination", it) }
                    }
                    views.setOnClickPendingIntent(viewId, PendingIntent.getActivity(
                        context, widgetId * 3 + index, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    ))
                }
            manager.updateAppWidget(widgetId, views)
        }
    }
}
