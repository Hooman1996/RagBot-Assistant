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
  'S-176': 'بازبینی کارت به کارت', 'S-016': 'بازهٔ صورتحساب', 'W-statement': 'صورتحساب حساب'
};
export const el = id => document.getElementById(id);
export function node(tag, text) {
  const item = document.createElement(tag);
  item.textContent = text;
  return item;
}
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

export function workspace(graph, api, state, request, activity) {
  const forms = new Map(graph.forms.map(f => [f.id, f.fields]));
  const reviews = new Map(graph.features.map(f => [f.review, f.form]));
  const pane = el('screen');
  function render() {
    const screen = state.screen;
    history.replaceState(null, '', '#' + screen);
    pane.dataset.screen = screen;
    pane.replaceChildren();
    el('screen-title').textContent = titles[screen];
    if (screen === home || screen === 'S-193') {
      for (const feature of graph.features.filter(f => screen === home || f.intent === 'internal_transfer')) {
        const button = node('button', feature.title);
        button.id = feature.intent;
        button.onclick = () => request({message: feature.title});
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
  async function fill(form, values) {
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
      } else if (field.lookup?.startsWith('contacts.')) changes[ref] = '****' + value.slice(-4);
      else if (ref === 'account-transfer-reason') {
        const reasons = await api('/web/reasons');
        if (!Object.hasOwn(reasons, value)) throw Error('unknown_reason');
        changes[ref] = reasons[value];
      } else changes[ref] = masked(value);
    }
    if (values['ledger-account']) state.account = values['ledger-account'];
    Object.assign(state.fields, changes);
    state.draft = {draft_id: state.draft?.draft_id || crypto.randomUUID(), draft_version: (state.draft?.draft_version || 0) + 1, review_version: 0};
    render();
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
        Object.assign(state, {fields: {}, draft: null, account: null, rows: []});
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
        const edge = graph.edges.find(e => e.from === state.screen && e.to === feature.review);
        if (!edge || targets[edge.action] !== feature.review) throw Error('unsupported_route');
        if (feature.review === 'W-statement') {
          activity('در حال دریافت صورتحساب');
          const params = new URLSearchParams({account: state.account,
            start: digits(el('ledger-start').value), end: digits(el('ledger-end').value)});
          const result = await api('/web/statement?' + params);
          if (result.rows.some(r => !Number.isSafeInteger(r.amount) || (r.balance !== null && !Number.isSafeInteger(r.balance))))
            throw Error('invalid_amount');
          state.rows = result.rows;
        }
        state.screen = feature.review;
        state.draft.review_version = state.draft.draft_version;
        render();
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
  return {observe, execute};
}
