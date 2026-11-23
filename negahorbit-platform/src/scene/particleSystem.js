import { createProgram } from '../engine/webgl.js';
import { mat4Create, mat4FromTRS, mat4Invert, transformPointMat4, lerpVec3, lerp } from '../engine/math.js';
import { PARTICLE_COUNT, FORM_MODE_ID, buildAllServiceShapes } from './serviceShapes.js';

const PARTICLE_VS = `
  precision highp float;

  attribute vec3 aPosA;
  attribute vec4 aMetaA;
  attribute vec3 aPosB;
  attribute vec4 aMetaB;

  uniform mat4 uModel;
  uniform mat4 uViewProj;
  uniform float uMorph;
  uniform float uTime;
  uniform float uSwirl;
  uniform float uSwitchPulse;
  uniform float uFormMode;
  uniform float uPointSize;
  uniform float uPixelRatio;

  uniform vec3 uLocalMouse;
  uniform float uRepelActive;

  uniform vec3 uColor0;
  uniform vec3 uColor1;
  uniform vec3 uColor2;
  uniform vec3 uColor3;
  uniform vec3 uLaserColor;
  uniform vec3 uHoverColor;

  varying vec3 vColor;
  varying float vAlphaMul;

  vec3 pickThemeColor(float bucket) {
    if (bucket < 0.30) return uColor0;
    if (bucket < 0.58) return uColor1;
    if (bucket < 0.84) return uColor2;
    return uColor3;
  }

  void main() {
    float t = clamp(uMorph, 0.0, 1.0);
    vec3 pos = mix(aPosA, aPosB, t);
    vec4 meta = mix(aMetaA, aMetaB, t);

    float bucket = meta.x;
    float role = (t < 0.5) ? aMetaA.y : aMetaB.y;
    float phase = meta.z;
    float sizeMul = meta.w;

    // 1. GPU Transition Swirl during chapter scroll or service switch
    float swirlAmt = uSwirl * 0.78 + uSwitchPulse * 0.48;
    if (swirlAmt > 0.004) {
      float ang = swirlAmt * (0.65 + 0.35 * sin(phase));
      float ca = cos(ang);
      float sa = sin(ang);
      vec2 rotXZ = vec2(pos.x * ca - pos.z * sa, pos.x * sa + pos.z * ca);
      pos.x = rotXZ.x;
      pos.z = rotXZ.y + sin(phase + uTime * 2.5) * swirlAmt * 0.32;
    }

    // 2. Service-Specific Live GPU Motion (zero CPU work!)
    float laserBoost = 0.0;

    // Hibot Speech Bubbles: staggered typing-dot bounce
    if (uFormMode > 0.5 && uFormMode < 1.5 && role > 1.5) {
      pos.y += sin(uTime * 4.4 - phase) * 0.14;
      laserBoost = 0.35 + 0.35 * sin(uTime * 4.4 - phase);
    }

    // STT Microphone + Equalizer Bars: live vertical frequency dance
    if (uFormMode > 4.5 && uFormMode < 5.5 && role > 0.5 && role < 1.5) {
      float eqScale = 0.65 + 0.45 * sin(uTime * 4.6 + phase * 1.7) * cos(uTime * 2.3 + phase);
      pos.y = 0.25 + (pos.y - 0.25) * eqScale;
      laserBoost = clamp((eqScale - 0.65) * 0.9, 0.0, 0.75);
    }

    // TTS Speaker Synthesis: outward harmonic wave ripple
    if (uFormMode > 5.5 && uFormMode < 6.5 && role > 0.5 && role < 1.5) {
      float wave = sin(uTime * 4.0 - phase * 1.4);
      float radBoost = 1.0 + wave * 0.085;
      pos.y *= radBoost;
      pos.z *= radBoost;
      pos.x += wave * 0.09;
      laserBoost = clamp(wave * 0.45 + 0.25, 0.0, 0.75);
    }

    // Agentic Card Orbit: gentle orbital breathing pulse on satellites
    if (uFormMode > 6.5 && uFormMode < 7.5 && role > 1.5) {
      float pulse = 1.0 + sin(uTime * 3.2 + phase) * 0.06;
      pos *= pulse;
      laserBoost = 0.35 + 0.25 * sin(uTime * 3.2 + phase);
    }

    // OCR Scanner: sweeping laser bar + scanning glow across document lines
    if (uFormMode > 7.5) {
      float sweepY = sin(uTime * 1.55) * 1.85;
      if (role > 0.5 && role < 1.5) {
        pos.y += sweepY;
        laserBoost = 0.95;
      } else {
        float distLaser = abs(pos.y - sweepY);
        if (distLaser < 0.38) {
          laserBoost = (1.0 - distLaser / 0.38) * 0.82;
        }
      }
    }

    // 3. GPU Localized 3D Cursor Repulsion & Hover Glow
    float hoverGlow = uSwitchPulse * 0.28;
    if (uRepelActive > 0.5) {
      vec3 d = pos - uLocalMouse;
      float dist = length(vec3(d.x, d.y, d.z * 0.55));
      float radius = 2.45;
      if (dist < radius && dist > 0.001) {
        float f = 1.0 - dist / radius;
        float push = f * f * 0.95;
        pos += (d / dist) * push;
        hoverGlow = min(1.0, hoverGlow + f * 0.85);
      }
    }

    vec3 baseCol = mix(pickThemeColor(aMetaA.x), pickThemeColor(aMetaB.x), t);
    vec3 col = mix(baseCol, uLaserColor, clamp(laserBoost, 0.0, 1.0));
    col = mix(col, uHoverColor, clamp(hoverGlow, 0.0, 0.92));

    vec4 worldPos = uModel * vec4(pos, 1.0);
    vec4 clipPos = uViewProj * worldPos;
    gl_Position = clipPos;

    float shimmer = 0.90 + 0.10 * sin(uTime * 2.0 + phase);
    float perspectiveScale = 15.5 / max(2.2, clipPos.w);
    gl_PointSize = clamp(uPointSize * sizeMul * shimmer * (1.0 + hoverGlow * 0.35 + laserBoost * 0.25) * perspectiveScale * uPixelRatio, 1.5, 18.0);

    vColor = col;
    vAlphaMul = mix(1.0, 0.65, clamp((clipPos.w - 10.0) / 16.0, 0.0, 1.0));
  }
`;

