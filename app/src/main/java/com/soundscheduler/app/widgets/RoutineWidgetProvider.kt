package com.soundscheduler.app.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.soundscheduler.app.MainActivity
import com.soundscheduler.app.R
import com.soundscheduler.app.utils.PremiumManager

class RoutineWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        if (!PremiumManager.isPremium()) {
            // Do not display widget functionality for free users
            return
        }

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_routine)

            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            views.setTextViewText(R.id.widget_text, "Sound Scheduler\nTap to view routines")

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}