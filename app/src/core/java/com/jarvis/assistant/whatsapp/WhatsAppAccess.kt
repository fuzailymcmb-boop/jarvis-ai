package com.jarvis.assistant.whatsapp

import android.content.Context
import com.jarvis.assistant.data.WhatsAppMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Core contains no notification listener, notification-reading API, or inline reply action. */
object WhatsAppAccess {
    val messages = MutableStateFlow<List<WhatsAppMessage>>(emptyList()).asStateFlow()

    @Suppress("UNUSED_PARAMETER")
    fun replyToConfirmedMessage(key: String, text: String): WhatsAppReplyResult =
        WhatsAppReplyResult.UNAVAILABLE

    @Suppress("UNUSED_PARAMETER")
    fun openNotificationAccessSettings(context: Context): Boolean = false
}
