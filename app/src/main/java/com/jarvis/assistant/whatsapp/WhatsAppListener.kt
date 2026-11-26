package com.jarvis.assistant.whatsapp

import android.app.Notification
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.jarvis.assistant.data.WhatsAppMessage

/**
 * Uses only the public Android notification listener and the actions WhatsApp
 * itself exposes. No chat database access, scraping, accessibility automation,
 * unofficial WhatsApp API, or unapproved sends.
 */
class WhatsAppListener : NotificationListenerService() {
    override fun onListenerConnected() {
        activeInstance = this
        WhatsAppStore.clear()
        try { activeNotifications?.forEach(::readNotification) } catch (_: SecurityException) { }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        readNotification(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        WhatsAppStore.remove(sbn.key)
    }

    override fun onListenerDisconnected() {
        if (activeInstance === this) activeInstance = null
        WhatsAppStore.clear()
    }

    override fun onDestroy() {
        if (activeInstance === this) activeInstance = null
        WhatsAppStore.clear()
        super.onDestroy()
    }

    private fun readNotification(sbn: StatusBarNotification) {
        if (sbn.packageName !in PACKAGES) return
        val notification = sbn.notification
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val sender = notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val message = (notification.extras.getCharSequence(Notification.EXTRA_TEXT)
            ?: notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT))?.toString()?.trim().orEmpty()

        if (sender.isBlank() || message.isBlank()) return
        if (message.length > 4000) return  // avoid huge or malformed notification content
        val supportsReply = notification.actions?.any { action ->
            action.remoteInputs?.any { it.allowFreeFormInput } == true
        } == true
        WhatsAppStore.upsert(WhatsAppMessage(
            notificationKey = sbn.key,
            sender = sender.take(120),
            text = message,
            whenMillis = sbn.postTime,
            canReply = supportsReply,
            business = sbn.packageName == "com.whatsapp.w4b"
        ))
    }

    private fun submitReply(key: String, replyText: String): ReplyResult {
        if (replyText.isBlank() || replyText.length > 1500) return ReplyResult.INVALID
        val sbn = try { activeNotifications?.firstOrNull { it.key == key } }
        catch (_: SecurityException) { null } ?: return ReplyResult.NOT_FOUND
        if (sbn.packageName !in PACKAGES) return ReplyResult.NOT_FOUND

        val action = sbn.notification.actions?.firstOrNull { action ->
            action.remoteInputs?.any { it.allowFreeFormInput } == true
        } ?: return ReplyResult.NO_REPLY_ACTION
        val inputs = action.remoteInputs ?: return ReplyResult.NO_REPLY_ACTION
        val payload = Bundle()
        inputs.filter { it.allowFreeFormInput }.forEach { payload.putCharSequence(it.resultKey, replyText) }
        val replyIntent = Intent()
        RemoteInput.addResultsToIntent(inputs, replyIntent, payload)
        return try {
            action.actionIntent.send(this, 0, replyIntent)
            ReplyResult.SUBMITTED
        } catch (_: Exception) { ReplyResult.ERROR }
    }

    enum class ReplyResult { SUBMITTED, NOT_FOUND, NO_REPLY_ACTION, INVALID, ERROR, NOT_CONNECTED }

    companion object {
        private val PACKAGES = setOf("com.whatsapp", "com.whatsapp.w4b")

        @Volatile private var activeInstance: WhatsAppListener? = null

        /** Must only be called AFTER the user taps the confirmation button. */
        fun replyToConfirmedMessage(notificationKey: String, text: String): ReplyResult =
            activeInstance?.submitReply(notificationKey, text) ?: ReplyResult.NOT_CONNECTED
    }
}
