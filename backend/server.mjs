/**
 * JARVIS personal voice assistant backend — Node.js 22, no npm dependencies.
 * Holds the OpenAI API key only on the server.
 * Production: run behind HTTPS, private authentication and a rate-limited proxy.
 */
import { createServer } from 'node:http';
import { timingSafeEqual } from 'node:crypto';
import { pathToFileURL } from 'node:url';

// The app can send twelve 1500-character turns plus a 2000-character message.
// Allow their UTF-8/JSON encoding while keeping requests strictly bounded.
const MAX_BODY_BYTES = 131_072;
const MAX_HISTORY_TURNS = 12;
const MAX_MESSAGE_CHARS = 2_000;
const SYSTEM_INSTRUCTIONS = [
  'You are JARVIS, a calm, capable, personable assistant with a little dry wit when appropriate.',
  'Answer the user directly in natural spoken language, normally in one to three short sentences.',
  'Use longer explanations when requested. Avoid Markdown, decorative symbols and lengthy lists in spoken replies.',
  'Ask one focused clarification question when essential information is missing; do not invent the answer.',
  'Be warm and useful. Mention limitations only when relevant, without repeating a disclaimer in every reply.',
  'You cannot read, send or reply to WhatsApp, access phone state, schedule reminders, or control the device.',
  'Those actions are implemented solely by the Android app after user permission and confirmation.',
  'Never claim to have performed actions or seen messages that are not in the supplied conversation.',
  'You have no live web, email or calendar access through this service. Be honest about information you cannot verify.',
  'Do not request passwords, API keys or security tokens from the user.'
].join(' ');

function json(res, status, content) {
  res.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'Cache-Control': 'no-store',
    'X-Content-Type-Options': 'nosniff',
    'Referrer-Policy': 'no-referrer'
  });
  res.end(JSON.stringify(content));
}

function constantTimeTokenMatches(header, expected) {
  if (!header?.startsWith('Bearer ')) return false;
  const supplied = Buffer.from(header.slice(7), 'utf8');
  const required = Buffer.from(expected, 'utf8');
  return supplied.length === required.length && timingSafeEqual(supplied, required);
}

async function readJson(req) {
  let total = 0;
  const chunks = [];
  for await (const chunk of req) {
    total += chunk.length;
    if (total > MAX_BODY_BYTES) {
      const err = new Error('Request too large');
      err.status = 413;
      throw err;
    }
    chunks.push(chunk);
  }
  try { return JSON.parse(Buffer.concat(chunks).toString('utf8')); }
  catch {
    const err = new Error('Invalid JSON');
    err.status = 400;
    throw err;
  }
}

export function normalizedPayload(raw) {
  if (!raw || typeof raw !== 'object') throw Object.assign(new Error('Invalid request'), { status: 400 });
  const message = raw.message;
  if (typeof message !== 'string' || !message.trim() || message.length > MAX_MESSAGE_CHARS)
    throw Object.assign(new Error('Message must contain 1–2000 characters'), { status: 400 });
  const items = Array.isArray(raw.history) ? raw.history.slice(-MAX_HISTORY_TURNS) : [];
  const history = items
    .filter(item => item && ['user', 'assistant'].includes(item.role) && typeof item.content === 'string')
    .map(item => ({ role: item.role, content: item.content.slice(0, 1500) }));
  return { message: message.trim(), history };
}

export function responseText(response) {
  const text = (response?.output || [])
    .filter(item => item?.type === 'message')
    .flatMap(item => item.content || [])
    .filter(part => part?.type === 'output_text' && typeof part.text === 'string')
    .map(part => part.text)
    .join('\n').trim();
  return text || (typeof response?.output_text === 'string' ? response.output_text.trim() : '');
}

export function createAppServer(config, upstreamFetch = fetch) {
  const apiKey = typeof config.apiKey === 'string' ? config.apiKey.trim() : '';
  const sharedToken = typeof config.sharedToken === 'string' ? config.sharedToken.trim() : '';
  const model = config.model || 'gpt-6-luna';
  if (!apiKey || !sharedToken || sharedToken.length < 20 ||
      /^(YOUR_|REPLACE_)/i.test(apiKey) || /^(YOUR_|REPLACE_)/i.test(sharedToken))
    throw new Error('Set OPENAI_API_KEY and a random JARVIS_SHARED_TOKEN of at least 20 characters.');

  const hits = new Map();
  return createServer(async (req, res) => {
    if (req.method === 'GET' && req.url === '/health') return json(res, 200, { ok: true });
    const statusRequest = req.method === 'GET' && req.url === '/api/status';
    const chatRequest = req.method === 'POST' && req.url === '/api/chat';
    if (!statusRequest && !chatRequest) return json(res, 404, { error: 'Not found' });
    if (!constantTimeTokenMatches(req.headers.authorization, sharedToken))
      return json(res, 401, { error: 'Authentication required' });
    if (statusRequest) {
      // This verifies only reachability and this server's token. It never calls AI.
      return json(res, 200, {
        service: 'jarvis',
        api_version: 1,
        server: 'reachable',
        authentication: 'accepted',
        provider_check: 'not_performed',
        capabilities: { chat: true, remote_device_actions: false }
      });
    }

    // Small personal-server IP rate limiter. Use an edge provider for production deployments.
    const clientIp = req.socket.remoteAddress || 'unknown';
    const now = Date.now();
    const attempts = (hits.get(clientIp) || []).filter(time => now - time < 60_000);
    if (attempts.length >= 20) return json(res, 429, { error: 'Too many requests; try again shortly' });
    attempts.push(now);
    hits.set(clientIp, attempts);
    if (hits.size > 2000) hits.clear();

    try {
      const { message, history } = normalizedPayload(await readJson(req));
      const upstream = await upstreamFetch('https://api.openai.com/v1/responses', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${apiKey}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          model,
          store: false,
          // Luna supports none; reserve the small voice-answer budget for spoken text.
          ...(model === 'gpt-6-luna' ? { reasoning: { effort: 'none' } } : {}),
          instructions: SYSTEM_INSTRUCTIONS,
          input: [...history, { role: 'user', content: message }],
          max_output_tokens: 550
        }),
        signal: AbortSignal.timeout(42_000)
      });
      if (!upstream.ok) {
        // Do not log / expose a raw provider response or potentially sensitive request.
        return json(res, 502, { error: `AI provider request failed (${upstream.status})` });
      }
      const payload = await upstream.json();
      const reply = responseText(payload);
      if (!reply) return json(res, 502, { error: 'AI provider returned no answer' });
      return json(res, 200, { reply: reply.slice(0, 5000) });
    } catch (error) {
      return json(res, error.status || 500, {
        error: error.status ? error.message : 'AI service temporarily unavailable'
      });
    }
  });
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  const server = createAppServer({
    apiKey: process.env.OPENAI_API_KEY,
    sharedToken: process.env.JARVIS_SHARED_TOKEN,
    model: process.env.OPENAI_MODEL || 'gpt-6-luna'
  });
  const port = Number(process.env.PORT || 8787);
  const host = process.env.HOST || '127.0.0.1';
  server.listen(port, host, () => {
    console.log(`JARVIS backend listening on ${host}:${port}. Use HTTPS for real devices.`);
  });
}