const PARTICLE_FS = `
  precision highp float;

  varying vec3 vColor;
  varying float vAlphaMul;

  uniform float uOpacity;
  uniform float uIsLight;

  void main() {
    vec2 uv = gl_PointCoord - vec2(0.5);
    float d = length(uv);
    if (d > 0.5) discard;

    // Dark themes: soft luminous sprite glow | Light Stone theme: crisp architectural ink dot
    float alphaDark = pow(smoothstep(0.5, 0.05, d), 1.45);
    float alphaLight = smoothstep(0.5, 0.24, d);
    float alpha = mix(alphaDark, alphaLight, uIsLight);

    vec3 col = vColor;
    if (uIsLight < 0.5) {
      col += smoothstep(0.24, 0.0, d) * 0.14;
    }

    gl_FragColor = vec4(col, alpha * uOpacity * vAlphaMul);
  }
`;

const STAR_VS = `
  precision highp float;
  attribute vec3 aPos;
  attribute float aScale;
  uniform mat4 uModel;
  uniform mat4 uViewProj;
  uniform float uPixelRatio;
  varying float vFade;
  void main() {
    vec4 clipPos = uViewProj * uModel * vec4(aPos, 1.0);
    gl_Position = clipPos;
    gl_PointSize = clamp(aScale * (18.0 / max(4.0, clipPos.w)) * uPixelRatio, 1.2, 6.0);
    vFade = aScale * 0.55;
  }
`;

const STAR_FS = `
  precision highp float;
  varying float vFade;
  uniform vec3 uStarColor;
  uniform float uStarOpacity;
  void main() {
    float d = length(gl_PointCoord - vec2(0.5));
    if (d > 0.5) discard;
    float a = smoothstep(0.5, 0.1, d);
    gl_FragColor = vec4(uStarColor, a * uStarOpacity * vFade);
  }
`;

