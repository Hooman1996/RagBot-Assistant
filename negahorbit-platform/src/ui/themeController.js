import { THEMES, THEME_ORDER, THEME_STORAGE_KEY } from '../config/themes.js';

export class ThemeController {
  constructor({ container, onThemeChange, onToast }) {
    this.container = container;
    this.onThemeChange = onThemeChange;
    this.onToast = onToast;

    this.currentId = this.readInitialTheme();
    this.render();
    this.apply(this.currentId, { silent: true, persist: false });
    this.bindEvents();
  }

  readInitialTheme() {
    try {
      const saved = localStorage.getItem(THEME_STORAGE_KEY);
      if (saved && THEMES[saved]) return saved;
    } catch (_) {}
    return 'obsidian-champagne';
  }

  applyDefaultIfUnset(themeId) {
    if (!themeId || !THEMES[themeId]) return;
    let hasSaved = false;
    try { hasSaved = Boolean(localStorage.getItem(THEME_STORAGE_KEY)); } catch (_) {}
    if (!hasSaved) {
      this.apply(themeId, { silent: true, persist: false });
    }
  }

  getActiveTheme() {
    return THEMES[this.currentId] || THEMES['obsidian-champagne'];
  }

  render() {
    if (!this.container) return;
    const swatches = THEME_ORDER.map((id) => {
      const t = THEMES[id];
      return `
        <button type="button"
                class="theme-swatch-btn"
                data-theme-id="${t.id}"
                title="${t.name} (Press T to cycle)"
                aria-label="Switch to ${t.name} palette">
          <span class="swatch-disc" style="background: linear-gradient(135deg, ${t.swatches[0]} 50%, ${t.swatches[1]} 50%);"></span>
        </button>
      `;
    }).join('');

    this.container.innerHTML = `
      <div class="theme-swatches">${swatches}</div>
      <span class="theme-divider"></span>
      <span class="theme-label" id="active-theme-label"></span>
    `;

    this.container.addEventListener('click', (e) => {
      const btn = e.target.closest('[data-theme-id]');
      if (!btn) return;
      this.apply(btn.dataset.themeId);
    });
  }

  apply(themeId, { silent = false, persist = true } = {}) {
    const theme = THEMES[themeId] || THEMES['obsidian-champagne'];
    this.currentId = theme.id;
    document.documentElement.setAttribute('data-theme', theme.id);
    if (persist) {
      try { localStorage.setItem(THEME_STORAGE_KEY, theme.id); } catch (_) {}
    }

    if (this.container) {
      this.container.querySelectorAll('[data-theme-id]').forEach((btn) => {
        btn.classList.toggle('is-active', btn.dataset.themeId === theme.id);
      });
      const label = this.container.querySelector('#active-theme-label');
      if (label) label.textContent = theme.name;
    }

    this.onThemeChange(theme);
    if (!silent && this.onToast) {
      this.onToast(`Palette: ${theme.name}`);
    }
  }

  cycle() {
    const idx = THEME_ORDER.indexOf(this.currentId);
    const nextId = THEME_ORDER[(idx + 1) % THEME_ORDER.length];
    this.apply(nextId);
  }

  bindEvents() {
    window.addEventListener('keydown', (e) => {
      if (e.metaKey || e.ctrlKey || e.altKey) return;
      const tag = (e.target?.tagName || '').toLowerCase();
      if (tag === 'input' || tag === 'textarea' || tag === 'select' || e.target?.isContentEditable) return;
      if (e.key.toLowerCase() === 't') {
        e.preventDefault();
        this.cycle();
      }
    });
  }
}
