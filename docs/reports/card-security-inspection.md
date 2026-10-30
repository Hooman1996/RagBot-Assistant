# FR-04b — Card blocking, PIN and second-password activation

Issue [#50](https://github.com/Hooman1996/RagBot-Assistant/issues/50), following FR-04a #48. This is a local frontend batch. No backend, database, assistant, bridge, original-app execution or banking network access was added.

## Scope decisions

The owner approved the next batch after FR-04a. The FR-04 decisions recorded on #48 apply: valid confirms end in a local result labelled **Designed for mock** (nothing is blocked, assigned or activated), and any OTP is a fixed local code (`12345`, shown as a hint in the OTP sheet).

## Implemented flows

- `S-121` signed-in block: **مسدودسازی کارت** on any card group opens a list of that group's cards, as the recovered route passes the tab's list. Tapping a card shows the recovered warning and question; **تایید** shows the result. The recovered flow has no OTP. The original warning names the bank; the replica says "the bank's branches".
- `S-135` PIN: **رمز کارت** (debit) opens a card picker and four expandable sections: assign first PIN, assign second PIN, change first PIN, change second PIN. Recovered rules: first PIN 4 digits, second PIN 5–12 digits, retype must match, all fields required. Valid input shows a confirm sheet, then the OTP sheet, then the result.
- `S-136` dynamic second-PIN activation: **فعال‌سازی رمز پویا** (debit) opens a card picker and the recovered notice; **ارسال کد** opens the OTP sheet, then the result.
- `S-118` / `S-122` pre-login block: the original reaches these from the pre-login menu (the legacy form, or the v2 form when a menu flag is set). They have labelled review entries on the demo page. Fields are card number, CVV2, expiry and the second password, with a **دریافت رمز** dynamic-password request that explains it is unavailable. Valid input shows the result.

A wrong OTP shows an error and stays on the sheet. Cancelling the OTP or confirm sheet returns to the form with no result. **متوجه شدم** on the result returns to the card tab.

## Secrets and state

PIN, OTP, CVV2 and second-password values are kept only in non-saved composition state. They are never sent, logged, stored or restored after recreation; a test checks that a typed PIN is gone after recreation. Card choice, open section, flow stage and non-secret form fields are saved.

## Private evidence and assets

CS-ROUTES, CS-PAGES, CS-PIN and CS-OTP refer to focused static findings under `local/evidence/card-security/`. No new private assets were added.

## Original-app fidelity

Original UI fidelity is **unscored**. Known gaps:

- The OTP sheet's countdown timer and resend wait, the SMS auto-fill and the dynamic-password SMS request are not replicated.
- The original expiry picker sheet is replaced by month/year text.
- The pre-login forms reuse the FR-03 field checks (16-digit card number, CVV2, MM/YY expiry, second password of at least 5 digits). The exact original rules for these forms are only partly recovered (required plus a general validity error).
- Failure results and the original's per-operation success texts beyond "operation succeeded" and "card blocked" are not shown.
- S-118 and S-122 share one form in the replica, although the original v2 form is a separate Compose screen.

## Acceptance

Baseline on main `5dfa636`: repository checks 131/131, pytest 112, Ruff pass, lint 0 errors/107 warnings, and Android 12 JVM / 24/24 device tests.

The Android baseline needed two retries, both kept privately. On the first run the emulator was not running; it was restarted with the pinned restricted-network profile. The first run after that boot reported an instrumentation process crash in an FR-02 test with no crash record; it did not reproduce on the next, unchanged run.

Final results are in the PR. Android: 14 JVM tests and 28/28 server device tests (0 skipped). Two earlier device runs failed on test issues: an FR-03 fixture assertion that no longer held, and ambiguous text lookups where a rule hint and an error share the same text. Both runs are kept privately.

Rendered review on emulator-5554 covers the block list, warning and result, PIN sections and errors, the OTP sheet, and the pre-login form errors. It found and fixed a card number reordered inside the RTL result sentence. There are no replica crash records. Captures are private under `local/evidence/card-security/qa/`.

During this batch a second assistant session also began working in the same checkout. It made no source edits, renamed one baseline log, then handed the batch over and stopped.

## Opening the batch and stopping point

Open **RagBot Replica → ورود به خانه → کارت** and use **مسدودسازی کارت**, **رمز کارت** or **فعال‌سازی رمز پویا**. The demo page's **مسدودی بدون ورود** buttons open the pre-login forms. The OTP code is `12345`.

This report records FR-04b as delivered. FR-04c, [wallet-linked card journeys](wallet-card-inspection.md), followed in #52.
