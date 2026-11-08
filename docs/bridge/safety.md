# How rule 4 is enforced

Rule 4: the assistant never authorizes or submits a transaction. No operation, route, endpoint or
button may reach a confirm, submit, pay, PIN, OTP, password or CVV2 action.

## The five walls

A command has to pass all of these to touch a control.

| Wall | Where | What it does |
|---|---|---|
| 1. The operations | `contracts/schemas/command.schema.json`, `Executor.policyProblem` | There are four operations. None submits. Any other name is `rejected_policy` |
| 2. The routes | `ReplicaGraph.kt`, `Routes.exported`, `assistant/navigator.py` | Only `REAL` and `DEMO` edges are exported. A `USER_ONLY` edge is in neither graph, and the navigator refuses any edge whose tags are user-only |
| 3. The fields | `ProfileForms.kt`, `Policy.fillable`, `Navigator.fields_of` | A secret field is never a requirement, so nobody asks for it, and it is refused before the screen is touched |
| 4. The last gate | `Policy.mayTap`, `Policy.mayType`, called from `Actions` | An allow-list. A tag is tapped only if an exported edge or a non-secret field names it, and anything on the user-only list is refused even then |
| 5. The screen | `ComposeControls.setText`, `Actions.type`, `Actions.tap` | A password field is never written or read. A disabled control is never fired. A dialog hides what is behind it, as it does for a user |

`show_review` is safe because it stops: on a page in `Reviews.pages` it taps nothing.

`navigate`'s arguments carry row keys and nothing else. The executor refuses a name no edge
declares and a key that is not one plain word, and the tag a key makes must still pass `Policy`.

The backend adds its own limits. `assistant/command_queue.py` builds only the four operations, and
`assistant/task_runner.py` only ever sends three of them. A run refuses any value its plan did not
ask for, and it refuses a row key that would spell a user-only tag before the app sees it. The
harness takes a device and a destination, and refuses any other field. `POST /graph/run` takes a
screen and values, and refuses any other field. `test_no_route_can_confirm_submit_or_pay` pins the
full list of backend routes.

## What the tests prove

All of these are JVM tests in `android/app/src/test/java/org/ragbot/replica/bridge/PolicyTest.kt`,
unless marked.

| Claim | Test |
|---|---|
| Every user-only action is refused on every screen | `everyUserOnlyActionIsRefusedOnEveryScreen` |
| A tap on one never reaches the control | `tappingAUserOnlyActionNeverReachesTheControl` |
| A control nothing names is refused | `aControlNoEdgeOrFieldNamesIsRefused` |
| No route to any screen crosses a user-only edge | `noRouteToAnyScreenCrossesAUserOnlyEdge` |
| Going to a screen behind a user step taps nothing | `navigatingToAScreenBehindAUserStepNeverTapsThatStep` |
| Every secret field is refused before the screen is touched | `everySecretFieldIsRefusedBeforeTheScreenIsTouched` |
| A field outside the form is refused | `aFieldOutsideTheFormIsRefused` |
| A choice or row key cannot spell a confirm tag | `aChoiceValueCannotNameAConfirmControl`, `aRowKeyCannotReachAUserOnlyRow`, `noRowKeyMakesATagPolicyAllows`, `aRowKeyWithStepsInItIsRefused` |
| An argument can only name a declared selection | `anArgumentNoEdgeDeclaresIsRefused` |
| Every row edge declares what it needs | `everyRowTagDeclaresItsSelection` |
| No secret field is marked as needed | `noSecretFieldIsMarkedNeeded` |
| `submit`, `confirm`, `authorize`, `pay` and the old `select_candidate` are refused | `anUnknownOperationIsRefused` |
| `show_review` taps nothing on any review page | `showReviewStopsOnEveryReviewPage` |
| A secret value is never observed | `aSecretValueIsNeverObserved` |
| A disabled control is never fired | `ExecutorTest.aDisabledControlIsNeverFired`, and on the device `BridgeJourneysTest.aDisabledControlStopsTheRoute` |
| The demo journeys move no money | `BridgeJourneysTest` and `BridgeServicesTest` check the balance after each test (device) |
| No exported edge uses a user-only action | `ReplicaProfileTest.noExportedEdgeUsesAUserOnlyAction` |
| The navigator refuses a route over a user-only action | `test_navigator.py::test_a_route_that_crosses_a_user_only_action_is_refused` |
| A secret field is never a requirement | `test_navigator.py::test_a_secret_field_is_never_a_requirement` |
| A run refuses a value its plan did not ask for | `test_task_runner.py::test_a_value_the_plan_does_not_ask_for_is_refused` |
| A row key spelling a user-only tag never reaches the app | `test_task_runner.py::test_a_row_key_that_spells_a_user_only_tag_never_reaches_the_app` |
| Every feature sends only the allowed operations | `test_task_runner.py::test_no_feature_ever_sends_an_operation_outside_the_four` |
| Every feature's route stays off user-only actions, and every requirement can be asked | `python tools/project_checks.py profile` |
| This page lists every user-only action | `thisPageOfTheDocsListsEveryUserOnlyAction` |

