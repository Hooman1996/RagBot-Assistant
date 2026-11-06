# What changes for production

The replica is a stand-in for the real app. This page says what stays, what the app team builds
and what never moves. Nothing here has been agreed with the app team yet.

## What stays ours, unchanged

- The Python command loop, the graph page and, later, the assistant and its lookup tools.
- The contracts in `contracts/`: the command, observation and result schemas, the transition table
  and the pull API.
- The idea of a profile: screens, edges, forms, features and user-only actions, as data.

## What the app team builds

The Kotlin executor has two halves. One half is logic that does not know the app, and can be
ported as it is. The other half is bindings to the real screens.

| Part | Files | What the app team does |
|---|---|---|
| Logic | `Contract.kt`, `ContractJson.kt`, `Policy.kt`, `Routes.kt`, `Actions.kt`, `Navigator.kt`, `FormFiller.kt`, `Observer.kt`, `Executor.kt`, `CommandPoller.kt`, `BackendClient.kt` | Copy, then point them at the real profile |
| Bindings | `Controls.kt` (the interface), `ComposeControls.kt`, `BridgeState.kt` | Re-implement `Controls` for the real screens, fields and menu IDs |
| Map | `app_profiles/replica.profile.json` | Replace it with a profile of the real app |

The replica's tags, such as `menu-card-transfer`, are ours. The real app needs its own stable ID on
every control the profile names. `ComposeControls.kt` reads Compose test tags and semantics. If
real screens use Android views, or a mix, that file is what changes.

## What the app team provides

The lookup tools read data through the adapter. Today the data is JSON in
`mock_services/fixtures/`. In production, the adapter calls the app team's APIs instead.

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

The confirm and submit calls. No PIN, OTP, password, CVV2 or signature ever passes through the
bridge, in either direction. The assistant prepares; the user confirms in the app. See
[safety](safety.md). In the real app, these rules should be enforced a second time by the app
itself, not only by the profile.

## Checklist for the app team

1. Give every control in the profile a stable ID in the real app.
2. List every user-only action and secret field in the real app. Review this list with us first.
3. Write the real profile, and pass `python tools/project_checks.py profile`.
4. Implement `Controls` for the real screens.
5. Publish the current page, the open dialog and a loading flag from the app.
6. Port the policy tests, and add every real user-only action to them.
7. Run the journey tests against a test build. Check that no money moves.
8. Agree how a device authenticates to the backend.
9. Replace the mock data behind the adapter with the lookup APIs.
10. Turn the loop off by default, and control who can turn it on.

## Open questions for the app team

- Do real screens use Compose, Android views, or a mix? Do they have stable IDs?
- Can real menus be server-driven? The recovered Home menu is, so the profile may need updating
  from the server.
- How should the loop reach the backend: polling, as now, or a push channel?
- Which login and session does a command run under, and how does the backend prove it is ours?
- What should happen if the app is in the background when a command arrives?
- How do pickers fill in the real app: by key, as here, or by another reference?
- `session_id` equals `device_id` in development, because the contract's resume call names only a
  session. What is the real mapping?
- Where should a device's draft live: in the app, as now, or in the mock service's successor (D-006)?
