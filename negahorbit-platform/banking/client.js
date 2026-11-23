import {AgentMascot} from '../src/ui/agentMascot.js';
import {ThemeController} from '../src/ui/themeController.js';
import {workspace, el, node, masked} from './bridge.js';

let mascot;
const theme = new ThemeController({container: el('themes'), onThemeChange: t => mascot?.setTheme(t)});
mascot = new AgentMascot({stageEl: el('character'), initialTheme: theme.getActiveTheme()});

const storage = 'negahorbit-banking-session-v1';
const state = JSON.parse(sessionStorage.getItem(storage)) || {
  session: crypto.randomUUID(), conversation: crypto.randomUUID(), screen: 'S-026',
  version: 1, fields: {}, rows: [], draft: null
};
const save = () => sessionStorage.setItem(storage, JSON.stringify(state));
save();
const sessionPath = '/sessions/' + state.session;
// A navigation can abort fetch before pagehide. Keep the delivered command for the next document.
let leaving = false;
window.addEventListener('beforeunload', () => { leaving = true; });
window.addEventListener('pageshow', () => { leaving = false; });
let bridge, busy = false, sending = false, connected = false, historyKey;
let clarification;
const activity = text => { if (el('activity').textContent !== text) el('activity').textContent = text; };
const providerError = 'ارتباط با سرویس اطلاعات بانکی برقرار نشد. لطفاً دوباره تلاش کنید.';
async function api(path, body) {
  const response = await fetch('/assistant/v1' + path, {
    method: body === undefined ? 'GET' : 'POST',
    headers: {'X-Assistant-Session': state.session, 'Content-Type': 'application/json'},
    body: body === undefined ? undefined : JSON.stringify(body), cache: 'no-store'
  });
  if (!response.headers.get('content-type')?.includes('application/json')) throw Error('bad_response');
  const result = await response.json();
  if (!response.ok) {
    const err = Error(response.status === 422 ? 'invalid_request' : response.status === 503 ? 'lookup_unavailable' : 'request_failed');
    err.status = response.status;
    throw err;
  }
  return result;
}
function controls() {
  const disabled = !connected || busy || sending;
  el('send').disabled = disabled;
  for (const button of document.querySelectorAll('#screen button, #choices button')) button.disabled = disabled;
}
async function request(payload) {
  if (!connected || busy || sending) return;
  sending = true;
  controls();
  try {
    await api('/messages', {session_id: state.session, device_id: state.session,
      request_id: crypto.randomUUID(), conversation_id: state.conversation,
      user_context: {user_ref: 'web-demo', locale: 'fa-IR'}, observation: bridge.observe(), ...payload});
    busy = true;
    el('message').value = '';
    el('error').textContent = '';
    state.event = '';
    save();
    activity('در حال بررسی درخواست');
  } catch { el('error').textContent = 'پیام ارسال نشد. دوباره تلاش کنید.'; }
  finally { sending = false; controls(); }
}
el('compose').onsubmit = e => { e.preventDefault(); const message = el('message').value.trim(); if (message) request({message}); };
el('cancel').onclick = async () => {
  try { await api(sessionPath + '/cancel', {}); activity('در حال لغو کار'); }
  catch { el('error').textContent = 'لغو کار انجام نشد.'; }
};

function actionText(step) {
  if (step.outcome !== 'succeeded') return 'انجام کار متوقف شد';
  if (step.operation === 'navigate') return 'صفحه باز شد';
  if (step.operation === 'fill_form') return 'فرم تکمیل شد';
  if (step.operation === 'show_review') return step.screen === 'W-statement' ? 'صورتحساب نمایش داده شد' : 'بازبینی نمایش داده شد';
  return 'صفحه بررسی شد';
}
function showHistory(h) {
  busy = h.busy;
  clarification = h.current?.kind === 'clarification' ? h.current : null;
  const key = JSON.stringify([h.messages, clarification]);
  if (key !== historyKey) {
    historyKey = key;
    const messages = el('messages');
    messages.replaceChildren(...h.messages.map(m => {
      const p = node('p', masked(m.text)); p.className = m.from; return p;
    }));
    messages.scrollTop = messages.scrollHeight;
    el('choices').replaceChildren(...(clarification?.options || []).map(option => {
      const button = node('button', masked(option.label));
      const selection = {ref: option.ref, question_id: clarification.question_id};
      button.onclick = () => request({selection});
      return button;
    }));
  }
  const steps = h.steps;
  const status = h.current?.task_state;
  el('cancel').hidden = !(busy || clarification || ['ready_for_user', 'paused'].includes(status));
  const endings = {failed: 'درخواست انجام نشد', cancelled: 'کار لغو شد', paused: 'کار متوقف شده است'};
  if (clarification) activity('منتظر پاسخ شما');
  else if (busy) activity(state.event || 'در حال انجام درخواست');
  else activity(endings[status] || (steps.length ? actionText(steps.at(-1)) : 'آمادهٔ دریافت درخواست'));
  controls();
}
async function poll() {
  try {
    if (!bridge) bridge = workspace(await api('/web/profile'), api, state, request, activity);
    if (!state.pending && !state.result) {
      const reply = await api(sessionPath + '/resume', bridge.observe());
      if (reply.kind === 'command') { state.pending = reply.command; save(); }
    }
    if (state.pending) {
      const result = await bridge.execute(state.pending);
      if (leaving) return;
      state.result = result;
      state.event = actionText({...state.result, operation: state.pending.operation, screen: state.screen});
      if (state.result.error_code === 'lookup_unavailable') el('error').textContent = providerError;
      delete state.pending;
      save();
      activity(state.event);
    }
    if (state.result) {
      try { await api('/results', state.result); }
      catch (err) { if (err.status !== 409) throw err; }
      delete state.result;
      save();
    }
    const h = await api('/history?conversation_id=' + state.conversation);
    connected = true;
    showHistory(h);
  } catch {
    connected = false;
    activity('اتصال برقرار نیست؛ در حال اتصال دوباره');
    controls();
  } finally { setTimeout(poll, connected ? 240 : 1000); }
}
poll();
