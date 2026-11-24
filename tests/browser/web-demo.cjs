// Run against the real standalone workflow and a configured synthetic provider.
const {chromium} = require(process.env.PLAYWRIGHT_CORE || 'playwright-core');
const assert = require('node:assert/strict');
const {performance} = require('node:perf_hooks');

const compareSteps = (history, commands, results) => {
  const reported = history.steps.map(({operation, screen, outcome}) => [operation, screen, outcome]);
  assert.deepEqual(reported, results.map(r => [commands.get(r.command_id), r.observation.screen, r.outcome]));
  assert.equal(new Set(results.map(r => r.command_id)).size, results.length, 'duplicate bridge result');
  return reported;
};

async function replay() {
  const browser = await chromium.launch({executablePath: process.env.CHROMIUM_PATH, headless: true,
    args: ['--enable-unsafe-swiftshader', '--use-gl=angle', '--use-angle=swiftshader']});
  try {
    const origin = process.env.DEMO_URL || 'http://127.0.0.1:8101';
    const page = await browser.newPage({baseURL: origin, reducedMotion: 'reduce'});
    const capture = async label => {
      if (process.env.DEMO_CAPTURE_DIR)
        await page.screenshot({path:process.env.DEMO_CAPTURE_DIR + '/' + label + '.png', fullPage:true});
    };
    page.setDefaultTimeout(90000);
    const pageErrors = [], external = [], commands = new Map(), results = [];
    let history, historyPolls = 0;
    page.on('pageerror', err => pageErrors.push(err.message));
    // Run with the public internet unavailable. Record attempted asset requests as failures.
    await page.route('**/*', route => {
      const url = new URL(route.request().url());
      if (url.origin === origin) return route.continue();
      external.push(url.origin);
      return route.abort();
    });
    const routes = [], activities = [];
    page.on('request', req => {
      if (new URL(req.url()).pathname === '/assistant/v1/results') {
        const result = req.postDataJSON();
        results.push(result);
        routes.push(...(result.trace || []));
      }
    });
    page.on('response', async response => {
      const path = new URL(response.url()).pathname;
      if (!response.ok() || !/^\/assistant\/v1\/(history|sessions\/[^/]+\/resume)$/.test(path)) return;
      try {
        const data = await response.json();
        if (data.command) commands.set(data.command.command_id, data.command.operation);
        if (path.endsWith('/history')) { history = data; historyPolls++; }
      } catch (error) {
        // A reload can close a request whose headers were already received.
        if (!/navigation|aborted|No resource/i.test(error.message)) pageErrors.push(error.message);
      }
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
    const begin = performance.now();
    const timing = (label, start) => console.log(label + ': ' + ((performance.now() - start) / 1000).toFixed(1) + ' s');
    await page.goto('/');
    const selector = '[data-service-id="banking-agentic-assistant"]';
    const launch = async () => {
      const item = page.locator('li' + selector);
      if (await item.count()) await item.click();
      await page.locator('button' + selector).click();
      await screen('S-026');
      await idle();
    };
    const apiRoute = '/assistant/v1/web/profile';
    assert.equal((await page.request.get(apiRoute)).status(), 422);
    const profile = await page.request.get(apiRoute, {headers:{'X-Assistant-Session':'web-rehearsal'}});
    assert.equal(profile.status(), 200);
    const expectedIntents = ['account_statement', 'card_transfer', 'internal_transfer'];
    assert.deepEqual((await profile.json()).features.map(f => f.intent).sort(), expectedIntents);
    await launch();
    assert.equal(new URL(page.url()).pathname, '/banking/');
    assert.deepEqual((await page.locator('#screen > button').evaluateAll(nodes => nodes.map(node => node.id))).sort(), expectedIntents);
    await page.getByRole('link', {name:'بازگشت به نگاه اوربیت'}).click();
    await page.locator('#open-orbits-btn').click();
    await page.locator('[data-jump-chapter="4"]').click();
    await page.waitForFunction(() => Math.abs(document.querySelector('#section-4').getBoundingClientRect().top) < 60);
    await page.locator('#open-json-btn').click();
    const addresses = page.locator('#json-drawer.is-open input[data-field="url"]');
    const entries = () => addresses.evaluateAll(fields => fields.map(f => [f.dataset.serviceId, f.value]));
    const registry = await entries();
    assert.equal(registry.length, 8);
    await page.locator('#json-drawer input[data-field="url"]' + selector).fill('/banking/?rehearsal=1');
    await page.reload();
    await page.locator('#open-json-btn').click();
    assert.equal(await page.locator('#json-drawer input[data-field="url"]' + selector).inputValue(), '/banking/?rehearsal=1');
    assert.deepEqual((await entries()).filter(([id]) => id !== 'banking-agentic-assistant'),
      registry.filter(([id]) => id !== 'banking-agentic-assistant'));
    await page.locator('[data-drawer-action="close"]').click();
    await launch();
    assert.equal(new URL(page.url()).search, '?rehearsal=1');
    await page.getByRole('link', {name:'بازگشت به نگاه اوربیت'}).click();
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
    await launch();
    assert.equal(new URL(page.url()).pathname, '/banking/');
    timing('PASS portal launch, settings override and reload, route and session forwarding', begin);
    await page.locator('[data-theme-id="architectural-stone"]').click();
    await page.setViewportSize({width: 380, height: 780});
    assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
    if (!process.env.STATEMENT_ONLY) {
      const start = performance.now();
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
      timing('PASS internal transfer, corrected review, cancellation', start);
    }
    const statementStart = performance.now();
    const resultStart = results.length;
    await send('صورتحساب حسابم رو نشون بده');
    await page.waitForSelector('#choices button');
    assert.equal(await page.locator('#activity').innerText(), 'منتظر پاسخ شما');
    await page.reload();
    await page.waitForSelector('#choices button');
    assert.equal(await page.locator('html').getAttribute('data-theme'), 'architectural-stone');
    await capture('clarification-380');
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
      assert.equal(await page.locator('#progress li').count(), 0);
      timing('PASS provider failure', statementStart);
      assert.deepEqual(external, []);
      assert.deepEqual(pageErrors, []);
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
    const progress = page.locator('#progress');
    await progress.locator('summary').click();
    const reported = compareSteps(history, commands, results.slice(resultStart));
    const labels = {navigate:'صفحه آماده شد', fill_form:'فرم تکمیل شد', show_review:'صورتحساب نمایش داده شد', observe:'صفحه بررسی شد'};
    assert.ok(reported.every(([, , outcome]) => outcome === 'succeeded'));
    const visible = reported.map(([operation]) => labels[operation]);
    assert.deepEqual(await progress.locator('li').allTextContents(), visible);
    assert.equal(await page.locator('#screen button, #screen input:not([readonly])').count(), 0);
    console.log('PASS ordered bridge progress', reported.map(([op, screen], i) => op + ' ' + screen + ' → ' + visible[i]).join(' | '));
    timing('PASS statement ' + expected.map(r => r[2]).join(', '), statementStart);

    for (const width of [1280, 380]) {
      await page.setViewportSize({width,height:800});
      for (const id of ['obsidian-champagne', 'slate-platinum', 'espresso-copper', 'architectural-stone']) {
        await page.locator('[data-theme-id="' + id + '"]').click();
        assert.equal(await page.locator('html').getAttribute('data-theme'), id);
        assert.equal(await progress.evaluate(el => getComputedStyle(el).direction), 'rtl');
        assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
        await page.locator('#character #robot-inner').click();
        assert.equal(await page.locator('#character .agent-bounce-active').count(), 1);
        await progress.locator('summary').click();
        assert.equal(await progress.locator('ol').isVisible(), false);
        await progress.locator('summary').click();
        assert.equal(await progress.locator('ol').isVisible(), true);
        assert.deepEqual(await progress.locator('li').allTextContents(), visible);
        await capture(id + '-' + width);
      }
    }
    // Sample two new history polls after a severed request. The last completed action stays put.
    await page.route('**/history?*', route => route.abort(), {times: 1});
    await page.getByText('اتصال برقرار نیست؛ در حال اتصال دوباره', {exact: true}).waitFor();
    const pollsBeforeReconnect = historyPolls, resultsBeforeReconnect = results.length;
    await idle();
    while (historyPolls < pollsBeforeReconnect + 2)
      await page.waitForResponse(r => new URL(r.url()).pathname === '/assistant/v1/history' && r.ok());
    assert.equal(results.length, resultsBeforeReconnect);
    assert.deepEqual(await progress.locator('li').allTextContents(), visible);
    const isolation = await browser.newPage({baseURL: origin});
    try {
      await isolation.goto('/banking/');
      await isolation.locator('#send:enabled').waitFor();
      const secondSession = await isolation.evaluate(() => JSON.parse(sessionStorage.getItem('negahorbit-banking-session-v1')).session);
      const foreign = await isolation.request.get('/assistant/v1/history?' + new URLSearchParams({conversation_id:history.conversation_id}),
        {headers:{'X-Assistant-Session':secondSession}});
      assert.equal(foreign.status(), 200);
      const isolated = await foreign.json();
      assert.deepEqual([isolated.current, isolated.messages, isolated.steps, isolated.agent_steps], [null, [], [], []]);
      assert.equal(await isolation.locator('#progress li').count(), 0);
      assert.equal(await isolation.locator('#messages').innerText(), '');
    } finally { await isolation.close(); }
    console.log('PASS four palettes at desktop and narrow widths, offline assets, reconnect, repeated polling, session isolation');
    assert.deepEqual(external, []);
    assert.deepEqual(pageErrors, []);
    if (process.env.STATEMENT_ONLY) return;
    const cardStart = performance.now();
    await send('۲۵۰ هزار تومن به رضا کارت به کارت کن');
    await page.locator('#choices button').filter({hasText:/3566|۳۵۶۶/}).first().click();
    await screen('S-176');
    await idle();
    assert.equal(await read('card-transfer-source'), '****3566');
    assert.equal(await read('card-transfer-destination'), '****8880');
    assert.equal(await read('card-transfer-amount'), '2500000 ریال');
    assert.equal(await page.locator('[data-screen="W-receipt"]').count(), 0);
    await capture('review-380');
    await page.locator('#user-confirm:enabled').click();
    await screen('W-receipt');
    assert.match(await page.locator('#screen').innerText(), /رضا محمدی/);
    timing('PASS card transfer and user-only mock receipt', cardStart);

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
    assert.deepEqual(external, []);
    assert.deepEqual(pageErrors, []);
    timing('Complete rehearsal', begin);
  } finally { await browser.close(); }
}
replay().catch(err => { console.error(err); process.exitCode = 1; });
