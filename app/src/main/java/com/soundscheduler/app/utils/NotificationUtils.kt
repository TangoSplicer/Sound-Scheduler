// File: app/src/main/java/com/soundscheduler/app/utils/NotificationUtils.kt

package com.soundscheduler.app.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.soundscheduler.app.R

object NotificationUtils {
    
    private const val TAG = "NotificationUtils"
    private const val CHANNEL_ID = "sound_scheduler_channel"

    fun createNotificationChannel(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "Sound Scheduler Alerts"
                val descriptionText = "Notifications for routine triggers"
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }
                
                val notificationManager: NotificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
                
                Log.d(TAG, "Notification channel created successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create notification channel", e)
        }
    }

    fun sendNotification(context: Context, title: String, message: String) {
        try {
            // Validate inputs
            require(title.isNotBlank()) { "Notification title cannot be blank" }
            require(message.isNotBlank()) { "Notification message cannot be blank" }
            
            // Check permissions on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Skipping notification.")
                    return
                }
            }

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            NotificationManagerCompat.from(context)
                .notify(System.currentTimeMillis().toInt(), builder.build())
            
            Log.d(TAG, "Notification sent successfully: $title")
            
            // Track analytics (if implemented)
            UsageAnalyticsManager.incrementNotificationsSent(context)
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when sending notification", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send notification", e)
        }
    }
}

