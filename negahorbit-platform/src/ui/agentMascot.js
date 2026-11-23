import { clamp, lerp, smoothstep } from '../engine/math.js';

const ROBOT_SVG_MARKUP = `<svg id="robot-svg" viewBox="0 0 380 470" fill="none" xmlns="http://www.w3.org/2000/svg" class="agent-svg">
            <defs>
              <!-- Ribbed Charcoal Knit Texture Pattern -->
              <pattern id="knitRibPattern" width="5" height="6" patternUnits="userSpaceOnUse">
                <line x1="2.5" y1="0" x2="2.5" y2="6" stroke="#090C0B" stroke-opacity="0.65" stroke-width="1.5"/>
                <line x1="4.2" y1="0" x2="4.2" y2="6" stroke="#36443F" stroke-opacity="0.32" stroke-width="0.9"/>
              </pattern>

              <!-- Fine Diagonal Knit Pattern for Sleeves -->
              <pattern id="sleeveKnitPattern" width="6" height="6" patternUnits="userSpaceOnUse" patternTransform="rotate(28)">
                <line x1="0" y1="0" x2="0" y2="6" stroke="#080B0A" stroke-opacity="0.58" stroke-width="1.6"/>
                <line x1="3" y1="0" x2="3" y2="6" stroke="#3A4C45" stroke-opacity="0.28" stroke-width="1"/>
              </pattern>

              <!-- Circular LED Dot-Matrix Pattern inside Eyes (Dynamically Recolored per Theme) -->
              <pattern id="ledDotMatrix" width="4.4" height="4.4" patternUnits="userSpaceOnUse">
                <rect id="led-dot-bg" width="4.4" height="4.4" fill="#D8C39E" fill-opacity="0.18"/>
                <circle id="led-dot-core" cx="2.2" cy="2.2" r="1.48" fill="#F2EFE9"/>
              </pattern>

              <!-- Visor Micro-Scanline Glass Overlay -->
              <pattern id="visorScanlines" width="4" height="3.2" patternUnits="userSpaceOnUse">
                <line x1="0" y1="1.6" x2="4" y2="1.6" stroke="#FFFFFF" stroke-opacity="0.045" stroke-width="0.8"/>
              </pattern>

              <!-- Soft Ambient Floor Shadow (Adapts for Dark & Light Stone Canvas) -->
              <radialGradient id="floorGlow" cx="50%" cy="50%" r="50%">
                <stop id="floor-stop-0" offset="0%" stop-color="#136F52" stop-opacity="0.26"/>
                <stop id="floor-stop-1" offset="55%" stop-color="#0B0C0B" stop-opacity="0.18"/>
                <stop id="floor-stop-2" offset="100%" stop-color="#0B0C0B" stop-opacity="0"/>
              </radialGradient>

              <!-- Helmet Shell with Subtle Neutral-Emerald Rim Highlight -->
              <radialGradient id="helmetShellGrad" cx="44%" cy="18%" r="74%">
                <stop id="helmet-stop-0" offset="0%" stop-color="#556B62"/>
                <stop offset="28%" stop-color="#202E29"/>
                <stop offset="68%" stop-color="#0E1412"/>
                <stop offset="100%" stop-color="#060807"/>
              </radialGradient>

              <!-- Glossy Curved Dark Glass Visor -->
              <radialGradient id="visorGlassGrad" cx="48%" cy="38%" r="65%">
                <stop offset="0%" stop-color="#182622"/>
                <stop offset="48%" stop-color="#0B1210"/>
                <stop offset="86%" stop-color="#050807"/>
                <stop offset="100%" stop-color="#020303"/>
              </radialGradient>

              <!-- Curved Specular Glass Reflection -->
              <linearGradient id="glassSpecular" x1="126" y1="54" x2="220" y2="150" gradientUnits="userSpaceOnUse">
                <stop offset="0%" stop-color="#FFFFFF" stop-opacity="0.42"/>
                <stop offset="38%" stop-color="#FFFFFF" stop-opacity="0.10"/>
                <stop offset="100%" stop-color="#FFFFFF" stop-opacity="0"/>
              </linearGradient>
              <linearGradient id="visorRimBounce" x1="190" y1="184" x2="190" y2="136" gradientUnits="userSpaceOnUse">
                <stop id="visor-bounce-stop" offset="0%" stop-color="#D8C39E" stop-opacity="0.22"/>
                <stop offset="100%" stop-color="#D8C39E" stop-opacity="0"/>
              </linearGradient>

              <!-- ClipPath for Wide-Range Visor Eyes -->
              <clipPath id="visorGlassClip">
                <ellipse cx="190" cy="116" rx="64" ry="60"/>
              </clipPath>

              <!-- Soft LED Eye Bloom -->
              <filter id="eyeGlow" x="-75%" y="-75%" width="250%" height="250%">
                <feGaussianBlur stdDeviation="4.8" result="blur1"/>
                <feGaussianBlur stdDeviation="2.2" result="blur2"/>
                <feMerge>
                  <feMergeNode in="blur1"/>
                  <feMergeNode in="blur2"/>
                  <feMergeNode in="SourceGraphic"/>
                </feMerge>
              </filter>

              <!-- Matte-Black Headphone Shell & Cushion Gradients -->
              <linearGradient id="hpBandGrad" x1="190" y1="22" x2="190" y2="128" gradientUnits="userSpaceOnUse">
                <stop offset="0%" stop-color="#353A38"/>
                <stop offset="45%" stop-color="#181B1A"/>
                <stop offset="100%" stop-color="#0A0C0B"/>
              </linearGradient>
              <linearGradient id="hpCupLeftGrad" x1="94" y1="90" x2="128" y2="156" gradientUnits="userSpaceOnUse">
                <stop offset="0%" stop-color="#2D3230"/>
                <stop offset="55%" stop-color="#131615"/>
                <stop offset="100%" stop-color="#070908"/>
              </linearGradient>
              <linearGradient id="hpCupRightGrad" x1="286" y1="90" x2="252" y2="156" gradientUnits="userSpaceOnUse">
                <stop offset="0%" stop-color="#2D3230"/>
                <stop offset="55%" stop-color="#131615"/>
                <stop offset="100%" stop-color="#070908"/>
              </linearGradient>

              <!-- Technical Puffer Vest Gradients (Dynamically Tuned for Sage vs Deep Malachite) -->
              <linearGradient id="vestYokeGrad" x1="190" y1="186" x2="190" y2="250" gradientUnits="userSpaceOnUse">
                <stop offset="0%" stop-color="#272C2A"/>
                <stop offset="45%" stop-color="#151817"/>
                <stop offset="100%" stop-color="#0B0D0C"/>
              </linearGradient>

              <linearGradient id="pufferBaffleGrad" x1="0" y1="0" x2="0" y2="1">
                <stop id="vest-stop-0" offset="0%" stop-color="#1D8260"/>
                <stop id="vest-stop-1" offset="28%" stop-color="#136F52"/>
                <stop id="vest-stop-2" offset="76%" stop-color="#0C4A36"/>
                <stop id="vest-stop-3" offset="100%" stop-color="#07291E"/>
              </linearGradient>

              <linearGradient id="pufferCollarGrad" x1="190" y1="168" x2="190" y2="206" gradientUnits="userSpaceOnUse">
                <stop id="collar-stop-0" offset="0%" stop-color="#248F6B"/>
                <stop id="collar-stop-1" offset="55%" stop-color="#116247"/>
                <stop id="collar-stop-2" offset="100%" stop-color="#083022"/>
              </linearGradient>

              <!-- Charcoal Knit Sweater Base -->
              <linearGradient id="charcoalKnitGrad" x1="190" y1="184" x2="190" y2="334" gradientUnits="userSpaceOnUse">
                <stop offset="0%" stop-color="#2B302E"/>
                <stop offset="55%" stop-color="#1B1F1E"/>
                <stop offset="100%" stop-color="#0E1110"/>
              </linearGradient>

              <!-- Matte Articulated Gloved Hands -->
              <radialGradient id="gloveGrad" cx="35%" cy="28%" r="72%">
                <stop offset="0%" stop-color="#3E4F48"/>
                <stop offset="45%" stop-color="#1B2420"/>
                <stop offset="100%" stop-color="#0A0E0C"/>
              </radialGradient>

              <!-- Tapered Charcoal Joggers -->
              <linearGradient id="joggersGrad" x1="190" y1="330" x2="190" y2="404" gradientUnits="userSpaceOnUse">
                <stop offset="0%" stop-color="#1E2321"/>
                <stop offset="60%" stop-color="#121514"/>
                <stop offset="100%" stop-color="#090B0A"/>
              </linearGradient>

              <!-- Malachite & Crisp White-Sole Sneakers -->
              <linearGradient id="sneakerUpperGrad" x1="0" y1="0" x2="0" y2="1">
                <stop id="shoe-stop-0" offset="0%" stop-color="#197F5D"/>
                <stop id="shoe-stop-1" offset="52%" stop-color="#105E43"/>
                <stop id="shoe-stop-2" offset="100%" stop-color="#082E20"/>
              </linearGradient>
              <linearGradient id="whiteSoleGrad" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#F2EFE9"/>
                <stop offset="60%" stop-color="#D3CFC6"/>
                <stop offset="100%" stop-color="#9A968E"/>
              </linearGradient>

              <!-- Negah Chest Badge Diamond Gradient & Clip -->
              <linearGradient id="badgeDiamondGrad" x1="0%" y1="100%" x2="100%" y2="0%">
                <stop id="badge-stop-0" offset="0%" stop-color="#0B4F39"/>
                <stop id="badge-stop-1" offset="52%" stop-color="#136F52"/>
                <stop id="badge-stop-2" offset="100%" stop-color="#3E9C7B"/>
              </linearGradient>
              <clipPath id="chestBadgeClip">
                <rect x="225" y="216" width="20" height="20" rx="4.5" transform="rotate(45 235 226)"/>
              </clipPath>
            </defs>

            <!-- Soft Ambient Floor Shadow -->
            <ellipse cx="190" cy="444" rx="116" ry="18" fill="url(#floorGlow)"/>

            <!-- ==================== MALACHITE & WHITE-SOLE SNEAKERS ==================== -->
            <g>
              <path d="M120 412 C121 397, 138 389, 158 391 L172 394 C178 396, 180 403, 180 412 L180 422 C180 427, 174 430, 166 430 L121 430 C113 430, 110 424, 112 418 Z" fill="url(#sneakerUpperGrad)"/>
              <path d="M113 420 C118 408, 134 405, 147 411 L151 422 H113 Z" fill="#0C1210" fill-opacity="0.72"/>
              <path d="M144 391 C152 387, 166 387, 173 394 L168 403 C160 398, 148 398, 141 402 Z" fill="#DCD8CE" fill-opacity="0.88"/>
              <path d="M140 399 L153 402 M144 405 L157 408 M148 411 L160 413" stroke="#080C0A" stroke-width="2.2" stroke-linecap="round"/>
              <path d="M111 422 L180 422 L180 428 C180 432.5, 175 435, 167 435 L120 435 C113 435, 111 431, 111 426 Z" fill="url(#whiteSoleGrad)"/>
              <line x1="115" y1="429" x2="176" y2="429" stroke="#626664" stroke-opacity="0.38" stroke-width="1"/>
              <rect x="160" y="423.5" width="14" height="3.2" rx="1.6" fill="var(--brand)"/>
            </g>

            <g>
              <path d="M260 412 C259 397, 242 389, 222 391 L208 394 C202 396, 200 403, 200 412 L200 422 C200 427, 206 430, 214 430 L259 430 C267 430, 270 424, 268 418 Z" fill="url(#sneakerUpperGrad)"/>
              <path d="M267 420 C262 408, 246 405, 233 411 L229 422 H267 Z" fill="#0C1210" fill-opacity="0.72"/>
              <path d="M236 391 C228 387, 214 387, 207 394 L212 403 C220 398, 232 398, 239 402 Z" fill="#DCD8CE" fill-opacity="0.88"/>
              <path d="M240 399 L227 402 M236 405 L223 408 M232 411 L220 413" stroke="#080C0A" stroke-width="2.2" stroke-linecap="round"/>
              <path d="M200 422 L269 422 L269 426 C269 431, 267 435, 260 435 L213 435 C205 435, 200 432.5, 200 428 Z" fill="url(#whiteSoleGrad)"/>
              <line x1="204" y1="429" x2="265" y2="429" stroke="#626664" stroke-opacity="0.38" stroke-width="1"/>
              <rect x="206" y="423.5" width="14" height="3.2" rx="1.6" fill="var(--brand)"/>
            </g>

            <!-- ==================== DARK TAPERED JOGGERS ==================== -->
            <g>
              <path d="M126 332 H254 L250 388 C249 396, 242 400, 233 400 H207 C200 400, 196 394, 196 386 V366 C196 360, 184 360, 184 366 V386 C184 394, 180 400, 173 400 H147 C138 400, 131 396, 130 388 Z" fill="url(#joggersGrad)"/>
              <path d="M152 345 C148 360, 150 374, 154 388 M228 345 C232 360, 230 374, 226 388" stroke="#050807" stroke-opacity="0.7" stroke-width="3" stroke-linecap="round"/>
              <rect x="134" y="388" width="44" height="12" rx="5" fill="#171C1B" stroke="#2D3532" stroke-opacity="0.55" stroke-width="1"/>
              <rect x="202" y="388" width="44" height="12" rx="5" fill="#171C1B" stroke="#2D3532" stroke-opacity="0.55" stroke-width="1"/>
            </g>

            <!-- ==================== ARTICULATED GLOVED HANDS ==================== -->
            <g>
              <path d="M88 308 C77 314, 72 329, 77 341 C79 346, 85 349, 88 344 C90 340, 88 331, 91 327 C93 335, 95 346, 101 347 C106 347, 108 340, 106 331 C109 335, 113 336, 116 332 C119 328, 115 315, 109 307 Z" fill="url(#gloveGrad)"/>
              <path d="M80 330 L87 331 M94 335 L103 335" stroke="var(--accent)" stroke-opacity="0.25" stroke-width="1.2" stroke-linecap="round"/>
            </g>
            <g>
              <path d="M292 308 C303 314, 308 329, 303 341 C301 346, 295 349, 292 344 C290 340, 292 331, 289 327 C287 335, 285 346, 279 347 C274 347, 272 340, 274 331 C271 335, 267 336, 264 332 C261 328, 265 315, 271 307 Z" fill="url(#gloveGrad)"/>
              <path d="M300 330 L293 331 M286 335 L277 335" stroke="var(--accent)" stroke-opacity="0.25" stroke-width="1.2" stroke-linecap="round"/>
            </g>

            <!-- ==================== CHARCOAL RIBBED KNIT SLEEVES ==================== -->
            <g>
              <path d="M122 204 C98 220, 80 256, 74 288 C72 299, 78 310, 90 314 L114 316 C122 311, 128 296, 132 278 Z" fill="url(#charcoalKnitGrad)"/>
              <path d="M122 204 C98 220, 80 256, 74 288 C72 299, 78 310, 90 314 L114 316 C122 311, 128 296, 132 278 Z" fill="url(#sleeveKnitPattern)"/>
              <rect x="78" y="298" width="40" height="17" rx="7.5" transform="rotate(11 78 298)" fill="#181D1B" stroke="#2D3532" stroke-opacity="0.55" stroke-width="1"/>
              <path d="M85 301 L82 314 M92 302 L89 315 M99 303 L96 316 M106 305 L103 317" stroke="#080C0B" stroke-opacity="0.7" stroke-width="1.4"/>
            </g>

            <g>
              <path d="M258 204 C282 220, 300 256, 306 288 C308 299, 302 310, 290 314 L266 316 C258 311, 252 296, 248 278 Z" fill="url(#charcoalKnitGrad)"/>
              <path d="M258 204 C282 220, 300 256, 306 288 C308 299, 302 310, 290 314 L266 316 C258 311, 252 296, 248 278 Z" fill="url(#sleeveKnitPattern)"/>
              <rect x="262" y="305" width="40" height="17" rx="7.5" transform="rotate(-11 262 305)" fill="#181D1B" stroke="#2D3532" stroke-opacity="0.55" stroke-width="1"/>
              <path d="M274 305 L277 317 M281 303 L284 316 M288 302 L291 315 M295 301 L298 314" stroke="#080C0B" stroke-opacity="0.7" stroke-width="1.4"/>
            </g>

            <!-- ==================== CHARCOAL RIBBED TURTLENECK TORSO CENTER ==================== -->
            <g>
              <rect x="150" y="176" width="80" height="162" rx="18" fill="url(#charcoalKnitGrad)"/>
              <rect x="150" y="176" width="80" height="162" rx="18" fill="url(#knitRibPattern)"/>
              <path d="M150 184 C168 193, 212 193, 230 184 L226 206 C208 214, 172 214, 154 206 Z" fill="#232826"/>
              <path d="M150 184 C168 193, 212 193, 230 184 L226 206 C208 214, 172 214, 154 206 Z" fill="url(#knitRibPattern)"/>
              <rect x="154" y="324" width="72" height="14" rx="5" fill="#171C1A"/>
              <rect x="154" y="324" width="72" height="14" rx="5" fill="url(#knitRibPattern)"/>
            </g>

            <!-- ==================== QUILTED TECHNICAL PUFFER VEST ==================== -->
            <g>
              <path d="M128 186 C128 172, 144 166, 160 176 L172 204 L126 204 Z" fill="url(#pufferCollarGrad)" stroke="#FFFFFF" stroke-opacity="0.12" stroke-width="1"/>
              <path d="M252 186 C252 172, 236 166, 220 176 L208 204 L254 204 Z" fill="url(#pufferCollarGrad)" stroke="#FFFFFF" stroke-opacity="0.12" stroke-width="1"/>
            </g>

            <!-- LEFT PUFFER VEST PANEL -->
            <g>
              <path d="M122 198 C134 190, 156 194, 172 204 L172 246 H118 C114 230, 116 210, 122 198 Z" fill="url(#vestYokeGrad)" stroke="#2F3533" stroke-opacity="0.45" stroke-width="1"/>
              <path d="M118 246 H172 V276 H114 C112 266, 113 254, 118 246 Z" fill="url(#pufferBaffleGrad)" stroke="#051A13" stroke-width="1.4"/>
              <path d="M114 276 H172 V308 H115 C112 298, 112 286, 114 276 Z" fill="url(#pufferBaffleGrad)" stroke="#051A13" stroke-width="1.4"/>
              <path d="M115 308 H172 V334 C156 336, 130 336, 120 331 C116 326, 114 316, 115 308 Z" fill="url(#pufferBaffleGrad)" stroke="#051A13" stroke-width="1.4"/>
              <path d="M128 264 L122 296" stroke="#06120D" stroke-width="3.2" stroke-linecap="round"/>
              <path d="M128 264 L122 296" stroke="var(--accent)" stroke-opacity="0.32" stroke-width="1" stroke-dasharray="2 2"/>
              <rect x="125" y="266" width="4" height="8" rx="1.5" transform="rotate(10 125 266)" fill="#242C29" stroke="var(--accent)" stroke-opacity="0.35" stroke-width="0.6"/>
              <rect x="169" y="204" width="5.5" height="130" rx="2" fill="#0B100E"/>
              <line x1="172" y1="206" x2="172" y2="332" stroke="#595E5B" stroke-width="2" stroke-dasharray="2.2 2.2"/>
            </g>

            <!-- RIGHT PUFFER VEST PANEL -->
            <g>
              <path d="M258 198 C246 190, 224 194, 208 204 L208 246 H262 C266 230, 264 210, 258 198 Z" fill="url(#vestYokeGrad)" stroke="#2F3533" stroke-opacity="0.45" stroke-width="1"/>
              <path d="M262 246 H208 V276 H266 C268 266, 267 254, 262 246 Z" fill="url(#pufferBaffleGrad)" stroke="#051A13" stroke-width="1.4"/>
              <path d="M266 276 H208 V308 H265 C268 298, 268 286, 266 276 Z" fill="url(#pufferBaffleGrad)" stroke="#051A13" stroke-width="1.4"/>
              <path d="M265 308 H208 V334 C224 336, 250 336, 260 331 C264 326, 266 316, 265 308 Z" fill="url(#pufferBaffleGrad)" stroke="#051A13" stroke-width="1.4"/>
              <path d="M252 264 L258 296" stroke="#06120D" stroke-width="3.2" stroke-linecap="round"/>
              <path d="M252 264 L258 296" stroke="var(--accent)" stroke-opacity="0.32" stroke-width="1" stroke-dasharray="2 2"/>
              <rect x="251" y="266" width="4" height="8" rx="1.5" transform="rotate(-10 251 266)" fill="#242C29" stroke="var(--accent)" stroke-opacity="0.35" stroke-width="0.6"/>
              <rect x="205.5" y="204" width="5.5" height="130" rx="2" fill="#0B100E"/>
              <line x1="208" y1="206" x2="208" y2="332" stroke="#595E5B" stroke-width="2" stroke-dasharray="2.2 2.2"/>
              <rect x="205.5" y="330" width="5" height="11" rx="2" fill="#2A322F" stroke="var(--accent)" stroke-opacity="0.4" stroke-width="0.7"/>
            </g>

            <!-- Technical Matte-Black Collar & Hem Drawcords with Aglets -->
            <g stroke-linecap="round">
              <path d="M154 196 L154 246" stroke="#222826" stroke-width="3.4"/>
              <rect x="152" y="242" width="4" height="9" rx="1.8" fill="#101412" stroke="var(--accent)" stroke-opacity="0.45" stroke-width="0.6"/>
              <path d="M226 196 L226 246" stroke="#222826" stroke-width="3.4"/>
              <rect x="224" y="242" width="4" height="9" rx="1.8" fill="#101412" stroke="var(--accent)" stroke-opacity="0.45" stroke-width="0.6"/>
              <path d="M152 332 L152 356" stroke="#1E2321" stroke-width="2.8"/>
              <rect x="150.2" y="352" width="3.6" height="7" rx="1.5" fill="#101412" stroke="var(--accent)" stroke-opacity="0.4" stroke-width="0.6"/>
              <path d="M228 332 L228 356" stroke="#1E2321" stroke-width="2.8"/>
              <rect x="226.2" y="352" width="3.6" height="7" rx="1.5" fill="#101412" stroke="var(--accent)" stroke-opacity="0.4" stroke-width="0.6"/>
            </g>

            <!-- EMBROIDERED NEGAH BANK CIRCUIT-DIAMOND CHEST BADGE -->
            <g id="chest-emblem-group">
              <rect x="223" y="214" width="24" height="24" rx="6" transform="rotate(45 235 226)" fill="#071C15" stroke="var(--accent)" stroke-opacity="0.52" stroke-width="1" stroke-dasharray="2.4 1.6"/>
              <rect x="225" y="216" width="20" height="20" rx="4.5" transform="rotate(45 235 226)" fill="url(#badgeDiamondGrad)" stroke="#F2EFE9" stroke-opacity="0.6" stroke-width="0.8"/>
              <g clip-path="url(#chestBadgeClip)" stroke="#F2EFE9" stroke-width="1.25" stroke-linecap="round" stroke-linejoin="round">
                <path d="M228 216 V225 L230 227 V237"/>
                <path d="M232 213 V239"/>
                <path d="M236 213 V227 L238.5 229.5 V238"/>
                <path d="M240 216 V224 L242.5 226.5 V236"/>
              </g>
            </g>

            <!-- ==================== SLEEK OVER-EAR HEADPHONES & VISOR HELMET ==================== -->
            <g id="robot-head-rig">
            <g id="headphones-group">
              <path d="M107 116 C107 52, 142 26, 190 26 C238 26, 273 52, 273 116" stroke="url(#hpBandGrad)" stroke-width="14" stroke-linecap="round"/>
              <path d="M116 92 C124 50, 152 31, 190 31 C228 31, 256 50, 264 92" stroke="#FFFFFF" stroke-opacity="0.18" stroke-width="1.8" stroke-linecap="round"/>

              <!-- Left Acoustic Ear-Cup -->
              <g>
                <rect x="112" y="92" width="16" height="58" rx="8" fill="#0B0E0D" stroke="#252B29" stroke-width="1"/>
                <rect x="97" y="96" width="18" height="50" rx="8.5" fill="url(#hpCupLeftGrad)" stroke="#2F3634" stroke-width="1"/>
                <path id="hp-led-left" class="hp-led-ring" d="M102 104 V138" stroke="#D8C39E" stroke-width="2.6" stroke-linecap="round"/>
                <path id="hp-arc-left" class="hp-sound-arc" d="M88 106 C81 114, 81 128, 88 136" stroke="#D8C39E" stroke-width="1.8" stroke-linecap="round" fill="none"/>
              </g>

              <!-- Right Acoustic Ear-Cup -->
              <g>
                <rect x="252" y="92" width="16" height="58" rx="8" fill="#0B0E0D" stroke="#252B29" stroke-width="1"/>
                <rect x="265" y="96" width="18" height="50" rx="8.5" fill="url(#hpCupRightGrad)" stroke="#2F3634" stroke-width="1"/>
                <path id="hp-led-right" class="hp-led-ring" d="M278 104 V138" stroke="#D8C39E" stroke-width="2.6" stroke-linecap="round"/>
                <path id="hp-arc-right" class="hp-sound-arc" d="M292 106 C299 114, 299 128, 292 136" stroke="#D8C39E" stroke-width="1.8" stroke-linecap="round" fill="none"/>
              </g>
            </g>

            <!-- Rounded Metallic Helmet Shell -->
            <ellipse cx="190" cy="116" rx="71" ry="67" fill="url(#helmetShellGrad)" stroke="var(--accent)" stroke-opacity="0.28" stroke-width="1.5"/>

            <!-- Glossy Curved Dark Visor Faceplate -->
            <ellipse id="visor-hitbox" cx="190" cy="116" rx="64" ry="60" fill="url(#visorGlassGrad)" stroke="#2F3B36" stroke-opacity="0.7" stroke-width="1.6"/>
            <ellipse cx="190" cy="116" rx="64" ry="60" fill="url(#visorScanlines)"/>
            <ellipse cx="190" cy="116" rx="64" ry="60" fill="url(#visorRimBounce)"/>

            <!-- Wide-Range 3D Cursor-Tracking Circular LED Dot-Matrix Eyes -->
            <g clip-path="url(#visorGlassClip)">
              <g id="robot-eyes-rig">
                <g id="robot-eyes-blink-wrap">
                  <g id="robot-eyes-normal" filter="url(#eyeGlow)">
                    <circle id="eye-left-glow" cx="163" cy="117" r="14.5" fill="#D8C39E" fill-opacity="0.62"/>
                    <circle cx="163" cy="117" r="14.5" fill="url(#ledDotMatrix)"/>
                    <circle id="eye-right-glow" cx="217" cy="117" r="14.5" fill="#D8C39E" fill-opacity="0.62"/>
                    <circle cx="217" cy="117" r="14.5" fill="url(#ledDotMatrix)"/>
                  </g>
                  <g id="robot-eyes-happy" opacity="0" filter="url(#eyeGlow)">
                    <path id="eye-happy-left" d="M149 120 C155 106, 171 106, 177 120" stroke="#E5D8BC" stroke-width="6" stroke-linecap="round" fill="none"/>
                    <path id="eye-happy-right" d="M203 120 C209 106, 225 106, 231 120" stroke="#E5D8BC" stroke-width="6" stroke-linecap="round" fill="none"/>
                  </g>
                </g>
              </g>

              <!-- Curved Specular Glass Reflection -->
              <g id="visor-specular">
                <path d="M140 72 C164 56, 206 56, 230 70 C212 86, 172 96, 144 120 C132 102, 132 82, 140 72 Z" fill="url(#glassSpecular)"/>
                <path d="M234 152 C244 142, 249 128, 250 112" stroke="#FFFFFF" stroke-opacity="0.18" stroke-width="2" stroke-linecap="round"/>
              </g>
            </g>

            <!-- Subtle Chapter Status Micro-LEDs along Visor Chin Rim -->
            <g id="visor-status-leds">
              <circle id="status-led-0" cx="176" cy="171" r="1.8" fill="#E5D8BC"/>
              <circle id="status-led-1" cx="185" cy="172.5" r="1.8" fill="#D8C39E" fill-opacity="0.35"/>
              <circle id="status-led-2" cx="195" cy="172.5" r="1.8" fill="#D8C39E" fill-opacity="0.35"/>
              <circle id="status-led-3" cx="204" cy="171" r="1.8" fill="#D8C39E" fill-opacity="0.35"/>
            </g>
                      </g>
          </svg>`;

