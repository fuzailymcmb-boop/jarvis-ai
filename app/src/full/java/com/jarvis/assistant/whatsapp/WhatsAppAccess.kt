package com.jarvis.assistant.whatsapp

import android.content.Context
import android.content.Intent
import android.provider.Settings

/** Notification access exists only in the explicitly named Full edition. */
object WhatsAppAccess {
    val messages = WhatsAppStore.messages

    fun replyToConfirmedMessage(key: String, text: String): WhatsAppReplyResult =
        WhatsAppListener.replyToConfirmedMessage(key, text)

    fun openNotificationAccessSettings(context: Context): Boolean = try {
        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        true
    } catch (_: Exception) {
        false
    }
}
