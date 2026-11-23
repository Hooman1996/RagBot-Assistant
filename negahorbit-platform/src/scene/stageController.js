import { WebGLContext } from '../engine/webgl.js';
import {
  mat4Create,
  mat4Perspective,
  mat4LookAt,
  mat4Multiply,
  mat4Invert,
  ndcToWorldPlaneZ,
  clamp,
  lerp,
  lerpVec3
} from '../engine/math.js';
import { GPUParticleSystem } from './particleSystem.js';

export class StageController {
  constructor({ canvas, initialTheme, store, agentMascot, onChapterChange, onFormChange, onFpsSample }) {
    this.canvas = canvas;
    this.store = store;
    this.agentMascot = agentMascot;
    this.onChapterChange = onChapterChange;
    this.onFormChange = onFormChange;
    this.onFpsSample = onFpsSample;

    this.webgl = new WebGLContext(canvas);
    this.particles = new GPUParticleSystem(this.webgl, initialTheme);

    this.curClearRgb = new Float32Array(initialTheme.canvasRgb);
    this.tgtClearRgb = new Float32Array(initialTheme.canvasRgb);

    this.cameraPos = new Float32Array([0, 0, 14.8]);
    this.projMat = mat4Create();
    this.viewMat = mat4Create();
    this.viewProjMat = mat4Create();
    this.invViewProjMat = mat4Create();
    this.updateCamera();

    this.maxScroll = 1;
    this.targetScrollProgress = 0;
    this.smoothScrollProgress = 0;
    this.lastActiveChapter = 0;
    this.activeFormName = 'ambient-void';

    this.mouse = {
      x: 0,
      y: 0,
      px: window.innerWidth * 0.5,
      py: window.innerHeight * 0.5,
      smoothX: 0,
      smoothY: 0,
      isDown: false,
      lastX: 0,
      lastY: 0
    };
    this.worldMouse = new Float32Array([0, 0, 0]);
    this.dragRot = { x: 0, y: 0, velX: 0, velY: 0 };
    this.isMobile = window.innerWidth < 1024;

    this.running = true;
    this.startTime = performance.now();
    this.lastTime = this.startTime;
    this.fpsFrames = 0;
    this.fpsAccum = 0;

    this.cacheScrollMetrics();
    this.bindEvents();
    this.loop = this.loop.bind(this);
    requestAnimationFrame(this.loop);
  }

  updateCamera() {
    const aspect = this.webgl.width / Math.max(this.webgl.height, 1);
    mat4Perspective(this.projMat, (45 * Math.PI) / 180, aspect, 0.1, 120);
    mat4LookAt(this.viewMat, this.cameraPos, [0, 0, 0], [0, 1, 0]);
    mat4Multiply(this.viewProjMat, this.projMat, this.viewMat);
    mat4Invert(this.invViewProjMat, this.viewProjMat);
  }

  cacheScrollMetrics() {
    const vh = Math.max(window.innerHeight, 1);
    this.maxScroll = Math.max(document.documentElement.scrollHeight - vh, 1);
    this.isMobile = window.innerWidth < 1024;
    this.agentMascot?.resize(window.innerWidth, vh);
    this.readScrollProgress();
  }

  readScrollProgress() {
    this.targetScrollProgress = clamp((window.scrollY / this.maxScroll) * 4.0, 0, 4.0);
    const activeChapter = Math.round(this.targetScrollProgress);
    if (activeChapter !== this.lastActiveChapter) {
      this.lastActiveChapter = activeChapter;
      this.agentMascot?.updateChapterAccents(activeChapter);
      this.agentMascot?.pulseBadge();
      this.syncActiveForm(false);
      this.onChapterChange(activeChapter);
    }
  }

  syncActiveForm(withPulse = false) {
    const chapter = Math.round(this.targetScrollProgress);
    const formName = this.store.getFormForChapter(chapter);
    if (formName !== this.activeFormName) {
      this.activeFormName = formName;
      this.particles.transitionToForm(formName, withPulse);
      this.onFormChange(formName, chapter);
    }
    if (withPulse) {
      this.agentMascot?.pulseBadge();
    }
  }