// Three refined concentric Hero orbital rings (crisp anti-aliased vector arcs — zero 3D perspective distortion!)
const ORBIT_RINGS = [
  { id: 0, rx: 222, ry: 72,  tiltDeg: -16, speed:  0.52, phase: 0.4,  strokeW: 1.35, satR: 5.2, dashed: false },
  { id: 1, rx: 266, ry: 88,  tiltDeg:  13, speed: -0.38, phase: 2.35, strokeW: 1.15, satR: 4.5, dashed: true  },
  { id: 2, rx: 310, ry: 104, tiltDeg:  -4, speed:  0.27, phase: 4.5,  strokeW: 0.95, satR: 3.8, dashed: false }
];

function buildOrbitLayerSvg(layerId, isFront) {
  // SVG coordinates (+Y down):
  // Back arc (passes BEHIND character):  M -rx 0 A rx ry 0 0 1 rx 0
  // Front arc (passes IN FRONT of body): M rx 0 A rx ry 0 0 1 -rx 0
  const arcs = ORBIT_RINGS.map((r) => {
    const d = isFront
      ? `M ${r.rx} 0 A ${r.rx} ${r.ry} 0 0 1 ${-r.rx} 0`
      : `M ${-r.rx} 0 A ${r.rx} ${r.ry} 0 0 1 ${r.rx} 0`;
    const gradId = isFront ? `orbitFrontGrad-${r.id}` : `orbitBackGrad-${r.id}`;
    const dashAttr = r.dashed ? 'stroke-dasharray="7 6"' : '';

    let ticks = '';
    if (r.id === 2) {
      const tickAngles = isFront
        ? [20, 45, 70, 110, 135, 160]
        : [200, 225, 250, 290, 315, 340];
      ticks = tickAngles.map((deg) => {
        const rad = (deg * Math.PI) / 180;
        const x1 = (r.rx - 4) * Math.cos(rad);
        const y1 = (r.ry - 2.2) * Math.sin(rad);
        const x2 = (r.rx + 4) * Math.cos(rad);
        const y2 = (r.ry + 2.2) * Math.sin(rad);
        return `<line x1="${x1.toFixed(1)}" y1="${y1.toFixed(1)}" x2="${x2.toFixed(1)}" y2="${y2.toFixed(1)}" stroke="var(--accent)" stroke-opacity="${isFront ? '0.45' : '0.20'}" stroke-width="1"/>`;
      }).join('');
    }

    const satPrefix = isFront ? 'front' : 'back';
    return `
      <g transform="rotate(${r.tiltDeg})">
        <path d="${d}"
              fill="none"
              stroke="url(#${gradId})"
              stroke-width="${r.strokeW}"
              stroke-linecap="round"
              ${dashAttr} />
        ${ticks}
        <g id="orbit-sat-${satPrefix}-${r.id}" opacity="0">
          <circle id="orbit-trail2-${satPrefix}-${r.id}" cx="0" cy="0" r="${(r.satR * 0.45).toFixed(1)}" fill="var(--brand)" opacity="0.28" />
          <circle id="orbit-trail1-${satPrefix}-${r.id}" cx="0" cy="0" r="${(r.satR * 0.65).toFixed(1)}" fill="var(--accent)" opacity="0.48" />
          <circle cx="0" cy="0" r="${(r.satR * 2.3).toFixed(1)}" fill="url(#orbitSatGlow-${satPrefix})" />
          <circle cx="0" cy="0" r="${r.satR}" fill="var(--text-primary)" stroke="var(--brand)" stroke-width="1.2" />
        </g>
      </g>
    `;
  }).join('');

  const defs = ORBIT_RINGS.map((r) => {
    if (isFront) {
      return `
        <linearGradient id="orbitFrontGrad-${r.id}" x1="-100%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" stop-color="var(--accent)" stop-opacity="0.16"/>
          <stop offset="28%" stop-color="var(--accent)" stop-opacity="${r.id === 0 ? '0.80' : '0.58'}"/>
          <stop offset="50%" stop-color="var(--brand)" stop-opacity="${r.id === 0 ? '0.90' : '0.70'}"/>
          <stop offset="72%" stop-color="var(--accent)" stop-opacity="${r.id === 0 ? '0.80' : '0.58'}"/>
          <stop offset="100%" stop-color="var(--accent)" stop-opacity="0.16"/>
        </linearGradient>
      `;
    }
    return `
      <linearGradient id="orbitBackGrad-${r.id}" x1="-100%" y1="0%" x2="100%" y2="0%">
        <stop offset="0%" stop-color="var(--accent)" stop-opacity="0.16"/>
        <stop offset="50%" stop-color="var(--accent)" stop-opacity="${r.id === 0 ? '0.30' : '0.20'}"/>
        <stop offset="100%" stop-color="var(--accent)" stop-opacity="0.16"/>
      </linearGradient>
    `;
  }).join('');

  const satPrefix = isFront ? 'front' : 'back';
  return `
    <svg id="${layerId}" class="hero-orbit-svg ${isFront ? 'hero-orbit-front' : 'hero-orbit-back'}" viewBox="-350 -240 700 480" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
      <defs>
        ${defs}
        <radialGradient id="orbitSatGlow-${satPrefix}" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stop-color="var(--accent)" stop-opacity="${isFront ? '0.75' : '0.45'}"/>
          <stop offset="55%" stop-color="var(--brand)" stop-opacity="${isFront ? '0.28' : '0.14'}"/>
          <stop offset="100%" stop-color="var(--brand)" stop-opacity="0"/>
        </radialGradient>
      </defs>
      ${arcs}
    </svg>
  `;
}

