import {AgentMascot} from '../src/ui/agentMascot.js';
import {ThemeController} from '../src/ui/themeController.js';
import {newId} from './bank.js';
import {workspace, el, node, masked} from './bridge.js';

import {dictation} from './dictation.js';

const editor = el('message');
const cancelVoice = dictation(editor, el('mic'), el('discard'), el('voice-status'));
let mascot;
const theme = new ThemeController({container: el('themes'), onThemeChange: t => mascot?.setTheme(t)});
const paletteNames = {
  'obsidian-champagne':'مشکی و شامپاینی', 'slate-platinum':'خاکستری و پلاتینی',
  'espresso-copper':'قهوه‌ای و مسی', 'architectural-stone':'سنگی'
};
for (const button of el('themes').querySelectorAll('[data-theme-id]')) {
  button.title = 'انتخاب رنگ‌بندی ' + paletteNames[button.dataset.themeId];
  button.setAttribute('aria-label', button.title);
}
mascot = new AgentMascot({stageEl: el('character'), initialTheme: theme.getActiveTheme()});

const storage = 'negahorbit-banking-session-v1';
const state = JSON.parse(sessionStorage.getItem(storage)) || {
  session: newId(), conversation: newId(), screen: 'S-026',
  version: 1, fields: {}, rows: [], draft: null
};
const save = () => sessionStorage.setItem(storage, JSON.stringify(state));
save();
state.editing = false;
const sessionPath = '/sessions/' + state.session;
// A navigation can abort fetch before pagehide. Keep the delivered command for the next document.
let leaving = false;
window.addEventListener('beforeunload', () => { leaving = true; });
window.addEventListener('pageshow', () => { leaving = false; });
let bridge, busy = false, sending = false, connected = false, choicesKey, progressKey;
let clarification, taskStatus, previousMessages = [];
const interactive = () => connected && !busy && !sending && !state.editing;
const canConfirm = () => interactive() && (state.manual || taskStatus === 'ready_for_user');
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
  const disabled = !connected || busy || sending || state.editing;
  el('send').disabled = disabled;
  const confirm = el('user-confirm');
  if (confirm) confirm.disabled = !canConfirm() || state.cancelled || confirm.dataset.eligible !== 'true';
  for (const button of document.querySelectorAll('#screen button:not(#user-confirm), #choices button, #bank-home')) button.disabled = disabled;
}
async function request(payload) {
  if (!connected || busy || sending || state.editing) return;
  cancelVoice();
  const submitted = editor.value;
  sending = true;
  controls();
  try {
    await api('/messages', {session_id: state.session, device_id: state.session,
      request_id: newId(), conversation_id: state.conversation,
      user_context: {user_ref: 'web-demo', locale: 'fa-IR'}, observation: bridge.observe(), ...payload});
    busy = true;
    if (editor.value === submitted) {
      cancelVoice();
      editor.value = '';
    }
    el('error').textContent = '';
    state.event = '';
    save();
    activity('در حال بررسی درخواست');
  } catch { el('error').textContent = 'پیام ارسال نشد. دوباره تلاش کنید.'; }
  finally { sending = false; controls(); }
}
el('open-bank').onclick = () => el('screen-title').focus();
el('open-chat').onclick = () => (document.querySelector('#choices button:enabled') || editor).focus();
el('compose').onsubmit = e => { e.preventDefault(); const message = el('message').value.trim(); if (message) request({message}); };
editor.onkeydown = e => {
  if (e.key !== 'Enter' || e.shiftKey || e.isComposing || e.keyCode === 229) return;
  e.preventDefault();
  if (!e.repeat && !el('send').disabled) el('compose').requestSubmit(el('send'));
};
el('cancel').onclick = async () => {
  cancelVoice();
  try { await api(sessionPath + '/cancel', {}); bridge.cancel(); activity('در حال لغو کار'); }
  catch { el('error').textContent = 'لغو کار انجام نشد.'; }
};

function actionText(step) {
  if (step.outcome !== 'succeeded') return 'انجام کار متوقف شد';
  if (step.operation === 'navigate') return 'صفحه آماده شد';
  if (step.operation === 'fill_form') return 'فرم تکمیل شد';
  if (step.operation === 'show_review') return step.screen === 'W-statement' ? 'صورتحساب نمایش داده شد' : 'بازبینی نمایش داده شد';
  return 'صفحه بررسی شد';
}
function updateMessages(lines) {
  // History normally grows by appending. Only inspect the DOM for a changed suffix.
  let first = 0;
  while (first < lines.length && previousMessages[first]?.text === lines[first].text &&
         previousMessages[first]?.from === lines[first].from) first++;
  if (first === lines.length && first === previousMessages.length) return;
  previousMessages = lines;
  const list = el('messages');
  const follow = list.scrollHeight - list.clientHeight - list.scrollTop <= 4;
  while (list.children.length > first) list.lastChild.remove();
  for (const message of lines.slice(first)) {
    const item = node('p', masked(message.text));
    item.className = message.from;
    list.append(item);
  }
  if (follow) list.scrollTop = list.scrollHeight;
}
function showHistory(h) {
  busy = h.busy;
  clarification = h.current?.kind === 'clarification' ? h.current : null;
  updateMessages(h.messages);
  const key = JSON.stringify(clarification);
  if (key !== choicesKey) {
    choicesKey = key;
    el('choices').replaceChildren(...(clarification?.options || []).map(option => {
      const button = node('button', masked(option.label));
      const selection = {ref: option.ref, question_id: clarification.question_id};
      button.onclick = () => request({selection});
      return button;
    }));
  }
  const steps = h.steps.map(actionText);
  if (clarification) steps.push('پرسش برای تکمیل اطلاعات');
  const nextProgress = JSON.stringify(steps);
  if (nextProgress !== progressKey) {
    progressKey = nextProgress;
    const progress = el('progress');
    progress.querySelector('ol').replaceChildren(...steps.map(text => node('li', text)));
    progress.hidden = steps.length === 0;
  }
  const status = h.current?.task_state;
  taskStatus = status;
  el('cancel').hidden = !(busy || clarification || ['ready_for_user', 'paused'].includes(status));
  if (status === 'cancelled') bridge.cancel();
  const endings = {failed: 'درخواست انجام نشد', cancelled: 'کار لغو شد', paused: 'کار متوقف شده است'};
  if (state.screen === 'W-receipt' && !busy && !clarification) {
    el('cancel').hidden = true;
    activity('انتقال آزمایشی با تأیید شما ثبت شد');
  } else if (clarification) activity('منتظر پاسخ شما');
  else if (busy) activity(state.event || 'در حال انجام درخواست');
  else activity(endings[status] || (steps.length ? steps.at(-1) : 'آمادهٔ دریافت درخواست'));
  controls();
}
async function poll() {
  try {
    if (!bridge) {
      const [graph, portfolio, reasons] = await Promise.all([api('/web/profile'), api('/web/portfolio'), api('/web/reasons')]);
      bridge = workspace(graph, api, state, portfolio, reasons, save, activity, interactive, canConfirm, cancelVoice);
    }
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
