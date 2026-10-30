# Group transfer requirements (FR-09)

Approved issue [#62](https://github.com/Hooman1996/RagBot-Assistant/issues/62).
F-039 group-transfer pages, F-030 group history and F-031 group receipt are frontend
previews only. Labels distinguish **Recovered** static evidence, **Designed for mock**
behavior and **Unknown** service/runtime behavior. GT-RECOVERY identifies the private
metadata, decompile, caller and resource ledger under `local/evidence/group-transfers/`.
The original application was never run and no banking service was contacted.

## Surfaces and caller contracts

| Existing node / purpose | Recovered incoming context and interactions | Current fixture / deliberate preview behavior | Future requirements; confidence / preliminary effort |
|---|---|---|---|
| S-187 group first page | Transfer/account menu registry keys for group transfer start it; later pages return to it on cancel. Source account picker, total amount (IRR), manual or file entry choice, optional withdrawal identifier and source description, Continue/Cancel. An in-progress group check offers continue or new request | `AccountFixture` sources; `GroupTransferDraft`; withdrawal ≤17 digits, description ≤50 (shared extra-info limits); positive total. Balance and in-progress check are service-backed and not shown | Source eligibility, balance, in-progress group lookup with its resume mode and delete-on-new. High identity/routes; 1 day |
| S-184 manual destination list | Opened by manual choice with the group info. Rows: IBAN without IR, amount, reason, optional payment identifier (≤30 digits) and destination description; add up to 30; delete; familiar destinations; accept | Per-row validation, add/delete, synthetic reason picker, live exact total; familiar destinations unavailable | Reason catalogue per destination, service-decided payment-identifier requirement, familiar destinations, pre-validation job. High rules; 1 day |
| S-186 file pick | Opened by file choice. Picks a spreadsheet, shows name/record count/error, clears it, downloads example and guide | Five built-in synthetic sheets (valid, invalid rows, service-rejected sample, empty, 501 rows); downloads unavailable | Safe document picker, template/guide downloads (M-001 file type), real parsing limits. High rules, unknown MIME filter; 1–2 days |
| S-182 file destination list | Opened from S-186 with group info and file. Count header, invalid-only filter, search, edit, delete, delete-all/invalid records, total, conflict, accept | Same parser output; invalid filter, edit, per-row and invalid-row deletion, exact total, recovered accept checks. Search not reproduced | Search, server pre-validation and edit/delete service calls. High rules; 1 day |
| S-190 waiting | Opened after pre-validation (first mode) or registration (second mode), or when resuming. Initial and manual status refresh, minimum loader period, cancel sheet | Text and controls recovered; **Designed for mock**: Refresh is a deterministic local exit to the next page; no timer or polling | Status lookup, ready predicate for second mode, cancel deletes the server group. High routes, unknown second-mode predicate; 1 day |
| S-188 validation | Opened when first-mode processing is ready. Server-validated rows with ready/invalid/unknown status, filter, search, edit/delete, conflict, continue to registration; Back asks to cancel | Synthetic ready/rejected statuses from the fixture; continue opens S-185 when rejected rows exist, else waiting (second mode) | Typed validation result and status mapping, edit/delete/registry calls. Medium; 1–2 days |
| S-185 registration errors | Opened with registration errors; remove illegal records then continue, or cancel; Back asks to cancel | Rejected synthetic rows; removal recalculates the exact total and opens waiting (second mode) | Error records and group update with a new amount. Medium; 1 day |
| S-189 verify | Opened when second-mode processing is ready. Source, destination count, amount, withdrawal identifier/description, destination details, note/attachment, OTP, accept, cancel | Local summary and destination details; note/attachment, OTP and accept are unavailable; cancel returns to S-187 | OTP challenge, key exchange/signature, transfer and user-only authorization. High caller, never connect assistant handlers; 2 days |
| S-096 group receipt | Opened after a final or unresolved outcome. Time, total, destination count, source, trace/track, share, history | Independent labelled synthetic sample; trace/share/history unavailable; no verify-to-receipt route | Typed outcome and unresolved-result presentation. High identity; 0.5 day |
| S-089 group history | Transfer-trace registry keys start it. Paged list with amount search, item fields, attachment/trace-file download, destination drill-down | Two synthetic records, amount search and empty result; downloads unavailable | Paged history, trace-file export, attachment download. High identity/fields; 1 day |
| S-087 history destinations | Opened from a history item with its trace. Destination-number search and paged rows | Fixture rows for the selected sample, search/empty result | Paged destination details. High; 0.5 day |
| S-088 destination detail | Opened from a destination row with its trace and row identity. Type, status, description, payment identifier, reason, destination description, reference and tracking numbers | Labelled synthetic values | Destination payment inquiry record. High fields; 0.5 day |

S-183 is allocated to FR-09 but is not a group-transfer page: its own content, label
and wage-advance caller identify a wage-advance request receipt (**Recovered**). Its
catalogue name/label are corrected with the ID and allocation unchanged; moving it to
the wage-advance batches needs owner approval. It is not registered or implemented.

Effort is preliminary future mock/frontend work, excluding real bank integration and
authorization design. Twelve of the thirteen allocations are implemented.

## Entry and result relationships

The recovered menu registry starts S-187 from the group-transfer key of the transfer
and account menus. FR-08's S-193 menu therefore gains a third synthetic child row; its
membership and order are **Designed for mock**. S-089 is started by a separate
transfer-trace menu that the replica does not have, so it and S-096 use labelled
independent review entries. The receipt's history control is not traced to a target.

Recovered order: S-187 → S-184 (manual) or S-186 → S-182 (file) → S-190 first mode →
S-188 → S-185 when registration returns errors → S-190 second mode → S-189 → S-096.
Waiting, validation and errors pages close when they hand over, and their Back asks
to cancel. Cancel returns to S-187, which keeps its inputs (it is brought back to the
front, not recreated). Resuming an in-progress group opens waiting in first mode for
the not-yet-validated steps and in second mode for registration and later steps.
The replica never resumes: there is no server group.

## File schema, limits and validation

**Recovered file type:** Office Open XML workbook (`.xlsx`) read by an in-memory
workbook parser; legacy `.xls` is not read. First sheet only; row 0 is a header.
Columns: 0 row label (only used to detect blank rows), 1 IBAN (`IR` removed),
2 amount text, 3 reason label, 4 payment identifier, 5 destination description,
6 reason key. A row whose cells 0–5 are all blank is skipped; a non-text cell in
columns 0–5 makes the whole row unreadable and it is skipped. Row identity is the
sheet row number. The picker MIME filter is encrypted and remains **Unknown**.

**Recovered limits:** a file must yield 1–500 readable rows; 0 or more than 500 is a
file error. Manual entry stops adding at 30 rows. No byte-size limit and no
client-side duplicate-row rule exist in the recovered code; any duplicate or size
policy is service-side and **Unknown**. Large files are parsed fully in memory.

**Recovered row validity (file):** IBAN passes the FR-08 mod-97 checks, amount is a
whole number ≥ 1, and both reason label and key are present, except that one
own-bank destination code is exempt from the reason requirement. The exemption is
kept in the private ledger; **Designed for mock:** the replica requires a synthetic
reason on every row and does not model a reason key.

**Recovered row validity (manual):** destination required and IBAN-valid, amount
required, reason required, and a payment identifier required when the service marks
the destination as governmental. That marking is **Unknown**, so the replica keeps
the identifier optional (≤30 digits).

**Recovered accept checks:** every row valid; more than one destination (exact
warning text reproduced); then if the row total differs from the first-page total a
conflict sheet offers the corrected total. Otherwise the client asks for a job trace
and pre-validates. The replica stops at local checks and opens waiting.

**Units:** all amounts are exact Long rial (IRR) integers. `TransferRules.amountRial`
rejects signs, decimals, separators and overflow; Persian digits are normalized. The
group total uses overflow-checked addition and reports overflow instead of wrapping.
File amount text is parsed the same way; the original's parser accepts any Long and
its own ≥ 1 check, which the replica's positive check matches.

Fixture IBANs use an all-zero bank code with valid checksums, so no real bank or
account is implied. Arithmetic validity does not prove a recipient exists.

## State, cancellation and accessibility

Non-secret draft state (source, total, mode, identifiers/descriptions, rows, chosen
synthetic file, rejected sample and waiting mode) is saveable and survives Back and
recreation; history/destination search text is retained per page. Changing entry
mode starts a fresh destination list. Dialogs cancel without mutation. No OTP,
password or note is collected. Persian/RTL text, LTR IBAN/amount fields, numeric
keyboards and keyboard clearance follow FR-08 components. The original's minimum
4-second waiting loader is not reproduced; the preview has no timers.

## Candidate models and future services

| Candidate | Caller-supported relationship | Remaining contract gaps |
|---|---|---|
| M-001 | File-type enumeration (spreadsheet, PDF) passed by S-186's example (spreadsheet) and guide (PDF) downloads as a GET query parameter | The inventory places this parameter on E-154 ("transfer trace batch") and lists E-164 ("get file template") as a DELETE with an M-001 body. The service's own annotations show the file-type query belongs to the template download; E-150/E-163 show the same swapped name/shape pattern. Recorded, not rewritten here |
| M-031 | Group verify accept and receipt consume the final financial result; trace lookup returns it directly | Encrypted transfer, OTP and signature payloads remain **Unknown** |
| M-029 | Encrypted envelope returned by the other group calls | Inner group summary, validation, registry, history and destination records are recovered only as client-side model fields |

No other endpoint is assigned by model resemblance. Proposed future mock records:
source eligibility and balance, in-progress group, pre-validation job and summary,
per-row validation/registry results, edit/delete with recalculated amount, status
steps, OTP challenge, history pages, destination details, trace/attachment files and
outcomes. They need approved typed contracts and user-only authorization boundaries.
FR-09 implements no HTTP service, request, database, file picker, permission or
external launcher.

## Unimplemented and uncertain states

All populated records, waiting outcomes, validation statuses, registration errors and
generic empty/loading/error/retry states are **Designed for mock**. Search on file and
validation lists, edit/delete on validation, attachment upload, OTP, accept, receipt
trace/share/history, history/trace downloads, familiar destinations, balance and the
in-progress resume sheet are absent or unavailable. Exact sheet geometry, status
colours and pagination are not reproduced. Private overlay/font provenance remains
SD-ASSETS (84 resources); public substitutes remain disclosed. Original-app fidelity
is unscored.