export class AgentMascot {
  constructor({ stageEl, initialTheme }) {
    this.stageEl = stageEl;
    this.theme = initialTheme;

    this.stageEl.innerHTML = `
      <div class="agent-orbit-wrap" id="agent-orbit-back-wrap">
        ${buildOrbitLayerSvg('orbit-back-svg', false)}
      </div>
      <div id="robot-inner" class="agent-breathe" title="Click to interact with NegahOrbit Agent">
        <svg id="robot-holo-pulse" class="holo-pulse-ring" viewBox="0 0 280 280" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
          <rect x="48" y="48" width="184" height="184" rx="38" transform="rotate(45 140 140)" stroke="var(--accent)" stroke-width="1.8" stroke-opacity="0.9"/>
          <rect x="64" y="64" width="152" height="152" rx="30" transform="rotate(45 140 140)" stroke="var(--brand)" stroke-width="1.2" stroke-dasharray="6 6" stroke-opacity="0.8"/>
          <path d="M140 12 V38 M140 242 V268 M12 140 H38 M242 140 H268" stroke="var(--accent)" stroke-width="1.4" stroke-linecap="round" stroke-opacity="0.8"/>
        </svg>
        <div id="robot-bounce-wrap">
          ${ROBOT_SVG_MARKUP}
        </div>
      </div>
      <div class="agent-orbit-wrap hero-orbit-front-wrap" id="agent-orbit-front-wrap">
        ${buildOrbitLayerSvg('orbit-front-svg', true)}
      </div>
    `;

    // Cache DOM references once (ZERO per-frame DOM queries or layout reads!)
    this.robotInner = this.stageEl.querySelector('#robot-inner');
    this.bounceWrap = this.stageEl.querySelector('#robot-bounce-wrap');
    this.holoPulse = this.stageEl.querySelector('#robot-holo-pulse');
    this.headRig = this.stageEl.querySelector('#robot-head-rig');
    this.eyesRig = this.stageEl.querySelector('#robot-eyes-rig');
    this.eyesBlink = this.stageEl.querySelector('#robot-eyes-blink-wrap');
    this.eyesNormal = this.stageEl.querySelector('#robot-eyes-normal');
    this.eyesHappy = this.stageEl.querySelector('#robot-eyes-happy');
    this.visorSpecular = this.stageEl.querySelector('#visor-specular');
    this.chestEmblem = this.stageEl.querySelector('#chest-emblem-group');
    this.headphonesGroup = this.stageEl.querySelector('#headphones-group');
    this.orbitBackWrap = this.stageEl.querySelector('#agent-orbit-back-wrap');
    this.orbitFrontWrap = this.stageEl.querySelector('#agent-orbit-front-wrap');

    this.statusLeds = [0, 1, 2, 3].map(i => this.stageEl.querySelector(`#status-led-${i}`));
    this.satRefs = ORBIT_RINGS.map((r) => ({
      ring: r,
      front: this.stageEl.querySelector(`#orbit-sat-front-${r.id}`),
      back: this.stageEl.querySelector(`#orbit-sat-back-${r.id}`),
      fT1: this.stageEl.querySelector(`#orbit-trail1-front-${r.id}`),
      fT2: this.stageEl.querySelector(`#orbit-trail2-front-${r.id}`),
      bT1: this.stageEl.querySelector(`#orbit-trail1-back-${r.id}`),
      bT2: this.stageEl.querySelector(`#orbit-trail2-back-${r.id}`)
    }));

    this.eyeX = 0;
    this.eyeY = 0;
    this.eyeVelX = 0;
    this.eyeVelY = 0;
    this.headShiftX = 0;
    this.headShiftY = 0;
    this.headRot = 0;
    this.blinkTimer = 2.4;
    this.blinkPhase = 0;
    this.doubleBlinkQueued = false;
    this.happyTimer = 0;
    this.nodBoost = 0;
    this.activeChapter = 0;

    this.vw = window.innerWidth;
    this.vh = window.innerHeight;
    this.baseWidth = this.computeBaseWidth();

    this.setTheme(initialTheme);
    this.bindEvents();
  }

