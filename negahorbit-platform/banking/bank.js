// The replica's synthetic ledger belongs to this tab. Posting is a direct user interaction.
export const el = id => document.getElementById(id);
export function node(tag, text) {
  const item = document.createElement(tag);
  item.textContent = text;
  return item;
}


export const rial = amount => Number.isSafeInteger(amount) ? amount.toLocaleString('fa-IR') + ' ریال' : 'نامشخص';
export function today() {
  const d = new Date();
  return [d.getFullYear(), String(d.getMonth()+1).padStart(2,'0'), String(d.getDate()).padStart(2,'0')].join('-');
}
export async function destinationKey(number) {
  const digits = number.replace(/[۰-۹]/g, c => '۰۱۲۳۴۵۶۷۸۹'.indexOf(c));
  const bytes = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(digits));
  return [...new Uint8Array(bytes)].map(n => n.toString(16).padStart(2,'0')).join('');
}

export function bank(state, portfolio, save) {
  state.entries ??= [];
  function balance(key) {
    const source = portfolio.accounts.find(a => a.key === key);
    if (!Number.isSafeInteger(source?.balance)) return null;
    return state.entries.filter(e => e.account === key).reduce((sum, e) => sum + e.amount, source.balance);
  }
  function accounts() {
    const list = el('holdings');
    list.replaceChildren();
    for (const account of portfolio.accounts) {
      const item = node('article', '');
      item.append(node('strong', account.label + ' ' + account.number),
        node('p', 'موجودی قابل برداشت: ' + rial(balance(account.key))));
      for (const card of portfolio.cards.filter(c => c.account === account.key))
        item.append(node('small', 'کارت ' + card.number + ' · ' + card.status));
      list.append(item);
    }
  }
  function review(pane, posted, allowed) {
    const card = state.screen === 'S-176';
    const prefix = card ? 'card-transfer-' : 'account-transfer-';
    const instrument = (card ? portfolio.cards : portfolio.accounts).find(item => item.key === state.source);
    const source = card ? instrument?.account : instrument?.key;
    const remaining = balance(source);
    const recipient = portfolio.recipients[card ? 'card' : 'account'].find(r => r.key === state.recipient);
    const amount = Number(state.fields[prefix + 'amount']?.split(' ')[0]);
    const same = instrument?.identity === recipient?.key;
    pane.append(node('p', 'دارنده مقصد: ' + (recipient?.name || 'نامشخص')),
      node('p', 'موجودی حساب مبدا: ' + rial(remaining)));
    let problem = '';
    if (!state.draft || !recipient || remaining === null || same || !Number.isSafeInteger(amount) || amount <= 0)
      problem = 'اطلاعات انتقال برای تأیید کافی نیست.';
    else if (amount > remaining) problem = 'موجودی کافی نیست. کسری: ' + rial(amount - remaining);
    pane.append(node('p', state.cancelled ? 'این پیش‌نویس لغو شد.' : 'پیش‌نویس · مبلغی برداشت نشده است.'));
    if (problem) pane.append(node('p', problem));
    const confirm = node('button', 'تأیید انتقال آزمایشی');
    confirm.id = 'user-confirm';
    confirm.dataset.eligible = String(!problem && !state.cancelled);
    confirm.disabled = Boolean(problem || state.cancelled || !allowed());
    confirm.onclick = event => {
      if (!event.isTrusted || !allowed() || confirm.disabled || state.cancelled || !confirm.isConnected ||
          state.entries.some(e => e.id === state.draft.draft_id)) return;
      const record = {id: state.draft.draft_id, date: today(), account: source, amount: -amount,
        source: instrument.number, destination: recipient.number,
        balance: remaining - amount, description: (card ? 'کارت به کارت' : 'انتقال داخلی') + ' · ' + recipient.name,
        unit: 'IRR'};
      state.entries.push(record);
      const prior = state.screen;
      state.screen = 'W-receipt';
      state.version++;
      try { save(); } catch (err) { state.entries.pop(); state.screen = prior; state.version--; throw err; }
      accounts();
      posted();
    };
    pane.append(confirm);
  }
  accounts();
  return {review};
}
