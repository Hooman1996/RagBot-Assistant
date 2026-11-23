export function compileShader(gl, type, source) {
  const shader = gl.createShader(type);
  gl.shaderSource(shader, source);
  gl.compileShader(shader);
  if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
    const log = gl.getShaderInfoLog(shader);
    gl.deleteShader(shader);
    throw new Error(`Shader compile error: ${log}`);
  }
  return shader;
}

export function createProgram(gl, vsSource, fsSource) {
  const vs = compileShader(gl, gl.VERTEX_SHADER, vsSource);
  const fs = compileShader(gl, gl.FRAGMENT_SHADER, fsSource);
  const program = gl.createProgram();
  gl.attachShader(program, vs);
  gl.attachShader(program, fs);
  gl.linkProgram(program);
  gl.deleteShader(vs);
  gl.deleteShader(fs);
  if (!gl.getProgramParameter(program, gl.LINK_STATUS)) {
    const log = gl.getProgramInfoLog(program);
    gl.deleteProgram(program);
    throw new Error(`Program link error: ${log}`);
  }
  return program;
}

export class WebGLContext {
  constructor(canvas) {
    this.canvas = canvas;
    const opts = {
      antialias: true,
      alpha: false,
      depth: true,
      powerPreference: 'high-performance'
    };
    this.gl = canvas.getContext('webgl2', opts) || canvas.getContext('webgl', opts);
    if (!this.gl) {
      throw new Error('WebGL is not supported in this browser.');
    }
    this.gl.enable(this.gl.BLEND);
    this.pixelRatio = 1;
    this.width = window.innerWidth;
    this.height = window.innerHeight;
    this.resize(window.innerWidth, window.innerHeight);
  }

  resize(width, height) {
    this.width = Math.max(1, width);
    this.height = Math.max(1, height);
    this.pixelRatio = Math.min(window.devicePixelRatio || 1, 2);
    this.canvas.width = Math.floor(this.width * this.pixelRatio);
    this.canvas.height = Math.floor(this.height * this.pixelRatio);
    this.gl.viewport(0, 0, this.canvas.width, this.canvas.height);
  }

  clear(rgb) {
    const gl = this.gl;
    gl.clearColor(rgb[0], rgb[1], rgb[2], 1.0);
    gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);
  }
}
