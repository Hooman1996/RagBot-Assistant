# Local Fonts (100% Offline Ready)

NegahOrbit works completely offline with zero external CDN requests using curated local system font stacks (`DIN Condensed / Impact / Arial Narrow` for tall condensed display headings, `Inter / Segoe UI / SF Pro` for UI, and `JetBrains Mono / SF Mono / Consolas` for telemetry).

If you want to bundle custom `.woff2` files (e.g., Oswald, Space Grotesk, JetBrains Mono) into an air-gapped deployment, place them in this folder and uncomment the `@font-face` rules at the top of `src/styles/tokens.css`.
