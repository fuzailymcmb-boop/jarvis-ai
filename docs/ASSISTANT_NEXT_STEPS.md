# Making JARVIS a useful personal assistant

## Current milestone

The owner has installed the Android app. The next acceptance check is an ordinary
spoken question receiving a real AI answer and spoken playback on that phone.
The existing Core APK supports this when a working HTTPS chat endpoint and its
personal access token are configured. No APK update is needed for that connection.

This repository does not contain a live endpoint, provider API credential, or
proof of a successful real-provider conversation. A successful mock test or health
check is not a live AI connection.

## Connect the installed app

The repository includes a Render Blueprint and a container deployment option.
The Render Blueprint is for personal testing and selects the free compute plan;
OpenAI API usage is separate. No service or subscription is created by adding the
configuration to this repository.

1. Connect a hosting account and a provider API account through the supported
   integrations, or use the deployment button below and configure the server
   privately in the hosting dashboard.
2. Set `OPENAI_API_KEY` as a server secret. The Blueprint generates
   `JARVIS_SHARED_TOKEN`; this separate personal token is what the Android app uses.
3. Wait for the host to report a successful deployment. Verify `/health` on the
   actual HTTPS origin. This only confirms that the process is responding.
4. Verify authenticated `GET /api/status` with the personal token. Its
   `provider_check: not_performed` response deliberately does not claim that the
   provider key, model access, billing, or AI output has been tested.
5. In the installed app, open **Settings**, enter the actual HTTPS origin followed
   by `/api/chat`, enter the personal JARVIS server token, and save. Do not enter
   the OpenAI API key in the app.
6. First type **Explain something interesting in two short sentences**. Confirm a
   real answer. Then enable spoken responses, tap the microphone, and repeat with
   a spoken question. Microphone input and Android text-to-speech depend on the
   speech services installed on the phone.

[Deploy the prepared backend to Render](https://render.com/deploy?repo=https://github.com/fuzailymcmb-boop/jarvis-ai)

Render's free web services sleep after 15 minutes without traffic and can take
about a minute to wake. The installed app has a 45-second response timeout, so an
initial request after sleeping can time out. Open the service's `/health` URL and
wait for it to respond before retrying during testing. Responsive everyday voice
use needs hosting that remains available; review any paid plan before enabling it.

Never commit provider keys, personal tokens, phone data, or signing keys to this
public repository. Existing app integrations inside ChatGPT are not automatically
available to this separate Android app.

## Product sequence

| Stage | User-visible result | Acceptance check |
|---|---|---|
| Connected conversation | Ask a general question and hear a useful answer | Real authenticated AI reply and spoken playback on the phone |
| Foreground conversation mode | Start a visible session and take turns speaking without tapping for every turn | Stop always works; no listening while speaking or after leaving the app; silence/time limits apply |
| Preferences and explicit memory | Choose name, language, voice, response length, and facts to remember | Saved choices survive restart; user can view, edit, and delete them |
| Phone action handoffs | Prepare a call, map search, calendar entry, or WhatsApp draft | Correct target opens; user reviews and completes the action in that app |
| Natural language actions | Understand varied reminder wording and ask about ambiguous times | Phone validates structured proposals and reports actual execution results |
| Realtime voice | Lower latency, natural interruptions, and smoother turn-taking | Real-device audio tests and an authenticated realtime backend |
| Wake word and assistant integration | Activate JARVIS through supported Android assistant behavior | Explicit opt-in, device compatibility, battery and background testing |
| Account and home integrations | Use authorised calendars, business systems, or home devices | Per-service access and action confirmation; no implied access from ChatGPT connections |

These are development stages, not features already enabled. The movie's unrestricted
control is fictional; actual behavior depends on Android, app integrations, granted
permissions, connectivity, and the connected model.

## WhatsApp and the installed Core edition

Core prepares text and hands it to WhatsApp. The user selects the recipient,
reviews, and sends it. Core has no notification listener and cannot read incoming
WhatsApp messages or reply through notifications. Full contains those features
with confirmation, but its Play Protect/distribution issue is a separate work
item. A new assistant personality does not add messaging access.

## APK update prerequisite

The installed test APK was debug-signed on a hosted CI runner. The current workflow
does not explicitly preserve that runner's debug signing key. Before distributing
another APK, establish a durable signing arrangement, compare certificates, and
provide a migration plan if signatures differ. Do not assume an in-place update
or instruct the user to erase local reminders and settings without accounting for
their data. The backend connection can proceed with the installed app.

## References

- [OpenAI Realtime API](https://developers.openai.com/api/docs/guides/realtime)
- [Android speech recognition](https://developer.android.com/reference/android/speech/SpeechRecognizer)
- [Android voice interaction service](https://developer.android.com/reference/android/service/voice/VoiceInteractionService)
- [Android common action intents](https://developer.android.com/guide/components/intents-common)
- [Android app signing](https://developer.android.com/studio/publish/app-signing)
- [Render Blueprint specification](https://render.com/docs/blueprint-spec)
- [Render free hosting limits](https://render.com/docs/free)
