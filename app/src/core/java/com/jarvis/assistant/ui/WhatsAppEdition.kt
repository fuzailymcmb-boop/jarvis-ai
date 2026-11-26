package com.jarvis.assistant.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.assistant.data.WhatsAppMessage

@Suppress("UNUSED_PARAMETER")
@Composable
internal fun EditionWhatsAppScreen(
    messages: List<WhatsAppMessage>, onRead: () -> Unit,
    onPermission: () -> Unit, onOpen: () -> Unit, onReply: (WhatsAppMessage) -> Unit,
    message: String, onMessageChange: (String) -> Unit, onShare: (String) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)
        .verticalScroll(androidx.compose.foundation.rememberScrollState())) {
        PageHeading("WHATSAPP", "Core edition · manual message drafts")
        InfoPanel("YOU REVIEW AND SEND",
            "Core cannot read WhatsApp messages or notifications and cannot reply automatically. " +
                "Write a draft here, then choose the recipient and press Send inside WhatsApp.")
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = message,
            onValueChange = { onMessageChange(it.take(1500)) },
            label = { Text("Your message draft") },
            placeholder = { Text("Write the message you want to review in WhatsApp") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            maxLines = 8
        )
        Text("${message.length}/1500 characters", color = Muted, fontSize = 10.sp,
            modifier = Modifier.padding(top = 5.dp))
        Spacer(Modifier.height(12.dp))
        Button(onClick = { onShare(message) }, enabled = message.isNotBlank(),
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("REVIEW IN WHATSAPP")
        }
        OutlinedButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
            Text("OPEN WHATSAPP")
        }
        Spacer(Modifier.height(12.dp))
        Text("JARVIS does not select a recipient or send the draft. " +
            "You complete those steps in WhatsApp.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
        Spacer(Modifier.height(24.dp))
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
internal fun EditionNotificationPermission(onOpen: () -> Unit) = Unit

@Suppress("UNUSED_PARAMETER")
@Composable
internal fun EditionReplyDialog(draft: ReplyDraft, onDismiss: () -> Unit, onSend: (String) -> Unit) = Unit
