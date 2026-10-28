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
| Version name | 5.1.0, recorded in `local/evidence/findings/identity.md` (P1a-01 read it from the manifest, confirming the file name) | Observed |
| Package name | recorded in `local/evidence/findings/identity.md` | Observed |
| Version code | 510 | Observed |
| Minimum SDK | 24 | Observed |
| Target SDK | 37 | Observed |
| Signing schemes | v1, v2 and v3 present; v3.1 absent; one signer (read with `inspect_apk_signature`, P1a-01) | Observed |
| v1 signing certificate SHA-256 | `90b84a91e863603e9d22f6cb794ff9eb731e586fc8847865e33f0981ae3ff2f8` (one certificate) | Observed |

## How the manifest rows were read

The rows above were filled in P1a-01 (28 Oct 2026) with the REA MCP server: apktool 2.12.1
decoded the binary manifest (package, version, min/target SDK), and `inspect_apk_signature` read
the signing schemes. See `docs/reports/P1a-01-toolkit.md` for the toolkit and recoverability
findings, and `local/evidence/findings/identity.md` for the private values.
