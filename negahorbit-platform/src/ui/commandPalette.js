import { escapeHtml, normalizeServiceUrl } from './store.js';
import { THEMES, THEME_ORDER } from '../config/themes.js';

export class CommandPalette {
  constructor({ modalEl, inputEl, resultsEl, store, themeController, onJumpService, onLaunchService, onOpen }) {
    this.modalEl = modalEl;
    this.inputEl = inputEl;
    this.resultsEl = resultsEl;
    this.store = store;
    this.themeController = themeController;
    this.onJumpService = onJumpService;
    this.onLaunchService = onLaunchService;
    this.onOpen = onOpen;

    this.isOpen = false;
    this.selectedIdx = 0;
    this.items = [];

    this.bindEvents();
  }

  open() {
    this.onOpen?.();
    this.isOpen = true;
    this.modalEl.classList.add('is-open');
    this.inputEl.value = '';
    this.selectedIdx = 0;
    this.renderResults('');
    setTimeout(() => this.inputEl.focus(), 35);
  }

  close() {
    this.isOpen = false;
    this.modalEl.classList.remove('is-open');
  }

  renderResults(query) {
    const q = (query || '').trim().toLowerCase();
    const services = this.store.getAllServices().filter((s) => {
      if (!q) return true;
      return `${s.title} ${s.kicker} ${s.tagline} ${s.category} ${s.form} ${s.url}`.toLowerCase().includes(q);
    });

    const themeMatches = q
      ? THEME_ORDER.map(id => THEMES[id]).filter(t => `theme palette ${t.name} ${t.id}`.toLowerCase().includes(q))
      : [];

    this.items = [
      ...services.map(s => ({ type: 'service', data: s })),
      ...themeMatches.map(t => ({ type: 'theme', data: t }))
    ];

    if (this.selectedIdx >= this.items.length) this.selectedIdx = 0;

    if (!this.items.length) {
      this.resultsEl.innerHTML = `<div style="padding:28px;text-align:center;font-family:var(--font-mono);font-size:11px;color:var(--text-muted);text-transform:uppercase;">No matching services or themes</div>`;
      return;
    }

    this.resultsEl.innerHTML = this.items.map((item, idx) => {
      const selClass = idx === this.selectedIdx ? 'is-selected' : '';
      if (item.type === 'theme') {
        return `
          <div class="cmd-row ${selClass}" data-cmd-idx="${idx}">
            <div>
              <div class="cmd-row-title">Palette: ${escapeHtml(item.data.name)}</div>
              <div class="cmd-row-sub">Switch NegahOrbit theme immediately</div>
            </div>
            <span class="service-kicker-tag">THEME</span>
          </div>
        `;
      }
      const svc = item.data;
      const wired = Boolean(normalizeServiceUrl(svc.url));
      return `
        <div class="cmd-row ${selClass}" data-cmd-idx="${idx}">
          <div style="min-width:0;">
            <div class="cmd-row-title">
              <span>${escapeHtml(svc.title)}</span>
              <span class="service-kicker-tag">${escapeHtml(svc.kicker)}</span>
            </div>
            <div class="cmd-row-sub">${escapeHtml(svc.tagline)}</div>
          </div>
          <button type="button" class="cta-pill" data-cmd-launch="${escapeHtml(svc.id)}" style="padding:5px 11px;font-size:10px;">
            ${wired ? 'Launch ↗' : 'Wire ↗'}
          </button>
        </div>
      `;
    }).join('');
  }

  activateItem(item) {
    if (!item) return;
    this.close();
    if (item.type === 'theme') {
      this.themeController.apply(item.data.id);
    } else {
      this.onJumpService(item.data.id);
    }
  }

  bindEvents() {
    this.modalEl.addEventListener('click', (e) => {
      if (e.target === this.modalEl) {
        this.close();
        return;
      }
      const launchBtn = e.target.closest('[data-cmd-launch]');
      if (launchBtn) {
        e.stopPropagation();
        this.close();
        this.onLaunchService(launchBtn.dataset.cmdLaunch);
        return;
      }
      const row = e.target.closest('[data-cmd-idx]');
      if (row) {
        this.activateItem(this.items[Number(row.dataset.cmdIdx)]);
      }
    });

    this.resultsEl.addEventListener('mouseover', (e) => {
      const row = e.target.closest('[data-cmd-idx]');
      if (!row) return;
      this.selectedIdx = Number(row.dataset.cmdIdx);
      this.resultsEl.querySelectorAll('.cmd-row').forEach((r, i) => {
        r.classList.toggle('is-selected', i === this.selectedIdx);
      });
    });

    this.inputEl.addEventListener('input', (e) => {
      this.selectedIdx = 0;
      this.renderResults(e.target.value);
    });

    window.addEventListener('keydown', (e) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault();
        if (this.isOpen) this.close();
        else this.open();
        return;
      }
      if (!this.isOpen) return;
      if (e.key === 'Escape') {
        this.close();
      } else if (e.key === 'ArrowDown' && this.items.length) {
        e.preventDefault();
        this.selectedIdx = (this.selectedIdx + 1) % this.items.length;
        this.renderResults(this.inputEl.value);
      } else if (e.key === 'ArrowUp' && this.items.length) {
        e.preventDefault();
        this.selectedIdx = (this.selectedIdx - 1 + this.items.length) % this.items.length;
        this.renderResults(this.inputEl.value);
      } else if (e.key === 'Enter' && this.items[this.selectedIdx]) {
        e.preventDefault();
        this.activateItem(this.items[this.selectedIdx]);
      }
    });
  }
}
