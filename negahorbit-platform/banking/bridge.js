import {bank, destinationKey, el, node, rial, today} from './bank.js';

// Screen changes and observations share one rendered, tab-local workspace.
const home = 'S-026';
const operations = ['observe', 'navigate', 'fill_form', 'show_review'];
const targets = {
  'menu-transfer': 'S-193', 'transfer-internal': 'S-192', 'menu-card-transfer': 'S-174',
  'menu-statement': 'S-016', 'account-transfer-continue': 'S-173',
  'card-transfer-review': 'S-176', 'view-statement': 'W-statement'
};
const titles = {
  'S-026': 'خدمات بانکی', 'S-193': 'انتقال وجه', 'S-192': 'انتقال داخلی',
  'S-173': 'بازبینی انتقال داخلی', 'S-174': 'کارت به کارت',
  'S-176': 'بازبینی کارت به کارت', 'S-016': 'بازهٔ صورتحساب', 'W-statement': 'صورتحساب حساب', 'W-receipt': 'رسید انتقال آزمایشی'
};
export {el, node} from './bank.js';
export function masked(text) {
  return String(text).replace(/(?:IR)?[0-9۰-۹٠-٩]{9,}|(?:[0-9۰-۹٠-٩]{4}[- ]){3}[0-9۰-۹٠-٩]{4}/gi, value => '****' + value.slice(-4));
}
const digits = value => value.replace(/[۰-۹]/g, c => String('۰۱۲۳۴۵۶۷۸۹'.indexOf(c)));
function route(edges, start, target) {
  const queue = [[start, []]], seen = new Set([start]);
  for (const [screen, path] of queue) {
    if (screen === target) return path;
    for (const edge of edges.filter(e => e.from === screen && !seen.has(e.to))) {
      seen.add(edge.to);
      queue.push([edge.to, [...path, edge]]);
    }
  }
}

