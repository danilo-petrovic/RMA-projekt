package com.example.projekt.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.projekt.R
import kotlin.random.Random

fun showNotification(context: Context, message: String) {
    val channelId = "trip_notifications"
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "Trips",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
    }

    val notification = NotificationCompat.Builder(context, channelId)
        .setContentTitle("JoinMe notification")
        .setContentText(message)
        .setSmallIcon(R.drawable.ic_world_plane)
        .build()

    notificationManager.notify(Random.nextInt(), notification)
}
