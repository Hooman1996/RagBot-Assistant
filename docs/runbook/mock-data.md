# Mock data

All people, cards, accounts and numbers in the replica are synthetic. They live in editable JSON
under `mock_services/fixtures/`, one file per domain, with one schema each in
`mock_services/schemas/`. Amounts are integers in rials: each file carries `"unit": "IRR"`.

Menus, labels and validation rules stay in Kotlin. They are the app's structure, not its data.

## What each file holds

| File | Holds |
|---|---|
| `customer.json` | The demo user: name, login, OTP, transfer limits, addresses; credit inquiries, wage advance, credit product and reward points |
| `accounts.json` | Accounts and balances; the identifier-conversion sample and fee; account types and branches for opening |
| `cards.json` | Cards, candidates for the fetch page, credit summaries, file loans |
| `contacts.json` | `people` (such as رضا) with a card, account, IBAN and wallet; other saved destinations |
| `bills.json` | Bills by bill ID and payment ID |
| `operators.json` | Mobile operators with prefixes, limits and suggested amounts |
| `charities.json` | Charities |
| `loans.json` | Loans, loan groups and their options; products of the loan request |
| `cheques.json` | Cheque books and leaves, reports, requests, card board tasks, digital cheques, Sayad cheques, fees |
| `vehicles.json` | Saved plates, contacts and licences; toll items, fees, pollution quota; violations, transfer tax, documents, licence status and points |
| `wallets.json` | Wallets, known wallet destinations, statement rows |
| `transactions.json` | Recent transactions, statement rows, group files and history, standing orders; spending and budgets |
| `insurance.json` | The insured person and policies |
| `scenarios.json` | Forced outcomes and the Persian failure messages |

## Change the data

Edit the file in `mock_services/fixtures/`, then check it:

```bash
python tools/project_checks.py mock-data
```

A rebuild bundles the files into the app. To change data without rebuilding, push the edited file
to the app's override folder and relaunch the app. A file there replaces the bundled file of the
same name:

```bash
adb -s emulator-5554 push mock_services/fixtures/contacts.json \
  /sdcard/Android/data/org.ragbot.replica/files/mock/contacts.json
adb -s emulator-5554 shell am force-stop org.ragbot.replica
adb -s emulator-5554 shell am start -n org.ragbot.replica/.MainActivity
```

Run the app once before the first push, so that Android creates the folder. To go back to the
bundled data, remove the override:

```bash
adb -s emulator-5554 shell rm /sdcard/Android/data/org.ragbot.replica/files/mock/contacts.json
```

Uninstalling the app removes the folder. `connectedDebugAndroidTest` uninstalls it, so push your
overrides again after a test run.

The app checks every file when it starts. If a file is bad, it shows a page naming the file and the
problem instead of crashing.

## Add a person or a card

- **A person:** add an entry to `people` in `contacts.json`. All six fields are required. The person
  then appears as a saved card, account, IBAN and wallet, and transfers to them succeed.
- **A card:** add an entry to `cards` in `cards.json`. `primaryAccount` must be the `number` of an
  account in `accounts.json` for the card to spend from it.

Card numbers must pass the Luhn check and IBANs the mod-97 check. Keep them obviously synthetic:
start cards with `000000` and use bank code `000` in IBANs. The project check rejects a bad checksum.

## Force a failure for a demo

Add an entry to `forced` in `scenarios.json`, and push the file:

```json
{ "forced": [ { "operation": "card-transfer", "outcome": "insufficient-balance" } ], "messages": { } }
```

Keep the existing `messages` in the real file.

- `operation` is the name of a service, or `*` for every service.
- `outcome` is a key of `messages`, which is the failure the user sees, or `success`, which skips the
  checks and goes through.

Operations: `card-transfer`, `account-transfer`, `group-transfer`, `standing-order`, `bill`,
`charge`, `charity`, `insurance`, `credit-repayment`, `loan-installment`, `toll`, `toll-inquiry`,
`funding`, `cheque-book`, `digital-cheque`, `cardboard`, `card-block`, `card-request`,
`conversion-fee`, `wallet-charge`, `wallet-decharge`, `wallet-w2w`, `wallet-buy`, `wallet-mobile`,
`wallet-bill`, `card-balance`, `sayad`, `sayad-inquiry`, `violation`, `transfer-tax`,
`credit-report`, `bounced-cheques`, `credit-rate`, `loan-request`, `account-opening`,
`wage-advance`, `credit-product`, `rewards-transfer`.

Common outcomes: `insufficient-balance`, `wrong-otp`, `service-unavailable`.

Without a forced outcome, the bank checks each request against the files: the recipient exists, the
card or IBAN is valid, the balance and limits allow it, the bill exists and is unpaid, and the OTP
equals `otp` in `customer.json`.

## Session

Balances and histories change in memory, so a transfer to رضا appears in سوابق. They reset when the
app restarts. Rotating the device keeps them.
