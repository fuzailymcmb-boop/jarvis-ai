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

async function startTestServer() {
  const server = createAppServer({ apiKey: 'test-api-key', sharedToken: TOKEN }, fakeOpenAi);
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
