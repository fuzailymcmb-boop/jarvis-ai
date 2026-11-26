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
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        PageHeading("WHATSAPP", "Recent active notifications · personal + business")
        InfoPanel("PRIVATE BY DESIGN",
            "Android notification access is system-wide. JARVIS filters it to WhatsApp and keeps those notifications in memory only. No chat history is read. Replies require your confirmation.")
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TinyAction("ALLOW ACCESS", Icons.Default.LockOpen, Modifier.weight(1f), onPermission)
            TinyAction("OPEN APP", Icons.Default.OpenInNew, Modifier.weight(1f), onOpen)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onRead, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
            Text("READ NOTIFICATIONS ALOUD")
        }
        Spacer(Modifier.height(10.dp))
        if (messages.isEmpty()) {
            EmptyPanel("NO ACTIVE NOTIFICATIONS", "Grant Android notification access and receive a new WhatsApp message to see it here.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(11.dp),
                contentPadding = PaddingValues(bottom = 16.dp)) {
                items(messages, key = { it.notificationKey }) { msg ->
                    Surface(shape = RoundedCornerShape(16.dp), color = Panel,
                        border = BorderStroke(1.dp, Cyan.copy(alpha = 0.18f))) {
                        Column(Modifier.padding(15.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(msg.sender, color = White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(if (msg.business) "BUSINESS" else "WHATSAPP", color = Cyan, fontSize = 9.sp)
                            }
                            Spacer(Modifier.height(7.dp))
                            Text(msg.text, color = Muted, fontSize = 13.sp, lineHeight = 19.sp, maxLines = 5)
                            Spacer(Modifier.height(11.dp))
                            Button(onClick = { onReply(msg) }, enabled = msg.canReply,
                                colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Background),
                                shape = RoundedCornerShape(10.dp)) {
                                Icon(Icons.Default.Reply, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (msg.canReply) "DRAFT A REPLY" else "REPLY IN WHATSAPP")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun EditionReplyDialog(draft: ReplyDraft, onDismiss: () -> Unit, onSend: (String) -> Unit) {
    var text by remember(draft.key) { mutableStateOf(draft.proposed) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm WhatsApp reply") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("To: ${draft.contact}\nThis will submit a reply through the current WhatsApp notification.", fontSize = 13.sp)
                OutlinedTextField(value = text, onValueChange = { text = it.take(1500) },
                    label = { Text("Message") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                Text("Nothing is sent until you select Confirm & send.", color = Cyan, fontSize = 11.sp)
            }
        },
        confirmButton = {
            Button(onClick = { onSend(text) }, enabled = text.isNotBlank()) { Text("CONFIRM & SEND") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}

@Composable
internal fun EditionNotificationPermission(onOpen: () -> Unit) {
    PermissionRow("WHATSAPP NOTIFICATION ACCESS",
        "Read current notifications and use supported quick replies", onOpen)
}
