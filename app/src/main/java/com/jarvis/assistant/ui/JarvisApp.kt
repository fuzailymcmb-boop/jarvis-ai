package com.jarvis.assistant.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.assistant.data.ChatLine
import com.jarvis.assistant.data.Reminder
import com.jarvis.assistant.data.Speaker
import com.jarvis.assistant.data.WhatsAppMessage
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

private val Background = Color(0xFF050E19)
private val Panel = Color(0xFF0E2232)
private val Cyan = Color(0xFF24DBEC)
private val Indigo = Color(0xFF788CFF)
private val Muted = Color(0xFF92ACBF)
private val White = Color(0xFFEAFBFF)

enum class JarvisTab { HOME, WHATSAPP, REMINDERS, SETTINGS }
data class ReplyDraft(val key: String, val contact: String, val proposed: String)

@Composable
fun JarvisApp(
    activeTab: JarvisTab,
    onChangeTab: (JarvisTab) -> Unit,
    conversation: List<ChatLine>,
    reminders: List<Reminder>,
    whatsApp: List<WhatsAppMessage>,
    status: String,
    listening: Boolean,
    thinking: Boolean,
    onMic: () -> Unit,
    onText: (String) -> Unit,
    onAddReminder: (String, Long) -> Unit,
    onDeleteReminder: (Reminder) -> Unit,
    onReadWhatsApp: () -> Unit,
    onOpenWhatsApp: () -> Unit,
    onRequestReply: (WhatsAppMessage) -> Unit,
    pendingReply: ReplyDraft?,
    onDismissReply: () -> Unit,
    onConfirmReply: (String) -> Unit,
    backendUrl: String,
    hasToken: Boolean,
    speakReplies: Boolean,
    onSaveConnection: (String, String) -> Unit,
    onClearToken: () -> Unit,
    onToggleSpeech: (Boolean) -> Unit,
    onRequestNotifications: () -> Unit,
    onOpenNotificationAccess: () -> Unit,
    onRequestExactAlarms: () -> Unit,
    exactRemindersEnabled: Boolean
) {
    MaterialTheme(colorScheme = darkColorScheme(
        primary = Cyan,
        onPrimary = Background,
        background = Background,
        onBackground = White,
        surface = Panel,
        onSurface = White,
        secondary = Indigo,
    )) {
        Column(
            modifier = Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(Background, Color(0xFF091D2C), Background)))
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            Header(status)
            Box(Modifier.weight(1f)) {
                when (activeTab) {
                    JarvisTab.HOME -> HomeScreen(conversation, listening, thinking, onMic, onText,
                        onReadWhatsApp, { onChangeTab(JarvisTab.REMINDERS) })
                    JarvisTab.WHATSAPP -> WhatsAppScreen(whatsApp, onReadWhatsApp,
                        onOpenNotificationAccess, onOpenWhatsApp, onRequestReply)
                    JarvisTab.REMINDERS -> RemindersScreen(reminders, onAddReminder, onDeleteReminder)
                    JarvisTab.SETTINGS -> SettingsScreen(backendUrl, hasToken, speakReplies,
                        onSaveConnection, onClearToken, onToggleSpeech, onRequestNotifications,
                        onOpenNotificationAccess, onRequestExactAlarms, exactRemindersEnabled)
                }
            }
            BottomBar(activeTab, onChangeTab)
        }

        if (pendingReply != null) {
            ReplyConfirmationDialog(pendingReply, onDismissReply, onConfirmReply)
        }
    }
}

