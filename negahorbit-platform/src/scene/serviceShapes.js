export const PARTICLE_COUNT = 14000;

export const FORM_MODE_ID = {
  'ambient-void': 0,
  'speech-bubbles': 1,
  'ecosystem-globe': 2,
  'crm-constellation': 3,
  'rag-documents': 4,
  'microphone-wave': 5,
  'speaker-synthesis': 6,
  'agentic-card-orbit': 7,
  'ocr-scanner': 8
};

function mulberry32(seed) {
  let a = seed >>> 0;
  return function () {
    a |= 0;
    a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

function sampleSuperPill(pos, i3, rand, cx, cy, cz, rx, ry, rz) {
  const u = rand() * Math.PI * 2;
  const v = Math.acos(2 * rand() - 1);
  const exp = 0.56;
  const sx = Math.sin(v) * Math.cos(u);
  const sy = Math.cos(v);
  const sz = Math.sin(v) * Math.sin(u);
  const shell = 0.94 + rand() * 0.08;
  pos[i3]     = cx + Math.sign(sx) * Math.pow(Math.abs(sx), exp) * rx * shell;
  pos[i3 + 1] = cy + Math.sign(sy) * Math.pow(Math.abs(sy), exp) * ry * shell;
  pos[i3 + 2] = cz + Math.sign(sz) * Math.pow(Math.abs(sz), exp) * rz * shell;
}

function sampleSphere(pos, i3, rand, cx, cy, cz, r, surfaceBias = false) {
  const u = rand() * Math.PI * 2;
  const v = Math.acos(2 * rand() - 1);
  const rad = surfaceBias ? r * (0.94 + rand() * 0.08) : r * Math.cbrt(rand());
  pos[i3]     = cx + rad * Math.sin(v) * Math.cos(u);
  pos[i3 + 1] = cy + rad * Math.cos(v);
  pos[i3 + 2] = cz + rad * Math.sin(v) * Math.sin(u);
}

function sampleBoxSurface(pos, i3, rand, cx, cy, cz, hx, hy, hz) {
  const face = Math.floor(rand() * 6);
  let x = (rand() * 2 - 1) * hx;
  let y = (rand() * 2 - 1) * hy;
  let z = (rand() * 2 - 1) * hz;
  if (face === 0) x = hx;
  else if (face === 1) x = -hx;
  else if (face === 2) y = hy;
  else if (face === 3) y = -hy;
  else if (face === 4) z = hz;
  else z = -hz;
  pos[i3] = cx + x;
  pos[i3 + 1] = cy + y;
  pos[i3 + 2] = cz + z;
}

/**
 * Generates { positions: Float32Array(COUNT*3), meta: Float32Array(COUNT*4) }
 * meta.x = color bucket [0..1]
 * meta.y = role tag (0=normal, 1=accent/laser/highlight, 2=typing-dot/pulse-node)
 * meta.z = deterministic phase [0..2PI]
 * meta.w = particle size scale [0.65..1.55]
 */
function generateShape(name, seed) {
  const rand = mulberry32(seed);
  const pos = new Float32Array(PARTICLE_COUNT * 3);
  const meta = new Float32Array(PARTICLE_COUNT * 4);
  const goldenRatio = (1 + Math.sqrt(5)) / 2;

  for (let i = 0; i < PARTICLE_COUNT; i++) {
    const i3 = i * 3;
    const i4 = i * 4;
    const t = i / PARTICLE_COUNT;

    let colorBucket = rand();
    let role = 0.0;
    let phase = rand() * Math.PI * 2;
    let sizeMul = 0.8 + rand() * 0.65;

    switch (name) {
      /* 0. HERO AMBIENT COSMIC DUST FIELD */
      case 'ambient-void': {
        const rad = 5.4 + Math.pow(rand(), 0.62) * 10.5;
        const theta = rand() * Math.PI * 2;
        const phi = Math.acos(2 * rand() - 1);
        pos[i3]     = rad * Math.sin(phi) * Math.cos(theta) * 1.45;
        pos[i3 + 1] = rad * Math.cos(phi) * 0.92;
        pos[i3 + 2] = (rand() - 0.5) * 11.0 - 3.5;
        sizeMul = 0.6 + rand() * 0.7;
        break;
      }

      /* 1. HIBOT CHATBOT: Two Interlocking 3D Speech Bubbles + Tails + 3 Typing Dots */
      case 'speech-bubbles': {
        if (t < 0.44) {
          sampleSuperPill(pos, i3, rand, 0.72, 0.88, 0.55, 2.25, 1.32, 0.72);
          colorBucket = rand() < 0.25 ? 0.92 : (0.35 + rand() * 0.45);
          sizeMul = 0.95;
        } else if (t < 0.80) {
          sampleSuperPill(pos, i3, rand, -0.95, -1.05, -0.45, 1.98, 1.15, 0.66);
          colorBucket = rand() < 0.35 ? 0.72 : (0.12 + rand() * 0.45);
          sizeMul = 0.88;
        } else if (t < 0.92) {
          // 3 bright typing dots inside the top bubble
          const dotIdx = i % 3;
          sampleSphere(pos, i3, rand, 0.72 + (dotIdx - 1) * 0.76, 0.88, 0.68, 0.24, false);
          colorBucket = 0.96;
          role = 2.0;
          phase = dotIdx * 1.1; // staggered sequential typing bounce in GPU shader
          sizeMul = 1.4;
        } else {
          // Corner tails for both bubbles
          const topTail = i % 2 === 0;
          const k = rand();
          const spread = (1 - k) * 0.42;
          if (topTail) {
            pos[i3]     = 1.95 + k * 0.85 + (rand() - 0.5) * spread;
            pos[i3 + 1] = -0.25 - k * 0.72 + (rand() - 0.5) * 0.16;
            pos[i3 + 2] = 0.55 + (rand() - 0.5) * 0.22;
          } else {
            pos[i3]     = -2.02 - k * 0.78 + (rand() - 0.5) * spread;
            pos[i3 + 1] = -1.96 - k * 0.62 + (rand() - 0.5) * 0.16;
            pos[i3 + 2] = -0.45 + (rand() - 0.5) * 0.22;
          }
          colorBucket = 0.78;
          sizeMul = 1.05;
        }
        break;
      }

      /* 2. NEGAHBOOM CHATBOT: Interconnected Open-Banking Ecosystem Globe + Meridians + Orbit Rings */
      case 'ecosystem-globe': {
        if (t < 0.52) {
          // Crisp latitude & longitude geodesic meridian lines
          const isLat = i % 2 === 0;
          const R = 2.65;
          if (isLat) {
            const band = (i % 9) - 4;
            const lat = (band / 5) * (Math.PI * 0.46);
            const lon = rand() * Math.PI * 2;
            pos[i3]     = R * Math.cos(lat) * Math.cos(lon);
            pos[i3 + 1] = R * Math.sin(lat);
            pos[i3 + 2] = R * Math.cos(lat) * Math.sin(lon);
          } else {
            const merid = (i % 12) * (Math.PI / 6);
            const lat = (rand() - 0.5) * Math.PI;
            pos[i3]     = R * Math.cos(lat) * Math.cos(merid);
            pos[i3 + 1] = R * Math.sin(lat);
            pos[i3 + 2] = R * Math.cos(lat) * Math.sin(merid);
          }
          colorBucket = rand() < 0.25 ? 0.92 : (0.25 + rand() * 0.5);
          sizeMul = 0.92;
        } else if (t < 0.74) {
          // 14 bright ecosystem partner hubs on the globe surface
          const hubIdx = i % 14;
          const ht = (hubIdx + 0.5) / 14;
          const inc = Math.acos(1 - 2 * ht);
          const az = 2 * Math.PI * hubIdx / goldenRatio;
          const R = 2.72;
          sampleSphere(pos, i3, rand, R * Math.sin(inc) * Math.cos(az), R * Math.cos(inc), R * Math.sin(inc) * Math.sin(az), 0.26, false);
          colorBucket = 0.95;
          role = 2.0;
          sizeMul = 1.28;
        } else {
          // Two inclined NegahBoom ecosystem rings
          const ring = i % 2;
          const ang = rand() * Math.PI * 2;
          const rr = 3.55 + ring * 0.42 + (rand() - 0.5) * 0.08;
          const x = Math.cos(ang) * rr;
          const y = (rand() - 0.5) * 0.08;
          const z = Math.sin(ang) * rr;
          const tiltX = ring === 0 ? 0.55 : -0.62;
          const tiltZ = ring === 0 ? 0.28 : -0.34;
          const y1 = y * Math.cos(tiltX) - z * Math.sin(tiltX);
          const z1 = y * Math.sin(tiltX) + z * Math.cos(tiltX);
          pos[i3]     = x * Math.cos(tiltZ) - y1 * Math.sin(tiltZ);
          pos[i3 + 1] = x * Math.sin(tiltZ) + y1 * Math.cos(tiltZ);
          pos[i3 + 2] = z1;
          colorBucket = 0.76;
          role = 1.0;
          sizeMul = 1.0;
        }
        break;
      }

      /* 3. CRM CHATBOT: Customer-360 Profile Bust Hub + Orbiting Ticket/Contact Nodes + Routing Arcs */
      case 'crm-constellation': {
        const satCount = 8;
        const satIdx = i % satCount;
        const satAng = (satIdx / satCount) * Math.PI * 2;
        const satX = Math.cos(satAng) * 3.25;
        const satY = Math.sin(satAng * 2) * 0.95;
        const satZ = Math.sin(satAng) * 2.85;

        if (t < 0.20) {
          // Stylized 3D customer avatar head in center
          sampleSphere(pos, i3, rand, 0, 0.85, 0, 0.74, true);
          colorBucket = 0.94;
          sizeMul = 1.15;
        } else if (t < 0.44) {
          // Customer avatar shoulders/bust in center
          const u = rand() * Math.PI * 2;
          const v = rand() * Math.PI * 0.55; // upper dome arch
          const rx = 1.38, ry = 0.96, rz = 0.76;
          pos[i3]     = rx * Math.sin(v) * Math.cos(u);
          pos[i3 + 1] = -0.95 + ry * Math.cos(v);
          pos[i3 + 2] = rz * Math.sin(v) * Math.sin(u);
          colorBucket = 0.78;
          sizeMul = 1.05;
        } else if (t < 0.72) {
          // 8 orbiting CRM record cards / customer touchpoint mini-panels
          sampleBoxSurface(pos, i3, rand, satX, satY, satZ, 0.48, 0.34, 0.08);
          colorBucket = rand() < 0.4 ? 0.92 : 0.55;
          role = 2.0;
          sizeMul = 0.98;
        } else {
          // Curved routing links connecting CRM touchpoints to Customer-360 hub
          const k = rand();
          const arcLift = Math.sin(k * Math.PI) * 0.65;
          pos[i3]     = satX * k + (rand() - 0.5) * 0.06;
          pos[i3 + 1] = satY * k + arcLift + (rand() - 0.5) * 0.06;
          pos[i3 + 2] = satZ * k + (rand() - 0.5) * 0.06;
          colorBucket = 0.80;
          role = 1.0;
          sizeMul = 0.85;
        }
        break;
      }

      /* 4. DOCUMENT CHATBOT: Layered Knowledge Docs + 3D Magnifying Search Lens + Citation Beams */
      case 'rag-documents': {
        if (t < 0.64) {
          // 3 fanned 3D document sheets with crisp borders & paragraph lines
          const sheet = i % 3;
          const zLayer = (sheet - 1) * 0.78;
          const isBorder = i % 7 === 0;
          let lx, ly;
          if (isBorder) {
            const side = i % 4;
            if (side === 0) { lx = (rand() - 0.5) * 3.5; ly = 2.35; }
            else if (side === 1) { lx = (rand() - 0.5) * 3.5; ly = -2.35; }
            else if (side === 2) { lx = -1.75; ly = (rand() - 0.5) * 4.7; }
            else { lx = 1.75; ly = (rand() - 0.5) * 4.7; }
            colorBucket = 0.84;
            sizeMul = 1.12;
          } else {
            const lineIdx = Math.floor(rand() * 11);
            ly = 1.82 - lineIdx * 0.35 + (rand() - 0.5) * 0.04;
            lx = -1.35 + rand() * (lineIdx % 3 === 2 ? 1.85 : 2.7);
            colorBucket = 0.32 + rand() * 0.38;
            sizeMul = 0.85;
          }
          const tilt = -0.24 + (sheet - 1) * 0.08;
          pos[i3]     = lx * Math.cos(tilt) - zLayer * Math.sin(tilt) - 0.35;
          pos[i3 + 1] = ly - 0.35;
          pos[i3 + 2] = lx * Math.sin(tilt) + zLayer * Math.cos(tilt);
        } else if (t < 0.84) {
          // 3D Magnifying Glass Lens hovering in front of the documents
          const isHandle = t > 0.78;
          if (!isHandle) {
            const a = rand() * Math.PI * 2;
            const ringR = 1.05 + (rand() - 0.5) * 0.14;
            pos[i3]     = 1.05 + Math.cos(a) * ringR;
            pos[i3 + 1] = 0.35 + Math.sin(a) * ringR;
            pos[i3 + 2] = 1.48 + (rand() - 0.5) * 0.14;
            colorBucket = 0.94;
            role = 1.0;
            sizeMul = 1.25;
          } else {
            const k = rand();
            pos[i3]     = 1.80 + k * 1.15 + (rand() - 0.5) * 0.16;
            pos[i3 + 1] = -0.40 - k * 1.15 + (rand() - 0.5) * 0.16;
            pos[i3 + 2] = 1.48 + (rand() - 0.5) * 0.16;
            colorBucket = 0.82;
            sizeMul = 1.15;
          }
        } else {
          // Rising citation streams converging into an AI answer diamond above the pages
          if (t < 0.93) {
            const k = rand();
            const stream = i % 4;
            const sx = -1.3 + stream * 0.85;
            pos[i3]     = sx * (1 - k) + 0.15 * k + (rand() - 0.5) * 0.08;
            pos[i3 + 1] = -0.2 + k * 2.95;
            pos[i3 + 2] = (stream - 1.5) * 0.35 * (1 - k);
            colorBucket = 0.88;
            role = 2.0;
          } else {
            sampleSphere(pos, i3, rand, 0.15, 2.85, 0, 0.42, false);
            colorBucket = 0.97;
            role = 2.0;
            sizeMul = 1.35;
          }
        }
        break;
      }

      /* 5. STT (SPEECH-TO-TEXT): 3D Studio Condenser Microphone + Live Equalizer Soundwave Bars */
      case 'microphone-wave': {
        if (t < 0.40) {
          // Rounded studio microphone capsule grille (center)
          const u = rand() * Math.PI * 2;
          const h = (rand() - 0.5) * 2.15;
          const cap = Math.abs(h) > 0.65 ? Math.sqrt(Math.max(0.02, 1 - Math.pow((Math.abs(h) - 0.65) / 0.48, 2))) : 1.0;
          const r = 0.78 * cap;
          // Mesh grille horizontal bands
          const isGrille = Math.abs(Math.sin(h * 18.0)) > 0.55;
          pos[i3]     = Math.cos(u) * r;
          pos[i3 + 1] = h + 0.55;
          pos[i3 + 2] = Math.sin(u) * r;
          colorBucket = isGrille ? 0.90 : 0.45;
          sizeMul = isGrille ? 1.15 : 0.85;
        } else if (t < 0.56) {
          // U-shaped studio shock-mount cradle + stand stem & circular base
          if (t < 0.48) {
            const a = Math.PI + rand() * Math.PI; // bottom U arc
            const r = 1.18 + (rand() - 0.5) * 0.09;
            pos[i3]     = Math.cos(a) * r;
            pos[i3 + 1] = 0.20 + Math.sin(a) * r;
            pos[i3 + 2] = (rand() - 0.5) * 0.12;
          } else if (t < 0.52) {
            // Vertical stem
            const a = rand() * Math.PI * 2;
            pos[i3]     = Math.cos(a) * 0.11;
            pos[i3 + 1] = -0.98 - rand() * 1.15;
            pos[i3 + 2] = Math.sin(a) * 0.11;
          } else {
            // Weighted circular base
            const a = rand() * Math.PI * 2;
            const r = Math.sqrt(rand()) * 1.15;
            pos[i3]     = Math.cos(a) * r;
            pos[i3 + 1] = -2.15 + (rand() - 0.5) * 0.1;
            pos[i3 + 2] = Math.sin(a) * r;
          }
          colorBucket = 0.75;
          sizeMul = 1.0;
        } else {
          // Symmetrical 3D vertical acoustic equalizer bars flanking the microphone (animated in GPU!)
          const barIdx = i % 16;
          const side = barIdx < 8 ? -1 : 1;
          const slot = barIdx % 8; // 0..7 outward from mic
          const bx = side * (1.65 + slot * 0.34);
          const barHeight = 2.4 * Math.exp(-slot * 0.24) + 0.35;
          pos[i3]     = bx + (rand() - 0.5) * 0.14;
          pos[i3 + 1] = (rand() - 0.5) * barHeight * 2.0 + 0.25;
          pos[i3 + 2] = (rand() - 0.5) * 0.22;
          colorBucket = slot < 3 ? 0.94 : (0.55 + rand() * 0.3);
          role = 1.0; // GPU animates vertical EQ wave oscillation
          phase = slot * 0.65;
          sizeMul = 1.05;
        }
        break;
      }

      /* 6. TTS (TEXT-TO-SPEECH): 3D Acoustic Speaker Cone + Radiating Harmonic Voice Rings */
      case 'speaker-synthesis': {
        if (t < 0.38) {
          // Sculptural 3D acoustic speaker horn / cone facing +X
          const k = rand(); // 0 back magnet .. 1 front flare
          const a = rand() * Math.PI * 2;
          const r = 0.48 + Math.pow(k, 1.45) * 1.65;
          pos[i3]     = -2.45 + k * 1.85;
          pos[i3 + 1] = Math.cos(a) * r;
          pos[i3 + 2] = Math.sin(a) * r;
          colorBucket = k > 0.85 ? 0.94 : (0.28 + k * 0.45);
          sizeMul = 1.0;
        } else if (t < 0.48) {
          // Central acoustic voice dome driver
          sampleSphere(pos, i3, rand, -1.15, 0, 0, 0.55, true);
          colorBucket = 0.96;
          role = 2.0;
          sizeMul = 1.25;
        } else {
          // 6 concentric 3D harmonic voice waves radiating outward along +X (animated in GPU!)
          const waveIdx = i % 6;
          const a = rand() * Math.PI * 2;
          const xPos = -0.15 + waveIdx * 0.72 + (rand() - 0.5) * 0.08;
          const waveR = 0.95 + waveIdx * 0.46 + Math.sin(a * 4.0) * 0.12;
          pos[i3]     = xPos;
          pos[i3 + 1] = Math.cos(a) * waveR;
          pos[i3 + 2] = Math.sin(a) * waveR;
          colorBucket = waveIdx % 2 === 0 ? 0.88 : 0.58;
          role = 1.0; // GPU radiates harmonic pulse outward
          phase = waveIdx * 0.85;
          sizeMul = 1.05;
        }
        break;
      }

      /* 7. AGENTIC BANKING ASSISTANT: 3D Floating EMV Banking Card + 3 NegahOrbit Execution Rings + Satellites */
      case 'agentic-card-orbit': {
        if (t < 0.44) {
          // Sleek floating 3D Banking Card with crisp border, EMV chip & contactless wave icon
          const isBorder = i % 6 === 0;
          const isChip = !isBorder && (i % 9 === 0);
          let cx, cy, cz = (rand() - 0.5) * 0.10;
          if (isBorder) {
            const side = i % 4;
            if (side === 0) { cx = (rand() - 0.5) * 3.5; cy = 1.12; }
            else if (side === 1) { cx = (rand() - 0.5) * 3.5; cy = -1.12; }
            else if (side === 2) { cx = -1.75; cy = (rand() - 0.5) * 2.24; }
            else { cx = 1.75; cy = (rand() - 0.5) * 2.24; }
            colorBucket = 0.95;
            sizeMul = 1.2;
          } else if (isChip) {
            // EMV Smart Chip rectangle on left side of card
            cx = -0.95 + (rand() - 0.5) * 0.62;
            cy = 0.12 + (rand() - 0.5) * 0.48;
            cz = 0.09;
            colorBucket = 0.96;
            role = 2.0;
            sizeMul = 1.3;
          } else {
            // Card body + subtle embossed number line
            const isNumberRow = rand() < 0.22;
            cx = (rand() - 0.5) * 3.35;
            cy = isNumberRow ? (-0.42 + (rand() - 0.5) * 0.12) : ((rand() - 0.5) * 2.1);
            colorBucket = isNumberRow ? 0.85 : (0.18 + rand() * 0.35);
            sizeMul = isNumberRow ? 1.05 : 0.78;
          }
          // Subtle hero card angle
          const ry = -0.28, rx = 0.16;
          const x1 = cx * Math.cos(ry) + cz * Math.sin(ry);
          const z1 = -cx * Math.sin(ry) + cz * Math.cos(ry);
          pos[i3]     = x1;
          pos[i3 + 1] = cy * Math.cos(rx) - z1 * Math.sin(rx);
          pos[i3 + 2] = cy * Math.sin(rx) + z1 * Math.cos(rx);
        } else if (t < 0.86) {
          // 3 inclined NegahOrbit autonomous execution rings around the banking card
          const ring = i % 3;
          const a = rand() * Math.PI * 2;
          const rx = 3.55 + ring * 0.34;
          const ry = 2.35 + ring * 0.26;
          const x = Math.cos(a) * rx + (rand() - 0.5) * 0.06;
          const y = (rand() - 0.5) * 0.06;
          const z = Math.sin(a) * ry + (rand() - 0.5) * 0.06;
          const tx = [0.62, -0.70, 1.24][ring];
          const tz = [0.32, -0.45, 0.18][ring];
          const y1 = y * Math.cos(tx) - z * Math.sin(tx);
          const z1 = y * Math.sin(tx) + z * Math.cos(tx);
          pos[i3]     = x * Math.cos(tz) - y1 * Math.sin(tz);
          pos[i3 + 1] = x * Math.sin(tz) + y1 * Math.cos(tz);
          pos[i3 + 2] = z1;
          colorBucket = 0.80;
          role = 1.0;
          sizeMul = 1.0;
        } else {
          // 4 orbiting autonomous step nodes (Plan, Verify, Execute, Confirm)
          const sat = i % 4;
          const ang = (sat / 4) * Math.PI * 2 + 0.4;
          sampleSphere(pos, i3, rand, Math.cos(ang) * 3.45, Math.sin(ang * 2) * 1.45, Math.sin(ang) * 2.55, 0.34, false);
          colorBucket = 0.97;
          role = 2.0;
          sizeMul = 1.35;
        }
        break;
      }

      /* 8. OCR VISION: Tilted 3D ID/Check Document + 4 Viewfinder Corner Brackets + Sweeping Optical Laser */
      case 'ocr-scanner': {
        const rotY = 0.36, rotX = -0.18, rotZ = 0.12;
        const rotateDoc = (lx, ly, lz) => {
          const x1 = lx * Math.cos(rotY) + lz * Math.sin(rotY);
          const z1 = -lx * Math.sin(rotY) + lz * Math.cos(rotY);
          const y2 = ly * Math.cos(rotX) - z1 * Math.sin(rotX);
          const z2 = ly * Math.sin(rotX) + z1 * Math.cos(rotX);
          pos[i3]     = x1 * Math.cos(rotZ) - y2 * Math.sin(rotZ);
          pos[i3 + 1] = x1 * Math.sin(rotZ) + y2 * Math.cos(rotZ);
          pos[i3 + 2] = z2;
        };

        if (t < 0.72) {
          // Structured ID/Document card with photo frame & crisp extracted text rows
          let lx, ly, lz = (rand() - 0.5) * 0.08;
          if (t < 0.14) {
            // Photo box frame on top-left of ID document
            const side = i % 4;
            if (side === 0) { lx = -1.65 + rand() * 1.05; ly = 1.35; }
            else if (side === 1) { lx = -1.65 + rand() * 1.05; ly = 0.05; }
            else if (side === 2) { lx = -1.65; ly = 0.05 + rand() * 1.30; }
            else { lx = -0.60; ly = 0.05 + rand() * 1.30; }
            colorBucket = 0.92;
            sizeMul = 1.12;
          } else if (t < 0.56) {
            // Crisp structured OCR text lines
            const row = Math.floor(rand() * 9);
            ly = 1.25 - row * 0.34 + (rand() - 0.5) * 0.04;
            const minX = row < 4 ? -0.30 : -1.65;
            const width = row < 4 ? 2.05 : (row === 8 ? 2.4 : 3.35);
            lx = minX + rand() * width;
            colorBucket = 0.45 + rand() * 0.3;
            sizeMul = 0.92;
          } else {
            // Subtle document border & second depth layer
            const side = i % 4;
            lz = (i % 2 === 0) ? 0.0 : -0.45;
            if (side === 0) { lx = (rand() - 0.5) * 4.2; ly = 1.85; }
            else if (side === 1) { lx = (rand() - 0.5) * 4.2; ly = -1.85; }
            else if (side === 2) { lx = -2.1; ly = (rand() - 0.5) * 3.7; }
            else { lx = 2.1; ly = (rand() - 0.5) * 3.7; }
            colorBucket = 0.80;
            sizeMul = 1.0;
          }
          rotateDoc(lx, ly, lz);
        } else if (t < 0.88) {
          // 4 Bold L-shaped Optical Viewfinder Corner Brackets framing the scan zone
          const corner = i % 4;
          const sx = (corner === 0 || corner === 2) ? -1 : 1;
          const sy = (corner === 0 || corner === 1) ? 1 : -1;
          const armH = (i % 8 < 4);
          const armLen = 0.95;
          const bx = sx * 2.58;
          const by = sy * 2.28;
          const lx = armH ? (bx - sx * rand() * armLen) : (bx + (rand() - 0.5) * 0.07);
          const ly = armH ? (by + (rand() - 0.5) * 0.07) : (by - sy * rand() * armLen);
          rotateDoc(lx, ly, 0.32);
          colorBucket = 0.96;
          role = 2.0;
          sizeMul = 1.25;
        } else {
          // Razor-sharp Optical Scanning Laser Bar (swept up/down in GPU vertex shader!)
          const lx = (rand() - 0.5) * 5.2;
          const ly = (rand() - 0.5) * 0.06;
          const lz = 0.42 + (rand() - 0.5) * 0.06;
          rotateDoc(lx, ly, lz);
          colorBucket = 0.98;
          role = 1.0; // GPU sweeps laser across Y and illuminates passing text!
          sizeMul = 1.35;
        }
        break;
      }

      default: {
        sampleSphere(pos, i3, rand, 0, 0, 0, 2.4, true);
      }
    }

    meta[i4]     = colorBucket;
    meta[i4 + 1] = role;
    meta[i4 + 2] = phase;
    meta[i4 + 3] = sizeMul;
  }

  return { positions: pos, meta };
}

export function buildAllServiceShapes() {
  const names = Object.keys(FORM_MODE_ID);
  const shapes = {};
  names.forEach((name, idx) => {
    shapes[name] = generateShape(name, 202611 + idx * 7919);
  });
  return shapes;
}
