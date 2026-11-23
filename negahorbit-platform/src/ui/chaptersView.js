import { escapeHtml, normalizeServiceUrl } from './store.js';

function statusBadgeHtml(status) {
  const s = (status || 'live').toLowerCase();
  if (s === 'beta') {
    return '<span class="status-badge is-beta"><span class="status-dot"></span>BETA</span>';
  }
  if (s === 'coming-soon' || s === 'soon') {
    return '<span class="status-badge is-soon"><span class="status-dot"></span>SOON</span>';
  }
  return '<span class="status-badge is-live"><span class="status-dot"></span>LIVE</span>';
}

export class ChaptersView {
  constructor({ rootEl, railEl, store, onLaunchService, onOpenCommandPalette, onScrollToChapter }) {
    this.rootEl = rootEl;
    this.railEl = railEl;
    this.store = store;
    this.onLaunchService = onLaunchService;
    this.onOpenCommandPalette = onOpenCommandPalette;
    this.onScrollToChapter = onScrollToChapter;
    this.activeChapterIndex = 0;

    this.bindDelegatedEvents();
    this.render();
  }

  render() {
    const categories = this.store.getCategories();

    const heroHtml = `
      <section id="section-0" class="viewport-section ${this.activeChapterIndex === 0 ? 'is-active' : ''}" data-chapter-index="0">
        <div class="chapter-panel hero-panel">
          <div class="chapter-kicker">00 / NegahOrbit Platform</div>
          <h1 class="display-title hero-title">NEGAHORBIT<br/>BANKING AI</h1>
          <p class="hero-subtitle">
            Eight enterprise AI satellites—Chatbots, ASR, Agentic Banking, and OCR—orchestrated in one unified financial orbit.
          </p>
          <div class="hero-actions">
            <button type="button" class="cta-link-btn" data-action="scroll-to" data-chapter="1">
              <span>Explore Orbits</span>
              <span class="cta-link-line"></span>
            </button>
            <button type="button" class="pill-btn" data-action="open-cmd">
              <span>Quick Jump</span>
              <kbd>⌘K</kbd>
            </button>
          </div>
        </div>
        <button type="button" class="scroll-cue" data-action="scroll-to" data-chapter="1" aria-label="Scroll to Chatbots orbit">
          <span class="scroll-cue-text">SCROLL</span>
          <span class="scroll-cue-track"><span class="scroll-cue-drop"></span></span>
        </button>
      </section>
    `;

    const chaptersHtml = categories.map((cat) => {
      const services = this.store.getServicesByCategory(cat.id);
      const activeSvc = this.store.getActiveServiceForCategory(cat.id);
      if (!activeSvc) return '';

      const titleHtml = escapeHtml(cat.title).replace(/\n/g, '<br/>');
      const resolvedUrl = normalizeServiceUrl(activeSvc.url);
      const launchText = resolvedUrl ? 'Launch Service ↗' : 'Configure IP / URL ↗';

      const listHtml = services.length > 1 ? `
        <ul class="service-list" role="list">
          ${services.map((svc) => {
            const isActive = svc.id === activeSvc.id;
            const wired = Boolean(normalizeServiceUrl(svc.url));
            return `
              <li class="service-item ${isActive ? 'is-active' : ''}"
                  data-action="service-item"
                  data-category="${escapeHtml(cat.id)}"
                  data-service-id="${escapeHtml(svc.id)}"
                  tabindex="0"
                  role="button">
                <span>${escapeHtml(svc.title)}</span>
                <span class="service-kicker-tag">${escapeHtml(svc.kicker)}</span>
                ${wired ? '<span class="service-arrow">↗</span>' : ''}
              </li>
            `;
          }).join('')}
        </ul>
      ` : '';

      return `
        <section id="section-${cat.sectionIndex}"
                 class="viewport-section ${this.activeChapterIndex === cat.sectionIndex ? 'is-active' : ''}"
                 data-chapter-index="${cat.sectionIndex}"
                 data-category="${escapeHtml(cat.id)}">
          <div class="chapter-panel service-panel">
            <div class="chapter-kicker">${escapeHtml(cat.orbitName || `Orbit ${cat.chapter}`)}</div>
            <h2 class="display-title chapter-title">${titleHtml}</h2>
            ${listHtml}
            <div class="service-detail">
              <div class="service-meta-row">
                <span>${escapeHtml(activeSvc.title)} — ${escapeHtml(activeSvc.kicker)}</span>
                <span>•</span>
                ${statusBadgeHtml(activeSvc.status)}
              </div>
              <p class="service-tagline">${escapeHtml(activeSvc.tagline)}</p>
              <div class="service-endpoint-preview">${escapeHtml(resolvedUrl || activeSvc.url || 'Unconfigured endpoint')}</div>
              <div class="service-detail-actions">
                <button type="button" class="cta-pill" data-action="launch-service" data-service-id="${escapeHtml(activeSvc.id)}">
                  <span>${launchText}</span>
                </button>
                <span class="form-tag">3D: ${escapeHtml(activeSvc.form)}</span>
              </div>
            </div>
          </div>
        </section>
      `;
    }).join('');

    this.rootEl.innerHTML = heroHtml + chaptersHtml;
    this.renderRail(categories);
  }