  computeBaseWidth() {
    if (this.vw >= 1024) return 360;
    if (this.vw >= 640) return 300;
    return 225;
  }

  resize(vw, vh) {
    this.vw = vw;
    this.vh = vh;
    this.baseWidth = this.computeBaseWidth();
  }

  setTheme(theme) {
    this.theme = theme;
    const a = theme.agent;
    const setAttr = (id, attr, val) => {
      const el = this.stageEl.querySelector(`#${id}`);
      if (el) el.setAttribute(attr, val);
    };

    setAttr('led-dot-core', 'fill', a.eyeCore);
    setAttr('led-dot-bg', 'fill', a.eyeBg);
    setAttr('eye-left-glow', 'fill', a.eyeGlow);
    setAttr('eye-right-glow', 'fill', a.eyeGlow);
    setAttr('eye-happy-left', 'stroke', a.eyeBg);
    setAttr('eye-happy-right', 'stroke', a.eyeBg);
    setAttr('hp-led-left', 'stroke', a.hpLed);
    setAttr('hp-led-right', 'stroke', a.hpLed);
    setAttr('hp-arc-left', 'stroke', a.hpLed);
    setAttr('hp-arc-right', 'stroke', a.hpLed);
    setAttr('visor-bounce-stop', 'stop-color', a.visorBounce);
    setAttr('floor-stop-0', 'stop-color', a.floor0);
    setAttr('floor-stop-0', 'stop-opacity', a.floor0Op);
    setAttr('floor-stop-1', 'stop-color', a.floor1);
    setAttr('floor-stop-1', 'stop-opacity', a.floor1Op);
    setAttr('floor-stop-2', 'stop-color', a.floor1);

    a.vest.forEach((c, i) => setAttr(`vest-stop-${i}`, 'stop-color', c));
    a.collar.forEach((c, i) => setAttr(`collar-stop-${i}`, 'stop-color', c));
    a.shoe.forEach((c, i) => setAttr(`shoe-stop-${i}`, 'stop-color', c));
    a.badge.forEach((c, i) => setAttr(`badge-stop-${i}`, 'stop-color', c));

    this.updateChapterAccents(this.activeChapter);
  }

