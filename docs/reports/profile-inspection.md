# FR-05 — Profile, settings and saved addresses

Issue [#54](https://github.com/Hooman1996/RagBot-Assistant/issues/54), following FR-04c #52. This is a local frontend batch. No backend, database, assistant, bridge, original-app execution or banking network access was added.

## Implemented content and identity

**Profile tab.** The Home **پروفایل** tab is hosted content of `S-026`, traced from the Home tab dispatcher (PF-TAB); it has no graph ID of its own. It shows a profile header (avatar, name, customer number, the first accounts "و..."), the configured profile menu, the last-login line and the app version. The menu rows and their order are server menus in the original, so the replica's row list is **Designed for mock**. Rows for limits and digital signature belong to other batches and explain that they are unavailable.

**Rows and pages, as routed by the recovered profile router (PF-ROUTES):**

- **Edit profile `S-065`:** name, customer number and a profile photo (camera/gallery are unavailable). Expandable sections change the login password, username, mobile number or email. Each section uses its recovered rule; a valid change asks for the OTP (fixed local code `12345`) and ends in the FR-04 labelled result, which states that nothing changed.
- **Additional information `S-064`:**
  - Fields: English first, last and father's names; birth-certificate issue date; birth province and city; marital status; education; job; income band; optional tax code.
  - Saving keeps the values in local profile state and shows the recovered success text, marked local-only.
  - The original form also has a country step and an 18+ age rule that the replica does not enforce.
- **Account management `S-063` and card management `S-126`:** these open the existing FR-02 and FR-03 pages, so the FR-03 demo review button for `S-126` is removed.
- **Saved addresses `S-464`:** list and delete (with the recovered confirmation), plus **افزودن آدرس** → add address `S-463`. The add form has a title, province and city pickers, a 10-digit postal code and the address. Addresses are kept in local state and shared with the FR-04a/FR-04c address steps, whose add-address buttons now open `S-463`.
- **App settings menu `S-025`:** this previously unassigned node is now proven to be the profile app-settings menu. Its catalogue name and family are corrected; the ID is unchanged.
  - Biometric login `S-070`: enrollment is a platform feature, so it is unavailable.
  - Financial services `S-072` and their biometric-login variant `S-073` (storefront): each switch change asks for the login password. The password is not checked or stored, and the switch changes in local state only.
  - Card display status `S-130`: opens the FR-03 page, so its demo review button is removed.
- **Invitation `S-069`:** guide text with a copyable synthetic code. Sharing and invitation history (`S-013`, FR-45) are unavailable. Brand names in the original guide text are replaced with neutral wording.
- **Changelog `S-074`:** a local version list.
- **Notification settings `S-071`:** the opening caller isn't statically recoverable, so it keeps a labelled review entry on the demo page. The switches change locally, and turning one off asks first, as recovered.

## Data and state

All values are synthetic. Profile information, saved addresses and switch states are kept in the app's saved UI state, survive Back and recreation, and are never sent. Passwords, OTP codes and login passwords are never saved.

## Private evidence and assets

PF-TAB, PF-ROUTES, PF-PAGES and PF-RULES refer to focused static findings under `local/evidence/profile/`. No new private assets were added.

## Original-app fidelity

Original UI fidelity is **unscored**. Known gaps:

- The profile header artwork and selected-tab icon, the original picker sheets and the address lookup from postal code are not replicated.
- The "complete information" form's country step and age rule are not enforced.
- Server-driven menu sets, the weak-password acceptance checkbox and notification permission prompts are not modelled.

## Acceptance

Baseline on main `1d12c60`: repository checks 131/131, pytest 112, Ruff pass, lint 0 errors/107 warnings, and Android 15 JVM / 31/31 device tests.

Final results are in the PR. Android: 17 JVM tests and 36/36 server device tests (0 skipped). The first run failed because a test edit didn't apply (registry list); it is kept privately.

Rendered review on emulator-5554 covers the Profile tab, edit profile, additional information, the address list and add form, the settings menu and financial services. There are no replica crash records. Captures are private under `local/evidence/profile/qa/`.

## Opening the batch and stopping point

Open **RagBot Replica → ورود به خانه → پروفایل**. The demo page's **تنظیمات اعلان‌ها** opens notification settings.

Only FR-05 is implemented. [FR-06, notifications and support inbox](../plan/frontend-replication-steps.md#fr-06--notifications-and-support-inbox) is the recommended next approval. It remains proposed.
