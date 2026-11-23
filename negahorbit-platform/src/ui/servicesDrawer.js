import { escapeHtml, normalizeServiceUrl } from './store.js';

export class ServicesDrawer {
  constructor({ drawerEl, store, onJumpService, onToast, onOpen }) {
    this.drawerEl = drawerEl;
    this.store = store;
    this.onJumpService = onJumpService;
    this.onToast = onToast;
    this.onOpen = onOpen;

    this.bodyEl = drawerEl.querySelector('#json-drawer-body');
    this.toastEl = drawerEl.querySelector('#json-drawer-toast');
    this.importInput = drawerEl.querySelector('#import-json-input');
    this.activeTab = 'visual';

    this.bindEvents();
  }

  open(focusServiceId = null) {
    this.onOpen?.();
    this.drawerEl.classList.add('is-open');
    this.setTab('visual');

    if (focusServiceId) {
      setTimeout(() => {
        const input = this.bodyEl.querySelector(`input[data-field="url"][data-service-id="${focusServiceId}"]`);
        if (input) {
          input.scrollIntoView({ behavior: 'smooth', block: 'center' });
          input.focus();
          input.select();
          input.classList.remove('url-flash');
          void input.offsetWidth;
          input.classList.add('url-flash');
          this.showDrawerToast('Enter an IP:port or URL (then export services.json to persist on disk).');
        }
      }, 220);
    }
  }

  close() {
    this.drawerEl.classList.remove('is-open');
  }

  setTab(tab) {
    this.activeTab = tab;
    this.drawerEl.querySelectorAll('[data-json-tab]').forEach((btn) => {
      btn.classList.toggle('is-active', btn.dataset.jsonTab === tab);
    });
    this.renderBody();
  }

  showDrawerToast(msg) {
    if (!this.toastEl) return;
    this.toastEl.textContent = msg;
    this.toastEl.classList.remove('is-hidden');
    clearTimeout(this._t);
    this._t = setTimeout(() => this.toastEl.classList.add('is-hidden'), 3200);
  }

  renderBody() {
    if (this.activeTab === 'raw') {
      this.bodyEl.innerHTML = `
        <textarea id="raw-json-textarea" class="raw-textarea" spellcheck="false">${escapeHtml(JSON.stringify(this.store.config, null, 2))}</textarea>
        <div style="display:flex;align-items:center;justify-content:space-between;gap:10px;">
          <span id="raw-json-error" style="font-family:var(--font-mono);font-size:11px;color:#D99A4E;"></span>
          <button type="button" class="cta-pill" data-drawer-action="apply-raw">Apply Raw JSON</button>
        </div>
      `;
      return;
    }

    const services = this.store.getAllServices();
    this.bodyEl.innerHTML = services.map((svc) => {
      const wired = Boolean(normalizeServiceUrl(svc.url));
      return `
        <div class="svc-card ${wired ? 'is-wired' : ''}">
          <div class="svc-card-head">
            <div style="display:flex;align-items:center;gap:8px;">
              <span class="service-kicker-tag">0${svc.order}</span>
              <strong style="font-family:var(--font-grotesk);font-size:0.92rem;">${escapeHtml(svc.title)}</strong>
              <span class="service-kicker-tag">${escapeHtml(svc.kicker)}</span>
            </div>
            <button type="button" class="close-text-btn" data-preview-service="${escapeHtml(svc.id)}">Preview 3D</button>
          </div>
          <div>
            <label class="field-label">Address (URL or IP:Port)</label>
            <input class="field-control"
                   type="text"
                   data-service-id="${escapeHtml(svc.id)}"
                   data-field="url"
                   value="${escapeHtml(svc.url)}"
                   placeholder="http://192.168.10.25:8001 or https://..." />
          </div>
          <div class="field-row-2">
            <div>
              <label class="field-label">Open In</label>
              <select class="field-control" data-service-id="${escapeHtml(svc.id)}" data-field="openIn">
                <option value="same-tab" ${svc.openIn === 'same-tab' ? 'selected' : ''}>same-tab</option>
                <option value="new-tab" ${svc.openIn === 'new-tab' ? 'selected' : ''}>new-tab</option>
              </select>
            </div>
            <div>
              <label class="field-label">Status</label>
              <select class="field-control" data-service-id="${escapeHtml(svc.id)}" data-field="status">
                <option value="live" ${svc.status === 'live' ? 'selected' : ''}>live</option>
                <option value="beta" ${svc.status === 'beta' ? 'selected' : ''}>beta</option>
                <option value="coming-soon" ${svc.status === 'coming-soon' ? 'selected' : ''}>coming-soon</option>
              </select>
            </div>
          </div>
          <div>
            <label class="field-label">Tagline</label>
            <input class="field-control"
                   type="text"
                   data-service-id="${escapeHtml(svc.id)}"
                   data-field="tagline"
                   value="${escapeHtml(svc.tagline)}" />
          </div>
        </div>
      `;
    }).join('');
  }

