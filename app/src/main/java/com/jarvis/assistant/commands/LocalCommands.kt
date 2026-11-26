package com.jarvis.assistant.commands

import java.time.Duration
import java.time.Instant

sealed interface JarvisCommand {
    data class NewReminder(val title: String, val time: Instant) : JarvisCommand
    data object ReadWhatsApp : JarvisCommand
    data class ReplyWhatsApp(val contact: String, val reply: String) : JarvisCommand
    data object OpenWhatsApp : JarvisCommand
    data object ShowReminders : JarvisCommand
    data object Unknown : JarvisCommand
}

/**
 * Only the phone app interprets local commands. Never give an AI model permission
 * to send a message or schedule an alarm from plain text it generated.
 */
object LocalCommands {
    private val after = Regex("^remind me to (.+?) in (\\d{1,4}) (minute|minutes|hour|hours)$", RegexOption.IGNORE_CASE)
    private val before = Regex("^remind me in (\\d{1,4}) (minute|minutes|hour|hours) to (.+)$", RegexOption.IGNORE_CASE)
    private val reply = Regex("^reply to (.+?) (?:saying|with|:|,)\\s*(.+)$", RegexOption.IGNORE_CASE)

    fun parse(raw: String, now: Instant = Instant.now()): JarvisCommand {
        val command = raw.trim().replace(Regex("^(?:hey\\s+)?jarvis[,!:]?\\s*", RegexOption.IGNORE_CASE), "")
            .trim().trimEnd('.', '!', '?')

        after.matchEntire(command)?.let {
            val amount = it.groupValues[2].toLongOrNull() ?: return JarvisCommand.Unknown
            if (amount !in 1..1440) return JarvisCommand.Unknown
            val duration = if (it.groupValues[3].startsWith("hour", true)) Duration.ofHours(amount) else Duration.ofMinutes(amount)
            return JarvisCommand.NewReminder(it.groupValues[1].trim(), now.plus(duration))
        }
        before.matchEntire(command)?.let {
            val amount = it.groupValues[1].toLongOrNull() ?: return JarvisCommand.Unknown
            if (amount !in 1..1440) return JarvisCommand.Unknown
            val duration = if (it.groupValues[2].startsWith("hour", true)) Duration.ofHours(amount) else Duration.ofMinutes(amount)
            return JarvisCommand.NewReminder(it.groupValues[3].trim(), now.plus(duration))
        }
        reply.matchEntire(command)?.let {
            return JarvisCommand.ReplyWhatsApp(it.groupValues[1].trim(), it.groupValues[2].trim())
        }
        val normalized = command.lowercase()
        return when {
            normalized.startsWith("read whatsapp") ||
                normalized.startsWith("read my whatsapp") ||
                normalized.startsWith("read latest whatsapp") ||
                normalized.startsWith("read my latest whatsapp") ||
                normalized.startsWith("any whatsapp") ||
                normalized.startsWith("check whatsapp") -> JarvisCommand.ReadWhatsApp
            normalized == "open whatsapp" -> JarvisCommand.OpenWhatsApp
            normalized == "show reminders" || normalized == "my reminders" -> JarvisCommand.ShowReminders
            else -> JarvisCommand.Unknown
        }
    }
}