@Composable
private fun Header(status: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(37.dp).clip(CircleShape).background(Cyan.copy(alpha = 0.12f))
                .border(1.dp, Cyan, CircleShape), contentAlignment = Alignment.Center) {
                Text("J", fontWeight = FontWeight.Black, fontSize = 22.sp, color = Cyan)
            }
            Column {
                Text("JARVIS", color = White, fontSize = 21.sp, letterSpacing = 4.sp, fontWeight = FontWeight.Bold)
                Text("PERSONAL AI ASSISTANT", color = Muted, fontSize = 9.sp, letterSpacing = 1.5.sp)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(Modifier.size(7.dp).background(Cyan, CircleShape))
            Text("READY", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
    HorizontalDivider(color = Cyan.copy(alpha = 0.15f))
    Text(status.uppercase(), color = Muted, fontSize = 10.sp, letterSpacing = 1.8.sp,
        modifier = Modifier.padding(start = 22.dp, top = 8.dp, bottom = 3.dp), maxLines = 2)
}

@Composable
private fun HomeScreen(
    conversation: List<ChatLine>, listening: Boolean, thinking: Boolean,
    onMic: () -> Unit, onText: (String) -> Unit, onReadWhatsApp: () -> Unit, onReminders: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    val state = rememberLazyListState()
    LaunchedEffect(conversation.size) {
        if (conversation.isNotEmpty()) state.animateScrollToItem(conversation.size - 1)
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                JarvisOrb(listening, thinking, onMic)
                Text(when { listening -> "LISTENING"; thinking -> "THINKING"; else -> "AWAITING COMMAND" },
                    color = Cyan, letterSpacing = 3.sp, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                Spacer(Modifier.height(7.dp))
                Text("Tap the core and speak", color = Muted, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            TinyAction("READ WHATSAPP", Icons.Default.Chat, Modifier.weight(1f), onReadWhatsApp)
            TinyAction("REMINDERS", Icons.Default.Schedule, Modifier.weight(1f), onReminders)
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn(
            state = state,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(conversation) { ChatBubble(it) }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            OutlinedTextField(
                value = input, onValueChange = { input = it.take(2000) },
                modifier = Modifier.weight(1f), singleLine = true,
                placeholder = { Text("Type a command…", color = Muted) },
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Cyan, unfocusedBorderColor = Cyan.copy(alpha = 0.35f),
                    focusedContainerColor = Panel, unfocusedContainerColor = Panel),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (input.isNotBlank()) { onText(input); input = "" }
                })
            )
            IconButton(
                onClick = { if (input.isNotBlank()) { onText(input); input = "" } },
                modifier = Modifier.size(50.dp).clip(CircleShape).background(Cyan)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Background)
            }
        }
    }
}

@Composable
private fun JarvisOrb(listening: Boolean, thinking: Boolean, onTap: () -> Unit) {
    val spin = rememberInfiniteTransition(label = "orb")
    val turn by spin.animateFloat(0f, 360f,
        animationSpec = infiniteRepeatable(tween(12500, easing = LinearEasing)), label = "rotation")
    val pulse by spin.animateFloat(0.55f, 1f,
        animationSpec = infiniteRepeatable(tween(1700), repeatMode = androidx.compose.animation.core.RepeatMode.Reverse),
        label = "pulse")
    Box(Modifier.size(205.dp).clickable(onClick = onTap), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val mid = center
            val large = size.minDimension * 0.455f
            val small = size.minDimension * 0.32f
            drawCircle(Cyan.copy(alpha = 0.06f), large)
            drawCircle(Cyan.copy(alpha = 0.36f * pulse), large, style = Stroke(1.5.dp.toPx()))
            drawCircle(Indigo.copy(alpha = 0.5f), small, style = Stroke(1.dp.toPx()))
            drawArc(Cyan, turn, 105f, false,
                topLeft = Offset(mid.x - large, mid.y - large),
                size = Size(large * 2, large * 2),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
            drawArc(Indigo, -turn * 1.4f, 80f, false,
                topLeft = Offset(mid.x - small, mid.y - small),
                size = Size(small * 2, small * 2),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            drawCircle(Cyan.copy(alpha = if (listening) 0.25f else 0.10f),
                size.minDimension * 0.24f)
        }
        Icon(if (thinking) Icons.Default.AutoAwesome else Icons.Default.Mic,
            contentDescription = "Tap to speak to JARVIS", tint = Cyan,
            modifier = Modifier.size(51.dp))
    }
}

@Composable
private fun ChatBubble(item: ChatLine) {
    val mine = item.speaker == Speaker.USER
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Surface(
            shape = RoundedCornerShape(15.dp),
            color = if (mine) Color(0xFF153F5E) else Panel,
            border = BorderStroke(1.dp, if (mine) Indigo.copy(alpha = 0.30f) else Cyan.copy(alpha = 0.26f)),
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (mine) "YOU" else "JARVIS", color = if (mine) Indigo else Cyan,
                    fontWeight = FontWeight.Bold, fontSize = 9.sp, letterSpacing = 1.5.sp)
                Text(item.text, fontSize = 13.sp, color = White, lineHeight = 19.sp)
            }
        }
    }
}

