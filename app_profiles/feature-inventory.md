# Whole-app feature and entry-point inventory (P1a-03)

A static, whole-app list of every declared entry point, feature area and navigation surface in
the catalogued banking app, from the decompiled code and decoded manifest. It does **not** run
the app, trace any journey, recover any API, or draw the screen catalogue / navigation graph
(that is P1a-04).

- **Method:** apktool manifest/resource decode + JADX class/metadata analysis (REA MCP server) of
  the P1a-01 APK. No execution, no network, no string decryption.
- **Sanitization:** semantic names and evidence IDs only (see `README.md`). Real class/manifest
  locations and brand names are in `local/evidence/findings/inventory-private.md` (not committed).
- **Labels:** `Recovered` (precise code/manifest location), `Inferred` (reasoned guess),
  `Unknown` (server-driven / obfuscated / absent, with reason). Nothing is `Observed` — the app
  was not run; a declared component or route is not proof of a reachable path.

## Summary of what the manifest declares

- **274 activities** declared. **1** is the app-owned exported entry point (the launcher, F-001).
  The great majority (~266, resolved to screens in P1a-04) are **app-owned screens** — they extend
  the app's own base activity, and the obfuscator only renamed them; it did not turn them into
  library classes. Only a handful (~8) are third-party SDK or tooling activities. (An earlier draft
  of this line miscounted the ~233 obfuscated app activities as repackaged UI-library classes.)
- **10 services, 4 receivers, 7 providers** — all in library/SDK packages (push, analytics,
  crash reporting, file sharing, startup, baseline profile).
- **8 intent-filters total.** `<data>` elements: **0**. URL schemes / hosts: **0**. App
  shortcuts: **0**. So the app declares **no deep links, custom schemes or shortcuts** — it is
  entered through the launcher and then navigates internally.

## 1. Entry points and exported component surface

