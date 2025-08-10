package com.ioannapergamali.mysmartroute.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ioannapergamali.mysmartroute.R

object NotificationUtils {
    private const val CHANNEL_ID = "default_channel"

    fun showNotification(
        context: Context,
        title: String,
        text: String,
        id: Int = 0,
        pendingIntent: PendingIntent? = null
    ) {
        // Δημιουργία καναλιού ειδοποίησης για Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notifications),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        pendingIntent?.let {
            builder.setContentIntent(it)
                .setAutoCancel(true)
        }

        val notification = builder.build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