export class GPUParticleSystem {
  constructor(webgl, initialTheme) {
    this.webgl = webgl;
    this.gl = webgl.gl;

    this.program = createProgram(this.gl, PARTICLE_VS, PARTICLE_FS);
    this.starProgram = createProgram(this.gl, STAR_VS, STAR_FS);

    this.locs = {
      aPosA: this.gl.getAttribLocation(this.program, 'aPosA'),
      aMetaA: this.gl.getAttribLocation(this.program, 'aMetaA'),
      aPosB: this.gl.getAttribLocation(this.program, 'aPosB'),
      aMetaB: this.gl.getAttribLocation(this.program, 'aMetaB'),
      uModel: this.gl.getUniformLocation(this.program, 'uModel'),
      uViewProj: this.gl.getUniformLocation(this.program, 'uViewProj'),
      uMorph: this.gl.getUniformLocation(this.program, 'uMorph'),
      uTime: this.gl.getUniformLocation(this.program, 'uTime'),
      uSwirl: this.gl.getUniformLocation(this.program, 'uSwirl'),
      uSwitchPulse: this.gl.getUniformLocation(this.program, 'uSwitchPulse'),
      uFormMode: this.gl.getUniformLocation(this.program, 'uFormMode'),
      uPointSize: this.gl.getUniformLocation(this.program, 'uPointSize'),
      uPixelRatio: this.gl.getUniformLocation(this.program, 'uPixelRatio'),
      uLocalMouse: this.gl.getUniformLocation(this.program, 'uLocalMouse'),
      uRepelActive: this.gl.getUniformLocation(this.program, 'uRepelActive'),
      uColor0: this.gl.getUniformLocation(this.program, 'uColor0'),
      uColor1: this.gl.getUniformLocation(this.program, 'uColor1'),
      uColor2: this.gl.getUniformLocation(this.program, 'uColor2'),
      uColor3: this.gl.getUniformLocation(this.program, 'uColor3'),
      uLaserColor: this.gl.getUniformLocation(this.program, 'uLaserColor'),
      uHoverColor: this.gl.getUniformLocation(this.program, 'uHoverColor'),
      uOpacity: this.gl.getUniformLocation(this.program, 'uOpacity'),
      uIsLight: this.gl.getUniformLocation(this.program, 'uIsLight')
    };

    this.starLocs = {
      aPos: this.gl.getAttribLocation(this.starProgram, 'aPos'),
      aScale: this.gl.getAttribLocation(this.starProgram, 'aScale'),
      uModel: this.gl.getUniformLocation(this.starProgram, 'uModel'),
      uViewProj: this.gl.getUniformLocation(this.starProgram, 'uViewProj'),
      uPixelRatio: this.gl.getUniformLocation(this.starProgram, 'uPixelRatio'),
      uStarColor: this.gl.getUniformLocation(this.starProgram, 'uStarColor'),
      uStarOpacity: this.gl.getUniformLocation(this.starProgram, 'uStarOpacity')
    };

    this.uploadStaticShapeBuffers();
    this.uploadStarfield();

    // Smooth GPU theme color state (only 7 small vec3 uniforms!)
    this.curColors = initialTheme.particleColors.map(c => new Float32Array(c));
    this.tgtColors = initialTheme.particleColors.map(c => new Float32Array(c));
    this.curLaser = new Float32Array(initialTheme.laserColor);
    this.tgtLaser = new Float32Array(initialTheme.laserColor);
    this.curHover = new Float32Array(initialTheme.hoverColor);
    this.tgtHover = new Float32Array(initialTheme.hoverColor);
    this.curStar = new Float32Array(initialTheme.starColor);
    this.tgtStar = new Float32Array(initialTheme.starColor);
    this.theme = initialTheme;

    // Active GPU form transition state
    this.fromForm = 'ambient-void';
    this.toForm = 'ambient-void';
    this.transitionT = 1.0;
    this.switchPulse = 0.0;

    // Sculpture transform & drag rotation
    this.pos = new Float32Array([0, 0, 0]);
    this.rot = new Float32Array([0, 0, 0]);
    this.scale = new Float32Array([1, 1, 1]);
    this.modelMat = mat4Create();
    this.invModelMat = mat4Create();
    this.starModelMat = mat4Create();
    this.localMouse = new Float32Array(3);
  }

  uploadStaticShapeBuffers() {
    const gl = this.gl;
    const shapes = buildAllServiceShapes();
    this.buffers = {};

    Object.keys(shapes).forEach((name) => {
      const posBuf = gl.createBuffer();
      gl.bindBuffer(gl.ARRAY_BUFFER, posBuf);
      gl.bufferData(gl.ARRAY_BUFFER, shapes[name].positions, gl.STATIC_DRAW);

      const metaBuf = gl.createBuffer();
      gl.bindBuffer(gl.ARRAY_BUFFER, metaBuf);
      gl.bufferData(gl.ARRAY_BUFFER, shapes[name].meta, gl.STATIC_DRAW);

      this.buffers[name] = { posBuf, metaBuf };
    });
  }

