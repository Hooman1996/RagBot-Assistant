// Exercise actual browser input, including the user-only mock posting boundary.
const {chromium} = require(process.env.PLAYWRIGHT_CORE || 'playwright-core');
const assert = require('node:assert/strict');

async function replay() {
  const browser = await chromium.launch({executablePath:process.env.CHROMIUM_PATH});
  try {
    const page = await browser.newPage({baseURL:process.env.DEMO_URL || 'http://127.0.0.1:8101',
      viewport:{width:380,height:780}});
    page.setDefaultTimeout(90000);
    page.on('pageerror', e => console.error(e));
    await page.goto('/banking/');
    const idle = () => page.locator('#send:enabled').waitFor();
    const screen = id => page.locator('[data-screen="'+id+'"]').waitFor();
    const send = async text => {
      await page.locator('#message').fill(text);
      await page.locator('#send').click();
    };
    const pick = async (id, suffix) => {
      const option = page.locator('#'+id+' option').filter({hasText:suffix}).first();
      await page.locator('#'+id).selectOption(await option.getAttribute('value'));
    };
    await idle();
    await send('500 هزار تومن برای رضا کارت به کارت کن');
    await page.locator('#choices button').filter({hasText:/4544|۴۵۴۴/}).click();
    await screen('S-176');
    await idle();
    const box = await page.locator('#card-transfer-amount').boundingBox();
    assert.ok(box.y >= 0 && box.y + box.height < 780);
    assert.equal(await page.evaluate(()=>document.activeElement.id),'screen-title');
    assert.match(await page.locator('#screen').innerText(), /رضا محمدی/);
    assert.match(await page.locator('#screen').innerText(), /کسری/);
    assert.ok(await page.locator('#user-confirm').isDisabled());
    await send('اوکیه بزن بره');
    await idle();
    assert.equal(await page.locator('#screen').getAttribute('data-screen'),'S-176');
    await page.locator('#cancel').click();
    await page.getByText('این پیش‌نویس لغو شد.').waitFor();
    console.log('PASS foreground review, insufficient balance, chat approval and cancellation');
    await page.locator('#bank-home').click();
    await page.setViewportSize({width:1280,height:800});
    for (const [intent,prefix] of [['card_transfer','card-transfer'],['internal_transfer','account-transfer']]) {
      await page.locator('#'+intent).click();
      await pick(prefix+'-source', prefix === 'card-transfer' ? '3566' : '3231');
      await pick(prefix+'-destination','رضا');
      await page.locator('#'+prefix+'-amount').fill('250000');
      if (intent === 'internal_transfer') await page.locator('#account-transfer-reason').selectOption({index:1});
      await page.getByRole('button',{name:'بازبینی انتقال'}).click();
      await page.getByRole('button',{name:'ویرایش انتقال'}).click();
      assert.equal(await page.locator('#'+prefix+'-amount').inputValue(), '250000');
      await page.getByRole('button',{name:'بازبینی انتقال'}).click();
      const confirm = page.locator('#user-confirm');
      await confirm.waitFor();
      await confirm.evaluate(b=>b.click());
      assert.equal(await page.locator('[data-screen="W-receipt"]').count(),0);
      await confirm.click();
      await screen('W-receipt');
      const receipt = await page.locator('#screen').innerText();
      assert.match(receipt,/شماره پیگیری/);
      await page.reload();
      await screen('W-receipt');
      assert.equal(await page.locator('#screen').innerText(),receipt);
      await page.locator('#bank-home').click();
    }
    await page.locator('#portfolio summary').click();
    assert.match(await page.locator('#holdings').innerText(), /۱۲٬۰۰۰٬۰۰۰/);
    await page.locator('#account_statement').click();
    await pick('ledger-account','3231');
    await page.getByRole('button',{name:'نمایش صورتحساب'}).click();
    await screen('W-statement');
    const rows = await page.locator('#screen tr:has(td)').allTextContents();
    assert.equal(rows.length,2);
    assert.ok(rows.every(row=>row.includes('-250000') && row.includes('رضا')));
    console.log('PASS both manual confirmations, receipt reload, updated account and two statement debits');
  } finally {await browser.close();}
}
replay().catch(e=>{console.error(e);process.exitCode=1;});
