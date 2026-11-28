// Record synthetic Persian audio through Chromium and the configured real speech gateway.
const {chromium} = require(process.env.PLAYWRIGHT_CORE || 'playwright-core');
const assert = require('node:assert/strict');

async function rehearse() {
  const fixture = process.env.SYNTHETIC_WAV;
  assert.ok(fixture, 'Set SYNTHETIC_WAV to a synthetic Persian microphone fixture');
  const args = [
    '--use-fake-ui-for-media-stream',
    '--use-fake-device-for-media-stream',
    '--use-file-for-fake-audio-capture=' + fixture
  ];
  if (process.env.TEST_CERT_SPKI) args.push('--ignore-certificate-errors-spki-list=' + process.env.TEST_CERT_SPKI);
  const browser = await chromium.launch({executablePath:process.env.CHROMIUM_PATH, args});
  try {
    const page = await browser.newPage({viewport:{width:380, height:780}, reducedMotion:'reduce'});
    const messages = [];
    let uploads = 0;
    page.on('request', request => {
      const path = new URL(request.url()).pathname;
      if (path === '/assistant/v1/messages') messages.push(request.postDataJSON().message);
      if (path === '/speech/transcribe') uploads++;
    });
    await page.goto((process.env.DEMO_URL || 'http://127.0.0.1:8101') + '/banking/');
    assert.ok(await page.evaluate(() => isSecureContext));
    await page.locator('#mic:enabled').click();
    await page.getByRole('button', {name:'توقف ضبط'}).waitFor();
    await page.waitForTimeout(5400);
    await page.locator('#mic').click();
    await page.waitForFunction(() => document.querySelector('#message').value.trim(), null, {timeout:365000});
    const editor = page.locator('#message'), draft = await editor.inputValue();
    assert.match(draft, /[آ-ی]/);
    assert.equal(uploads, 1);
    assert.deepEqual(messages, []);
    const edit = draft.replace(/\S+/, draft.startsWith('سلام') ? 'درود' : 'سلام');
    assert.notEqual(edit, draft);
    await editor.fill(edit);
    await Promise.all([
      page.waitForResponse(r => r.url().endsWith('/messages') && r.ok()),
      page.locator('#send').click()
    ]);
    assert.deepEqual(messages, [edit]);
    console.log('PASS real ASR, one upload, Persian draft (' + draft.length + ' characters), one edited send.');
  } finally { await browser.close(); }
}
rehearse().catch(error => { console.error(error); process.exitCode = 1; });
