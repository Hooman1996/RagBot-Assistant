import { DEFAULT_SERVICES_CONFIG } from '../config/defaultServices.js';

const STORAGE_KEY = 'negahorbit_services_override_v2';
const STORAGE_HASH_KEY = 'negahorbit_services_hash_v2';
const SERVICES_FILE_URL = './services.json';

function clone(obj) {
  return JSON.parse(JSON.stringify(obj));
}

function hashString(str) {
  let h = 2166136261;
  for (let i = 0; i < str.length; i++) {
    h ^= str.charCodeAt(i);
    h = Math.imul(h, 16777619);
  }
  return (h >>> 0).toString(16);
}

export function normalizeServiceUrl(rawUrl) {
  if (!rawUrl || typeof rawUrl !== 'string') return null;
  const trimmed = rawUrl.trim();
  if (!trimmed || /^todo\b/i.test(trimmed)) return null;
  if (/^https?:\/\/[^\s]+$/i.test(trimmed)) return trimmed;
  if (trimmed.startsWith('/') && !trimmed.includes(' ')) return trimmed;
  // Bare IP:port or hostname:port (e.g. 192.168.10.25:8001, localhost:8080)
  if (/^([a-z0-9.-]+|\d{1,3}(\.\d{1,3}){3})(:\d{1,5})(\/[^\s]*)?$/i.test(trimmed)) {
    return `http://${trimmed}`;
  }
  // Bare domain (e.g. hibot.negahbank.ir/app)
  if (/^[a-z0-9-]+\.[a-z]{2,}(\/[^\s]*)?$/i.test(trimmed)) {
    return `https://${trimmed}`;
  }
  return null;
}

export function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

export class PlatformStore {
  constructor() {
    this.fileConfig = null;
    this.config = this.loadInitialLocal();
    this.activeServiceByCategory = {};
    this.listeners = new Set();
    this.syncDefaultActiveServices();
  }

  loadInitialLocal() {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      if (saved) {
        const parsed = JSON.parse(saved);
        if (parsed && Array.isArray(parsed.categories) && Array.isArray(parsed.services)) {
          return parsed;
        }
      }
    } catch (_) {}
    return clone(DEFAULT_SERVICES_CONFIG);
  }

  async loadFromServicesFile() {
    try {
      const res = await fetch(SERVICES_FILE_URL, { cache: 'no-store' });
      if (!res.ok) return null;
      const rawText = await res.text();
      const parsed = JSON.parse(rawText);
      if (!parsed || !Array.isArray(parsed.services)) return null;
      if (!Array.isArray(parsed.categories)) {
        parsed.categories = clone(DEFAULT_SERVICES_CONFIG.categories);
      }

      this.fileConfig = clone(parsed);
      const nextHash = hashString(rawText);
      let prevHash = null;
      let hasOverride = false;
      try {
        prevHash = localStorage.getItem(STORAGE_HASH_KEY);
        hasOverride = Boolean(localStorage.getItem(STORAGE_KEY));
      } catch (_) {}

      // If services.json changed on disk (or no local override exists), adopt services.json
      if (!hasOverride || prevHash !== nextHash) {
        this.config = clone(parsed);
        try {
          localStorage.removeItem(STORAGE_KEY);
          localStorage.setItem(STORAGE_HASH_KEY, nextHash);
        } catch (_) {}
        this.syncDefaultActiveServices();
        this.emit('config');
      }
      return parsed;
    } catch (_) {
      return null;
    }
  }

  syncDefaultActiveServices() {
    this.config.categories.forEach((cat) => {
      const list = this.getServicesByCategory(cat.id);
      if (!list.length) return;
      const current = this.activeServiceByCategory[cat.id];
      if (!list.some(s => s.id === current)) {
        this.activeServiceByCategory[cat.id] = list[0].id;
      }
    });
  }

  subscribe(fn) {
    this.listeners.add(fn);
    return () => this.listeners.delete(fn);
  }

  emit(eventType, payload) {
    this.listeners.forEach(fn => fn(eventType, payload));
  }

  getCategories() {
    return this.config.categories.slice().sort((a, b) => a.sectionIndex - b.sectionIndex);
  }

  getAllServices() {
    return this.config.services.slice().sort((a, b) => (a.order || 0) - (b.order || 0));
  }

  getServicesByCategory(categoryId) {
    return this.config.services
      .filter(s => s.category === categoryId)
      .sort((a, b) => (a.order || 0) - (b.order || 0));
  }

  getServiceById(serviceId) {
    return this.config.services.find(s => s.id === serviceId) || null;
  }

  getActiveServiceForCategory(categoryId) {
    const list = this.getServicesByCategory(categoryId);
    if (!list.length) return null;
    const id = this.activeServiceByCategory[categoryId];
    return list.find(s => s.id === id) || list[0];
  }

  getFormForChapter(chapterIndex) {
    if (chapterIndex <= 0) return 'ambient-void';
    const cat = this.config.categories.find(c => c.sectionIndex === chapterIndex);
    if (!cat) return 'ecosystem-globe';
    const svc = this.getActiveServiceForCategory(cat.id);
    return svc?.form || 'ecosystem-globe';
  }

  selectService(categoryId, serviceId) {
    if (this.activeServiceByCategory[categoryId] === serviceId) return false;
    this.activeServiceByCategory[categoryId] = serviceId;
    this.emit('selection', { categoryId, serviceId });
    return true;
  }

  updateServiceField(serviceId, field, value) {
    const svc = this.getServiceById(serviceId);
    if (!svc) return;
    svc[field] = value;
    this.persistOverride();
    this.emit('config', { serviceId, field });
  }

  replaceConfig(nextConfig) {
    if (!nextConfig || !Array.isArray(nextConfig.services)) {
      throw new Error('JSON must include a "services" array.');
    }
    const normalized = clone(nextConfig);
    if (!Array.isArray(normalized.categories)) {
      normalized.categories = clone(DEFAULT_SERVICES_CONFIG.categories);
    }
    this.config = normalized;
    this.syncDefaultActiveServices();
    this.persistOverride();
    this.emit('config');
  }

  resetConfig() {
    try { localStorage.removeItem(STORAGE_KEY); } catch (_) {}
    this.config = clone(this.fileConfig || DEFAULT_SERVICES_CONFIG);
    this.syncDefaultActiveServices();
    this.emit('config');
    return Boolean(this.fileConfig);
  }

  persistOverride() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(this.config));
    } catch (_) {}
  }
}
