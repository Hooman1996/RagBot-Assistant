// Chromium records a synthetic microphone using the real getUserMedia and MediaRecorder APIs.
const assert = require('node:assert/strict');
const {chromium} = require(process.env.PLAYWRIGHT_CORE || 'playwright-core');

async function run() {
  const browser = await chromium.launch({executablePath:process.env.CHROMIUM_PATH,
    args:['--use-fake-device-for-media-stream', '--use-fake-ui-for-media-stream']});
  try {
    const page = await browser.newPage({viewport:{width:380,height:780}, reducedMotion:'reduce'});
    page.setDefaultTimeout(10000);
    let ready = false, uploads = 0, health = 0, reply;
    const messages = [];
    await page.addInitScript(() => {
      window.tracks = [];
      const original = navigator.mediaDevices.getUserMedia.bind(navigator.mediaDevices);
      navigator.mediaDevices.getUserMedia = async constraints => {
        const stream = await original(constraints);
        window.tracks.push(...stream.getTracks());
        return stream;
      };
    });
    await page.route('**/speech/health', route => {
      health++;
      return route.fulfill({json:{status:'ok', model_loaded:ready}});
    });
    await page.route('**/speech/transcribe', route => {
      const body = route.request().postDataBuffer().toString('latin1');
      assert.match(body, /name="file"; filename="dictation\.webm"/);
      assert.doesNotMatch(body, /keep_files|conversation_id/);
      assert.equal(route.request().headers()['x-assistant-session'], undefined);
      uploads++;
      reply = options => route.fulfill(options);
    });
    await page.route('**/assistant/v1/messages', route => {
      messages.push(route.request().postDataJSON().message);
      return route.fulfill({json:{kind:'status'}});
    });
    await page.goto((process.env.DEMO_URL || 'http://127.0.0.1:8101') + '/banking/');
    const mic = page.locator('#mic'), editor = page.locator('#message'), discard = page.locator('#discard');
    await page.locator('#send:enabled').waitFor();
    assert.equal(await mic.isDisabled(), true);
    await editor.fill('پیام تایپی');
    await editor.press('Enter');
    await page.waitForFunction(() => !document.querySelector('#message').value);
    assert.deepEqual(messages.splice(0), ['پیام تایپی']);
    assert.equal(await page.evaluate(() => tracks.length), 0);
    ready = true;
    await page.evaluate(() => document.dispatchEvent(new Event('visibilitychange')));
    await page.locator('#mic:enabled').waitFor();
    const record = async () => {
      await mic.focus();
      await mic.press('Space');
      await page.getByRole('button',{name:'توقف ضبط'}).waitFor();
      await page.waitForTimeout(220);
    };
    const upload = async () => {
      await record();
      await Promise.all([page.waitForRequest('**/speech/transcribe'), mic.click()]);
      await page.waitForFunction(() => tracks.every(t => t.readyState === 'ended'));
    };
    await record();
    await discard.click();
    assert.equal(uploads, 0);
    await editor.fill('پیش');
    await upload();
    await editor.press('End');
    await page.keyboard.insertText(' نویس');
    await reply({json:{text:'  سلام  ', segments:[{text:'unused'}]}});
    await discard.waitFor({state:'hidden'});
    assert.equal(await editor.inputValue(), 'پیش نویس سلام');
    await editor.fill('سلام ویرایش شده');
    await editor.dispatchEvent('keydown', {key:'Enter',isComposing:true});
    await editor.press('Shift+Enter');
    assert.match(await editor.inputValue(), /\n$/);
    assert.deepEqual(messages, []);
    await editor.press('Enter');
    await editor.press('Enter');
    assert.deepEqual(messages, ['سلام ویرایش شده']);
    await upload();
    await discard.click();
    await reply({json:{text:'دیر'}});
    assert.equal(await editor.inputValue(), '');
    await editor.fill('پیش');
    await upload();
    await editor.press('Control+a');
    await page.keyboard.insertText('جدید');
    await discard.waitFor({state:'hidden'});
    await reply({json:{text:'دیر'}});
    assert.equal(await editor.inputValue(), 'جدید');
    for (const [response, status] of [
      [{json:{segments:[{text:'wrong'}]}}, 'گفتاری تشخیص داده نشد'],
      [{status:500}, 'تبدیل صدا انجام نشد'],
      [{status:504}, 'زمان انتظار تمام شد'],
      [{status:429, headers:{'Retry-After':'1'}}, 'سرویس مشغول است']
    ]) {
      await upload();
      await reply(response);
      await page.getByText(status, {exact:false}).waitFor();
      assert.equal(await editor.inputValue(), 'جدید');
    }
    assert.equal(await mic.isDisabled(), true);
    await page.locator('#mic:enabled').waitFor();
    assert.equal(uploads, 7);
    await upload();
    await editor.press('Enter');
    await discard.waitFor({state:'hidden'});
    await reply({json:{text:'دیر'}});
    await page.waitForFunction(() => !document.querySelector('#message').value);
    await upload();
    await page.locator('#account_statement').click();
    await discard.waitFor({state:'hidden'});
    await reply({json:{text:'دیر'}});
    assert.equal(await editor.inputValue(), '');
    // A new recording may start before the previous Send acknowledgement clears its draft.
    let acknowledge;
    await page.route('**/assistant/v1/messages', route => {
      acknowledge = () => route.fulfill({json:{kind:'status'}});
    }, {times:1});
    await editor.fill('ارسال');
    await Promise.all([page.waitForRequest('**/assistant/v1/messages'), editor.press('Enter')]);
    await upload();
    await acknowledge();
    await discard.waitFor({state:'hidden'});
    await reply({json:{text:'دیر'}});
    assert.equal(await editor.inputValue(), '');
    const beforePermission = uploads;
    await page.evaluate(() => { navigator.mediaDevices.getUserMedia = () => Promise.reject(new DOMException('', 'NotAllowedError')); });
    await mic.click();
    await page.getByText('اجازهٔ ضبط داده نشد.', {exact:false}).waitFor();
    assert.equal(uploads, beforePermission);
    assert.deepEqual(messages, ['سلام ویرایش شده', 'جدید']);
    assert.equal(health, 2);

    // Advance wall time to exercise both periodic health and recording limits.
    await page.clock.install();
    await page.reload();
    await page.locator('#mic:enabled').waitFor();
    await record();
    ready = false;
    await page.clock.fastForward(45001);
    await discard.waitFor({state:'visible'});
    await page.getByText('در حال تبدیل صدا به متن…').waitFor();
    await reply({json:{text:'محدود'}});
    await discard.waitFor({state:'hidden'});
    assert.ok(await mic.isDisabled());
    ready = true;
    await page.clock.fastForward(30001);
    await page.locator('#mic:enabled').waitFor();
    await page.addInitScript(() => { window.MediaRecorder = undefined; });
    await page.reload();
    await page.getByText('این مرورگر از ضبط صدا پشتیبانی نمی‌کند.').waitFor();
    assert.ok(await mic.isDisabled());
    await page.locator('#send:enabled').waitFor();
    console.log('PASS real synthetic recordings, health recovery, denied permission, draft edits, discard, late replies, provider errors and Retry-After. Zero automatic sends, one edited send, one typed outage turn.');
  } finally { await browser.close(); }
}
run().catch(error => { console.error(error); process.exitCode = 1; });
