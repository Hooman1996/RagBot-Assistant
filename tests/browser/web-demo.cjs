// Run against the real standalone workflow and a configured synthetic provider.
const {chromium} = require(process.env.PLAYWRIGHT_CORE || 'playwright-core');
const assert = require('node:assert/strict');

async function replay() {
  const browser = await chromium.launch({executablePath: process.env.CHROMIUM_PATH, headless: true,
    args: ['--enable-unsafe-swiftshader', '--use-gl=angle', '--use-angle=swiftshader']});
  try {
    const origin = process.env.DEMO_URL || 'http://127.0.0.1:8101';
    const page = await browser.newPage({baseURL: origin});
    page.setDefaultTimeout(90000);
    page.on('pageerror', err => console.error(err.message));
    const routes = [], activities = [];
    page.on('request', req => {
      if (new URL(req.url()).pathname === '/assistant/v1/results') routes.push(...(req.postDataJSON().trace || []));
    });
    await page.exposeFunction('recordActivity', text => activities.push(text));
    await page.addInitScript(() => {
      document.addEventListener('DOMContentLoaded', () => {
        const activity = document.querySelector('#activity');
        if (activity) new MutationObserver(() => window.recordActivity(activity.textContent))
          .observe(activity, {childList: true, subtree: true});
      });
    });
    const screen = id => page.waitForSelector('[data-screen="' + id + '"]');
    const send = async text => {
      await page.locator('#message').fill(text);
      await page.locator('#send').click();
    };
    const idle = () => page.waitForSelector('#send:enabled');
    const read = ref => page.locator('#' + ref).inputValue();
    await page.goto('/');
    // Seed the original registry hash and one customized service, then launch through the upgrade.
    await page.evaluate(async () => {
      const raw = await (await fetch('/services.json')).text();
      const original = raw.replace('"/banking/"', '"TODO: http://10.0.0.10:8007"');
      let hash = 2166136261;
      for (const c of original) hash = Math.imul(hash ^ c.charCodeAt(0), 16777619);
      const config = JSON.parse(original);
      config.services[0].url = '/custom/';
      localStorage.setItem('negahorbit_services_override_v2', JSON.stringify(config));
      localStorage.setItem('negahorbit_services_hash_v2', (hash >>> 0).toString(16));
    });
    await page.reload();
    await page.waitForFunction(() => {
      const saved = JSON.parse(localStorage.getItem('negahorbit_services_override_v2'));
      return saved?.services.length === 8 && saved.services[0].url === '/custom/' &&
        saved.services.find(s => s.id === 'banking-agentic-assistant').url === '/banking/';
    });
    const selector = '[data-service-id="banking-agentic-assistant"]';
    const item = page.locator('li' + selector);
    if (await item.count()) await item.click();
    await page.locator('button' + selector).click();
    await screen('S-026');
    assert.equal(new URL(page.url()).pathname, '/banking/');
    await page.locator('[data-theme-id="architectural-stone"]').click();
    await page.setViewportSize({width: 380, height: 780});
    assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
    if (!process.env.STATEMENT_ONLY) {
      await send('۳۰۰ هزار تومن از سپرده پس‌انداز به حساب رضا انتقال داخلی بزن بابت اجاره');
      await screen('S-173');
      await idle();
      assert.equal(await read('account-transfer-source'), '****0579');
      assert.equal(await read('account-transfer-destination'), '****7800');
      assert.equal(await read('account-transfer-amount'), '3000000 ریال');
      await send('نه، مبلغ رو ۳۲۰ هزار تومن کن');
      await page.waitForFunction(() => document.querySelector('#account-transfer-amount')?.value === '3200000 ریال' && location.hash === '#S-173');
      await idle();
      await page.locator('#cancel').click();
      await page.waitForFunction(() => document.querySelector('#activity').textContent.includes('لغو شد'));
      assert.ok(routes.some(e => e.from === (process.env.SHORT_ROUTE ? 'S-026' : 'S-193') && e.to === 'S-192'));
      console.log('PASS internal transfer, corrected review, cancellation');
    }
    await send('صورتحساب حسابم رو نشون بده');
    await page.waitForSelector('#choices button');
    await page.reload();
    await page.waitForSelector('#choices button');
    assert.equal(await page.locator('html').getAttribute('data-theme'), 'architectural-stone');
    const savings = page.locator('#choices button').filter({hasText: 'پس‌انداز'});
    if (await savings.count()) await savings.first().click();
    await idle();
    const date = days => {
      const d = new Date(); d.setDate(d.getDate() - days);
      return [d.getFullYear(), String(d.getMonth()+1).padStart(2,'0'), String(d.getDate()).padStart(2,'0')].join('-');
    };
    const start = date(7), end = date(0);
    await send('حساب سپرده پس‌انداز از تاریخ ' + start + ' تا تاریخ ' + end);
    if (process.env.EXPECT_FAILURE) {
      await page.waitForFunction(() => document.querySelector('#activity').textContent === 'درخواست انجام نشد');
      assert.equal(await page.locator('#screen table').count(), 0);
      assert.match(await page.locator('#messages p').last().innerText(), /سرویس اطلاعات بانکی/);
      console.log('PASS provider failure');
      return;
    }
    await screen('W-statement');
    await idle();
    const response = await page.request.get((process.env.PROVIDER_URL || 'http://127.0.0.1:8110') +
      '/statement?' + new URLSearchParams({account:'account-2',start,end}));
    const expected = (await response.json()).rows.map(r => [r.date,r.description,String(r.amount),String(r.balance)]);
    assert.equal(expected.length, 3);
    assert.deepEqual(await page.locator('table tr:has(td)').evaluateAll(rows => rows.map(r => [...r.cells].map(c => c.textContent))), expected);
    assert.equal(await page.locator('#activity').textContent(), 'صورتحساب نمایش داده شد');
    assert.ok(activities.includes('در حال دریافت صورتحساب'));
    console.log('PASS statement', expected.map(r => r[2]).join(', '));
    if (process.env.STATEMENT_ONLY) return;
    await send('۲۵۰ هزار تومن به رضا کارت به کارت کن');
    await page.locator('#choices button').filter({hasText:/3566|۳۵۶۶/}).first().click();
    await screen('S-176');
    await idle();
    assert.equal(await read('card-transfer-source'), '****3566');
    assert.equal(await read('card-transfer-destination'), '****8880');
    assert.equal(await read('card-transfer-amount'), '2500000 ریال');
    console.log('PASS card transfer');

    for (const days of [2, 0]) {
      const tab = await browser.newPage({baseURL: origin});
      try {
        await tab.goto('/banking/');
        const day = date(days);
        if (days === 2) {
          // Hold one read until reload so the saved session has a delivered command to recover.
          await tab.route('**/web/statement?*', () => {}, {times: 1});
        }
        await tab.locator('#message').fill('صورتحساب سپرده کوتاه‌مدت از ' + day + ' تا ' + day + ' رو نشون بده');
        await tab.locator('#send').click();
        if (days === 2) {
          await tab.getByText('در حال دریافت صورتحساب', {exact: true}).waitFor();
          await tab.reload();
        }
        await tab.waitForSelector('[data-screen="W-statement"]', {timeout: 90000});
        assert.equal(await tab.locator('#ledger-account').inputValue(), '****3231');
        if (days === 2) {
          assert.deepEqual(await tab.locator('table tr:has(td) td').allTextContents(),
            [day, 'برداشت از خودپرداز', '-750000', '10500000']);
        } else await tab.getByText('در این بازه تراکنشی ثبت نشده است.').waitFor();
      } finally { await tab.close(); }
    }
    console.log('PASS second account, refresh during provider read and empty range');
  } finally { await browser.close(); }
}
replay().catch(err => { console.error(err); process.exitCode = 1; });
