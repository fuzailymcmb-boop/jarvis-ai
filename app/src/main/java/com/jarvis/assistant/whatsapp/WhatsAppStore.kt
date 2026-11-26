package com.jarvis.assistant.whatsapp

import com.jarvis.assistant.data.WhatsAppMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory only: message content is never written to local files or uploaded. */
object WhatsAppStore {
    private val mutable = MutableStateFlow<List<WhatsAppMessage>>(emptyList())
    val messages = mutable.asStateFlow()

    fun upsert(item: WhatsAppMessage) {
        mutable.value = (mutable.value.filterNot { it.notificationKey == item.notificationKey } + item)
            .sortedByDescending { it.whenMillis }
            .take(30)
    }

    fun remove(key: String) {
        mutable.value = mutable.value.filterNot { it.notificationKey == key }
    }

    fun clear() { mutable.value = emptyList() }
}
