package com.jarvis.assistant.reminders

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.jarvis.assistant.MainActivity
import com.jarvis.assistant.R
import com.jarvis.assistant.data.ReminderRepository

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(EXTRA_ID, -1)
        val repository = ReminderRepository(context)
        val item = repository.all().firstOrNull { it.id == id && !it.delivered } ?: return
        if (!alertsEnabled(context)) return

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(
            CHANNEL, "JARVIS reminders", NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Alerts for reminders you explicitly create" })

        val openApp = PendingIntent.getActivity(
            context, id, Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(context, CHANNEL)
            .setContentTitle("JARVIS reminder")
            .setContentText(item.title)
            .setStyle(Notification.BigTextStyle().bigText(item.title))
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setSmallIcon(R.drawable.ic_jarvis)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .setCategory(Notification.CATEGORY_REMINDER)
            .build()
        try {
            manager.notify(id, notification)
            repository.markDelivered(id)
        } catch (_: SecurityException) {
            // Access can be revoked between the check and notify(). Keep it undelivered.
        }
    }

    companion object {
        const val EXTRA_ID = "reminder_id"
        const val CHANNEL = "jarvis_reminder_alerts"

        fun alertsEnabled(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return false
            val manager = context.getSystemService(NotificationManager::class.java)
            return manager.areNotificationsEnabled() &&
                manager.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE
        }
    }
}

class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
        ) ReminderScheduler.rescheduleAll(context)
    }
}
