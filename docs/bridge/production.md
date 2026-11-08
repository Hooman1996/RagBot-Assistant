# What changes for production

The replica is a stand-in for the real app. This page says what stays, what the app team builds
and what never moves. Nothing here has been agreed with the app team yet.

## What stays ours, unchanged

- The assistant, its tools, the lookup adapter's interface, the command loop, the navigator and the
  task runner.
- The contracts in `contracts/`: the command, observation and result schemas, the transition table
  and the pull API.
- The idea of a profile: screens, edges, forms, requirements, features and user-only actions, as
  data.

## What the app team provides

Exactly four things.

**1. The four bridge operations, in their app.**

| Operation | What it must do |
|---|---|
| `observe` | Report the screen, any dialog, a loading flag and the form's non-secret values |
| `navigate` | Reach a screen in the right state, with the arguments it needs. How is their business: taps, a direct open or a deep link. Our replica walks its declared edges only because it has no direct open |
| `fill_form` | One generic command for every form: set each field by its tag, through the form's own handlers, so its validation runs |
| `show_review` | Open the form's review page, and stop |

After each one, report the observation, including an unexpected screen or a validation error. The
bridge does not plan routes or decide what is missing. Our Kotlin files show one way to do this:
`Controls.kt` and `ComposeControls.kt` are the bindings to the screens, and `Routes.kt` is the part
a direct open makes unnecessary.

**2. Field tags on their forms.** A stable tag on every control the profile names. The replica's
tags, such as `card-transfer-amount`, are ours.

**3. The real graph, with each screen's requirements.** A profile that passes
`python tools/project_checks.py profile`: screens, edges, and for each required field its type, a
short description, an `ask` question and its lookup, and for each edge any selection it needs.

**4. The real lookup APIs.** Today `assistant/lookups.py` reads the JSON in
`mock_services/fixtures/`. In production, a real adapter with the same methods calls the app team's
APIs instead.

| Lookup | Replaces | Used for |
|---|---|---|
| Contacts and saved destinations | `contacts.json` | "to رضا" becomes a card or IBAN |
| Accounts and balances | `accounts.json` | Source account choices |
| Cards | `cards.json` | Source card choices |
| Bills | `bills.json` | Saved bills |
| Loans | `loans.json` | Repayment choices |
| Cheques | `cheques.json` | Cheque and Sayad choices |
| Vehicles | `vehicles.json` | Plates and licences |
| Wallets | `wallets.json` | Wallet choices |

They also provide a development backend address and a way to turn the loop on, for test builds and
for approved users.

## What never moves into the assistant

The app team keeps confirm and every money-moving service. They never give the assistant:

- The confirm or submit calls, or any route to them.
- A PIN, OTP, password, CVV2 or signature, in either direction.
- Any money-moving API. The lookups only read.

The assistant prepares; the user confirms in the app. See [safety](safety.md). In the real app,
these rules should be enforced a second time by the app itself, not only by the profile.

## Checklist for the app team

1. Give every control in the profile a stable tag in the real app.
2. List every user-only action and secret field in the real app. Review this list with us first.
3. Write the real profile, with each screen's requirements, and pass
   `python tools/project_checks.py profile`.
4. Build the four operations, and publish the current page, the open dialog and a loading flag.
5. Port the policy tests, and add every real user-only action to them.
6. Run the journey tests against a test build. Check that no money moves.
7. Agree how a device authenticates to the backend.
8. Put the lookup APIs behind an adapter with `assistant/lookups.py`'s methods.
9. Turn the loop off by default, and control who can turn it on.

## Open questions for the app team

- Do real screens use Compose, Android views, or a mix? Do they have stable IDs?
- Can real menus be server-driven? The recovered Home menu is, so the profile may need updating
  from the server.
- How should the loop reach the backend: polling, as now, or a push channel?
- Which login and session does a command run under, and how does the backend prove it is ours?
- What should happen if the app is in the background when a command arrives?
- How do pickers fill in the real app: by key, as here, or by another reference?
- Which screens can open directly with their arguments, and which need steps first?
- `session_id` equals `device_id` in development, because the contract's resume call names only a
  session. What is the real mapping?
- Where should a device's draft live: in the app, as now, or in the mock service's successor (D-006)?
