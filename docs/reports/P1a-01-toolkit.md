# P1a-01 — UI toolkit and what REA can recover

First reverse-engineering step on our own app. It identifies how the APK is built and how much
of it REA can recover, so the later whole-app catalogue (P1a-03), navigation graph (P1a-04) and
API map know what they are working with. It does **not** catalogue features or draw the graph.

- **Date:** 28 Oct 2026.
- **Tools:** REA MCP server — JADX headless 0.7.1 (static class/metadata analysis) and apktool
  2.12.1 (resource/manifest decode), plus `inspect_apk_signature` and the project JDK's `keytool`.
  No device, no emulator, no network, no app execution.
- **Artifact:** the P0-03 APK (SHA-256 `9230c7b7…1effded`); its hash still matches the baseline.
- **Evidence kept out of git:** raw REA output in `local/evidence/rea/`, the apktool decode in
  `local/evidence/apk-decoded/`, and all identifying values (package name, app label, signing
  subject, any host/URL/credential) in `local/evidence/findings/identity.md`.

Evidence labels: `Observed` (read directly from the artifact), `Recovered` (reconstructed by a
tool), `Inferred` (concluded from several observations), `Unknown`.

## UI toolkit — Jetpack Compose (Observed)

- 32 `META-INF/androidx.compose.*_*.version` marker files are present (runtime, foundation,
  material3, ui, animation, navigation-compose, activity-compose, hilt-navigation-compose), and
  `androidx.compose.*` is by far the largest code area: 22,679 of 30,183 `androidx` classes.
  **[Observed]**
- No `res/layout/` directory exists in the package (0 traditional XML layout resources).
  **[Observed]** Combined with the Compose runtime above, the UI is built in Compose, not XML
  Views. **[Inferred]**
- Some legacy AndroidX View libraries are still bundled (`appcompat`, `material`, `fragment`,
  `recyclerview`, `constraintlayout`, `databinding`/`viewbinding` markers), typical of a
  Compose-first app that keeps a few View-based pieces. **[Observed]**
- Not Flutter, not React Native: no `libflutter.so`/`libapp.so`, no `io.flutter.*`, no
  `index.android.bundle`, no Hermes/JS bundle anywhere in the APK. **[Observed]**

## Languages and build

- **Kotlin** is the primary language: `kotlin/*.kotlin_builtins`, `DebugProbesKt.bin`,
  `kotlinx.coroutines` (930 classes), `kotlinx.serialization` (409), `kotlinx.parcelize`, and
  Compose itself is Kotlin-only. **[Observed]**
- **Java** is present as the platform/base layer, with core-library desugaring (a top-level `j$`
  package, 53 classes). **[Observed]**
- Build system: Gradle with **R8** (minify + obfuscation + repackaging; see recoverability),
  **Hilt/Dagger** dependency injection (`dagger.hilt`, 168 classes; `*_HiltModules`/
  `*_HiltComponents` generated classes), and a **baseline profile** (`assets/dexopt/baseline.prof`
  and `.profm`). **[Observed]**
- No cross-platform runtime (no Flutter/Dart, no React Native, no Ktor multiplatform). **[Observed]**

## Recoverability

Overall: **structure is highly recoverable; member-level logic and string literals are not.**

- **Dex:** 11 dex files (`classes.dex`, `classes2.dex` … `classes11.dex`); apktool produced 11
  matching `smali*` folders. **[Observed]**
- **Load/decompile health:** JADX loaded all 11 dex and reported `class_count` =
  `raw_class_count` = 51,880 with resources fully decoded and no load error; member parsing of
  sampled classes succeeded. apktool decoded resources and smali without error. So JADX and
  apktool handle this APK cleanly — no dropped classes, no decode failure. **[Observed]**
- **Obfuscation: yes (Observed), applied selectively:**
  - The app's **own class names are preserved and meaningful** (its Activities, ViewModels and
    data classes keep descriptive names), most likely kept by Hilt and R8 keep rules. This means
    the screen/feature structure is recoverable by class name for P1a-03/P1a-04. **[Observed]**
  - **Member names are dictionary-renamed:** methods and fields are renamed from a fixed pool of
    real-looking identifiers, and the *same* names recur across unrelated classes — so method and
    field names cannot be trusted. **[Observed]**
  - **Third-party code is repackaged flat** into a single-letter top-level package (`o`, 25,993
    classes, all at depth 1) with dictionary names, so library origins are obscured. **[Observed]**
  - **String literals are encrypted:** a synthetic `String` decrypt stub with `byte[]`/`char[]`
    tables appears in app classes, so endpoints, keys and labels are not readable as static
    strings. **[Observed]** Recovering them needs deobfuscation or runtime observation — out of
    scope here. **[Inferred]**
  - **No R8 mapping file ships in the APK** (expected; the mapping is a build-side artifact).
    **[Observed]**
  - The combination (dictionary renaming + package flattening + string encryption) is consistent
    with an advanced/commercial obfuscator rather than stock R8 defaults. **[Inferred]**
