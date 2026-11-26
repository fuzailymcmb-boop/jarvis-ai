# JARVIS AI for Android

A personal Android assistant with a futuristic interface, spoken conversations, reminders, and WhatsApp notification replies that require confirmation.

[Build status and APK downloads](https://github.com/fuzailymcmb-boop/jarvis-ai/actions/workflows/build-apk.yml)

## What this version includes

| Feature | Behavior |
|---|---|
| Voice conversations | Tap the microphone, speak, and hear the response. General AI answers require the separate backend. |
| Reminders | Save reminders locally and receive Android notifications. Exact timing needs Android's exact-alarm permission. |
| WhatsApp | Read currently active WhatsApp / WhatsApp Business notifications after permission is granted. Review and confirm supported notification replies before sending. |
| Interface | Dark Android interface with an animated JARVIS orb, conversation view, reminder list, and connection settings. |
| Access controls | Explicit microphone and notification permissions. The server access token is encrypted using Android Keystore. |

This version uses tap-to-talk. Continuous wake-word listening, email, calendar integrations, and general phone automation are future features. `preview.html` is an interface demonstration, not the Android app.

## Get the APK

1. Open **Actions → Build JARVIS Android APK**.
2. Select the latest successful run on `main`.
3. Under **Artifacts**, download **JARVIS-AI-Android-debug-APK**. GitHub may ask you to sign in.
4. Extract the ZIP and install **JARVIS-AI-debug.apk** on an Android phone running Android 8.0 or later.
5. The ZIP also contains **SHA256SUMS.txt** with the APK checksum.

Each push to `main` runs the backend tests, Android unit tests, Android lint, APK compilation, and a clean-install launch check on an Android 10 emulator. The workflow verifies the APK signature before uploading it. A successful build does not substitute for testing microphone, reminders, and WhatsApp on a physical phone.

This is a debug APK for personal testing. It is not a Play Store release. Future release distribution needs a maintained signing key. Debug signing keys may differ across hosted build runs, so an update may require uninstalling a previous test build, which removes its local data.

## First launch

- **Microphone:** grant permission when you first tap the orb. Android speech services and text-to-speech must be available on the phone.
- **Reminder notifications:** allow notifications if requested. For exact timing, enable **Exact alarm access** in JARVIS Settings.
- **WhatsApp:** use **WhatsApp notification access** in JARVIS Settings and approve Android's notification-access screen. Only active notifications are available, not the full WhatsApp chat history.
- **AI connection:** enter the backend chat URL and personal server token in JARVIS Settings. Save them, then test an ordinary question.

## Set up the AI backend

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
gradle --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

The local APK is written to `app/build/outputs/apk/debug/app-debug.apk`. To generate wrapper files from a trusted Gradle installation:

```bash
gradle wrapper --gradle-version 8.13 --gradle-distribution-sha256-sum 20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78
```

## Validation and device checklist

Backend validation uses a mock AI provider and does not make paid API calls. Android build results, unit-test results, and lint reports are retained in GitHub Actions. Before relying on the app, test:

1. A typed AI question, a spoken question, and spoken playback.
2. A reminder a few minutes ahead, with the app in the background.
3. A reminder after reboot, and behavior with notifications disabled.
4. A fresh WhatsApp notification, a reviewed reply, and delivery inside WhatsApp.
5. Revoking microphone or notification access and checking the app's explanation.

The app cannot guarantee WhatsApp message delivery. Android notification access is a system-wide permission; the app filters events to WhatsApp packages. Notification text is held in memory and is not automatically sent to the AI backend.
