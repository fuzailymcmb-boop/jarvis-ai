package com.jarvis.assistant.data

enum class Speaker { USER, JARVIS }

data class ChatLine(val speaker: Speaker, val text: String)

data class Reminder(
    val id: Int,
    val title: String,
    val dueAtMillis: Long,
    val delivered: Boolean = false
)

data class WhatsAppMessage(
    val notificationKey: String,
    val sender: String,
    val text: String,
    val whenMillis: Long,
    val canReply: Boolean,
    val business: Boolean
)

data class AiTurn(val role: String, val content: String)
