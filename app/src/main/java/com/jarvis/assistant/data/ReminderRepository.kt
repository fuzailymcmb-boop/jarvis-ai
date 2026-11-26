package com.jarvis.assistant.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Reminders are stored only on the device, in app-private SharedPreferences. */
class ReminderRepository(context: Context) {
    private val prefs = context.getSharedPreferences("jarvis_reminders_v1", Context.MODE_PRIVATE)

    @Synchronized
    fun all(): List<Reminder> = try {
        val array = JSONArray(prefs.getString("reminders", "[]"))
        (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            Reminder(
                id = obj.getInt("id"),
                title = obj.getString("title"),
                dueAtMillis = obj.getLong("dueAtMillis"),
                delivered = obj.optBoolean("delivered", false)
            )
        }.sortedBy { it.dueAtMillis }
    } catch (_: Exception) { emptyList() }

    @Synchronized
    fun add(title: String, whenMillis: Long): Reminder {
        require(title.isNotBlank() && title.length <= 160)
        require(whenMillis > System.currentTimeMillis())
        val nextId = prefs.getInt("nextId", 1)
        val reminder = Reminder(nextId, title.trim(), whenMillis)
        write(all() + reminder, nextId + 1)
        return reminder
    }

    @Synchronized
    fun delete(id: Int) { write(all().filterNot { it.id == id }) }

    @Synchronized
    fun markDelivered(id: Int) {
        write(all().map { if (it.id == id) it.copy(delivered = true) else it })
    }

    @Synchronized
    private fun write(list: List<Reminder>, nextId: Int = prefs.getInt("nextId", 1)) {
        val json = JSONArray()
        list.forEach { reminder ->
            json.put(JSONObject()
                .put("id", reminder.id)
                .put("title", reminder.title)
                .put("dueAtMillis", reminder.dueAtMillis)
                .put("delivered", reminder.delivered))
        }
        prefs.edit().putString("reminders", json.toString()).putInt("nextId", nextId).commit()
    }
}
