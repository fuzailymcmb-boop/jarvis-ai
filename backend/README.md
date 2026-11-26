# JARVIS personal AI backend

This is a deployment-ready Node.js 22 service with no external npm dependencies. These files prepare the service for hosting; **they do not deploy a server or connect the installed Android app**. General AI conversation needs a running HTTPS endpoint, an OpenAI API credential on that server, and a separate personal server token in the app. Building the APK needs neither secret.

## Configuration

| Variable | Purpose |
| --- | --- |
| `OPENAI_API_KEY` | Your OpenAI API credential. Configure it on the server only; never put it in the Android app, Git repository or container image. |
| `JARVIS_SHARED_TOKEN` | A separate randomly generated personal server token, at least 20 characters. Generate 32 random bytes as shown below. Enter this token in JARVIS Settings; Android encrypts its saved copy with Keystore. |
| `OPENAI_MODEL` | Defaults to `gpt-6-luna`, with reasoning disabled for short spoken responses. Other configured models must support the Responses API and use their own reasoning defaults. |
| `HOST` | Defaults to `127.0.0.1` when running directly. The Docker image sets `0.0.0.0` so the container can receive connections. |
| `PORT` | Defaults to `8787`; hosting platforms can inject their assigned port. |

Startup rejects missing credentials and unchanged `YOUR_...` / `REPLACE_...` examples. Presence checks do not verify that an API credential is valid or that the account has credit.

For local development with Node.js 22, from the project root:

```sh
cd backend
cp .env.example .env
openssl rand -hex 32
# Edit .env: replace both credential examples with your own values.
node --env-file=.env server.mjs
```

Keep `.env` private. For hosted deployment, use the platform's secret environment-variable settings instead of committing the file. The server uses OpenAI API billing; no provider request is made at startup or during a health/status check.

## Routes and connection verification

| Route | Authentication | What a successful response establishes |
| --- | --- | --- |
| `GET /health` | None | The server process is responding. Returns only `{"ok":true}`. |
| `GET /api/status` | `Authorization: Bearer <JARVIS_SHARED_TOKEN>` | This is the JARVIS service and the personal server token was accepted. No AI provider request is made. |
| `POST /api/chat` | Same bearer token | A real AI request is made using `{ "message": "...", "history": [] }`; successful replies use `{ "reply": "..." }`. |

Authenticated status response:

```json
{
  "service": "jarvis",
  "api_version": 1,
  "server": "reachable",
  "authentication": "accepted",
  "provider_check": "not_performed",
  "capabilities": {
    "chat": true,
    "remote_device_actions": false
  }
}
```

The response never includes keys or tokens. Missing or incorrect authentication returns `401`; unsupported routes and methods return `404`.

**A successful health or status check does not verify OpenAI credentials, model access, available credit, or a working AI answer.** Only a successful real chat response verifies the conversation path at that moment. The `provider_check` field deliberately remains `not_performed` on status requests.

## Container deployment

Build with `backend/` as the Docker build context:

```sh
docker build -t jarvis-backend ./backend
```

The image uses Node.js 22, runs as the unprivileged `node` user, copies only `package.json` and `server.mjs`, and checks `/health`. The `.dockerignore` whitelist excludes `.env` files and other secrets from the build context.

For a local container behind a trusted HTTPS reverse proxy, supply a separate private environment file such as `backend/.env.container`. It must contain real server credentials and `HOST=0.0.0.0`:

```sh
docker run --rm --name jarvis-backend \
  --env-file backend/.env.container \
  -e HOST=0.0.0.0 -e PORT=8787 \
  -p 127.0.0.1:8787:8787 jarvis-backend
```

The published port above remains local to the host. A managed hosting platform should inject the same secrets and bind its assigned `PORT`; use its trusted HTTPS endpoint. **The container itself does not terminate TLS.** Do not send the personal bearer token over public plain HTTP. The basic in-process rate limit is for this personal prototype; place the service behind a production edge proxy for external use.

## Connect the Android app

1. Deploy the service on a compatible host with HTTPS and configure both server secrets there.
2. Open **JARVIS → Settings** and enter the exact chat endpoint, for example `https://YOUR_DOMAIN/api/chat`.
3. Enter **`JARVIS_SHARED_TOKEN`** as the server access token. Do not enter the OpenAI API key into the app.
4. Save the settings. An authenticated `/api/status` check can verify the server connection without using AI credit. If the installed app does not offer this check, saving settings alone does not test the connection.
5. Ask an ordinary question to verify a real answer, then enable spoken responses if desired.

Android emulators may use `http://10.0.2.2:8787/api/chat` with a debug build and a local server. A physical phone must use HTTPS. Redirects are intentionally rejected by the Android client: enter the final endpoint directly.

## Behavior and privacy

JARVIS replies calmly and conversationally, usually in one to three short spoken sentences, with occasional light wit. This changes the assistant's writing style; the actual voice comes from the phone's text-to-speech engine.

The AI backend does not control the phone, schedule alarms, access email/calendar, retrieve live web information, read WhatsApp notifications or send messages. Local actions remain the Android app's responsibility. It must validate permission and confirmation requirements, and an AI reply must never be treated as proof that a device action occurred.

The server retains no chat history and logs no message content. Chat requests use `store: false` to disable later retrieval through the provider API; this does not promise zero provider retention. The provider receives general chat input sent to this service. WhatsApp notification content is not automatically included in those requests. The service enforces a bounded request size and a basic per-IP chat rate limit.

## Tests

```sh
cd backend
npm test
```

Tests use mocked AI responses and make no paid API calls. They cover the existing chat behavior, permitted history size, example-credential rejection, status authentication, absence of provider calls from health/status routes, and the generic public health response. These checks do not establish that a production deployment exists or that real provider credentials work.
