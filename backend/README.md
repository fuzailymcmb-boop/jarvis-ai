# JARVIS personal API proxy

Node.js 20+; no external npm dependencies. Environment variables:

- `OPENAI_API_KEY`: your own provider secret; must never be bundled with Android app.
- `JARVIS_SHARED_TOKEN`: random secret (at least 20 characters), configured in JARVIS Settings; Android stores it encrypted with the Keystore.
- `OPENAI_MODEL`: defaults to `gpt-6-luna`, with reasoning disabled for short voice responses. Other configured models use their own default reasoning settings and must support the Responses API.
- `HOST`: defaults to `127.0.0.1` for local development.
- `PORT`: defaults to `8787`.

Routes: `GET /health` and authenticated `POST /api/chat` with `{message,history}`.

The backend retains no chat history, logs no message content, authenticates its only mutation-style endpoint, imposes body size limits and a basic per-IP rate limit. Requests explicitly set `store: false` to disable later retrieval of responses through the provider API; this does not promise zero provider retention. Run behind HTTPS and a production edge proxy for external use. Be aware the provider receives general chat input sent to this server; WhatsApp notification content is not automatically included in AI requests.

Replace both example credentials before starting the server. Startup rejects the unchanged `YOUR_...` and `REPLACE_...` placeholders. Keep the real `.env` file outside Git, and load its values into the server process as described in the project README. No API key or backend access token is required to compile the APK.

Tests: `npm test`.
