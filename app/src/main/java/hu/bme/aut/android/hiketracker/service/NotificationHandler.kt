package hu.bme.aut.android.hiketracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import hu.bme.aut.android.hiketracker.R
import hu.bme.aut.android.hiketracker.ui.MainActivity

class NotificationHandler (
    val NOTIF_FOREGROUND_ID: Int,
    val NOTIFICATION_CHANNEL_ID : String,
    val NOTIFICATION_CHANNEL_NAME : String,
    val context: Context
){
    fun createNotification(text: String): Notification? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        val notificationIntent = Intent(context, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(context,
            NOTIF_FOREGROUND_ID,
            notificationIntent,
            PendingIntent.FLAG_CANCEL_CURRENT)

        return NotificationCompat.Builder(
            context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Hike tracking is in progress")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_stat_directions_walk)
            .setVibrate(longArrayOf(1000))
            .setContentIntent(contentIntent).build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW)

        val notificationManager =
            getSystemService(context, NotificationManager::class.java) as NotificationManager?

        notificationManager?.createNotificationChannel(channel)
    }
}