| ID | Name | Type | Label | Basis |
|---|---|---|---|---|
| F-001 | App launcher | entry point | Recovered | The one exported app activity; `MAIN`/`LAUNCHER` intent-filter; splash screen. |
| F-002 | Internal router | entry point | Recovered | Non-exported dispatcher activity referenced from the launcher/login; routes to login vs. home. |
| F-003 | Push-message service | entry point | Recovered | Non-exported messaging service with a cloud-messaging-event filter (app's push handler, repackaged). |
| F-004 | Push-registration receiver | entry point | Recovered | Exported, permission-gated cloud-messaging receiver (push SDK). |
| F-005 | Sign-in revocation service | entry point | Recovered | Exported, permission-gated service from the Google Sign-In SDK. |
| F-006 | UI-preview tooling activity | entry point | Recovered | Exported Compose preview/tooling activity; dev tooling, not an app flow. |
| F-007 | File-sharing provider | entry point | Recovered | Non-exported file provider for sharing files with other apps. |
| F-008 | Baseline-profile receiver | entry point | Recovered | Exported, permission-gated profile-installer receiver (4 maintenance-action filters). |
| F-009 | Crash/performance init providers | entry point | Recovered | SDK auto-init content providers for crash/performance reporting. |
| F-010 | Analytics/measurement components | entry point | Recovered | Google measurement/data-transport services, receiver and discovery service. |
| F-011 | No deep links / custom schemes | entry point | Recovered | Manifest has 0 `<data>`, 0 schemes, 0 hosts. (Any runtime/branch-link handling is not covered here → would be Unknown.) |
| F-012 | No app shortcuts | entry point | Recovered | No shortcuts resource and no shortcuts meta-data. In-app quick actions exist in code (F-027). |
| F-013 | Runtime-registered receivers | entry point | Inferred | A small app receiver package exists but is not manifest-declared; receivers are registered dynamically. |

## 2. Feature areas

Top-level functional groups recovered from the preserved package/class structure (the app's own
class names survive obfuscation; the "Scale" column is the class count, a rough size signal).
Each is **Recovered** unless noted. A feature area existing in code is **not** proof it is
reachable or enabled for a given user (see F-066).

### App access and shell

| ID | Feature | Label | Basis |
|---|---|---|---|
| F-014 | Home / dashboard | Recovered | Main activity hosting the dashboard, accounts, cards and profile tabs; large (~50 classes). |
| F-015 | Login | Recovered | Dedicated login package (~42). |
| F-016 | Registration / sign-up | Recovered | Current + legacy registration packages, with nav graphs (~109). |
| F-017 | Intro / welcome | Recovered | Intro and welcome packages (~9). |
| F-018 | Device/app activation | Recovered | Activation package (~6). |
| F-019 | Identity verification (eKYC) | Recovered | eKYC auth + custom-camera packages (~11). |
| F-020 | In-app account opening | Recovered | Two account-opening packages with nav graphs (~63). |
| F-021 | User profile | Recovered | Profile package (~52). |
| F-022 | Settings | Recovered | Settings + per-service settings packages (~15). |
| F-023 | Dashboard storefront settings | Inferred | A "service storefront" settings package; purpose read from class names (~5). |
| F-024 | Changelog / what's new | Recovered | Changelog package (~4). |
| F-025 | Notifications / inbox | Recovered | Notifications package with detail/delete bottom sheets (~6). |
| F-026 | Support / tickets | Recovered | Support package incl. ticket viewing (~41). |
| F-027 | Quick-action shortcuts hub | Recovered | In-app shortcut home package with its own nav host (~4). |

### Accounts, cards and cheques

| ID | Feature | Label | Basis |
|---|---|---|---|
| F-028 | Accounts | Recovered | Account package (~3). |
| F-029 | Account statement | Recovered | Statement package (~6). |
| F-030 | Transactions history | Recovered | Transaction package (~32). |
| F-031 | Receipts | Recovered | Receipt package (bill/charge/transfer receipt activities) (~41). |
| F-032 | Wallet | Recovered | Wallet package (~83). |
| F-033 | Bank cards | Recovered | Card package (~78). |
| F-034 | Wallet-linked card | Recovered | Wallet-card issue/manage package + nav host (~52). |
| F-035 | Cheques | Recovered | Cheque package with a cheque menu (~48). |
| F-036 | Chequebook management | Recovered | Chequebook issue-status package (~11). |
| F-037 | National digital-cheque integration | Recovered | Digital-cheque "cartable" list/detail package (~11). |
| F-038 | Cheque acceptance / payment | Recovered | Pay-cheque package (accept, add-agent, issue) (~56). |

### Payments and transfers

| ID | Feature | Label | Basis |
|---|---|---|---|
| F-039 | Money transfer | Recovered | Large transfer package (card-to-card, interbank, continuous) + transfer-type menu (~125). |
| F-040 | Saved/trusted destinations | Recovered | Familiar-destination package + nav host (~25). |
| F-041 | Recurring transfers | Recovered | Repetitive-transactions package + nav host (~24). |
| F-042 | IBAN / account-number conversion | Recovered | SHEBA-conversion package (~49). |
| F-043 | Bill payment | Recovered | Bill package (~29). |
| F-044 | Mobile top-up | Recovered | Charge package (~35). |
| F-045 | Payment gateway / balance top-up | Recovered | Payment-gateway package (increase account/wallet balance, inflow/standalone) + 4 nav graphs (~135). |
| F-046 | Road-toll payment | Recovered | Toll-payment package + a large nav graph (~96). |
| F-047 | Charitable donations | Recovered | Charity package (~17). |

### Credit, investment and services

| ID | Feature | Label | Basis |
|---|---|---|---|
| F-048 | Loans / credit facilities | Recovered | Largest feature area (loan menu, marriage/child/other loans, "hi-loan") + 4 nav graphs (~489). |
| F-049 | Promissory notes | Recovered | Promissory package (issue, guarantee, mine) + nav host (~265). |
| F-050 | Guarantees / sureties | Recovered | Guarantee package with a menu state (~4). |
| F-051 | Digital signature | Recovered | Digital-signature package (issue, menu, list) + 3 nav graphs (~60). |
| F-052 | Brokerage / securities | Recovered | Broker package + nav host (~58). |
| F-053 | Crowdfunding | Recovered | Crowdfunding package (plans, tabs) + nav host (~64). |
| F-054 | Insurance | Recovered | Insurance package with an insurance menu (~18). |
| F-055 | Branded credit / BNPL product | Inferred | A branded-product package; it is a credit/wallet product, exact scope inferred from class names (~36). |
| F-056 | Wage-advance / earned-income service | Inferred | Large branded package (request, settlement, activation) + nav host; exact product inferred (~188). |
| F-057 | Loyalty / rewards club | Recovered | Branded loyalty-club package + nav host (~108). |
| F-058 | Personal finance management | Recovered | PFM package + nav host (~73). |
| F-059 | Inquiries / status checks | Recovered | Inquiries package (incl. a guaranty inquiry) + 2 nav graphs (~65). |
| F-060 | Referral / advocacy program | Recovered | Advocacy package with its own menu (~29). |
| F-061 | Agent / proxy management | Recovered | Agent package (~4). |
| F-062 | Saved addresses | Recovered | Address-management package (~10). |
| F-063 | Postal correspondence | Recovered | Postal-letter package + nav host (~16). |
| F-064 | Merchant / market service | Inferred | Large branded package (incl. installation information) + nav host; exact service inferred (~84). |

## 3. Navigation surfaces

| ID | Surface | Label | Basis |
|---|---|---|---|
| F-065 | Home bottom navigation | Recovered | Custom bottom bar on the home activity: 4 tabs (dashboard, accounts, cards, profile) + a central QR "fast payment" action. Built in code, not a standard navigation-bar widget. |
| F-066 | Server-driven dashboard menu grid | Inferred | The dashboard's feature tiles are populated from a server "menu" response type (referenced ~323×) via a menu helper and grid-item model. The structure is recovered; **which tiles appear is server-driven → their set is Unknown** without running against a backend. |
| F-067 | In-app Compose navigation graphs | Recovered | The Compose navigation host is instantiated 31× outside the library; destination builders (`composable`/`dialog`/`navigation`) are used 242×. Navigation is in-app Compose, consistent with P1a-01. |
| F-068 | App-named route destinations | Recovered | 136 destinations whose route type resolves to a readable app route class (e.g. per-feature "…Route" classes), spread across ~27 feature navigation packages. |
| F-069 | Obfuscated / computed route destinations | Unknown | 60 destinations whose route type resolves to a repackaged `o.*` class, 12 destinations keyed on string routes built from encrypted/computed values, and 11 nested sub-graphs keyed on `o.*`. Not nameable from static code. |
| F-070 | Nested sub-graphs & dialog destinations | Recovered | 22 nested navigation sub-graph builders and 4 dialog destinations in the Compose graphs; plus 2 small legacy XML navigation graphs (2 fragment destinations each). |
| F-071 | No drawer / tab bar / system nav widgets | Recovered | No navigation-drawer, tab-row, navigation-rail or View-system bottom-navigation/tab/drawer widgets are referenced outside libraries. A horizontal pager appears in 5 in-flow screens only. |

## Counts

- Entry-point / component-surface rows: **13** (12 Recovered, 1 Inferred). App-owned user entry
  points: **1** launcher; deep-link/scheme entry points: **0**; app shortcuts: **0**.
- Feature areas: **51** (47 Recovered, 4 Inferred).
- Navigation-surface rows: **7** (5 Recovered, 1 Inferred, 1 Unknown).
- Destinations behind these surfaces: **274** declared activities and **31** Compose navigation
  hosts carrying **~214** composable destinations, 22 nested sub-graphs and 4 dialogs
  (summarized per feature area and in §3, not listed leaf-by-leaf — that is P1a-04).

**Totals by label (71 rows):** Recovered **64**, Inferred **6**, Unknown **1**, Observed **0**.

## Biggest `Unknown` / `Inferred` areas and why

- **Dashboard menu set (F-066, Inferred→contents Unknown):** the home feature tiles come from a
  server "menu" response, so the catalogue the user actually sees is backend-driven and cannot be
  fixed statically.
- **Obfuscated/computed routes (F-069, Unknown):** ~60 route types are repackaged into the flat
  `o.*` obfuscator package and ~12 routes are built from encrypted/computed strings (string
  literals are encrypted, per P1a-01), so those destinations can't be named without runtime
  observation or deobfuscation.
- **Branded products (F-055, F-056, F-064, Inferred):** three large packages carry
  bank-specific product names; the feature exists and is sized, but the exact product scope is a
  reasoned guess from class names, to be confirmed in P1a-04 / P1d.

## Stop conditions

None hit. The P1a-01 decode was reusable, the APK hash still matched the record, and no item
required running the app, a network call or string decryption; blocked items are recorded above
as `Unknown`/`Inferred` with their reason.
