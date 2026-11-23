# NegahOrbit — Offline-Ready Banking AI Platform

Production modular codebase with **zero external CDN dependencies** (no Tailwind Play CDN, no remote Three.js CDN, no external fonts required). Built to run smoothly on air-gapped banking networks.

## Key Features
- **Crisp Interactive NegahOrbit Companion (`src/ui/agentMascot.js`)**: High-craft puffer-vest & headphones NegahOrbit mascot with theme-reactive colors, wide-range spring-physics LED dot-matrix eyes, grounded body with subtle independent head turn, organic blinking, happy click bounce, chest emblem pulse, and ASR headphone waves—with zero `getBoundingClientRect()` layout reads.
- **Undistorted Layered Hero Orbital Rings**: Three concentric tilted vector orbital rings with true front/back depth layering around the character (back arcs pass behind, front arcs sweep cleanly in front) and glowing satellites with trailing tails that fade away cleanly as the character docks lower-left.
- **Consistent Left Docking & Service Gaze**: Full-scale in the Hero (`00`), then smoothly docks lower-left across Chapters `01–04` while watching the active 3D service sculpture on the right.
- **100% GPU-Driven 14,000-Particle Engine (`src/scene/particleSystem.js`, `src/scene/serviceShapes.js`)**: All 8 domain-accurate service sculptures (`speech-bubbles`, `ecosystem-globe`, `crm-constellation`, `rag-documents`, `microphone-wave`, `speaker-synthesis`, `agentic-card-orbit`, `ocr-scanner`) morph and animate entirely in GLSL shaders on the GPU.
- **All 4 Luxury Themes**: Obsidian & Champagne, Slate & Platinum, Espresso & Copper, and Architectural Stone (light mode), switchable in the navbar or via `T`.
- **External `services.json` Registry**: Add full URLs, bare `IP:port` addresses (`192.168.10.25:8001`), or reverse-proxy paths (`/hibot`).

## Running Offline Locally
```bash
cd negahorbit-platform
python3 -m http.server 3000
```
Open **`http://localhost:3000`**.

## On-Premise Docker Deployment
```bash
docker build -t negahorbit-platform .
docker run -d -p 8080:80 \
  -v $(pwd)/services.json:/usr/share/nginx/html/services.json:ro \
  negahorbit-platform
```
