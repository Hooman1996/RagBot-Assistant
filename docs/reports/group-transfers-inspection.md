# FR-09 — Group transfer preparation and history

Approved issue [#62](https://github.com/Hooman1996/RagBot-Assistant/issues/62).
Owner approval covers FR-09 only. Next proposed batch is FR-10; it does not start
automatically. The [requirements matrix](group-transfers-requirements.md) records
the nodes, incoming context, file schema, limits, validation, fixtures, candidate
models, future services and confidence separately from uncertain runtime behavior.

## Baseline and recovery

Main was `9e7e690`, with a clean checkout. Baseline passed 131 repository checks,
112 Python tests, both Ruff checks, Android build, 31 JVM tests, 65 device tests
(0 skipped) and lint (0 errors / 117 warnings), matching FR-08's record. Baseline
output and reports remain under `local/evidence/group-transfers/`.

GT-RECOVERY reused the private P1a metadata decode and decoded resources, then
decompiled only the group-transfer classes offline. It identifies screen classes,
call sites, the spreadsheet row mapper, row validity, record limits, accept checks,
waiting modes and resume mapping, and resource strings. Static recovery never ran
the original app or contacted production. No original assets, code findings, raw
output or captures enter GitHub.

One allocated node is not a group page: S-183's own content, label and caller are a
wage-advance request receipt. Its catalogue/graph name and label are corrected (ID,
area and FR-09 allocation unchanged pending owner approval to move it), and it is
not registered. The catalogue's label totals are updated accordingly.

## Navigation and preview boundary

The recovered menu registry key for group transfer starts S-187. The replica adds it
as a third synthetic child of FR-08's S-193 transfer menu; menu membership is a mock.
S-187 opens S-184 manual entry or S-186 file entry; S-186 opens S-182. Both lists
apply the recovered accept checks (valid rows, more than one destination, total
conflict) before S-190 waiting. Waiting, validation and errors replace themselves
when they hand over, as the recovered pages finish, and Back asks whether to cancel
the process. Cancel returns to S-187 with its inputs kept and its list cleared.
S-188 continues to S-185 when the synthetic sample reports rejected rows, otherwise
to second-mode waiting and S-189 verify. Verify's note/attachment, OTP and accept are
unavailable; no handler submits, signs or authorizes a transfer, and there is no
route from verify to the receipt. S-096 is an independent labelled sample.

The original waiting page polls a status service with a minimum loader period. The
preview's **به‌روزرسانی** is a deterministic local exit and states so on the page.
S-089 history (started by a separate transfer-trace menu the replica does not have)
and S-096 use labelled independent demo entries; S-089 opens S-087 destinations and
S-088 detail as recovered.

File import is deterministic: five built-in synthetic sheets go through the same
parser that mirrors the recovered column mapping. No device or desktop file is read,
no picker or permission exists, and template/guide downloads are unavailable.
Non-secret drafts, rows, chosen sample, waiting mode and searches survive Back and
recreation. No network permission, Intent, authentication or bank service is added.

## Opening the screens

From the demo page, scroll to **بازبینی مستقل انتقال‌ها — فقط پیش‌نمایش محلی**.

| Entry | What opens |
|---|---|
| **منوی انتقال — نمونهٔ مستقل** → **انتقال وجه گروهی** | S-187; choose a source, enter a total in rial, choose manual or file entry |
| Manual entry → **ادامه** | S-184; synthetic IBANs without IR such as `920000000000000000000002` and `650000000000000000000003`, positive rial amounts, synthetic reason |
| File entry → **ادامه** → **انتخاب فایل اکسل** | S-186; built-in synthetic sheets: valid, invalid rows, registration-rejected, empty, 501 rows |
| Valid sheet → **ادامه** | S-182; invalid-only filter, edit, delete, remove invalid records |
| List **ادامه** | S-190 → **به‌روزرسانی** → S-188 → (S-185 for the rejected sample) → S-190 → S-189 |
| **سوابق تراکنش گروهی — نمونه** | S-089 → **جزئیات مقصدها** → S-087 → row → S-088 |
| **رسید مستقل انتقال گروهی — نمونه** | S-096 |

Scenario controls remain on the demo page. The existing loopback-only viewer and SSH
forwarding remain in the [runbook](../runbook/replica-development.md#desktop-viewer-and-stopping-the-simulation).

## Validation and remaining fidelity gaps

Recovered rules: spreadsheet first sheet, header skipped, blank rows skipped, IBAN
mod-97, amount ≥ 1, reason required, 1–500 file rows, 30 manual rows, more than one
destination and the total conflict. Replica deviations are explicit: a reason is
required on every file row (the own-bank exemption is not modelled), the manual
payment identifier stays optional because the service decides when it is needed,
and non-text cells are not modelled. Amounts are exact Long rial integers with
overflow-checked totals. No fee, limit, balance, duplicate rule or byte-size limit is
invented; those remain service-side Unknowns. M-001 and the E-150/E-154/E-163/E-164
inventory discrepancies are recorded without rewriting the inventory.

Search on file and validation lists, edit/delete on validation, in-progress resume,
balance, familiar destinations, downloads and pagination are not reproduced. Sheet
layouts are inline dialogs. All data and generic scenario states are synthetic. The
existing private 84-resource overlay and public substitute disclosure are reused; no
new asset extraction was needed. Original-app visual fidelity remains unscored.
Earlier batch fidelity and service gaps are unchanged.

## Verification and delivery

Final server acceptance passed: **131 repository checks, 112 Python tests, Ruff
check/format, Android assemble, 36 JVM tests and 70 device tests**, with zero
failures, errors or skips. Lint is **0 errors / 117 warnings**, unchanged from
baseline; comparison found no new or removed diagnostics. The complete suite keeps
earlier Home/Accounts/Cards/Profile/support/saved-destination/transfer coverage.
New checks cover the exact registry and the unregistered S-183, header/blank-row
parsing and column order, 1–500 row limits, invalid rows, exact totals and overflow,
the single-destination warning, total conflict, row correction and removal,
registration errors, guarded Back, cancel with retained first-page inputs,
recreation on lists/waiting/history, unavailable verify/attachment/download/share
actions and real keyboard clearance.

GT-QA includes replica PNG/XML captures under `local/evidence/group-transfers/qa/`:
demo entries, transfer menu, first page with and without the keyboard, manual rows
with the keyboard, reason dialog, single-destination warning, total conflict,
waiting, cancel dialog, validation, verify and its details/unavailable notice, the
cancelled first page with kept inputs, synthetic file dialog, 501-row error, file
list with row errors, invalid-row removal, history, destinations, destination detail
and receipt. Persian/RTL, LTR identifiers, dialogs, scrolling and system bars were
inspected on the pinned API 35 fa-IR 1080×2400/density-420/font-1 emulator with the
real keyboard. Controls stay reachable above the keyboard; no overlap or bar
obstruction was found and the crash buffer was empty. No warnings were suppressed
or assertions removed. Public CI uses substitutes. After green CI and merge, clean main is rebuilt
and reinstalled, and the displayed revision and installed APK hash are checked.
Stop before FR-10.
