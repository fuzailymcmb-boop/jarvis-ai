# JARVIS AI for Android

A personal Android assistant with a futuristic interface, spoken conversations, and reminders. It has two explicitly different editions: **Core**, without notification-reading access, and **Full**, with optional WhatsApp notification access and confirmed replies.

[Build status and APK downloads](https://github.com/fuzailymcmb-boop/jarvis-ai/actions/workflows/build-apk.yml)

## Choose an edition

| Feature | Core | Full |
|---|---|---|
| Tap-to-talk conversations and spoken responses | Yes; general AI answers require the separate backend | Yes; same backend requirement |
| Local reminders | Yes | Yes |
| Prepare a WhatsApp message | Opens WhatsApp so you can choose a recipient, review, and send it there | Can reply to supported active notifications after explicit confirmation |
| Read WhatsApp notifications | No; the listener service and reader code are excluded from the APK | Requires Android notification-access permission |
| Notification access settings | Not present | Available after an explanation of the access |

Core and Full use the same application ID and cannot be installed side by side. Full source and historical test releases remain available; Core is a feature-limited alternative, not a claim that Google has approved either edition.

## What both editions include

| Feature | Behavior |
|---|---|
| Voice conversations | Tap the microphone, speak, and hear the response. General AI answers require the separate backend. |
| Reminders | Save reminders locally and receive Android notifications. Exact timing needs Android's exact-alarm permission. |
| WhatsApp | Edition-specific behavior is described above. Core cannot read incoming notifications or send automatic replies. |
| Interface | Dark Android interface with an animated JARVIS orb, conversation view, reminder list, and connection settings. |
| Access controls | Explicit microphone and notification permissions. The server access token is encrypted using Android Keystore. |

This version uses tap-to-talk. Continuous wake-word listening, email, calendar integrations, and general phone automation are future features. `preview.html` is an interface demonstration, not the Android app.

## Get the APK

[Download JARVIS AI Core 0.1.1 APK](https://github.com/fuzailymcmb-boop/jarvis-ai/releases/download/v0.1.1-core.1/JARVIS-AI-Core-0.1.1.apk)

The public [Core test release](https://github.com/fuzailymcmb-boop/jarvis-ai/releases/tag/v0.1.1-core.1) includes a directly downloadable APK and its SHA-256 checksum, with no GitHub sign-in required. Android 8.0 or later is required. Keep Google Play Protect enabled when installing. If it asks to scan an unknown app, use the scan; if it blocks installation, retain the exact warning for investigation.

For builds from source:

1. Open **Actions → Build JARVIS Android APK** and select the latest successful run on `main`.
2. Under **Artifacts**, choose **JARVIS-AI-Core-Android-debug-APK** or **JARVIS-AI-Full-Android-debug-APK**. Workflow artifacts may require GitHub sign-in.
3. Extract the ZIP. Install the edition's `.apk`; **SHA256SUMS.txt** records its checksum.

Each push to `main` tests the backend, runs unit tests and Android lint for both editions, compiles both APKs, audits the actual Core APK manifest and DEX code for excluded notification access, and performs a clean-install launch check of Core on an Android 10 emulator. The Full APK is a positive control for the listener audit. The workflow verifies both APK signatures before uploading them. This establishes build integrity and basic compatibility, not Google Play Protect approval or complete physical-device testing.

These are debug APKs for personal testing, not Play Store releases. Future release distribution needs a maintained signing key. Debug signing keys may differ across hosted build runs, so updates can encounter a signature conflict. Uninstalling an existing test build removes its local data; do not do that without preserving anything you need.

## Play Protect and notification-reading access

Google documents an Internet-sideloading protection that can block apps declaring notification-listener, SMS-reading, or accessibility capabilities in affected markets. Its warning says the app can request sensitive data and may increase identity-theft or financial-fraud risk. Full declares a notification listener for its WhatsApp feature, matching one of those capabilities. This differs from an ordinary unknown-app scan request.

Core genuinely excludes the notification listener service, notification reader/cache, inline-notification reply implementation, and links to notification-access Settings. It retains only the permissions needed for network requests, user-initiated microphone input, its own reminder notifications, alarm scheduling, reboot rescheduling, and AndroidX's internal receiver protection. Posting JARVIS's own reminder notifications does not grant access to other apps' notifications.

Do not turn off Play Protect to install either edition. A successful emulator install cannot establish whether Play Protect will allow installation on a particular phone. Full requires further review of its distribution and permission use; Google provides an appeal route after developers have reviewed the relevant requirements. No appeal or Google approval is implied by this repository.

References: [Google Play Protect warning guidance](https://developers.google.com/android/play-protect/warning-dev-guidance), [Android notification listener API](https://developer.android.com/reference/android/service/notification/NotificationListenerService), and [Android notification permission](https://developer.android.com/develop/ui/views/notifications/notification-permission).

## First launch

- **Microphone:** grant permission when you first tap the orb. Android speech services and text-to-speech must be available on the phone.
- **Reminder notifications:** allow notifications if requested. For exact timing, enable **Exact alarm access** in JARVIS Settings.
- **WhatsApp in Core:** prepare a message and open it in WhatsApp. Choose the recipient and send it inside WhatsApp. Core cannot read incoming messages or automatically reply.
- **WhatsApp in Full:** review the notification-access explanation, then use **WhatsApp notification access** in JARVIS Settings if you choose to grant it. Only active notifications are available, not the full WhatsApp chat history.
- **AI connection:** enter the backend chat URL and personal server token in JARVIS Settings. Save them, then test an ordinary question.

## Set up the AI backend

The app must be connected to a real AI service before it can answer general
questions. Installing the APK or publishing this repository does not provide that
connection. The installed Core APK can connect through Settings without an APK
update.

[Deploy the prepared backend to Render](https://render.com/deploy?repo=https://github.com/fuzailymcmb-boop/jarvis-ai)

The included `render.yaml` selects a free testing service, prompts for the provider
key privately, and generates the separate personal JARVIS server token. It does
not create a hosting service until deployed. Free services can sleep and delay
the first response; AI API usage is billed separately. The deployment must be
configured and a real question verified before calling the AI connection ready.

See [connection instructions and assistant development stages](docs/ASSISTANT_NEXT_STEPS.md)
for the concrete setup and next features. The backend also includes a non-root
Docker image definition for another compatible host.

The backend requires Node.js 22, your own OpenAI API key, and a separate randomly generated personal access token. It does not require npm packages. API usage is billed to the API account.

```bash
cd backend
cp .env.example .env
# Edit .env locally. Replace both placeholder credentials.
# Generate a server token with: openssl rand -hex 32
node --env-file=.env server.mjs
```

For a physical phone, host the backend behind trusted HTTPS and set the app endpoint to `https://YOUR_DOMAIN/api/chat`. Keep the OpenAI key on that server. The app receives only the personal server access token.

For an Android emulator, the debug build permits the host endpoint `http://10.0.2.2:8787/api/chat`. All physical-phone and release endpoints require HTTPS.

This repository contains source code and placeholder settings only. Do not commit `.env`, credentials, signing keys, personal notification contents, or phone backups. Public repository visibility does not itself publish the data stored on a phone.

See [backend/README.md](backend/README.md) for backend behavior and limitations.

## Build locally

Use JDK 17, Android SDK 36, Android Build Tools 35.0.0, and Gradle 8.13. The repository does not include Gradle wrapper executables or its JAR; the automated workflow installs the pinned Gradle version directly.

```bash
gradle --no-daemon :app:testCoreDebugUnitTest :app:lintCoreDebug :app:assembleCoreDebug
# Full edition:
gradle --no-daemon :app:testFullDebugUnitTest :app:lintFullDebug :app:assembleFullDebug
```

The APKs are written to `app/build/outputs/apk/core/debug/app-core-debug.apk` and `app/build/outputs/apk/full/debug/app-full-debug.apk`. To generate wrapper files from a trusted Gradle installation:

```bash
gradle wrapper --gradle-version 8.13 --gradle-distribution-sha256-sum 20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78
```

## Validation and device checklist

Backend validation uses a mock AI provider and does not make paid API calls. Android build results, unit-test results, and lint reports are retained in GitHub Actions. Before relying on the app, test:

1. A typed AI question, a spoken question, and spoken playback.
2. A reminder a few minutes ahead, with the app in the background.
3. A reminder after reboot, and behavior with notifications disabled.
4. In Core, a prepared message handed to WhatsApp, recipient selection, and manually sending it there. In Full, a fresh WhatsApp notification, a reviewed reply, and delivery inside WhatsApp.
5. Revoking microphone or reminder-notification permission and checking the app's explanation; in Full, also revoke notification-reading access.

The app cannot guarantee WhatsApp message delivery. In Full, Android notification access is a system-wide permission; the listener filters events to WhatsApp packages. Notification text is held in memory and is not automatically sent to the AI backend. Core has no notification listener or notification cache.
