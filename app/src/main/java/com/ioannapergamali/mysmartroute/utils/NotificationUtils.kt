package com.ioannapergamali.mysmartroute.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.NotificationEntity
import com.ioannapergamali.mysmartroute.data.local.currentAppDateTime
import com.ioannapergamali.mysmartroute.utils.SessionManager
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Απλοποιεί τη δημιουργία ειδοποιήσεων.
 * Simplifies building notifications.
 */
object NotificationUtils {
    private const val CHANNEL_ID = "default_channel"
    private const val TAG = "NotificationUtils"

    fun showNotification(
        context: Context,
        title: String,
        text: String,
        id: Int = 0,
        pendingIntent: PendingIntent? = null,
        storeInRoom: Boolean = false,
        receiverId: String? = SessionManager.currentUserId(),
        senderId: String = SessionManager.currentUserId().orEmpty(),
        roomNotificationId: String? = null
    ) {
        // Δημιουργία καναλιού ειδοποίησης για Android 8+
        // Create notification channel for Android 8+
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

        if (storeInRoom) {
            val targetReceiver = receiverId ?: SessionManager.currentUserId()
            if (!targetReceiver.isNullOrBlank()) {
                CoroutineScope(Dispatchers.IO).launch {
                    runCatching {
                        val database = MySmartRouteDatabase.getInstance(context)
                        val now = runCatching { database.currentAppDateTime() }
                            .getOrElse { LocalDateTime.now() }
                        val entity = NotificationEntity(
                            id = roomNotificationId ?: UUID.randomUUID().toString(),
                            senderId = senderId,
                            receiverId = targetReceiver,
                            message = text,
                            sentDate = now.toLocalDate().toString(),
                            sentTime = now.toLocalTime().withSecond(0).withNano(0).toString()
                        )
                        database.notificationDao().insert(entity)
                    }.onFailure { error ->
                        Log.e(TAG, "Failed to store notification", error)
                    }
                }
            }
        }
    }
}
