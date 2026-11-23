export const THEME_STORAGE_KEY = 'negahorbit_theme_v2';

export const THEME_ORDER = [
  'obsidian-champagne',
  'slate-platinum',
  'espresso-copper',
  'architectural-stone'
];

export const THEMES = {
  'obsidian-champagne': {
    id: 'obsidian-champagne',
    name: 'Obsidian & Champagne',
    shortName: 'Champagne',
    mode: 'dark',
    swatches: ['#0B0C0B', '#D8C39E'],
    canvasRgb: [0.043, 0.047, 0.043],
    particleColors: [
      [0.42, 0.41, 0.38],
      [0.95, 0.94, 0.91],
      [0.85, 0.76, 0.62],
      [0.075, 0.435, 0.322]
    ],
    laserColor: [0.85, 0.76, 0.62],
    hoverColor: [0.96, 0.94, 0.90],
    starColor: [0.76, 0.70, 0.58],
    particleOpacity: 0.90,
    heroOpacity: 0.36,
    starOpacity: 0.52,
    pointSize: 3.1,
    agent: {
      eyeCore: '#F2EFE9',
      eyeBg: '#E5D8BC',
      eyeGlow: '#D8C39E',
      hpLed: '#D8C39E',
      statusLit: '#E5D8BC',
      statusDim: '#D8C39E',
      visorBounce: '#D8C39E',
      floor0: '#136F52', floor0Op: '0.26', floor1: '#0B0C0B', floor1Op: '0.18',
      vest: ['#1D8260', '#136F52', '#0C4A36', '#07291E'],
      collar: ['#248F6B', '#116247', '#083022'],
      shoe: ['#197F5D', '#105E43', '#082E20'],
      badge: ['#0B4F39', '#136F52', '#3E9C7B']
    }
  },
  'slate-platinum': {
    id: 'slate-platinum',
    name: 'Slate & Platinum',
    shortName: 'Platinum',
    mode: 'dark',
    swatches: ['#0A0E14', '#CBD5E1'],
    canvasRgb: [0.039, 0.055, 0.078],
    particleColors: [
      [0.39, 0.45, 0.55],
      [0.58, 0.64, 0.72],
      [0.96, 0.97, 0.98],
      [0.082, 0.612, 0.420]
    ],
    laserColor: [0.89, 0.91, 0.94],
    hoverColor: [0.96, 0.97, 0.99],
    starColor: [0.60, 0.68, 0.78],
    particleOpacity: 0.90,
    heroOpacity: 0.36,
    starOpacity: 0.54,
    pointSize: 3.1,
    agent: {
      eyeCore: '#F4F7FA',
      eyeBg: '#E2F1F8',
      eyeGlow: '#C7E3F0',
      hpLed: '#E2F1F8',
      statusLit: '#F4F7FA',
      statusDim: '#94A3B8',
      visorBounce: '#94A3B8',
      floor0: '#159C6B', floor0Op: '0.24', floor1: '#0A0E14', floor1Op: '0.18',
      vest: ['#218F67', '#159C6B', '#0D563C', '#072D1F'],
      collar: ['#28A074', '#147A54', '#093827'],
      shoe: ['#1D8F66', '#116B4A', '#083324'],
      badge: ['#0D563C', '#159C6B', '#48C496']
    }
  },
  'espresso-copper': {
    id: 'espresso-copper',
    name: 'Espresso & Copper',
    shortName: 'Copper',
    mode: 'dark',
    swatches: ['#0F0E0D', '#D4896A'],
    canvasRgb: [0.059, 0.055, 0.051],
    particleColors: [
      [0.48, 0.45, 0.40],
      [0.94, 0.93, 0.90],
      [0.83, 0.54, 0.42],
      [0.239, 0.478, 0.384]
    ],
    laserColor: [0.83, 0.54, 0.42],
    hoverColor: [0.96, 0.77, 0.69],
    starColor: [0.78, 0.58, 0.48],
    particleOpacity: 0.90,
    heroOpacity: 0.36,
    starOpacity: 0.50,
    pointSize: 3.1,
    agent: {
      eyeCore: '#F7E7DF',
      eyeBg: '#E6A387',
      eyeGlow: '#D4896A',
      hpLed: '#E6A387',
      statusLit: '#E6A387',
      statusDim: '#D4896A',
      visorBounce: '#D4896A',
      floor0: '#3D7A62', floor0Op: '0.25', floor1: '#0F0E0D', floor1Op: '0.18',
      vest: ['#528F77', '#3D7A62', '#265241', '#152E24'],
      collar: ['#5C9A82', '#386E58', '#1C3B2F'],
      shoe: ['#4A876F', '#326652', '#19362B'],
      badge: ['#265241', '#3D7A62', '#64A68B']
    }
  },
  'architectural-stone': {
    id: 'architectural-stone',
    name: 'Architectural Stone',
    shortName: 'Stone',
    mode: 'light',
    swatches: ['#F3F2EE', '#0F6B4A'],
    canvasRgb: [0.953, 0.949, 0.933],
    particleColors: [
      [0.12, 0.16, 0.145],
      [0.24, 0.29, 0.27],
      [0.059, 0.420, 0.290],
      [0.082, 0.612, 0.420]
    ],
    laserColor: [0.059, 0.420, 0.290],
    hoverColor: [0.059, 0.420, 0.290],
    starColor: [0.36, 0.40, 0.38],
    particleOpacity: 0.82,
    heroOpacity: 0.26,
    starOpacity: 0.34,
    pointSize: 2.85,
    agent: {
      eyeCore: '#DDF7EC',
      eyeBg: '#43C495',
      eyeGlow: '#159C6B',
      hpLed: '#159C6B',
      statusLit: '#43C495',
      statusDim: '#159C6B',
      visorBounce: '#159C6B',
      floor0: '#111614', floor0Op: '0.22', floor1: '#111614', floor1Op: '0.09',
      vest: ['#18805B', '#0F6B4A', '#0A4731', '#06261A'],
      collar: ['#1F8F67', '#0F6B4A', '#083625'],
      shoe: ['#157D58', '#0E5D40', '#072E20'],
      badge: ['#083D2A', '#0F6B4A', '#159C6B']
    }
  }
};
