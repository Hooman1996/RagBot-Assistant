# Replica completion report

**Result:** the replica is complete on mock data and ready for the owner's review. It covers the
app's most-used services, not the whole app (D-022). The bridge (#93) and the clickable graph (#94)
have not started.

Merged on `main` at `ac269c2`, 6 Nov 2026, and installed on emulator-5554, which shows that revision.

## What the replica covers

| Area | Works end to end against the mock bank |
|---|---|
| Access | Intro, welcome and login with the demo user; logout |
| Transfers | Card to card, internal, interbank, group, automatic transfer requests |
| Payments | Bills by account or card, mobile charge, charity, insurance installments, credit card repayment, loan installments |
| Wallet | Deposit, withdrawal, wallet to wallet, charge and bill from wallet, purchase with a sample code |
| Funding | Balance top-up through the sample gateway returns |
| Cheques | Chequebook requests, card board approve or reject, digital cheque transfer, Sayad cheques: register, confirm or reject, transfer, inquiry, history |
| Vehicles | Annual, traffic and freeway tolls, pollution, violations, transfer tax, documents, active plates, licence status, negative points |
| Inquiries | Credit report, bounced cheques, credit rate, with a fee |
| Loans | Generic loan request, with signature and promissory note as mock steps |
| Other | Personal finance dashboard and budgets, account opening, wage advance, credit product, rewards and point transfer, identifier conversion, card block, card requests |

Each service has a form, validation, a review, a user-only confirm with an OTP where the app has
one, a success or failure, and a history entry. Paid services also have a receipt.

## Recovered and designed

- **Recovered:** screen IDs, the navigation hierarchy of the earlier journeys, validation rules
  marked as recovered in the code, and the menu keys. Nothing recovered was renumbered.
- **Owner-traced:** the Home tiles and menu labels in the
  [validation sheet](graph-validation.md).
- **Designed for mock:** all data, every mock bank rule, the receipts' layout, the fields and steps
  of the services added under D-022, and the two menu pages `D-001` and `D-002`, which have no
  recovered node. The fields of those services were designed, not traced from the code.

## Edit the mock data

The [mock data runbook](../runbook/mock-data.md) explains each file, how to add a person or card, and
how to force a failure. In short: edit a file in `mock_services/fixtures/`, run
`python tools/project_checks.py mock-data`, then rebuild, or `adb push` the file to the override
folder and relaunch.

## Demo walk

Run on `ac269c2` on emulator-5554 by `DemoWalkTest`, a device test that taps the real screens in one
session. It passed.

| Step | Result |
|---|---|
| Log in | Passed through intro, welcome and login |
| Card to card to رضا, 250,000 rials | Receipt S-093 |
| Card to card over the balance | Failed with موجودی حساب کافی نیست, and stayed on the form |
| Pay a bill, 1,250,000 rials | Receipt S-092 |
| Buy mobile charge, 200,000 rials | Receipt S-094 |
| Check a Sayad cheque | Result S-166 |
| Pay a traffic toll, 5,000 fee and 445,000 rials | Receipt S-261 |
| Request a loan | Mock result S-337 |
| History | The new entries are listed |
| Balance | 12,500,000 down to exactly 10,350,000 rials |

The `adb push` override was checked by hand on the installed build. A bad `bills.json` showed the
error page naming the file, and removing it restored the app. The process-kill check
`android/tools/check_secret_state.py` passes.

## Left open

- **The app opens on the demo front page, not login.** That page carries the public-build
  disclosure and the review entries. ورود با کاربر نمایشی starts the login. The owner may want
  login first.
- **Three traces kept their demo paths.** S-118 and S-122 are typed-card forms, but the owner's
  picker is S-121. S-221 is the same page as S-222. See notes 8 and 9 of the sheet.
- **S-089 has two owner answers.** A first trace through سوابق, then not found. The later answer
  stands, and the سوابق tile opens S-091 as a designed route.
- **The profile's feature list is unchanged, at 53.** The new services have forms, edges and
  user-only actions in the profile, but no assistant intents yet. That belongs with #93.
- **The native crash is not explained.** One device run stopped in `libhwui` on the render thread,
  with no app code in the stack, on an emulator up for 30 hours. It did not recur after a reboot, as
  in the readiness report.
- **Still notices:** sharing, attachments, card and bill scanning, adding or deleting cards, invitation
  sending, and the old-result button on S-252. Tiles for deferred services keep their notice.
- **Unbuilt screens in the built areas:** S-160, S-161, S-452, S-438, S-439, S-415, S-416. Brokerage,
  crowdfunding, merchant terminals, registration, postal and the unresolved destinations stay
  deferred.
- **Fidelity is unscored.** Designed pages use the existing theme, and no comparison with the real
  app has been made.