  updateChapterAccents(activeIdx) {
    this.activeChapter = activeIdx;
    if (this.headphonesGroup) {
      this.headphonesGroup.classList.toggle('asr-headphones-active', activeIdx === 2);
    }
    const a = this.theme.agent;
    this.statusLeds.forEach((led, i) => {
      if (!led) return;
      const isLit = activeIdx === 0 ? (i === 0) : (i === activeIdx - 1);
      led.setAttribute('fill', isLit ? a.statusLit : a.statusDim);
      led.setAttribute('fill-opacity', isLit ? '1' : '0.32');
      led.setAttribute('r', isLit ? '2.2' : '1.6');
    });
  }

  pulseBadge() {
    this.nodBoost = 1.0;
    if (!this.chestEmblem) return;
    this.chestEmblem.classList.remove('emblem-pulse-active');
    void this.chestEmblem.offsetWidth;
    this.chestEmblem.classList.add('emblem-pulse-active');
  }

  triggerInteract() {
    this.happyTimer = 0.95;
    if (this.bounceWrap) {
      this.bounceWrap.classList.remove('agent-bounce-active');
      void this.bounceWrap.offsetWidth;
      this.bounceWrap.classList.add('agent-bounce-active');
    }
    if (this.holoPulse) {
      this.holoPulse.classList.remove('is-pulsing');
      void this.holoPulse.offsetWidth;
      this.holoPulse.classList.add('is-pulsing');
    }
    this.pulseBadge();
  }