  renderRail(categories) {
    if (!this.railEl) return;
    const items = [
      { idx: 0, label: '00', title: 'Hero Orbit' },
      ...categories.map(c => ({ idx: c.sectionIndex, label: c.chapter, title: c.title.replace(/\n/g, ' ') }))
    ];
    this.railEl.innerHTML = items.map(item => `
      <button type="button"
              class="rail-dot ${this.activeChapterIndex === item.idx ? 'is-active' : ''}"
              data-rail-index="${item.idx}"
              title="${escapeHtml(item.title)}">
        <span>${item.label}</span>
        <span class="rail-bar"></span>
      </button>
    `).join('');
  }

  setActiveChapter(index) {
    this.activeChapterIndex = index;
    this.rootEl.querySelectorAll('.viewport-section').forEach((sec) => {
      sec.classList.toggle('is-active', Number(sec.dataset.chapterIndex) === index);
    });
    if (this.railEl) {
      this.railEl.querySelectorAll('.rail-dot').forEach((btn) => {
        btn.classList.toggle('is-active', Number(btn.dataset.railIndex) === index);
      });
    }
  }

  bindDelegatedEvents() {
    this.rootEl.addEventListener('mouseover', (e) => {
      const item = e.target.closest('[data-action="service-item"]');
      if (!item) return;
      this.store.selectService(item.dataset.category, item.dataset.serviceId);
    });

    this.rootEl.addEventListener('click', (e) => {
      const target = e.target.closest('[data-action]');
      if (!target) return;
      const action = target.dataset.action;

      if (action === 'scroll-to') {
        this.onScrollToChapter(Number(target.dataset.chapter));
      } else if (action === 'open-cmd') {
        this.onOpenCommandPalette();
      } else if (action === 'launch-service') {
        this.onLaunchService(target.dataset.serviceId);
      } else if (action === 'service-item') {
        const { category, serviceId } = target.dataset;
        const alreadyActive = this.store.getActiveServiceForCategory(category)?.id === serviceId;
        if (alreadyActive) this.onLaunchService(serviceId);
        else this.store.selectService(category, serviceId);
      }
    });

    this.rootEl.addEventListener('keydown', (e) => {
      if (e.key !== 'Enter' && e.key !== ' ') return;
      const item = e.target.closest('[data-action="service-item"]');
      if (!item) return;
      e.preventDefault();
      this.onLaunchService(item.dataset.serviceId);
    });

    if (this.railEl) {
      this.railEl.addEventListener('click', (e) => {
        const btn = e.target.closest('[data-rail-index]');
        if (!btn) return;
        this.onScrollToChapter(Number(btn.dataset.railIndex));
      });
    }
  }
}