- **Native libraries:** 10 `.so` per ABI, for `arm64-v8a` and `armeabi-v7a`; all ELF AArch64/ARM,
  stripped, built with the Android NDK (C/C++). **[Observed]** Notable ones: `libtensorflowlite_jni.so`
  (TensorFlow Lite, with `.tflite` models under `res/raw/`), `libsentry.so`/`libsentry-android.so`
  (Sentry crash reporting), one app JNI library, and AndroidX native helpers
  (`libgraphics-core.so`, `libandroidx.graphics.path.so`, `libdatastore_shared_counter.so`).
  **[Observed]**
- **Embedded assets/config later recovery will need:** `resources.arsc`, the 11 dex, the baseline
  profile, the `.tflite` models, a Certificate-Transparency `log_list.json`, Firebase/Play-services
  `.properties`, and JSON handled by Gson / kotlinx.serialization. Apache POI + XmlBeans +
  OOXML schemas are bundled (document/spreadsheet generation). **[Observed]**

## Networking / API stack (Observed)

- **Retrofit2 + OkHttp3** (with Okio): `retrofit2.Retrofit`, `okhttp3.OkHttpClient` present, plus
  Retrofit converters for **Gson**, **kotlinx.serialization** and **scalars**. **[Observed]**
- A Certificate-Transparency library and a root `log_list.json` are present, indicating TLS
  certificate-transparency/pinning checks. **[Observed]**
- **No Ktor client and no gRPC** (`io.ktor.client`, `io.grpc.*` absent). **[Observed]** No app
  subclass of `android.webkit.WebView` was seen, though a static class search cannot by itself
  rule out WebView use. **[Unknown]**

## Top-level package layout (one level deep)

Generic third-party roots (the app's own package is recorded privately in the identity file):

| Root | Depth-2 highlights | Role |
|---|---|---|
| `androidx.*` | `compose` (dominant), `core`, `datastore`, `room`, `navigation`, `constraintlayout`, `appcompat`, `window` | Jetpack: Compose UI, DataStore, Room, Navigation |
| `kotlin.*`, `kotlinx.*` | `coroutines`, `serialization`, `parcelize` | Kotlin runtime, async, JSON, parcelable |
| `okhttp3.*`, `okio.*`, `retrofit2.*` | converters: gson, kotlinx.serialization, scalars | HTTP + REST client |
| `dagger.*` | `hilt` | Dependency injection |
| `coil3.*` | `compose`, `network` | Image loading |
| `io.sentry.*` | — | Crash/error reporting |
| `com.google.*` | Play services / Firebase, Material, Dagger | Google SDKs + Material |
| `com.*` (other) | Glide, Lottie, a CameraView library, a certificate-transparency library | Media, camera, TLS CT |
| `org.*` | `bouncycastle` (crypto), `apache`/`openxmlformats` (POI document export), `jsoup`, `osmdroid` (maps), `dom4j` | Crypto, document export, HTML, maps |
| `j$.*` | — | Core-library desugaring |
| `o.*` | flat, dictionary-named | R8-repackaged third-party code (obscured) |

This is a one-line pointer for the later catalogue/graph/API-map steps; it does not enumerate
features, endpoints or components (P1a-03, P1a-04).

## Manifest facts (safe to show)

| Field | Value | Label |
|---|---|---|
| `minSdkVersion` | 24 | Observed |
| `targetSdkVersion` | 37 | Observed |
| `versionCode` | 510 | Observed |
| Dex files | 11 | Observed |
| Obfuscated | Yes (dictionary member renaming, `o.*` repackaging, string encryption) | Observed |
| R8 mapping in APK | No | Observed |
| Native-lib toolkit | Android NDK (C/C++), ELF arm64-v8a + armeabi-v7a, stripped | Observed |
| Signing schemes | v1, v2, v3 present; v3.1 absent; one signer | Observed |

Package name, version name and the signing-certificate subject are recorded in
`local/evidence/findings/identity.md`.

## Stop conditions

None hit. REA opened the APK, the APK hash still matches the P0-03 record, and no fact required
running the app or a network call.
