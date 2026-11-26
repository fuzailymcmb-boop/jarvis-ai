import test from 'node:test';
import assert from 'node:assert/strict';
import { createAppServer, normalizedPayload, responseText } from './server.mjs';

const TOKEN = 'j4rvis-test-only-super-long-token-12345';

function fakeOpenAi(url, options) {
  assert.equal(url, 'https://api.openai.com/v1/responses');
  assert.equal(options.headers.Authorization, 'Bearer test-api-key');
  const request = JSON.parse(options.body);
  assert.equal(request.model, 'gpt-6-luna');
  assert.equal(request.store, false);
  assert.deepEqual(request.reasoning, { effort: 'none' });
  assert.equal(request.input.at(-1).role, 'user');
  assert.ok(!JSON.stringify(request).includes('notificationKey'));
  return Promise.resolve({
    ok: true,
    json: async () => ({ output: [{ type: 'message', content: [{ type: 'output_text', text: 'At your service.' }] }] })
  });
}

async function startTestServer(upstreamFetch = fakeOpenAi) {
  const server = createAppServer({ apiKey: 'test-api-key', sharedToken: TOKEN }, upstreamFetch);
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  return { server, url: `http://127.0.0.1:${server.address().port}` };
}

test('Parses assistant text from messages, not assumed output[0]', () => {
  assert.equal(responseText({output: [
    {type: 'reasoning', summary: []},
    {type: 'message', content:[{ type: 'output_text', text: 'Hello!' }]}
  ]}), 'Hello!');
});

test('Rejects invalid and oversized input', () => {
  assert.throws(() => normalizedPayload({message: ''}), /Message/);
  assert.throws(() => normalizedPayload({message: 'x'.repeat(2001)}), /Message/);
});

test('Server refuses unchanged example credentials', () => {
  assert.throws(() => createAppServer({
    apiKey: 'YOUR_OPENAI_API_KEY_HERE', sharedToken: TOKEN
  }, fakeOpenAi), /Set OPENAI_API_KEY/);
  assert.throws(() => createAppServer({
    apiKey: 'test-api-key', sharedToken: 'REPLACE_WITH_AT_LEAST_32_RANDOM_CHARACTERS'
  }, fakeOpenAi), /Set OPENAI_API_KEY/);
});

test('Backend rejects unauthenticated requests', async () => {
  const { server, url } = await startTestServer();
  try {
    const res = await fetch(`${url}/api/chat`, {method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({message:'hello'})});
    assert.equal(res.status, 401);
  } finally { server.close(); }
});

test('Authenticated request gets an AI response', async () => {
  const { server, url } = await startTestServer();
  try {
    const res = await fetch(`${url}/api/chat`, {method: 'POST', headers: {
      'Content-Type': 'application/json', Authorization: `Bearer ${TOKEN}`
    }, body: JSON.stringify({message: 'Hello Jarvis', history: []})});
    assert.equal(res.status, 200);
    assert.deepEqual(await res.json(), {reply:'At your service.'});
  } finally { server.close(); }
});

test('Full permitted chat history fits the HTTP body limit', async () => {
  const { server, url } = await startTestServer();
  try {
    const history = Array.from({ length: 12 }, (_, index) => ({
      role: index % 2 === 0 ? 'user' : 'assistant', content: '界'.repeat(1500)
    }));
    const res = await fetch(`${url}/api/chat`, { method: 'POST', headers: {
      'Content-Type': 'application/json', Authorization: `Bearer ${TOKEN}`
    }, body: JSON.stringify({ message: '界'.repeat(2000), history }) });
    assert.equal(res.status, 200);
    assert.deepEqual(await res.json(), { reply: 'At your service.' });
  } finally { server.close(); }
});

test('Bad method or route cannot call AI', async () => {
  const { server, url } = await startTestServer();
  try {
    assert.equal((await fetch(`${url}/api/chat`)).status, 404);
  } finally { server.close(); }
});

test('Status verifies the server token without contacting the AI provider', async () => {
  let upstreamCalls = 0;
  const { server, url } = await startTestServer(() => {
    upstreamCalls += 1;
    throw new Error('Status must not call the provider');
  });
  try {
    const res = await fetch(`${url}/api/status`, {
      headers: { Authorization: `Bearer ${TOKEN}` }
    });
    assert.equal(res.status, 200);
    assert.equal(res.headers.get('cache-control'), 'no-store');
    assert.deepEqual(await res.json(), {
      service: 'jarvis',
      api_version: 1,
      server: 'reachable',
      authentication: 'accepted',
      provider_check: 'not_performed',
      capabilities: { chat: true, remote_device_actions: false }
    });
    assert.equal(upstreamCalls, 0);
  } finally { server.close(); }
});

test('Status rejects missing or incorrect tokens without contacting the provider', async () => {
  let upstreamCalls = 0;
  const { server, url } = await startTestServer(() => {
    upstreamCalls += 1;
    throw new Error('Unauthenticated status must not call the provider');
  });
  try {
    const headersToCheck = [
      {},
      { Authorization: `Bearer ${TOKEN.slice(0, -1)}X` },
      { Authorization: `Basic ${TOKEN}` }
    ];
    for (const headers of headersToCheck) {
      const res = await fetch(`${url}/api/status`, { headers });
      assert.equal(res.status, 401);
      assert.deepEqual(await res.json(), { error: 'Authentication required' });
    }
    assert.equal(upstreamCalls, 0);
  } finally { server.close(); }
});

test('Health stays public and generic without contacting the provider', async () => {
  let upstreamCalls = 0;
  const { server, url } = await startTestServer(() => {
    upstreamCalls += 1;
    throw new Error('Health must not call the provider');
  });
  try {
    const res = await fetch(`${url}/health`);
    assert.equal(res.status, 200);
    assert.deepEqual(await res.json(), { ok: true });
    assert.equal(upstreamCalls, 0);
  } finally { server.close(); }
});

test('Unsupported status methods cannot call the provider', async () => {
  let upstreamCalls = 0;
  const { server, url } = await startTestServer(() => {
    upstreamCalls += 1;
    throw new Error('Unsupported status methods must not call the provider');
  });
  try {
    const res = await fetch(`${url}/api/status`, {
      method: 'POST', headers: { Authorization: `Bearer ${TOKEN}` }
    });
    assert.equal(res.status, 404);
    assert.deepEqual(await res.json(), { error: 'Not found' });
    assert.equal(upstreamCalls, 0);
  } finally { server.close(); }
});
