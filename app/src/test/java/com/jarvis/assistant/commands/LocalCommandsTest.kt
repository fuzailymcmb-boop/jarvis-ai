package com.jarvis.assistant.commands

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class LocalCommandsTest {
    private val now = Instant.parse("2026-11-14T14:00:00Z")

    @Test fun reminderWithDuration() {
        val command = LocalCommands.parse("Hey Jarvis, remind me to call John in 10 minutes", now)
        assertEquals(JarvisCommand.NewReminder("call John", now.plusSeconds(600)), command)
    }

    @Test fun reminderWithTimeFirst() {
        val command = LocalCommands.parse("Remind me in 2 hours to collect tents", now)
        assertEquals(JarvisCommand.NewReminder("collect tents", now.plusSeconds(7200)), command)
    }

    @Test fun whatsappReplyDraftIsNotSentByParser() {
        val command = LocalCommands.parse("Reply to John saying I will call soon", now)
        assertEquals(JarvisCommand.ReplyWhatsApp("John", "I will call soon"), command)
    }

    @Test fun ambiguousReminderIsRejected() {
        assertEquals(JarvisCommand.Unknown, LocalCommands.parse("remind me about John tomorrow", now))
    }
}