  uploadStarfield() {
    const gl = this.gl;
    this.starCount = 620;
    const pos = new Float32Array(this.starCount * 3);
    const scales = new Float32Array(this.starCount);
    for (let i = 0; i < this.starCount; i++) {
      pos[i * 3]     = (Math.random() - 0.5) * 44;
      pos[i * 3 + 1] = (Math.random() - 0.5) * 30;
      pos[i * 3 + 2] = -5.5 - Math.random() * 14;
      scales[i] = 0.8 + Math.random() * 1.4;
    }

    this.starPosBuf = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, this.starPosBuf);
    gl.bufferData(gl.ARRAY_BUFFER, pos, gl.STATIC_DRAW);

    this.starScaleBuf = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, this.starScaleBuf);
    gl.bufferData(gl.ARRAY_BUFFER, scales, gl.STATIC_DRAW);
  }

  setTheme(theme) {
    this.theme = theme;
    for (let i = 0; i < 4; i++) this.tgtColors[i].set(theme.particleColors[i]);
    this.tgtLaser.set(theme.laserColor);
    this.tgtHover.set(theme.hoverColor);
    this.tgtStar.set(theme.starColor);
  }

  transitionToForm(nextFormName, withPulse = false) {
    const valid = this.buffers[nextFormName] ? nextFormName : 'ecosystem-globe';
    if (valid === this.toForm) return;
    this.fromForm = this.toForm;
    this.toForm = valid;
    this.transitionT = 0.0;
    if (withPulse) this.switchPulse = 1.0;
  }

  getWorldCenter() {
    return this.pos;
  }

  render({ dt, elapsed, viewProj, targetPosX, dragRot, smoothMouse, worldMouse, heroWeight }) {
    const gl = this.gl;
    const colorLerp = 1.0 - Math.exp(-7.5 * dt);

    for (let i = 0; i < 4; i++) lerpVec3(this.curColors[i], this.curColors[i], this.tgtColors[i], colorLerp);
    lerpVec3(this.curLaser, this.curLaser, this.tgtLaser, colorLerp);
    lerpVec3(this.curHover, this.curHover, this.tgtHover, colorLerp);
    lerpVec3(this.curStar, this.curStar, this.tgtStar, colorLerp);

    // Advance GPU morph smoothly (zero per-particle CPU work)
    if (this.transitionT < 1.0) {
      this.transitionT = Math.min(1.0, this.transitionT + dt * 2.15);
    }
    const easedMorph = this.transitionT * this.transitionT * (3 - 2 * this.transitionT);
    const swirl = Math.sin(this.transitionT * Math.PI);
    this.switchPulse *= Math.pow(0.08, dt);

    // Sculpture position & gentle spin
    this.pos[0] += (targetPosX - this.pos[0]) * Math.min(1, dt * 6.5);
    this.pos[1] = Math.sin(elapsed * 0.78) * 0.14;
    this.pos[2] = 0;

    const formMode = FORM_MODE_ID[this.toForm] ?? 0;
    const spinSpeed = formMode === 8 ? 0.06 : (formMode === 7 ? 0.22 : (formMode === 1 ? 0.11 : 0.15));
    this.rot[0] = dragRot.x - smoothMouse.y * 0.22 + Math.sin(elapsed * 0.45) * 0.05;
    this.rot[1] = elapsed * spinSpeed + dragRot.y + smoothMouse.x * 0.30;
    this.rot[2] = 0;
    mat4FromTRS(this.modelMat, this.pos, this.rot, this.scale);
    mat4Invert(this.invModelMat, this.modelMat);
    transformPointMat4(this.localMouse, worldMouse, this.invModelMat);

    const isLight = this.theme.mode === 'light';
    gl.depthMask(false);
    if (isLight) gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA);
    else gl.blendFunc(gl.SRC_ALPHA, gl.ONE);

    /* --- Pass 1: Ambient Starfield / Stone Dust (1 draw call) --- */
    gl.useProgram(this.starProgram);
    const SL = this.starLocs;
    mat4FromTRS(this.starModelMat, [0, 0, 0], [-smoothMouse.y * 0.03, elapsed * 0.012 + smoothMouse.x * 0.04, 0], [1, 1, 1]);
    gl.uniformMatrix4fv(SL.uModel, false, this.starModelMat);
    gl.uniformMatrix4fv(SL.uViewProj, false, viewProj);
    gl.uniform1f(SL.uPixelRatio, this.webgl.pixelRatio);
    gl.uniform3fv(SL.uStarColor, this.curStar);
    gl.uniform1f(SL.uStarOpacity, this.theme.starOpacity);

    gl.enableVertexAttribArray(SL.aPos);
    gl.bindBuffer(gl.ARRAY_BUFFER, this.starPosBuf);
    gl.vertexAttribPointer(SL.aPos, 3, gl.FLOAT, false, 0, 0);

    gl.enableVertexAttribArray(SL.aScale);
    gl.bindBuffer(gl.ARRAY_BUFFER, this.starScaleBuf);
    gl.vertexAttribPointer(SL.aScale, 1, gl.FLOAT, false, 0, 0);

    gl.drawArrays(gl.POINTS, 0, this.starCount);
    gl.disableVertexAttribArray(SL.aPos);
    gl.disableVertexAttribArray(SL.aScale);

    /* --- Pass 2: Main 14,000-Particle GPU Morph Sculpture (1 draw call!) --- */
    gl.useProgram(this.program);
    const L = this.locs;
    const bufA = this.buffers[this.fromForm];
    const bufB = this.buffers[this.toForm];

    gl.uniformMatrix4fv(L.uModel, false, this.modelMat);
    gl.uniformMatrix4fv(L.uViewProj, false, viewProj);
    gl.uniform1f(L.uMorph, easedMorph);
    gl.uniform1f(L.uTime, elapsed);
    gl.uniform1f(L.uSwirl, swirl);
    gl.uniform1f(L.uSwitchPulse, this.switchPulse);
    gl.uniform1f(L.uFormMode, formMode);
    gl.uniform1f(L.uPointSize, lerp(this.theme.pointSize, 2.15, heroWeight) + this.switchPulse * 0.55);
    gl.uniform1f(L.uPixelRatio, this.webgl.pixelRatio);
    gl.uniform3fv(L.uLocalMouse, this.localMouse);
    gl.uniform1f(L.uRepelActive, heroWeight < 0.7 ? 1.0 : 0.0);

    gl.uniform3fv(L.uColor0, this.curColors[0]);
    gl.uniform3fv(L.uColor1, this.curColors[1]);
    gl.uniform3fv(L.uColor2, this.curColors[2]);
    gl.uniform3fv(L.uColor3, this.curColors[3]);
    gl.uniform3fv(L.uLaserColor, this.curLaser);
    gl.uniform3fv(L.uHoverColor, this.curHover);
    gl.uniform1f(L.uOpacity, lerp(this.theme.particleOpacity, this.theme.heroOpacity, heroWeight));
    gl.uniform1f(L.uIsLight, isLight ? 1.0 : 0.0);

    gl.enableVertexAttribArray(L.aPosA);
    gl.bindBuffer(gl.ARRAY_BUFFER, bufA.posBuf);
    gl.vertexAttribPointer(L.aPosA, 3, gl.FLOAT, false, 0, 0);

    gl.enableVertexAttribArray(L.aMetaA);
    gl.bindBuffer(gl.ARRAY_BUFFER, bufA.metaBuf);
    gl.vertexAttribPointer(L.aMetaA, 4, gl.FLOAT, false, 0, 0);

    gl.enableVertexAttribArray(L.aPosB);
    gl.bindBuffer(gl.ARRAY_BUFFER, bufB.posBuf);
    gl.vertexAttribPointer(L.aPosB, 3, gl.FLOAT, false, 0, 0);

    gl.enableVertexAttribArray(L.aMetaB);
    gl.bindBuffer(gl.ARRAY_BUFFER, bufB.metaBuf);
    gl.vertexAttribPointer(L.aMetaB, 4, gl.FLOAT, false, 0, 0);

    gl.drawArrays(gl.POINTS, 0, PARTICLE_COUNT);

    gl.disableVertexAttribArray(L.aPosA);
    gl.disableVertexAttribArray(L.aMetaA);
    gl.disableVertexAttribArray(L.aPosB);
    gl.disableVertexAttribArray(L.aMetaB);
    gl.depthMask(true);
  }
}