## Each user-only action

Every action below is blocked in the same places, `Routes.exported`, `Policy.isUserOnly` and
`Navigator.is_user_only`, and proved by the first two tests above. The list comes from
`ProfileUserOnly.kt`. A `*` stands for a row key.

**Confirm, submit and pay**

`account-transfer-submit`, `bill-accept`, `card-transfer-submit`, `cardboard-act`, `cardboard-reject`, `continuous-confirm`, `digital-confirm`, `digital-confirm-*`, `facility-ok`, `funding-confirm`, `funding-fetch`, `funding-outcome-*`, `group-verify-accept`, `payment-submit`, `request-submit`, `svc-confirm`, `toll-fee-*`, `topup-accept`, `wallet-op-submit`, `wallet-submit`

**PIN, OTP, password and CVV2**

`card-transfer-otp`, `facility-password`, `login-password`, `login-submit`, `noLogin-ok`, `otp-code`, `otp-ok`, `payment-card-otp`, `pin-confirm`, `pin-ok`, `secret-*`, `secure-ok`, `support-otp`, `support-otp-continue`

**Consent and signature**

`digital-activate`, `digital-deactivate`, `digital-update`, `svc-agree-*`, `svc-note-*`, `svc-sign-*`

**Changes to saved data, and requests in the user's name**

`activation-send`, `additional-ok`, `address-save`, `apply-accounts`, `block-ok`, `budget-save`, `card-visible`, `delete-ok`, `edit-ok`, `faq-support`, `fetch-ok`, `notification-delete-confirm`, `notify-off`, `repetitive-delete-confirm`, `saved-delete-confirm`, `statement-request`, `suggestion-submit`, `support-identity-continue`, `support-mobile`, `support-send`, `vehicle-delete-confirm`

## Consent is the user's too

Accepting terms, issuing a promissory note and signing are commitments in the user's own name, so
`svc-agree-*`, `svc-note-*` and `svc-sign-*` are user-only even though each is only a choice on a
page. These fields are marked secret, so nobody asks for them. A feature with a consent ends on its
first consent page. `BridgeJourneysTest.loanRequest` shows it: the route stops at each consent page
until the test, playing the user, taps it.

The toll inquiry and the account top-up have no review page of their own: the next step is already
the user's fee choice and OTP, or the payment gateway. Their forms are where the executor stops.

## Adding a new screen

1. Declare its edges in `ReplicaGraph.kt`. Give every confirm, submit or pay edge `USER_ONLY`.
2. Add any tag of that kind to `ProfileUserOnly.kt`, and mark secret fields in `ProfileForms.kt`.
3. Add the action to this page. A test fails until you do.
4. Regenerate the profile, and run `python tools/project_checks.py profile`.
