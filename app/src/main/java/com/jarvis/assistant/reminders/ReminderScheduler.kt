package com.jarvis.assistant.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.jarvis.assistant.data.Reminder
import com.jarvis.assistant.data.ReminderRepository

object ReminderScheduler {
    fun canBeExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    /** Always schedules; falls back to an inexact alarm if special access is not granted. */
    fun schedule(context: Context, item: Reminder): Boolean {
        if (item.delivered || item.dueAtMillis <= System.currentTimeMillis()) return false
        val manager = context.getSystemService(AlarmManager::class.java)
        val alarm = pendingIntent(context, item.id)
        val exact = canBeExact(context)
        try {
            if (exact) manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, item.dueAtMillis, alarm)
            else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, item.dueAtMillis, alarm)
        } catch (_: SecurityException) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, item.dueAtMillis, alarm)
            return false
        }
        return exact
    }

    fun cancel(context: Context, id: Int) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context, id))
    }

    fun rescheduleAll(context: Context) {
        ReminderRepository(context).all()
            .filter { !it.delivered && it.dueAtMillis > System.currentTimeMillis() }
            .forEach { schedule(context, it) }
    }

    private fun pendingIntent(context: Context, id: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, id,
            Intent(context, ReminderReceiver::class.java).putExtra(ReminderReceiver.EXTRA_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
