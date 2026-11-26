package com.jarvis.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import com.jarvis.assistant.ai.AiClient
import com.jarvis.assistant.audio.VoiceController
import com.jarvis.assistant.commands.JarvisCommand
import com.jarvis.assistant.commands.LocalCommands
import com.jarvis.assistant.data.*
import com.jarvis.assistant.reminders.ReminderScheduler
import com.jarvis.assistant.reminders.ReminderReceiver
import com.jarvis.assistant.security.BackendEndpoint
import com.jarvis.assistant.security.SecureTokenStore
import com.jarvis.assistant.ui.JarvisApp
import com.jarvis.assistant.ui.JarvisTab
import com.jarvis.assistant.ui.ReplyDraft
import com.jarvis.assistant.whatsapp.WhatsAppAccess
import com.jarvis.assistant.whatsapp.WhatsAppReplyResult
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    private val conversation = mutableStateListOf(
        ChatLine(Speaker.JARVIS, if (BuildConfig.NOTIFICATION_ACCESS_ENABLED)
            "I'm JARVIS Full. Tap the microphone to speak, or type below. Try: 'Remind me to call John in 10 minutes'."
            else "I'm JARVIS Core: voice, AI chat and reminders. I cannot read WhatsApp notifications or reply automatically. You can draft a message and review it inside WhatsApp.")
    )
    private val reminders = mutableStateListOf<Reminder>()
    private var currentTab by mutableStateOf(JarvisTab.HOME)
    private var status by mutableStateOf("SYSTEM ONLINE")
    private var listening by mutableStateOf(false)
    private var thinking by mutableStateOf(false)
    private var draft by mutableStateOf<ReplyDraft?>(null)
    private var backendUrl by mutableStateOf("")
    private var hasBackendToken by mutableStateOf(false)
    private var speakReplies by mutableStateOf(true)
    private var exactRemindersEnabled by mutableStateOf(false)
    private var manualWhatsAppDraft by mutableStateOf("")

    private lateinit var voice: VoiceController
    private lateinit var repo: ReminderRepository
    private lateinit var secrets: SecureTokenStore
    private val remoteHistory = mutableListOf<AiTurn>()

    private val microphonePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) voice.listen() else respond("Microphone permission was not granted. You can still type to JARVIS.") }

    private val notificationsPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        respond(if (granted) "Reminder notifications enabled." else "Notifications are disabled. A reminder may be saved without showing an alert.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = ReminderRepository(this)
        secrets = SecureTokenStore(this)
        val settings = getSharedPreferences("jarvis_settings", MODE_PRIVATE)
        backendUrl = settings.getString("backend_url", "") ?: ""
        hasBackendToken = !secrets.read().isNullOrBlank()
        speakReplies = settings.getBoolean("speak_replies", true)
        refreshReminders()

        voice = VoiceController(
            this,
            onText = ::handleText,
            onStatus = { status = it },
            onListening = { listening = it }
        )

        setContent {
            val messages by WhatsAppAccess.messages.collectAsState()
            JarvisApp(
                activeTab = currentTab,
                onChangeTab = { currentTab = it },
                conversation = conversation,
                reminders = reminders,
                whatsApp = messages,
                notificationAccessEnabled = BuildConfig.NOTIFICATION_ACCESS_ENABLED,
                manualWhatsAppDraft = manualWhatsAppDraft,
                onManualWhatsAppDraftChange = { manualWhatsAppDraft = it.take(1500) },
                onShareWhatsAppDraft = ::shareWhatsAppDraft,
                status = status,
                listening = listening,
                thinking = thinking,
                onMic = ::onMicrophoneTapped,
                onText = ::handleText,
                onAddReminder = ::addReminder,
                onDeleteReminder = ::deleteReminder,
                onReadWhatsApp = ::readWhatsApp,
                onOpenWhatsApp = ::openWhatsApp,
                onRequestReply = { msg ->
                    if (BuildConfig.NOTIFICATION_ACCESS_ENABLED) {
                        draft = ReplyDraft(msg.notificationKey, msg.sender, "")
                    }
                },
                pendingReply = draft,
                onDismissReply = { draft = null },
                onConfirmReply = ::confirmReply,
                backendUrl = backendUrl,
                hasToken = hasBackendToken,
                speakReplies = speakReplies,
                onSaveConnection = ::saveConnection,
                onClearToken = {
                    secrets.clear()
                    hasBackendToken = false
                    respond("Backend access token cleared from this device.")
                },
                onToggleSpeech = { enabled ->
                    speakReplies = enabled
                    settings.edit().putBoolean("speak_replies", enabled).apply()
                },
                onRequestNotifications = ::requestNotifications,
                onOpenNotificationAccess = ::requestNotificationAccess,
                onRequestExactAlarms = ::requestExactAlarmAccess,
                exactRemindersEnabled = exactRemindersEnabled
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (::repo.isInitialized) {
            // Android cancels exact alarms when the special permission is revoked.
            // Restore future alarms with the available timing mode on reopening.
            ReminderScheduler.rescheduleAll(this)
            exactRemindersEnabled = ReminderScheduler.canBeExact(this)
            refreshReminders()
        }
    }

    override fun onStop() {
        if (::voice.isInitialized) voice.stop()
        super.onStop()
    }

    override fun onDestroy() {
        if (::voice.isInitialized) voice.close()
        super.onDestroy()
    }

    private fun refreshReminders() {
        reminders.clear()
        reminders.addAll(repo.all())
    }

    private fun respond(text: String, speak: Boolean = true) {
        conversation.add(ChatLine(Speaker.JARVIS, text))
        status = "SYSTEM ONLINE"
        if (speak && speakReplies && ::voice.isInitialized &&
            lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        ) voice.speak(text)
    }

    private fun onMicrophoneTapped() {
        if (listening) return
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            voice.listen()
        } else microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun handleText(raw: String) {
        val text = raw.trim().take(2000)
        if (text.isEmpty()) return
        conversation.add(ChatLine(Speaker.USER, text))
        currentTab = JarvisTab.HOME
        when (val command = LocalCommands.parse(text)) {
            is JarvisCommand.NewReminder -> addReminder(command.title, command.time.toEpochMilli())
            JarvisCommand.ReadWhatsApp -> readWhatsApp()
            is JarvisCommand.ReplyWhatsApp -> {
                if (!BuildConfig.NOTIFICATION_ACCESS_ENABLED) {
                    manualWhatsAppDraft = command.reply.take(1500)
                    currentTab = JarvisTab.WHATSAPP
                    respond("Draft ready. Core cannot read notifications or send a reply itself. Tap Review in WhatsApp, choose the recipient there, and send it yourself.")
                    return
                }
                val matches = WhatsAppAccess.messages.value.filter {
                    it.sender.equals(command.contact, ignoreCase = true)
                }
                when {
                    matches.isEmpty() -> respond("I can't find an active WhatsApp notification from ${command.contact}. Open WhatsApp or wait for a new notification.")
                    matches.size > 1 -> respond("I found multiple conversations with that name. Please select the exact notification in the WhatsApp tab.")
                    else -> {
                        val item = matches.first()
                        if (!item.canReply) respond("WhatsApp did not include a reply action for this notification. Please reply in WhatsApp.")
                        else {
                            draft = ReplyDraft(item.notificationKey, item.sender, command.reply)
                            respond("I've prepared a reply to ${item.sender}. Review the text and tap Confirm & send. Nothing has been sent yet.")
                        }
                    }
                }
            }
            JarvisCommand.OpenWhatsApp -> openWhatsApp()
            JarvisCommand.ShowReminders -> {
                currentTab = JarvisTab.REMINDERS
                respond("Here are your saved reminders.")
            }
            JarvisCommand.Unknown -> askAi(text)
        }
    }

    private fun askAi(text: String) {
        if (thinking) {
            respond("I'm answering your previous question. Try again when the response arrives.", speak = false)
            return
        }
        if (backendUrl.isBlank() || !hasBackendToken) {
            respond("Local reminders are ready. For open-ended AI conversations, open Settings and connect your secure AI server.")
            return
        }
        thinking = true
        status = "JARVIS IS THINKING"
        val lastTurns = remoteHistory.takeLast(12)
        lifecycleScope.launch {
            try {
                val answer = AiClient().ask(backendUrl, secrets.read().orEmpty(), text, lastTurns)
                remoteHistory.add(AiTurn("user", text))
                remoteHistory.add(AiTurn("assistant", answer))
                if (remoteHistory.size > 24) remoteHistory.subList(0, remoteHistory.size - 24).clear()
                respond(answer)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                respond("I couldn't reach the AI server. ${error.localizedMessage ?: "Please check Settings."}", speak = false)
            } finally { thinking = false }
        }
    }

    private fun addReminder(title: String, dueAtMillis: Long) {
        val clean = title.trim()
        if (clean.isBlank() || clean.length > 160) {
            respond("Reminder text must be between 1 and 160 characters.")
            return
        }
        if (dueAtMillis <= System.currentTimeMillis() + 5000) {
            respond("Please choose a reminder time in the future.")
            return
        }
        try {
            val item = repo.add(clean, dueAtMillis)
            val precise = ReminderScheduler.schedule(this, item)
            refreshReminders()
            val time = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(Date(item.dueAtMillis))
            val notificationNote = if (ReminderReceiver.alertsEnabled(this)) "" else
                " Reminder notifications are disabled. Enable them in Settings to receive an alert."
            respond("Reminder saved for $time: ${item.title}. " +
                (if (precise) "Exact alarm is enabled." else "Android will use approximate timing until you allow exact alarms.") +
                notificationNote)
            if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) notificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } catch (error: Exception) {
            respond("Unable to create reminder: ${error.localizedMessage.orEmpty()}", speak = false)
        }
    }

    private fun deleteReminder(item: Reminder) {
        ReminderScheduler.cancel(this, item.id)
        repo.delete(item.id)
        refreshReminders()
        respond("Reminder removed: ${item.title}", speak = false)
    }

    private fun readWhatsApp() {
        if (!BuildConfig.NOTIFICATION_ACCESS_ENABLED) {
            respond("JARVIS Core cannot read WhatsApp messages or notifications. Use the WhatsApp tab to draft text, then choose the recipient and send it inside WhatsApp.")
            return
        }
        val messages = WhatsAppAccess.messages.value.take(3)
        if (messages.isEmpty()) {
            respond("I can't see any active WhatsApp notifications. If you haven't already, enable JARVIS under Settings, Notification access. This does not expose previous chat history.")
            return
        }
        respond("You have ${WhatsAppAccess.messages.value.size} active WhatsApp notification${if (WhatsAppAccess.messages.value.size == 1) "" else "s"}. " +
            messages.joinToString(". ") { "From ${it.sender}: ${it.text.take(280)}" })
    }

    private fun openWhatsApp() {
        val open = packageManager.getLaunchIntentForPackage("com.whatsapp")
            ?: packageManager.getLaunchIntentForPackage("com.whatsapp.w4b")
        if (open == null) respond("I can't find WhatsApp installed on this phone.")
        else try { startActivity(open) } catch (_: Exception) { respond("I couldn't open WhatsApp.") }
    }

    private fun confirmReply(text: String) {
        val request = draft ?: return
        draft = null
        if (!BuildConfig.NOTIFICATION_ACCESS_ENABLED) {
            respond("Core has no notification reply capability. Review and send your message inside WhatsApp.", speak = false)
            return
        }
        val outcome = WhatsAppAccess.replyToConfirmedMessage(request.key, text.trim())
        respond(when (outcome) {
            WhatsAppReplyResult.SUBMITTED -> "Reply submitted to WhatsApp for ${request.contact}. Please check WhatsApp to confirm delivery."
            WhatsAppReplyResult.NOT_FOUND -> "That notification is no longer available. No message was sent."
            WhatsAppReplyResult.NO_REPLY_ACTION -> "This notification doesn't permit inline replies. No message was sent."
            WhatsAppReplyResult.NOT_CONNECTED -> "Notification access isn't connected. No message was sent."
            WhatsAppReplyResult.INVALID -> "Reply must contain 1 to 1500 characters. No message was sent."
            WhatsAppReplyResult.ERROR -> "WhatsApp didn't accept the reply action. Delivery wasn't confirmed."
            WhatsAppReplyResult.UNAVAILABLE -> "Core has no notification reply capability. No message was sent."
        })
    }

    private fun shareWhatsAppDraft(text: String) {
        val message = text.trim()
        if (message.isBlank() || message.length > 1500) {
            respond("Draft a message between 1 and 1500 characters first.", speak = false)
            return
        }
        val packages = listOf("com.whatsapp", "com.whatsapp.w4b").filter {
            packageManager.getLaunchIntentForPackage(it) != null
        }
        if (packages.isEmpty()) {
            respond("I can't find WhatsApp installed on this phone.", speak = false)
            return
        }
        fun shareIntent(packageName: String) = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            setPackage(packageName)
        }
        val chooser = Intent.createChooser(shareIntent(packages.first()), "Review in WhatsApp")
        if (packages.size > 1) {
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                packages.drop(1).map(::shareIntent).toTypedArray())
        }
        try {
            startActivity(chooser)
            respond("Choose a recipient, review the draft and press Send inside WhatsApp. JARVIS has not sent a message.", speak = false)
        } catch (_: Exception) {
            respond("WhatsApp could not open the draft. You can copy your text and paste it into WhatsApp.", speak = false)
        }
    }

    private fun saveConnection(url: String, token: String) {
        val cleanUrl = url.trim().trimEnd('/')
        try {
            if (cleanUrl.isNotBlank()) BackendEndpoint.parse(cleanUrl, BuildConfig.DEBUG)
        } catch (_: Exception) {
            respond("Enter a valid HTTPS URL without embedded credentials, or http://10.0.2.2:8787/api/chat in the debug Android emulator.", speak = false)
            return
        }
        val changedEndpoint = cleanUrl != backendUrl
        try {
            when {
                cleanUrl.isBlank() -> secrets.clear()
                token.isNotBlank() -> secrets.store(token.trim())
                changedEndpoint -> secrets.clear()
            }
        } catch (_: Exception) {
            respond("Unable to save the access token securely on this device. The connection was not changed.", speak = false)
            return
        }
        if (changedEndpoint) remoteHistory.clear()
        backendUrl = cleanUrl
        getSharedPreferences("jarvis_settings", MODE_PRIVATE).edit()
            .putString("backend_url", cleanUrl).apply()
        hasBackendToken = !secrets.read().isNullOrBlank()
        respond(if (cleanUrl.isBlank()) "AI server disconnected. Local reminders remain available."
            else if (!hasBackendToken) "Server URL saved. Enter this server's access token to connect."
            else "Connection settings saved. Send a test question on the Home tab to check the server.", speak = false)
    }

    private fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        else if (ReminderReceiver.alertsEnabled(this)) {
            respond("Reminder notifications are enabled.", speak = false)
        } else {
            try {
                startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
            } catch (_: Exception) {
                respond("Enable JARVIS reminder notifications in Android Settings.", speak = false)
            }
        }
    }

    private fun requestNotificationAccess() {
        if (!BuildConfig.NOTIFICATION_ACCESS_ENABLED) return
        if (!WhatsAppAccess.openNotificationAccessSettings(this)) {
            respond("Please enable JARVIS Full under Android Settings > Apps > Special app access > Notification access.", speak = false)
        }
    }

    private fun requestExactAlarmAccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || ReminderScheduler.canBeExact(this)) {
            respond("Exact alarm scheduling is already available.", speak = false)
            return
        }
        try {
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:$packageName")))
        } catch (_: Exception) {
            respond("Open Android Settings and allow Alarms & reminders for JARVIS.", speak = false)
        }
    }
}