  bindEvents() {
    this.robotInner?.addEventListener('click', () => this.triggerInteract());
  }

  update({ dt, elapsed, scrollProgress, mousePx, mousePy, smoothMouseX, smoothMouseY, isMobile }) {
    // 1. Consistent Left Docking Choreography:
    //    Hero (0.0) -> Full-scale right (73% vw, 53% vh, scale 1.0) with layered Hero Orbital Rings
    //    Chapters 01–04 -> Permanently docked lower-left (9.5% vw, 77% vh, scale 0.48)
    const dockT = smoothstep(clamp(scrollProgress, 0, 1));

    const heroX = isMobile ? 0.50 : 0.73;
    const heroY = isMobile ? 0.76 : 0.53;
    const heroScale = isMobile ? 0.62 : 1.00;

    const dockX = isMobile ? 0.82 : 0.095;
    const dockY = isMobile ? 0.85 : 0.77;
    const dockScale = isMobile ? 0.34 : 0.48;

    const xNorm = lerp(heroX, dockX, dockT);
    const yNorm = lerp(heroY, dockY, dockT);
    const scale = lerp(heroScale, dockScale, dockT);

    const px = xNorm * this.vw + smoothMouseX * (12 * scale);
    const py = yNorm * this.vh + smoothMouseY * (8 * scale);

    this.stageEl.style.transform = `translate3d(${px.toFixed(1)}px, ${py.toFixed(1)}px, 0) translate(-50%, -50%) scale(${scale.toFixed(4)})`;

    // 2. Smoothly fade & scale Hero Orbital Rings (visible in Hero, cleanly tucked away once docked)
    const orbitOpacity = clamp(1.0 - scrollProgress * 1.35, 0, 1);
    const orbitScale = 0.84 + orbitOpacity * 0.16;
    const orbitTransform = `translate(-50%, -50%) scale(${orbitScale.toFixed(3)})`;

    if (this.orbitBackWrap && this.orbitFrontWrap) {
      this.orbitBackWrap.style.opacity = orbitOpacity.toFixed(3);
      this.orbitFrontWrap.style.opacity = orbitOpacity.toFixed(3);
      if (orbitOpacity > 0.01) {
        this.orbitBackWrap.style.transform = orbitTransform;
        this.orbitFrontWrap.style.transform = orbitTransform;
        this.updateOrbitalSatellites(elapsed);
      }
    }

    // 3. Target-Aware Gaze:
    //    Hero -> watches user cursor
    //    Docked Chapters 01–04 -> looks rightward toward the active 3D service sculpture
    const svgHeight = this.baseWidth * (470 / 380);
    const visorScreenX = px;
    const visorScreenY = py - (0.5 - 116 / 470) * svgHeight * scale;

    const sculptureScreenX = this.vw * 0.74;
    const sculptureScreenY = this.vh * 0.49;

    this.nodBoost *= Math.pow(0.14, dt);
    const watchWeight = clamp(dockT * 0.76 + this.nodBoost * 0.18, 0, 0.92);
    const targetScreenX = lerp(mousePx, sculptureScreenX, watchWeight);
    const targetScreenY = lerp(mousePy, sculptureScreenY, watchWeight);

    const vecX = targetScreenX - visorScreenX;
    const vecY = targetScreenY - visorScreenY;
    const reachX = Math.max(this.vw * 0.32, 220);
    const reachY = Math.max(this.vh * 0.30, 180);

    const normLookX = Math.tanh(vecX / reachX);
    const normLookY = Math.tanh(vecY / reachY);

    const targetEyeX = normLookX * 22.0;
    const targetEyeY = normLookY * 16.5;

    const stiffness = 138;
    const damping = 17.5;
    this.eyeVelX += ((targetEyeX - this.eyeX) * stiffness - this.eyeVelX * damping) * dt;
    this.eyeVelY += ((targetEyeY - this.eyeY) * stiffness - this.eyeVelY * damping) * dt;
    this.eyeX += this.eyeVelX * dt;
    this.eyeY += this.eyeVelY * dt;

    const lookMagX = Math.min(1, Math.abs(this.eyeX) / 22.0);
    const lookMagY = Math.min(1, Math.abs(this.eyeY) / 16.5);
    const eyeScaleX = 1.0 - lookMagX * 0.14;
    const eyeScaleY = 1.0 - lookMagY * 0.08;
    const eyeSkewY = (this.eyeX / 22.0) * (this.eyeY / 16.5) * 4.2;

    if (this.eyesRig) {
      this.eyesRig.setAttribute(
        'transform',
        `translate(${(190 + this.eyeX).toFixed(2)}, ${(116 + this.eyeY).toFixed(2)}) ` +
        `scale(${eyeScaleX.toFixed(3)}, ${eyeScaleY.toFixed(3)}) ` +
        `skewY(${eyeSkewY.toFixed(2)}) translate(-190, -116)`
      );
    }

    if (this.visorSpecular) {
      this.visorSpecular.setAttribute(
        'transform',
        `translate(${(-this.eyeX * 0.22).toFixed(2)}, ${(-this.eyeY * 0.16).toFixed(2)})`
      );
    }

    // Subtle independent head-only turn/nod around neck (190, 178) — body stays completely grounded!
    const headAlpha = 1.0 - Math.exp(-8.0 * dt);
    const nodY = Math.sin(this.nodBoost * Math.PI) * 3.5;
    this.headShiftX += (normLookX * 4.5 - this.headShiftX) * headAlpha;
    this.headShiftY += (normLookY * 3.2 + nodY - this.headShiftY) * headAlpha;
    this.headRot += (normLookX * 3.4 - this.headRot) * headAlpha;

    if (this.headRig) {
      this.headRig.setAttribute(
        'transform',
        `translate(${this.headShiftX.toFixed(2)}, ${this.headShiftY.toFixed(2)}) rotate(${this.headRot.toFixed(2)} 190 178)`
      );
    }

    // Organic single/double blinking & happy click arcs
    if (this.happyTimer > 0) {
      this.happyTimer = Math.max(0, this.happyTimer - dt);
      this.eyesNormal?.setAttribute('opacity', '0');
      this.eyesHappy?.setAttribute('opacity', '1');
      this.eyesBlink?.setAttribute('transform', 'translate(190, 117) scale(1, 1) translate(-190, -117)');
    } else {
      this.eyesNormal?.setAttribute('opacity', '1');
      this.eyesHappy?.setAttribute('opacity', '0');

      this.blinkTimer -= dt;
      if (this.blinkTimer <= 0 && this.blinkPhase <= 0) {
        this.blinkPhase = 0.16;
        if (!this.doubleBlinkQueued && Math.random() < 0.28) {
          this.doubleBlinkQueued = true;
          this.blinkTimer = 0.24;
        } else {
          this.doubleBlinkQueued = false;
          this.blinkTimer = 2.5 + Math.random() * 3.4;
        }
      }

      let blinkScaleY = 1.0;
      if (this.blinkPhase > 0) {
        this.blinkPhase = Math.max(0, this.blinkPhase - dt);
        blinkScaleY = 1.0 - Math.sin((this.blinkPhase / 0.16) * Math.PI) * 0.94;
      }
      this.eyesBlink?.setAttribute(
        'transform',
        `translate(190, 117) scale(1, ${blinkScaleY.toFixed(3)}) translate(-190, -117)`
      );
    }
  }

