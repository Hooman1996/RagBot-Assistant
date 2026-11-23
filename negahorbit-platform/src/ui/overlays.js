export class OverlaysController {
  constructor({ menuEl, contactEl, toastEl, hudFormEl, hudFpsEl, onScrollToChapter, onCloseOthers }) {
    this.menuEl = menuEl;
    this.contactEl = contactEl;
    this.toastEl = toastEl;
    this.hudFormEl = hudFormEl;
    this.hudFpsEl = hudFpsEl;
    this.onScrollToChapter = onScrollToChapter;
    this.onCloseOthers = onCloseOthers;
    this._toastTimer = null;

    this.bindEvents();
  }

  openMenu() {
    this.onCloseOthers?.();
    this.menuEl.classList.add('is-open');
  }

  openContact() {
    this.onCloseOthers?.();
    this.contactEl.classList.add('is-open');
  }

  closeAll() {
    this.menuEl.classList.remove('is-open');
    this.contactEl.classList.remove('is-open');
  }

  showToast(message) {
    if (!this.toastEl) return;
    this.toastEl.textContent = message;
    this.toastEl.classList.add('is-shown');
    clearTimeout(this._toastTimer);
    this._toastTimer = setTimeout(() => this.toastEl.classList.remove('is-shown'), 2400);
  }

  updateHudForm(formName, chapterIndex) {
    if (!this.hudFormEl) return;
    const prefix = chapterIndex === 0 ? 'HERO ORBIT' : `ORBIT 0${chapterIndex}`;
    this.hudFormEl.textContent = `${prefix} // ${formName.toUpperCase()}`;
  }

  updateHudFps(fps) {
    if (!this.hudFpsEl) return;
    this.hudFpsEl.textContent = `GPU · ${fps} FPS`;
  }

  bindEvents() {
    this.menuEl.addEventListener('click', (e) => {
      if (e.target.closest('[data-close-menu]')) {
        this.menuEl.classList.remove('is-open');
        return;
      }
      const jumpBtn = e.target.closest('[data-jump-chapter]');
      if (jumpBtn) {
        this.menuEl.classList.remove('is-open');
        const idx = Number(jumpBtn.dataset.jumpChapter);
        setTimeout(() => this.onScrollToChapter(idx), 160);
      }
    });

    this.contactEl.addEventListener('click', (e) => {
      if (e.target.classList.contains('drawer-backdrop') || e.target.closest('[data-close-contact]')) {
        this.contactEl.classList.remove('is-open');
      }
    });

    const form = this.contactEl.querySelector('#contact-form');
    const success = this.contactEl.querySelector('#contact-success');
    form?.addEventListener('submit', (e) => {
      e.preventDefault();
      success?.classList.remove('is-hidden');
      form.reset();
    });

    window.addEventListener('keydown', (e) => {
      if (e.key === 'Escape') this.closeAll();
    });
  }
}