export function workspace(graph, api, state, portfolio, reasons, save, activity, interactive, canConfirm) {
  const forms = new Map(graph.forms.map(f => [f.id, f.fields]));
  const reviews = new Map(graph.features.map(f => [f.review, f.form]));
  const pane = el('screen');
  const ledger = bank(state, portfolio, save);
  function foreground() {
    el('portfolio').open = false;
    el('screen-title').focus({preventScroll:true});
    el('screen-title').scrollIntoView({block:'start'});
  }
  function open(screen, manual) {
    Object.assign(state, {screen, manual, manualInput:{}, fields:{}, draft:null, account:null, source:null, recipient:null, cancelled:false, rows:[]});
    state.version++;
    save();
    render();
    foreground();
  }
  el('bank-home').onclick = () => { if (interactive()) open(home, false); };
  function editor(feature) {
    const form = node('form', '');
    for (const field of forms.get(feature.form).filter(f => !f.secret)) {
      let records;
      if (field.lookup === 'accounts') records = portfolio.accounts.map(a => [a.key, a.label + ' ' + a.number]);
      if (field.lookup === 'cards') records = portfolio.cards.filter(c => c.kind === 'DEBIT' && c.status === 'فعال').map(c => [c.key, c.number]);
      if (field.lookup?.startsWith('contacts.')) records = portfolio.recipients[field.lookup.split('.')[1]].map(r => [r.key, r.name + ' ' + r.number]);
      if (field.type === 'choice') records = Object.entries(reasons);
      const input = node(records ? 'select' : 'input', '');
      input.id = field.ref;
      input.required = Boolean(field.requires);
      if (records) {
        input.add(new Option('انتخاب کنید', ''));
        for (const [key,label] of records) input.add(new Option(label,key));
      } else if (field.ref.startsWith('ledger-')) { input.type = 'date'; input.value = today(); }
      else if (field.type === 'money') { input.inputMode = 'numeric'; }
      const prior = state.fields[field.ref];
      if (prior) {
        if (field.lookup === 'accounts' || field.lookup === 'cards') input.value = state.source || state.account;
        else if (field.lookup?.startsWith('contacts.')) input.value = state.recipient;
        else if (field.type === 'choice') input.value = Object.keys(reasons).find(key => reasons[key] === prior) || '';
        else input.value = field.type === 'money' ? prior.split(' ')[0] : prior;
      }
      if (state.manualInput && Object.hasOwn(state.manualInput, field.ref)) input.value = state.manualInput[field.ref];
      input.oninput = () => {
        state.manualInput ??= {};
        state.manualInput[field.ref] = input.value;
        save();
      };
      const label = node('label', field.description + (field.type === 'money' ? ' (ریال)' : ''));
      label.htmlFor = input.id;
      form.append(label, input);
    }
    const submit = node('button', feature.review === 'W-statement' ? 'نمایش صورتحساب' : 'بازبینی انتقال');
    const note = node('p', '');
    note.setAttribute('role','alert');
    form.append(submit, note);
    form.onsubmit = async event => {
      event.preventDefault();
      if (!interactive() || state.editing) return;
      state.editing = true;
      try {
        const values = {};
        for (const field of forms.get(feature.form).filter(f => !f.secret)) {
          const value = el(field.ref).value.trim();
          if (field.type === 'money' && !/^\d+$/.test(digits(value))) throw Error('invalid_amount');
          if (value) values[field.ref] = field.type === 'money' ? {value:Number(digits(value)),unit:'IRR'} : value;
        }
        await fill(feature.form, values, true);
        await review(feature);
        state.manualInput = {};
        state.version++;
        save();
      } catch { note.textContent = 'اطلاعات یا بازهٔ واردشده معتبر نیست یا سرویس در دسترس نیست.'; }
      finally { state.editing = false; }
    };
    pane.append(form);
  }
  function render() {
    const screen = state.screen;
    history.replaceState(null, '', '#' + screen);
    pane.dataset.screen = screen;
    pane.replaceChildren();
    el('screen-title').textContent = titles[screen];
    el('bank-home').hidden = screen === home;
    if (screen === 'W-receipt') {
      const record = state.entries.find(e => e.id === state.draft?.draft_id);
      if (record) pane.append(node('h3', record.description), node('p', 'مبلغ ثبت‌شده: ' + rial(-record.amount)),
        node('p', 'مبدا: ' + record.source + ' · مقصد: ' + record.destination),
        node('p', 'تاریخ: ' + record.date), node('p', 'مانده حساب: ' + rial(record.balance)), node('p', 'شماره پیگیری: ' + record.id));
      return;
    }
    if (state.manual && forms.has(screen)) {
      editor(graph.features.find(f => f.form === screen));
      return;
    }
    if (screen === home || screen === 'S-193') {
      for (const feature of graph.features.filter(f => screen === home || f.intent === 'internal_transfer')) {
        const button = node('button', feature.title);
        button.id = feature.intent;
        button.onclick = () => { if (interactive()) open(feature.form, true); };
        pane.append(button);
      }
      return;
    }
    const fields = forms.get(reviews.get(screen) || screen) || [];
    for (const field of fields.filter(f => !f.secret)) {
      const value = state.fields[field.ref] || '';
      if (!value && reviews.has(screen)) continue;
      const label = node('label', field.description);
      label.htmlFor = field.ref;
      const input = node('input', '');
      input.id = field.ref;
      input.readOnly = true;
      input.value = value;
      pane.append(label, input);
    }
    if (reviews.has(screen) && screen !== 'W-statement') {
      ledger.review(pane, () => { render(); foreground(); }, canConfirm);
      if (!state.cancelled) {
        const edit = node('button', 'ویرایش انتقال');
        edit.onclick = () => {
          if (!interactive()) return;
          state.screen = reviews.get(screen);
          state.manual = true;
          state.version++;
          save();
          render();
          foreground();
        };
        pane.append(edit);
      }
      if (state.manual && !state.cancelled) {
        const discard = node('button', 'لغو پیش‌نویس');
        discard.onclick = () => { if (interactive()) { state.cancelled = true; save(); render(); } };
        pane.append(discard);
      }
    }
    if (screen === 'W-statement') {
      if (!state.rows.length) pane.append(node('p', 'در این بازه تراکنشی ثبت نشده است.'));
      else {
        const table = document.createElement('table');
        const head = table.createTHead().insertRow();
        for (const title of ['تاریخ', 'شرح', 'مبلغ (ریال)', 'مانده (ریال)']) head.append(node('th', title));
        for (const item of state.rows) {
          const row = table.insertRow();
          for (const v of [item.date, masked(item.description), item.amount, item.balance ?? '—'])
            row.insertCell().textContent = v;
        }
        pane.append(table);
      }
    }
  }
  function observe() {
    return {
      observation_version: state.version, app_version: 1, profile_version: graph.profile_version,
      screen: pane.dataset.screen, modal: null, loading: false, supported_operations: operations,
      fields: [...pane.querySelectorAll('input')].filter(i => i.value).map(i => ({ref: i.id, value: i.value})),
      draft: state.draft
    };
  }
  async function fill(form, values, manual = false) {
    if (pane.dataset.screen !== form || !forms.has(form)) throw Error('wrong_form');
    const changes = {};
    for (const [ref, value] of Object.entries(values)) {
      const field = forms.get(form).find(f => f.ref === ref && !f.secret);
      if (!field || !el(ref)) throw Error('unknown_field');
      if (field.type === 'money') {
        if (value.unit !== 'IRR' || !Number.isSafeInteger(value.value) || value.value <= 0) throw Error('invalid_amount');
        changes[ref] = value.value + ' ریال';
      } else if (['accounts', 'cards'].includes(field.lookup)) {
        const labels = await api('/web/sources/' + field.lookup);
        if (!Object.hasOwn(labels, value)) throw Error('unknown_source');
        changes[ref] = labels[value];
        if (ref.endsWith('-source')) state.source = value;
      } else if (field.lookup?.startsWith('contacts.')) {
        state.recipient = manual ? value : await destinationKey(value);
        const target = portfolio.recipients[field.lookup.split('.')[1]].find(r => r.key === state.recipient);
        changes[ref] = target?.number || '****' + value.slice(-4);
      }
      else if (ref === 'account-transfer-reason') {
        const reasons = await api('/web/reasons');
        if (!Object.hasOwn(reasons, value)) throw Error('unknown_reason');
        changes[ref] = reasons[value];
      } else changes[ref] = masked(value);
    }
    if (values['ledger-account']) state.account = values['ledger-account'];
    if (manual) state.fields = changes;
    else Object.assign(state.fields, changes);
    state.draft = {draft_id: state.draft?.draft_id || crypto.randomUUID(), draft_version: (state.draft?.draft_version || 0) + 1, review_version: 0};
    if (!manual) render();
  }
  async function review(feature) {
    const edge = graph.edges.find(e => e.from === state.screen && e.to === feature.review);
    if (!edge || targets[edge.action] !== feature.review) throw Error('unsupported_route');
    if (feature.review === 'W-statement') {
      activity('در حال دریافت صورتحساب');
      const params = new URLSearchParams({account: state.account,
        start: digits(state.fields['ledger-start']), end: digits(state.fields['ledger-end'])});
      const result = await api('/web/statement?' + params);
      if (result.rows.some(r => !Number.isSafeInteger(r.amount) || (r.balance !== null && !Number.isSafeInteger(r.balance))))
        throw Error('invalid_amount');
      state.rows = [...state.entries].reverse().filter(row => row.account === state.account &&
        row.date >= params.get('start') && row.date <= params.get('end')).concat(result.rows);
    }
    state.screen = feature.review;
    state.draft.review_version = state.draft.draft_version;
    render();
    foreground();
  }
  async function execute(command) {
    let outcome = 'succeeded', postcondition = null, error_code, trace = [];
    const args = command.arguments;
    try {
      if (Date.parse(command.expires_at) <= Date.now()) { outcome = 'rejected_expired'; throw Error('expired'); }
      if (command.session_id !== state.session || command.device_id !== state.session ||
          command.profile_version !== graph.profile_version || command.protocol_version !== 1 ||
          !operations.includes(command.operation)) { outcome = 'rejected_policy'; throw Error('unsupported_command'); }
      if (command.expected_observation_version !== state.version ||
          (command.expected_draft_version !== undefined && command.expected_draft_version !== (state.draft?.draft_version || 0))) {
        outcome = 'rejected_stale'; throw Error('stale_observation');
      }
      if (command.operation === 'navigate') {
        if (!forms.has(args.destination)) throw Error('unknown_form');
        const edges = graph.edges.filter(e => !reviews.has(e.to));
        trace = route(edges, state.screen, args.destination);
        if (!trace) {
          const path = route(edges, home, args.destination);
          if (!path) throw Error('no_route');
          trace = [{from: state.screen, to: home, action: 'back'}, ...path];
        }
        // Each navigation starts a fresh form. The workflow supplies all known fields again.
        Object.assign(state, {manual:false, manualInput:{}, source:null, recipient:null, cancelled:false, fields: {}, draft: null, account: null, rows: []});
        for (const edge of trace) {
          const next = edge.action === 'back' ? home : targets[edge.action];
          if (!next || next !== edge.to) throw Error('unsupported_route');
          state.screen = next;
          render();
        }
        if (!trace.length) render();
        postcondition = 'screen_is_' + pane.dataset.screen;
      } else if (command.operation === 'fill_form') {
        await fill(args.form, args.values);
        postcondition = 'form_filled_' + pane.dataset.screen;
      } else if (command.operation === 'show_review') {
        const feature = graph.features.find(f => f.form === state.screen);
        if (!feature || !state.draft || forms.get(state.screen).some(f => f.requires && !el(f.ref)?.value))
          throw Error('incomplete_form');
        await review(feature);
        postcondition = 'review_shown_' + pane.dataset.screen;
      } else postcondition = 'observed';
    } catch (err) {
      if (outcome === 'succeeded') outcome = 'failed';
      error_code = /^[a-z][a-z_]+$/.test(err.message) ? err.message : 'lookup_unavailable';
    }
    state.version++;
    return {command_id: command.command_id, outcome, postcondition, error_code, trace: trace || [],
      observation: observe(), draft_version: state.draft?.draft_version || 0};
  }
  render();
  return {observe, execute, cancel() {
    if (!state.manual && state.draft && !state.cancelled) { state.cancelled = true; save(); render(); }
  }};
}
