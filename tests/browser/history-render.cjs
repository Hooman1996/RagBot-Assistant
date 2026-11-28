// Run against either revision of the web client with the same synthetic history.
const {chromium} = require(process.env.PLAYWRIGHT_CORE || 'playwright-core');
const assert = require('node:assert/strict');

(async () => {
  const browser = await chromium.launch({executablePath: process.env.CHROMIUM_PATH});
  try {
    const page = await browser.newPage({viewport: {width: 380, height: 780}, reducedMotion:'reduce'});
    const messages = Array.from({length: 32}, (_, i) => ({from:'user', text:'پیام آزمایشی '.repeat(12) + i}));
    let polls = 0;
    await page.route('**/history?*', route => {
      polls++;
      return route.fulfill({json:{messages, steps:[], busy:false}});
    });
    await page.goto((process.env.DEMO_URL || 'http://127.0.0.1:8101') + '/banking/');
    await page.waitForSelector('#messages p');
    await page.locator('#message').fill('آزمایش');
    await page.evaluate(() => {
      const list = document.querySelector('#messages');
      list.scrollTop = 0;
      window.removed = 0;
      new MutationObserver(records => records.forEach(r => window.removed += r.removedNodes.length))
        .observe(list, {childList:true});
      document.querySelector('#message').setSelectionRange(1, 3);
    });
    const initial = polls;
    while (polls < initial + 3) await page.waitForResponse(r => r.url().includes('/history?'));
    messages.push({from:'assistant',text:'پاسخ تازه'});
    await page.waitForFunction(() => document.querySelector('#messages').lastChild.textContent === 'پاسخ تازه');
    const trace = await page.evaluate(() => ({
      removed:window.removed, scrollTop:document.querySelector('#messages').scrollTop,
      focus:document.activeElement.id, selection:[document.activeElement.selectionStart, document.activeElement.selectionEnd]
    }));
    console.log(JSON.stringify(trace));
    assert.deepEqual(trace, {removed:0, scrollTop:0, focus:'message', selection:[1,3]});
  } finally { await browser.close(); }
})().catch(err => { console.error(err); process.exitCode = 1; });