@Composable
private fun TinyAction(label: String, icon: ImageVector, modifier: Modifier = Modifier, click: () -> Unit) {
    Surface(onClick = click, shape = RoundedCornerShape(12.dp), color = Panel,
        border = BorderStroke(1.dp, Cyan.copy(alpha = 0.28f)), modifier = modifier) {
        Row(Modifier.padding(horizontal = 11.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = Cyan)
            Spacer(Modifier.width(7.dp))
            Text(label, color = White, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
        }
    }
}

@Composable
private fun WhatsAppScreen(
    messages: List<WhatsAppMessage>, onRead: () -> Unit,
    onPermission: () -> Unit, onOpen: () -> Unit, onReply: (WhatsAppMessage) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        PageHeading("WHATSAPP", "Recent active notifications · personal + business")
        InfoPanel("PRIVATE BY DESIGN",
            "Only WhatsApp notifications you explicitly grant access to are displayed. No chat history is read or stored. Replies require your confirmation.")
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
private fun RemindersScreen(
    reminders: List<Reminder>, onAddReminder: (String, Long) -> Unit, onDeleteReminder: (Reminder) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var dueAt by remember { mutableLongStateOf(System.currentTimeMillis() + 60 * 60 * 1000) }
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        PageHeading("REMINDERS", "Local alerts · no cloud storage")
        Surface(color = Panel, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Cyan.copy(alpha = 0.20f))) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("CREATE A REMINDER", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                OutlinedTextField(value = title, onValueChange = { title = it.take(160) },
                    label = { Text("What should I remind you about?") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                Surface(onClick = {
                    val calendar = Calendar.getInstance().apply { timeInMillis = dueAt }
                    val year = calendar.get(Calendar.YEAR)
                    val month = calendar.get(Calendar.MONTH)
                    val day = calendar.get(Calendar.DAY_OF_MONTH)
                    DatePickerDialog(context, { _, y, mo, d ->
                        calendar.set(y, mo, d)
                        TimePickerDialog(context, { _, h, min ->
                            calendar.set(Calendar.HOUR_OF_DAY, h)
                            calendar.set(Calendar.MINUTE, min)
                            calendar.set(Calendar.SECOND, 0)
                            calendar.set(Calendar.MILLISECOND, 0)
                            dueAt = calendar.timeInMillis
                        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
                    }, year, month, day).show()
                }, color = Color(0xFF142B3F), shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, null, tint = Cyan)
                        Spacer(Modifier.width(10.dp))
                        Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(dueAt)),
                            color = White, modifier = Modifier.weight(1f), fontSize = 13.sp)
                        Icon(Icons.Default.Edit, "Select date and time", tint = Muted, modifier = Modifier.size(17.dp))
                    }
                }
                Button(onClick = { if (title.isNotBlank()) { onAddReminder(title, dueAt); title = "" } },
                    modifier = Modifier.fillMaxWidth(), enabled = title.isNotBlank(),
                    shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Default.AddAlarm, null)
                    Spacer(Modifier.width(8.dp))
                    Text("SAVE REMINDER", fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("YOUR REMINDERS (${reminders.size})", color = Muted, fontSize = 11.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(10.dp))
        if (reminders.isEmpty()) EmptyPanel("NO REMINDERS YET", "Create one above or say: 'Remind me to call John in 10 minutes'.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)) {
            items(reminders, key = { it.id }) { item ->
                Surface(color = Panel, shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, null, tint = if (item.delivered) Muted else Cyan)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.title, color = White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(item.dueAtMillis)) +
                                when {
                                    item.delivered -> " · ALERT POSTED"
                                    item.dueAtMillis <= System.currentTimeMillis() -> " · PAST DUE"
                                    else -> " · SCHEDULED"
                                },
                                color = Muted, fontSize = 10.sp)
                        }
                        IconButton(onClick = { onDeleteReminder(item) }) {
                            Icon(Icons.Default.DeleteOutline, "Delete reminder", tint = Muted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    backendUrl: String, hasToken: Boolean, speakReplies: Boolean,
    onSave: (String, String) -> Unit, onClearToken: () -> Unit, onToggleSpeech: (Boolean) -> Unit,
    onNotifications: () -> Unit, onWhatsApp: () -> Unit,
    onExactAlarms: () -> Unit, exact: Boolean
) {
    var url by remember(backendUrl) { mutableStateOf(backendUrl) }
    var token by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
        PageHeading("SETTINGS", "Access controls & secure AI connection")
        InfoPanel("AI SERVER", "JARVIS connects to your own backend. Your OpenAI API key stays on the server, never inside the Android app.")
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = url, onValueChange = { url = it },
            label = { Text("Backend URL") }, placeholder = { Text("https://your-domain/api/chat") },
            modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(9.dp))
        OutlinedTextField(value = token, onValueChange = { token = it },
            label = { Text(if (hasToken) "Access token (saved — leave blank to keep)" else "Server access token") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(), singleLine = true)
        Text("Changing the server URL clears its old token unless you enter a new one.",
            color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(9.dp))
        Button(onClick = { onSave(url, token); token = "" }, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)) { Text("SAVE AI CONNECTION") }
        if (hasToken) TextButton(onClick = onClearToken) { Text("Delete saved access token", color = Muted) }
        Spacer(Modifier.height(10.dp))
        Surface(color = Panel, shape = RoundedCornerShape(14.dp)) {
            Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VolumeUp, null, tint = Cyan)
                Spacer(Modifier.width(11.dp))
                Text("Speak responses aloud", color = White, modifier = Modifier.weight(1f), fontSize = 13.sp)
                Switch(checked = speakReplies, onCheckedChange = onToggleSpeech)
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("ANDROID PERMISSIONS", color = Cyan, letterSpacing = 1.6.sp, fontSize = 11.sp,
            fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        PermissionRow("WHATSAPP NOTIFICATION ACCESS", "Read current notifications and use supported quick replies", onWhatsApp)
        PermissionRow("REMINDER NOTIFICATIONS", "Allow JARVIS to display reminder alerts", onNotifications)
        PermissionRow("EXACT ALARM ACCESS", if (exact) "Enabled" else "Not enabled; alarms may be delayed", onExactAlarms)
        Spacer(Modifier.height(14.dp))
        InfoPanel("SAFETY", "No automatic sending. No chat-history scraping. No continuous microphone recording. Your recent notifications remain in memory only and are never sent to the AI server.")
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun PermissionRow(label: String, detail: String, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Panel, shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Security, null, tint = Cyan)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(label, color = White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(detail, color = Muted, fontSize = 10.sp)
            }
            Icon(Icons.Default.ChevronRight, "Open settings", tint = Muted)
        }
    }
}

@Composable
private fun ReplyConfirmationDialog(draft: ReplyDraft, onDismiss: () -> Unit, onSend: (String) -> Unit) {
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
private fun PageHeading(title: String, subtitle: String) {
    Spacer(Modifier.height(12.dp))
    Text(title, color = White, fontSize = 23.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
    Text(subtitle, color = Muted, fontSize = 12.sp)
    Spacer(Modifier.height(17.dp))
}

@Composable
private fun InfoPanel(title: String, text: String) {
    Surface(color = Cyan.copy(alpha = 0.07f), shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Cyan.copy(alpha = 0.2f))) {
        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = Cyan, fontSize = 10.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
            Text(text, color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun EmptyPanel(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Sensors, null, tint = Cyan, modifier = Modifier.size(38.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, color = White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun BottomBar(selected: JarvisTab, onChange: (JarvisTab) -> Unit) {
    HorizontalDivider(color = Cyan.copy(alpha = 0.2f))
    Row(Modifier.fillMaxWidth().background(Color(0xFF071522)).padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceEvenly) {
        listOf(
            Triple(JarvisTab.HOME, "JARVIS", Icons.Default.Home),
            Triple(JarvisTab.WHATSAPP, "WHATSAPP", Icons.Default.Chat),
            Triple(JarvisTab.REMINDERS, "REMINDERS", Icons.Default.Notifications),
            Triple(JarvisTab.SETTINGS, "SETTINGS", Icons.Default.Settings)
        ).forEach { (tab, label, icon) ->
            val active = selected == tab
            Column(Modifier.weight(1f).clickable { onChange(tab) }.padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, contentDescription = label, tint = if (active) Cyan else Muted,
                    modifier = Modifier.size(22.dp))
                Spacer(Modifier.height(4.dp))
                Text(label, color = if (active) Cyan else Muted, fontSize = 8.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}
