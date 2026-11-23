import { PlatformStore, normalizeServiceUrl } from './ui/store.js';
import { ThemeController } from './ui/themeController.js';
import { ChaptersView } from './ui/chaptersView.js';
import { CommandPalette } from './ui/commandPalette.js';
import { ServicesDrawer } from './ui/servicesDrawer.js';
import { OverlaysController } from './ui/overlays.js';
import { AgentMascot } from './ui/agentMascot.js';
import { StageController } from './scene/stageController.js';

async function initNegahOrbit() {
  const store = new PlatformStore();

  const scrollToChapter = (chapterIndex) => {
    const el = document.getElementById(`section-${chapterIndex}`);
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  let stage = null;
  let cmdPalette = null;
  let servicesDrawer = null;

  const overlays = new OverlaysController({
    menuEl: document.getElementById('menu-overlay'),
    contactEl: document.getElementById('contact-drawer'),
    toastEl: document.getElementById('global-toast'),
    hudFormEl: document.getElementById('hud-form-label'),
    hudFpsEl: document.getElementById('hud-fps-label'),
    onScrollToChapter: scrollToChapter,
    onCloseOthers: () => {
      cmdPalette?.close();
      servicesDrawer?.close();
    }
  });

  const themeController = new ThemeController({
    container: document.getElementById('theme-switcher'),
    onThemeChange: (theme) => stage?.setTheme(theme),
    onToast: (msg) => overlays.showToast(msg)
  });

  const launchService = (serviceId) => {
    const svc = store.getServiceById(serviceId);
    if (!svc) return;
    const resolved = normalizeServiceUrl(svc.url);
    if (resolved) {
      if (svc.openIn === 'new-tab') {
        window.open(resolved, '_blank', 'noopener,noreferrer');
      } else {
        window.location.href = resolved;
      }
    } else {
      servicesDrawer.open(svc.id);
    }
  };

  const jumpToService = (serviceId) => {
    const svc = store.getServiceById(serviceId);
    if (!svc) return;
    store.selectService(svc.category, svc.id);
    const cat = store.getCategories().find(c => c.id === svc.category);
    if (cat) scrollToChapter(cat.sectionIndex);
    stage?.syncActiveForm(true);
  };

  const chaptersView = new ChaptersView({
    rootEl: document.getElementById('chapters-root'),
    railEl: document.getElementById('orbit-rail'),
    store,
    onLaunchService: launchService,
    onOpenCommandPalette: () => cmdPalette.open(),
    onScrollToChapter: scrollToChapter
  });

  cmdPalette = new CommandPalette({
    modalEl: document.getElementById('cmd-modal'),
    inputEl: document.getElementById('cmd-input'),
    resultsEl: document.getElementById('cmd-results'),
    store,
    themeController,
    onJumpService: jumpToService,
    onLaunchService: launchService,
    onOpen: () => {
      overlays.closeAll();
      servicesDrawer?.close();
    }
  });

  servicesDrawer = new ServicesDrawer({
    drawerEl: document.getElementById('json-drawer'),
    store,
    onJumpService: jumpToService,
    onToast: (msg) => overlays.showToast(msg),
    onOpen: () => {
      overlays.closeAll();
      cmdPalette?.close();
    }
  });

  const fileConfig = await store.loadFromServicesFile();
  if (fileConfig?.defaultTheme) {
    themeController.applyDefaultIfUnset(fileConfig.defaultTheme);
  }

  const agentMascot = new AgentMascot({
    stageEl: document.getElementById('agent-stage'),
    initialTheme: themeController.getActiveTheme()
  });

  stage = new StageController({
    canvas: document.getElementById('webgl-canvas'),
    initialTheme: themeController.getActiveTheme(),
    store,
    agentMascot,
    onChapterChange: (chapterIdx) => {
      chaptersView.setActiveChapter(chapterIdx);
    },
    onFormChange: (formName, chapterIdx) => {
      overlays.updateHudForm(formName, chapterIdx);
    },
    onFpsSample: (fps) => {
      overlays.updateHudFps(fps);
    }
  });

  store.subscribe((eventType) => {
    chaptersView.render();
    stage.cacheScrollMetrics();
    stage.syncActiveForm(eventType === 'selection');
  });

  document.getElementById('open-cmd-btn')?.addEventListener('click', () => cmdPalette.open());
  document.getElementById('open-json-btn')?.addEventListener('click', () => servicesDrawer.open());
  document.getElementById('open-orbits-btn')?.addEventListener('click', () => overlays.openMenu());
  document.getElementById('open-contact-btn')?.addEventListener('click', () => overlays.openContact());
  document.getElementById('open-menu-btn')?.addEventListener('click', () => overlays.openMenu());
}

window.addEventListener('DOMContentLoaded', () => {
  initNegahOrbit().catch((err) => console.error('NegahOrbit init error:', err));
});