  bindEvents() {
    this.drawerEl.addEventListener('click', async (e) => {
      if (e.target.classList.contains('drawer-backdrop') || e.target.closest('[data-drawer-action="close"]')) {
        this.close();
        return;
      }

      const tabBtn = e.target.closest('[data-json-tab]');
      if (tabBtn) {
        this.setTab(tabBtn.dataset.jsonTab);
        return;
      }

      const previewBtn = e.target.closest('[data-preview-service]');
      if (previewBtn) {
        this.close();
        this.onJumpService(previewBtn.dataset.previewService);
        return;
      }

      const act = e.target.closest('[data-drawer-action]')?.dataset.drawerAction;
      if (act === 'copy') {
        const text = JSON.stringify(this.store.config, null, 2);
        try {
          await navigator.clipboard.writeText(text);
          this.showDrawerToast('Copied services.json to clipboard.');
          this.onToast?.('Copied services.json');
        } catch (_) {
          this.showDrawerToast('Clipboard blocked — use Export .json instead.');
        }
      } else if (act === 'export') {
        const blob = new Blob([JSON.stringify(this.store.config, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'services.json';
        a.click();
        URL.revokeObjectURL(url);
        this.showDrawerToast('Exported services.json — place it in the project root.');
      } else if (act === 'reset') {
        const hadFile = this.store.resetConfig();
        this.renderBody();
        this.showDrawerToast(hadFile ? 'Reloaded from services.json.' : 'Reset to default registry.');
      } else if (act === 'apply-raw') {
        const ta = this.bodyEl.querySelector('#raw-json-textarea');
        const errEl = this.bodyEl.querySelector('#raw-json-error');
        try {
          this.store.replaceConfig(JSON.parse(ta.value));
          if (errEl) errEl.textContent = '';
          this.showDrawerToast('Applied raw JSON to NegahOrbit.');
        } catch (err) {
          if (errEl) errEl.textContent = `Invalid JSON: ${err.message}`;
        }
      }
    });

    this.bodyEl.addEventListener('input', (e) => {
      const el = e.target.closest('[data-service-id][data-field]');
      if (!el || el.tagName === 'SELECT') return;
      this.store.updateServiceField(el.dataset.serviceId, el.dataset.field, el.value);
    });

    this.bodyEl.addEventListener('change', (e) => {
      const el = e.target.closest('select[data-service-id][data-field]');
      if (!el) return;
      this.store.updateServiceField(el.dataset.serviceId, el.dataset.field, el.value);
      this.renderBody();
    });

    this.importInput?.addEventListener('change', (e) => {
      const file = e.target.files?.[0];
      if (!file) return;
      const reader = new FileReader();
      reader.onload = () => {
        try {
          this.store.replaceConfig(JSON.parse(reader.result));
          this.renderBody();
          this.showDrawerToast(`Imported ${file.name}.`);
        } catch (err) {
          this.showDrawerToast(`Import failed: ${err.message}`);
        }
      };
      reader.readAsText(file);
      e.target.value = '';
    });
  }
}