  setTheme(theme) {
    this.tgtClearRgb.set(theme.canvasRgb);
    this.particles.setTheme(theme);
    this.agentMascot?.setTheme(theme);
  }

  bindEvents() {
    let resizeTimer = null;
    window.addEventListener('resize', () => {
      clearTimeout(resizeTimer);
      resizeTimer = setTimeout(() => {
        this.webgl.resize(window.innerWidth, window.innerHeight);
        this.updateCamera();
        this.cacheScrollMetrics();
      }, 60);
    });

    window.addEventListener('scroll', () => {
      this.readScrollProgress();
    }, { passive: true });

    window.addEventListener('pointermove', (e) => {
      const w = Math.max(window.innerWidth, 1);
      const h = Math.max(window.innerHeight, 1);
      this.mouse.px = e.clientX;
      this.mouse.py = e.clientY;
      this.mouse.x = (e.clientX / w) * 2 - 1;
      this.mouse.y = -(e.clientY / h) * 2 + 1;

      if (this.mouse.isDown) {
        const dx = e.clientX - this.mouse.lastX;
        const dy = e.clientY - this.mouse.lastY;
        this.dragRot.velY += dx * 0.0024;
        this.dragRot.velX += dy * 0.0024;
        this.mouse.lastX = e.clientX;
        this.mouse.lastY = e.clientY;
      }
    }, { passive: true });

    this.canvas.addEventListener('pointerdown', (e) => {
      this.mouse.isDown = true;
      this.mouse.lastX = e.clientX;
      this.mouse.lastY = e.clientY;
    });

    window.addEventListener('pointerup', () => {
      this.mouse.isDown = false;
    });

    document.addEventListener('visibilitychange', () => {
      this.running = !document.hidden;
      if (this.running) {
        this.lastTime = performance.now();
        requestAnimationFrame(this.loop);
      }
    });
  }

  loop(now) {
    if (!this.running) return;
    requestAnimationFrame(this.loop);

    const dt = Math.min((now - this.lastTime) * 0.001, 0.05);
    this.lastTime = now;
    const elapsed = (now - this.startTime) * 0.001;

    this.fpsFrames++;
    this.fpsAccum += dt;
    if (this.fpsAccum >= 0.5) {
      this.onFpsSample(Math.round(this.fpsFrames / this.fpsAccum));
      this.fpsFrames = 0;
      this.fpsAccum = 0;
    }

    const cAlpha = 1.0 - Math.exp(-7.5 * dt);
    lerpVec3(this.curClearRgb, this.curClearRgb, this.tgtClearRgb, cAlpha);
    this.webgl.clear(this.curClearRgb);

    this.mouse.smoothX += (this.mouse.x - this.mouse.smoothX) * 0.09;
    this.mouse.smoothY += (this.mouse.y - this.mouse.smoothY) * 0.09;
    this.smoothScrollProgress += (this.targetScrollProgress - this.smoothScrollProgress) * 0.085;

    this.dragRot.velX *= 0.92;
    this.dragRot.velY *= 0.92;
    this.dragRot.x += this.dragRot.velX;
    this.dragRot.y += this.dragRot.velY;

    ndcToWorldPlaneZ(this.worldMouse, this.mouse.x, this.mouse.y, this.invViewProjMat, 0);

    this.agentMascot?.update({
      dt,
      elapsed,
      scrollProgress: this.smoothScrollProgress,
      mousePx: this.mouse.px,
      mousePy: this.mouse.py,
      smoothMouseX: this.mouse.smoothX,
      smoothMouseY: this.mouse.smoothY,
      isMobile: this.isMobile
    });

    const heroWeight = clamp(1.0 - this.smoothScrollProgress, 0, 1);
    const aspect = this.webgl.width / Math.max(this.webgl.height, 1);
    const stageRightX = this.isMobile ? 0.0 : Math.min(3.55, aspect * 2.02);
    const targetSculptureX = lerp(stageRightX, 0.0, heroWeight);

    this.particles.render({
      dt,
      elapsed,
      viewProj: this.viewProjMat,
      targetPosX: targetSculptureX,
      dragRot: this.dragRot,
      smoothMouse: this.mouse,
      worldMouse: this.worldMouse,
      heroWeight
    });
  }
}
