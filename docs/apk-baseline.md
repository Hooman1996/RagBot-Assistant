# APK Baseline (P0-03)

The identity of the original APK, recorded before any decompilation. The APK itself stays in
`local/evidence/apk/` on the server and never enters git. Files are identified by hash, not by
name. `python tools/project_checks.py apk-baseline` checks the local files against the table below.

Recorded 27 October 2026 with `sha256sum`, Python `zipfile` (entry names only) and `openssl`.

## Files

| Role | Bytes | SHA-256 |
|---|---|---|
| `base` | 81430911 | `9230c7b7f6cf2084a157af8e5655a5bebbf03ad71d5ec42328c0432da1effded` |

## Identity

| Field | Value | Label |
|---|---|---|
| Split files | None. `local/evidence/apk/` holds this one APK | Observed |
| Complete without splits | Yes. It carries `resources.arsc`, 11 dex files and native libraries, and its manifest's string pool has no split attributes (`isSplitRequired`, `requiredSplitTypes`, `splitTypes`) | Inferred |
| ABIs | `arm64-v8a`, `armeabi-v7a` (folders under `lib/`) | Observed |
| Version name | 5.1.0, from the file name only | Inferred |
| Package name | Not read | Unknown |
| Version code | Not read | Unknown |
| Minimum SDK | Not read | Unknown |
| Target SDK | Not read | Unknown |
| Signing schemes | v1 (`META-INF/CERT.SF`, `CERT.RSA`) and an APK Signing Block (v2 or later). Which later schemes, not read | Observed / Unknown |
| v1 signing certificate SHA-256 | `90b84a91e863603e9d22f6cb794ff9eb731e586fc8847865e33f0981ae3ff2f8` (one certificate) | Observed |

## Missing tools

The manifest is binary XML. Reading the package name, version code, minimum and target SDK needs
`aapt2` or `apkanalyzer` (Android SDK build-tools or command-line tools). Reading the v2 and later
signing schemes needs `apksigner` (build-tools, which needs a JDK). None of these are on the server and
`local/toolchain/` is empty. Fill these rows once those tools are installed.
