# Stage 1 readiness (S1-CHECK, #91)

**Verdict: ready with gaps.** Checked on `main` at `28b826f`, 4 Nov 2026. Rechecked at `31625f2`
after the fixes in #96.

Every registered ID is valid, and nothing can reach a confirm, submit or pay handler without a user
tap. One device test failed during this check; #96 found the cause, fixed it and closed G2 (section
1). S3-01 and S3-02 must close the gaps in [section 6](#6-gaps-for-s3-01-and-s3-02). The biggest
one: **83 of the 189 screens have no path from Home**, and they include every first-wave form except
credit repayment. The owner's decisions are in [section 7](#7-owner-decisions).

Evidence labels: reachability is `Observed` from the replica's own handlers. "Recovered caller"
restates the label in `navigation-graph.json`.

## 1. Build and tests

| Step | Result at `31625f2` |
|---|---|
| `project_checks.py all` | 131 passed, 0 failed |
| `pytest` | 112 passed, 0 skipped |
| `ruff check`, `ruff format --check` | Pass |
| JVM tests | 100 passed, 0 failed, 0 skipped |
| Lint | 0 errors, 117 warnings, unchanged from the FR-17 baseline |
| Device tests, three full runs in a row on a fresh emulator | 124 finished in each, 0 failed, 0 skipped |

**Earlier failures, for the record.**

1. On clean `main` at `28b826f`,
   `RecurringTransfersTest.scheduleValidationCorrectionAndContinuousFormNeverSubmit` failed in 1 of 3
   full runs. #96 proved it was a test bug. The window reports the keyboard hidden while the page
   still has its keyboard height, so a touch can miss the clipped button. Under CPU load the click
   action missed 0 of 320 times, and a real touch 6 of 320. The form's validation and drafts were
   never at fault. The same touch helper remains in 10 other test files.
2. At `31625f2`, one full run crashed after 17 of 124 tests, with a native abort on the replica's
   render thread in the system graphics library. It had no assertion failure and no app frame. The
   emulator had been up for hours under those load tests, and the crash did not recur after a fresh
   boot. Its cause is not proved, so a repeat should be investigated.

## 2. Graph IDs

| Check | Result |
|---|---|
| IDs registered in `HomeDestinations.implemented` | 189, matching the expected total and the sum of the tracker's built batches |
| IDs missing from `navigation-graph.json` | 0 |
| IDs registered twice | 0, across the list in `HomeNavigationTest` and all 28 destination groups |

## 3. Reachability

**106 screens** are reachable from Home (S-026) through real handlers. **83** are reachable only
through a demo or review entry on the page before Home.

Demo-only screens, grouped by journey. "Real caller" means:

- **Recovered, control Unknown:** the graph records a launch from Home, but the replica has no
  matching control. The recovered caller is the all-services menu, which is server-driven (F-066).
- **Via demo:** reached only through another demo-only screen in the same row.
- **Unknown:** the graph records no caller.
- **Behind a step:** the caller is recovered but sits in deferred or pre-login screens, or behind a
  user-only step.

| Journey | Screens | Real caller |
|---|---|---|
| Transfers | S-193, S-187, S-178, S-089 | Recovered, control Unknown |
| Transfers | S-191, S-192, S-181, S-177, S-179, S-184, S-186, S-182, S-190, S-188, S-185, S-189, S-088 | Via demo |
| Transfers | S-180, S-087 | Unknown |
| Bills | S-209 | Recovered, control Unknown |
| Bills | S-207, S-205, S-206 | Via demo |
| Bills | S-208, S-210 | Unknown |
| Mobile top-up | S-211 | Recovered, control Unknown |
| Mobile top-up | S-215, S-216 | Via demo (S-212 itself is reachable from S-200 in account mode, which ends in a notice) |
| Mobile top-up | S-213, S-214 | Unknown |
| Charity | S-283 | Recovered, control Unknown |
| Charity | S-284, S-282 | Via demo |
| Loans | S-338 | Recovered, control Unknown |
| Loans | S-306, S-331 | Via demo (S-134→S-306 is also recovered, not wired) |
| Loans | S-324, S-307 | Behind a step: callers are deferred loan and club screens |
| Loans | S-309 | Unknown in the graph; the replica opens it from S-307 |
| Card transfer | S-174, S-175 | Unknown |
| Identifier conversion | S-201, S-202 | Unknown |
| Identifier conversion | S-011, S-203, S-204 | Via demo |
| Insurance | S-402 | Unknown |
| Insurance | S-401, S-404 | Via demo |
| Insurance | S-403 | Behind a step: only a return from S-404 is recovered |
| Saved destinations | S-066, S-197, S-198 | Unknown |
| Support | S-076, S-079, S-080 | Unknown |
| Support | S-082 | Via demo |
| Support | S-078, S-081 | Behind a step: callers are pre-login screens (deferred) |
| Settings, statement, cards | S-071, S-086, S-118, S-120, S-122 | Unknown |
| In-flow funding | S-221, S-223, S-227, S-232, S-230, S-226, S-234, S-233 | Behind a step: hosts S-220/S-229 and the wage-advance caller are not built |
| Wallet purchase | S-101 | Behind a step: S-100→S-101 needs a scanned payment; the scanner is deferred |
| Independent receipts | S-092, S-093, S-094, S-095, S-096, S-097, S-098, S-133, S-261, S-405 | Behind a step: they follow a user-only submit, or the S-090 receipt control, which shows a notice |

The Home tiles for card transfer, transfer, top-up, bill, statement, loan, conversion, charity and
insurance all show the "not ready" notice, confirmed on the device. Only wallet, support, cheque, cartable and vehicle open
a journey.

## 4. Assistant readiness per journey

**Entry:** `Home` is a real path. `Demo` means no Home path. `Demo (R)` means no Home path, but a
recovered caller exists (section 3).

**Fill:** `Draft` means the values live in a draft held by `ReplicaApp`, so a normal handler can set
them. `Local` means they live inside the page composable, which S3-01 must lift or expose (gap G3).

Every listed field has a test tag. Wave 1 is the first assistant wave under D-021.

### Wave 1

| Journey | Entry | Form fields (test tags) | Fill | Review | User-only | Gap |
|---|---|---|---|---|---|---|
| Card-to-card | Demo; S-200 repeat opens S-176 prefilled | `card-transfer-source`, `-destination`, `-amount` | Draft | S-176 | `card-transfer-cvv2`, `-expiry`, `-password`, `-otp`, `-submit` | G1 |
| Internal and interbank transfer | Demo (R) S-193 → S-192/S-191; S-200 repeat opens S-173 | `account-transfer-source`, `-destination`, `-amount`, `-reason`, `-payment-id`, `-withdraw-id`, `-source-description`, `-destination-description` | Draft | S-173 | `account-transfer-submit` | G1 |
| Group transfer | Demo (R) S-187 | `group-source`, `group-total`, `group-withdraw-id`, `group-source-description`, `group-row-N-iban`, `-amount`, `-reason`, `-payment-id`, `-description`; file samples `group-file-*` | Draft | S-189, after waiting pages | `group-verify-accept` | G1; `group-refresh` is a manual local step |
| Recurring transfer | Demo S-180; Demo (R) S-178 | `schedule-title`, `-start`, `-end`, `-frequency-*`, `-day-*`, `-type-*`, then the transfer form | Draft | S-177, then S-173 | `account-transfer-submit`; S-179 `continuous-confirm` | G1 |
| Saved recipients | Demo S-066, S-197, S-198; Home S-200 for repetitive transactions | `saved-name`, `saved-number`, `saved-description` | Local | None; editor saves locally | `saved-delete-confirm`, `repetitive-delete-confirm` | G1, G3, G5 |
| Bills | Demo (R) S-209; Demo S-208, S-210 | `bill-source`, `bill-id`, `bill-pay-id`, `bill-note` | Draft | S-207 | `bill-accept`; S-205/S-206 `payment-card-*`, `payment-submit` | G1 |
| Insurance installments | Demo S-403 → S-402 → S-401 | `insurance-policy-*`, `installment-N`, `insurance-source-*` | Draft | S-404 | `payment-submit` | G1, G4 |
| Credit repayment | Home → card tab → S-131 | `credit-amount`, `credit-source-*` | Local | S-132 | `payment-submit` | G3 |
| Mobile top-up | Demo (R) S-211; Demo S-213, S-214; S-200 repeat opens S-212 | `topup-source`, `topup-phone`, `topup-operator-*`, `topup-amount`, `topup-amount-*` | Draft | S-212 | `topup-accept`; S-215/S-216 `payment-card-*`, `payment-submit` | G1 |
| Charity | Demo (R) S-283 | `charity-*` row, `charity-amount`, `charity-source`, `charity-payment-id`, `charity-description` | Draft | S-282 | `payment-submit` | G1, G4 |
| Loan repayment | Demo (R) S-338 → S-306 | `loan-*` row, `loan-installment-N`, `loan-source-*`, `loan-payment-id` | Draft | S-331 | `payment-submit` | G1, G4 |
| Loan groups (read-only) | Demo S-324, S-307 | `loan-group-*`, `loan-option-*` | None | None | None | G1 |
| Identifier conversion | Demo S-201, S-202 | `conversion-mode-*`, `conversion-identifier`, `conversion-bank-*` | Local | S-011 is the result | None | G1, G3 |
| Paid identifier inquiry | Demo S-203, S-204 | `conversion-source`, `conversion-source-*` | Local | None | `payment-card-*`, `payment-submit` | G1, G3 |

### Wave 2

| Journey | Entry | Form fields (test tags) | Fill | Review | User-only | Gap |
|---|---|---|---|---|---|---|
| Accounts | Home → account tab: S-085, S-063 → S-067 | `account-search`, `choose-*`, `apply-accounts` | Local | None | `apply-accounts` | G3 |
| Statement | Home → account tab: S-016 → S-014 | `ledger-start`, `ledger-end` | Local | None | None | G3, G4 |
| Statement request | Demo S-086 | `statement-start`, `statement-end`, `export-account`, `export-format` | Local | None | `statement-request` | G1, G3, G6 |
| Transaction history | Home → card tab: S-091 → S-090 | `history-search` | Local | None | None | G4 |
| Cards (read-only) | Home → card tab: S-012, S-125, S-134; profile: S-126 → S-068, S-025 → S-130 | `card-search`, `manage-search`, `pick-*` | Local | None | `fetch-ok`, `card-visible` | G3 |
| Balance forms | Home → card tab S-119; Demo S-120 | `secure-number`; the rest are secret | Local | None | `secure-cvv2`, `-expiry`, `-password`, `-ok` | G1 (S-120), G6 |
| Card issue and changes | Home → card tab: S-124, S-127 → S-129 → S-123 | `source-*`, `delivery-branch`, `delivery-post`, `address-*`, `color-*` | Draft | S-128 | `request-submit` | G4 |
| Card block and PIN | Home → card tab: S-121, S-135, S-136; Demo S-118, S-122 | `block-*`, `pick-*`; S-118/S-122 `noLogin-number`, `noLogin-expiry` | Local | None | `block-ok`, `secret-*`, `pin-ok`, `pin-confirm`, `activation-send`, `otp-*`, `noLogin-ok` | G1 (S-118, S-122), G3 |
| Wallet card | Home → card tab: S-140 → S-137 → S-139; S-141 | `wallet-*`, `wallet-address-*` | Draft | S-139 | `wallet-submit`; S-141 as card PIN | None |
| Profile and addresses | Home → profile: S-065, S-064, S-464 → S-463 | `field-*`, `pick-*` | Local | None | `edit-ok` with `otp-*`, secret `field-current`, `-new`, `-retype`; `additional-ok`, `address-save`, `delete-ok` | G3 |
| Settings | Home → profile → S-025: S-070, S-072, S-073; Demo S-071 | `notify-*`, `facility-*`, `biometric` | Draft | None | `facility-password`, `facility-ok`, `notify-off` | G1 (S-071) |
| Notifications | Home S-075 | `notification-filter-*` | Local | None | `notification-delete-confirm` | None |
| Support | Home S-077; Demo S-076, S-078 → S-081, S-079, S-080 → S-082 | `chatbot-draft`, `suggestion-description`, `support-draft` | Draft | None | `support-mobile`, `support-otp`, `support-send`, `suggestion-submit` | G1 |
| Search and pins | Home S-084 | `shortcut-search`, `shortcut-pin-*` | Local | None | None | None |
| Wallet overview | Home S-111: S-115 → S-114, S-110, S-112 | `wallet-start`, `wallet-end`, `wallet-amount`, `wallet-picker` | Local | None | None | G3, G4 |
| Wallet to wallet | Home S-111 → S-116 | `wallet-op-source`, `-destination`, `-amount` | Draft | S-117 | `wallet-op-submit` | None |
| Wallet purchase | Home S-111 → S-100, which stops without a scan; Demo for S-101 | `wallet-op-source`, `-amount` | Draft | S-101 | `wallet-op-submit` | G1 |
| Wallet withdrawal and charge | Home S-111 → S-106; sheet → S-104 | `wallet-op-source`, `-destination`, `-amount` | Draft | S-107, S-105 | `wallet-op-submit` | G4 for the sheet |
| Wallet top-up and bill | Home S-111 → S-102, S-109 | `wallet-op-mobile`, `-operator-*`, `-amount-*`, `-bill`, `-pay` | Draft | S-103, S-099 | `wallet-op-submit` | None |
| Account and wallet funding | Home S-222; S-111 sheet → S-231; Demo for in-flow | `funding-amount`, `funding-proposed-*`, `funding-target-*` | Draft | Amount page | `funding-confirm`, `funding-outcome-*`, `funding-fetch` | G1 (in-flow); S-228 and later are payment steps |
| Cheque books and tracking | Home S-147: S-142 → S-146, S-149, S-150 | `cheque-account`, `cheque-book-*`, `cheque-no` | Local for `cheque-no`; account is Draft | None | None | G3, G4 |
| Chequebook request | Home S-147 → S-143 | `cheque-account`, `cheque-page-*` | Draft | S-144 | `payment-submit` | None |
| Cheque reports | Home S-147: S-145, S-148 → S-153 | `cheque-status-*`, `cheque-from`, `cheque-to` | Draft | None | None | None |
| Card board | Home S-154 → S-155, S-156 | `cardboard-tab-*`, `cardboard-*` row | Local | S-155, S-156 | `cardboard-act`, `cardboard-reject` | G4 |
| Digital cheque activation | Home S-147 → S-157; Demo for the active state | None | None | None | `digital-activate`, `-update`, `-deactivate`, `-confirm` | G1 (active state) |
| Digital cheque transfer | Home S-147: S-159 → S-158 → S-151, or S-151 | `cheque-account`, `digital-sayad`, `digital-sayad-*`, `digital-bounce` | Draft | S-152 | `payment-submit` | G4 |
| Vehicles | Home S-253: S-256, S-246, S-248, S-254 → S-252 | `vehicle-*` row | Local | None | `vehicle-delete-confirm` | G4 |
| Road tolls | Home S-253 → S-256 → S-241, S-275, S-250, S-257 | `toll-plate-N`, `toll-letter-*`, `toll-type-*`, `toll-owner-*`, `toll-code`, `toll-phone`, `toll-vin`, `toll-check-*` | Draft | S-240, S-274, S-249 | `toll-fee-*`, `otp-*`, `payment-submit` | G7 |

## 5. Safety

| Check | Result |
|---|---|
| Confirm, submit or pay without a user tap | **None.** Every such handler is an `onClick`. The only effects are the card pager index and notification paging |
| Assistant-reachable confirm handler | **None.** No bridge exists yet, and the contract's five operations include no tap or submit |
| External entry | One exported activity, launcher only. No incoming intent data, deep links, receivers, services or WebView. No INTERNET permission |
| Secrets in saved state | Pass after #96. S-119/S-120 kept CVV2 and the second password in saved state until #96 fixed it (G2). `android/tools/check_secret_state.py` now shows them empty after the process is killed. No PIN, OTP, CVV2 or password field is saved, no draft class holds a secret, and nothing is written to disk |
| Money | Integer `Long` with an IRR unit. No floating point in amounts or rules |
| Committed hosts and URLs | Pass. No original endpoint host. Remaining hosts are standards, tooling mirrors, `localhost` and test fixtures |
| Committed secrets | Pass |
| Committed brand strings | A recovered cheque menu key contains the bank's brand token. The owner accepts it: not a flaw (section 7) |
| National digital-cheque system name | The owner accepts it in public records: not a flaw (section 7) |

Some final steps end in a labelled local result rather than a notice: `request-submit`,
`wallet-submit`, `block-ok`, `pin-ok`, `pin-confirm`, `activation-send`, `otp-ok`, `noLogin-ok` and
`edit-ok`. They need a user tap today. S3-01 must list them in `user_only_actions` with the rest.

## 6. Gaps for S3-01 and S3-02

| ID | Gap | Closes in | What to do |
|---|---|---|---|
| G1 | 83 screens have no Home path, including every first-wave form except credit repayment. #93 expects the executor to reach each journey's form from Home | S3-01 | Follow the owner's decision in section 7. Do not invent real callers |
| G2 | S-119/S-120 saved CVV2 and the second password | Closed by #96 | S3-01 still marks them secret in the profile |
| G3 | Page-local form values on S-016, S-064, S-065, S-066, S-086, S-112, S-115, S-118, S-121, S-122, S-131, S-135, S-136, S-149, S-150, S-197, S-201 to S-204 and S-463. A bridge cannot set them through a normal handler | S3-01 | Lift the values the assistant fills into `ReplicaApp` drafts. Hits #93's stop condition if left |
| G4 | Row-dependent pages: S-014, S-090, S-114, S-146, S-155, S-156, S-158, S-179, S-252, S-306, S-401 need a selected record, and the wallet charge choice is a dialog. `navigate` has no argument for this | S3-02 | Stop on the list and return `blocked`, or use `select_candidate`. Never jump straight to the page |
| G5 | S-066 row actions need a swipe, and reordering needs a long press. Home layout editing needs a drag | S3-01 | Keep them out of the profile; they are not needed to reach a form |
| G6 | Two controls have no tag: the S-086 dropdown items, and the S-119 save row | S3-01 | Add tags. Almost all of the other untagged buttons are cancel, close or back |
| G7 | Toll results sit behind the user's OTP, so S-240, S-274, S-249 and S-258 cannot be reached by the bridge | S3-02 | `navigate` returns `blocked` at the OTP sheet. The same applies to S-081 behind `support-otp` |
| G8 | Direct `open(id)` crashes on S-123, S-124, S-127, S-128, S-129, S-137, S-139 and S-140 (null request) and S-114 (no row). Other pages render blank without a draft | S3-02 | Move only along declared edges. #94's node click must use the same route |
| G9 | No single observable state: current page, tab, dialogs and sheets are local to composables, and the demo page has no graph ID. Drafts have no ID or version, which `observation.schema.json` requires | S3-02 | Expose them from `ReplicaApp`, and add version counters |
| G10 | `profile.schema.json` allows field types `text`, `money` and `ref` only. #92 needs `choice` and a secret marker | S3-01 | Propose the schema change in the PR and wait for the owner's review before merging. `action` can carry the control tag and demo label |
| G11 | Independent receipts would show a success page for a payment that never happened | S3-01 | Exclude them from feature destinations |

Not a gap: `Continue`-style controls are often labelled "confirm" (`loan-source-confirm`,
`first-ok`), but they only open the next step. S3-01 should classify each one by its handler, not
by its label.

## 7. Owner decisions

Recorded on #92 and #96.

1. **Test failure:** find and fix it before merging, and fix G2 in the same issue. Done in #96.
2. **G1, Home paths:** in #92, add real Home links for the journeys the owner traces in the real
   app, using the taps the owner puts in `docs/reports/graph-validation.md`. Every other built screen
   gets a labelled demo path from a demo menu on Home, marked "demo path, not a real-app route".
3. **Brand token:** keep it as it is. Not a flaw. No rename and no history rewrite.
4. **National digital-cheque system name:** fine in public records. Not a flaw.
5. **G10:** in #92, propose the profile schema change in the PR and wait for the owner's review
   before merging it.

## 8. Left Unknown

Original-app fidelity stays unscored, and the one render-thread crash in section 1 is unexplained. The real controls for the eight recovered callers, and every
"Unknown" caller in section 3, stay Unknown until the owner's Stage 2 graph validation.