  updateOrbitalSatellites(elapsed) {
    for (let i = 0; i < this.satRefs.length; i++) {
      const s = this.satRefs[i];
      const r = s.ring;
      const theta = elapsed * r.speed + r.phase;
      const sinT = Math.sin(theta);
      const cosT = Math.cos(theta);

      const x = r.rx * cosT;
      const y = r.ry * sinT;

      const dir = r.speed >= 0 ? -1 : 1;
      const t1 = theta + dir * 0.10;
      const t2 = theta + dir * 0.20;
      const t1dx = r.rx * Math.cos(t1) - x;
      const t1dy = r.ry * Math.sin(t1) - y;
      const t2dx = r.rx * Math.cos(t2) - x;
      const t2dy = r.ry * Math.sin(t2) - y;

      const depthScale = 0.86 + 0.28 * ((sinT + 1) * 0.5);
      const isFront = sinT >= 0;

      if (isFront) {
        if (s.front) {
          s.front.setAttribute('transform', `translate(${x.toFixed(1)}, ${y.toFixed(1)}) scale(${depthScale.toFixed(3)})`);
          s.front.setAttribute('opacity', (0.72 + 0.28 * sinT).toFixed(2));
        }
        if (s.fT1) { s.fT1.setAttribute('cx', t1dx.toFixed(1)); s.fT1.setAttribute('cy', t1dy.toFixed(1)); }
        if (s.fT2) { s.fT2.setAttribute('cx', t2dx.toFixed(1)); s.fT2.setAttribute('cy', t2dy.toFixed(1)); }
        if (s.back) s.back.setAttribute('opacity', '0');
      } else {
        if (s.back) {
          s.back.setAttribute('transform', `translate(${x.toFixed(1)}, ${y.toFixed(1)}) scale(${depthScale.toFixed(3)})`);
          s.back.setAttribute('opacity', (0.62 + 0.28 * sinT).toFixed(2));
        }
        if (s.bT1) { s.bT1.setAttribute('cx', t1dx.toFixed(1)); s.bT1.setAttribute('cy', t1dy.toFixed(1)); }
        if (s.bT2) { s.bT2.setAttribute('cx', t2dx.toFixed(1)); s.bT2.setAttribute('cy', t2dy.toFixed(1)); }
        if (s.front) s.front.setAttribute('opacity', '0');
      }
    }
  }
}
