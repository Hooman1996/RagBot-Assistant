# Standalone Assistant API

The standalone application serves the workflow and its HTTP bridge on loopback. It loads its graph
and banking lookup bindings at startup. The model and embeddings clients use the development
settings in `.env`, with addresses documented in `.env.example`.

From the checkout, in the `assistant` conda environment, start the synthetic lookup provider.

```bash
python -m uvicorn mock_services.app:create_app --factory --host 127.0.0.1 --port 8110
```

In another terminal, start the Assistant with both configuration files.

```bash
set -a
source .env
set +a
python -m assistant --graph app_profiles/replica.profile.json \
  --services mock_services/service-bindings.example.json
```

The standalone API listens on port 8101 by default and calls the model and mock services during
a turn. `GET /openapi.json` serves the [mobile contract](../../contracts/openapi/assistant_mobile.json)
with its schemas bundled. The development backend still starts with `python -m assistant` on port
8100 and serves the graph viewer and test harness with the development fixtures.

## Graph and service bindings

The graph uses the existing [profile schema](../../app_profiles/profile.schema.json). Its feature
list controls what the assistant offers, and its edges and forms control routes and required
inputs. The generated replica profile is the starting graph. To try a variant, copy it under
`local/scratch/`, edit the copy, and restart with its path.

The [example service file](../../mock_services/service-bindings.example.json) maps the five
candidate journeys' lookups to typed, read-only operations.

| Lookup | Adapter | Provider |
|---|---|---|
| `accounts` | `accounts-http` | `GET /accounts` loads the source accounts and exact IRR balances. |
| `contacts.account` | `saved-accounts-http` | `GET /saved-accounts` loads named account recipients from the contact and saved-destination fixtures. |
| `cards` | `cards-http` | `GET /cards` loads card keys, numbers, kinds, and eligibility statuses. |
| `contacts.card` | `saved-cards-http` | `GET /saved-cards` loads named card recipients from those same contact and saved-destination fixtures. |
| `operators` | `operators-http` | `GET /operators` loads mobile prefixes and integer IRR charge limits. |
| `vehicles` | `saved-plates-http` | `GET /saved-plates` loads saved automobile plate choices and form fields. |
| `loans` | `loans-http` | `GET /loans` loads named loans and installment statuses. |

To switch a provider, copy the service file under `local/scratch/`, change the operation's
endpoint to the other mock's loopback URL, and restart the Assistant with that file. Endpoints are
explicit loopback HTTP URLs with a port and path. Startup reports invalid graphs, invalid bindings,
and missing bindings for enabled candidates. A configured lookup that fails or times out produces
a task error in history. Other journeys may still use lookups from the development fixtures.

## Session and bridge calls

Generate one opaque, unpredictable session ID for the demo client and send it as
`X-Assistant-Session` on each request. Use the same ID in the URL for resume and cancel and in
the body for messages. Use a conversation ID for the life of the panel. The first accepted
message binds that session to its `device_id`, which is echoed on bridge commands. The development
backend defaults the session header to `replica-dev` for the existing replica. The standalone
header is a demo session binding for loopback use. Hosted access requires a trusted client identity
at the integration boundary.

| Operation | Success | Rejection or empty state |
|---|---|---|
| `POST /assistant/v1/messages` | `200` task status. The turn runs in the background. | `403` mismatched session. `409` busy turn, wrong device, stale question or unknown option. |
| `GET /assistant/v1/history?conversation_id=…` | `200` history and the current clarification or task state. | `200` empty history for a conversation outside this session or one that has yet to start. |
| `POST /assistant/v1/sessions/{session_id}/cancel` | `200` status for the session's current conversation. Read history for the final state. | `403` mismatched session. `409` when there is no cancellable task or the turn cannot start. |
| `POST /assistant/v1/sessions/{session_id}/resume` | `200` with a command or idle status after recording the bridge observation. | `403` mismatched session. |
| `POST /assistant/v1/results` | `200` idle status after an accepted result or retry. | `409` unknown, undelivered, expired or stale command result. |

All five operations return `422` for a missing session header or an invalid request. Resume and
results own the live observation. The observation attached to a message is validated with the
message body. The process holds session state in memory and runs one agent turn at a time. A
command already delivered when a task is cancelled can still report its result. The bridge stops
before its next command. An undelivered command that expires pauses the task. If a delivered
command expires without a result, the task stops and reports uncertainty. The queue requires a
fresh bridge observation after either expiry.

Feedback, comments, session close, and satisfaction remain in the OpenAPI document as planned
operations. The standalone process returns `404` for them and for the graph viewer and test
harness.

## Repeat the HTTP check

Run from the checkout in the `assistant` environment.

```bash
python -m pytest -s tests/backend/test_standalone_api.py
```

The test starts the standalone CLI on a free loopback port with a separate mock model listener
and the repository's stand-in bridge. It checks the actual HTTP routes against the published
OpenAPI, drives a Persian card-transfer request from clarification to S-176, attempts cross-session
operations and late or stale results, and verifies cancellation. Both listeners stop after the
test. The check runs independently of the emulator, the graph viewer, and the development harness.

Run the Demo 3/8 checks for alternate graph and provider bindings.

```bash
python -m pytest -s tests/backend/test_configured_standalone.py
```

These checks start real mock providers on separate free loopback ports. They write the second graph
and both service files under `local/scratch/`. Each variant uses the same stand-in client and
existing mobile API.

## Demo 2/8 acceptance

The backend code tested on 22 Nov 2026 was `2b490451faaa7e1e4905fbc829a0197a694d1b39`. The
documented startup on port 8101 served OpenAPI with the model and embedding endpoints unset.
The HTTP check reported the following results.

```text
PASS five standalone routes matched OpenAPI and development and planned routes returned 404
PASS messages, history, resume, results and cancel on the standalone process
PASS invalid and stale inputs, session isolation, and review S-176 at 2,500,000 IRR
2 passed in 4.60s
```

The development backend at the same code revision passed `python tools/bridge_e2e.py` with the
existing replica build `06c1386add06`. The smoke stopped at S-176 with source `****3566`, recipient
`****8880`, and 250,000 IRR. S-003 was refused. The 174 project checks, 496 Python tests, and both
Ruff checks passed. See the [demo roadmap](../plan/revised-build-sequence.md#approved-demo-roadmap)
for the remaining steps.

## Demo 3/8 acceptance

The documented example started the Assistant on 8101 and the read-only mock provider on 8110.
The restart test used `app_profiles/replica.profile.json` for A. For B, it copied the graph and
combined the two existing menu taps into one edge, made the payment ID required, and removed card
transfer from the offered features. It used the same stand-in bridge for both runs.

| Run | Backend plan to the internal-transfer form | Endpoint evidence | Review |
|---|---|---|---|
| A | S-026 → S-193 → S-192 | Provider A served `GET /accounts` and `GET /saved-accounts`. The latter supplied recipient `****7800`. | S-173, `account-2`, 3,000,000 IRR |
| B | S-026 → S-192, with payment ID `12345` filled | `GET /accounts` stayed on A. `GET /saved-accounts` moved to B and supplied recipient `****5821`. | S-173, `account-2`, 3,000,000 IRR |

Startup rejected an unknown graph screen, an unsupported operation field, an edge to a user-only
action, an invalid endpoint, a mismatched adapter, and a missing internal-transfer binding.
HTTP 404 and an incompatible response both produced a banking lookup error in history before a
bridge command was sent. The 174 project checks, 505 Python tests, and both Ruff checks passed.

## Demo 4/8 acceptance

On 23 Nov 2026, the standalone backend ran `c4e43aef2ff977ec3663e45619f607284c6870f0`.
The debug replica `0.1` (version code 1) displayed build `c4e43aef2ff9` on the pinned
`ragbot_investigation` emulator. Its executor accepted profile version 1. The generated profile
matched `app_profiles/replica.profile.json`. The setup used the
[standalone replica runbook](../bridge/try-it.md#verify-the-replica-against-standalone).

The QA probe completed `observe` at D-000. The adapted standalone smoke reached S-173 and
verified the rendered review. The graph state route returned 404 on the standalone API.

| Panel turn | Bridge result | Rendered review |
|---|---|---|
| «۳۰۰ هزار تومن از سپرده پس‌انداز به حساب رضا انتقال داخلی بزن بابت اجاره» | `navigate` followed S-026 → S-193 → S-192, `fill_form` succeeded at S-192, and `show_review` reached S-173. | Source `****0579`, recipient `****5821`, 3,000,000 IRR, reason «پرداخت اجاره». |
| «نه، مبلغ رو ۳۲۰ هزار تومن کن» | `navigate` returned from S-173 to S-192, `fill_form` succeeded there, and `show_review` opened a fresh S-173. | Same accounts and reason, 3,200,000 IRR. |

The configured mock returned HTTP 200 on `GET /accounts` and `GET /saved-accounts`. The backend
trace recorded `contacts.account` resolving to `****5821` in the smoke and in both panel turns.
That ending came from the alternate provider. The replica fixture's recipient ends in `7800`.
Cancel Task cleared the standalone task and left the corrected review visible. All bridge runs
stayed within the user review boundary. The local evidence ID is `DEMO-4`.

The relevant device group passed 36 tests across `AssistantPanelTest`, `BridgeJourneysTest`,
and `BridgeServicesTest`. Earlier runs reproduced a blocked picker being reported before its
dismissal finished. The bridge now waits for the picker to close before reporting the result.
The 178 Android JVM tests, 505 Python tests, 174 project checks, and both Ruff checks also passed.
Android lint reported 0 errors and 116 warnings.

## Demo 5/8 candidate journey replay

On 23 Nov 2026, the standalone backend and synthetic provider ran the branch code recorded in
`726f16856064a93758fac14a6996c73293a1ac4f`. The default workflow used profile version 1 from
`app_profiles/replica.profile.json` (Git blob `437b9495872df83593df982a10e3cf5f90fdbf34`) and
`mock_services/service-bindings.example.json` (Git blob `3304489a69135ecd0f5ca74e8661c3eaa77dfa1f`).
The pinned `ragbot_investigation` emulator ran Android 15 and replica 0.1 (version code 1),
built from the merged Demo 4 revision `e874284d9d27` with the standalone bridge address and a
fresh demo session. The replica started with cleared app data, and the provider read the
repository's synthetic fixtures.

The replay sent the following requests through the in-app assistant panel. The backend history
confirmed the screen and final bridge operation. The replica UI tree supplied the actual values.
Identifiers are masked. Each endpoint listed in the matrix returned HTTP 200 during its replay.

| Candidate and panel input | Service endpoints | Expected review fields | Actual review fields and result |
|---|---|---|---|
| Card transfer. «۲۵۰ هزار تومن به رضا کارت به کارت کن», then choose source `****3566`. | `/cards`, `/saved-cards` | S-176, source `****3566`, destination `****8880` (رضا محمدی), 2,500,000 IRR. | **Pass.** S-176 displayed source `****3566`, destination `****8880`, رضا محمدی, and 2,500,000 IRR. The panel waited for a source choice on S-174 before opening review. |
| Internal transfer. «۳۰۰ هزار تومن از سپرده پس‌انداز به حساب رضا انتقال داخلی بزن بابت اجاره». | `/accounts`, `/saved-accounts` | S-173, savings `****0579`, saved recipient `****7800`, 3,000,000 IRR, reason «پرداخت اجاره». | **Pass.** S-173 displayed source `****0579`, recipient `****7800`, 3,000,000 IRR, and «پرداخت اجاره». The corrected review displayed 3,200,000 IRR with the same accounts and reason. |
| Mobile top-up. «از سپرده پس‌انداز برای [شمارهٔ آزمایشی H-079، پایان ۷۴۹۶] شارژ همراه اول ۲۰ هزار تومنی بخر». | `/accounts`, `/operators` | S-212, savings `****0579`, همراه اول, mobile `****7496`, 200,000 IRR. | **Pass.** S-212 displayed savings `****0579`, همراه اول, mobile `****7496`, and 200,000 IRR. |
| Saved-plate pollution inquiry. «عوارض آلودگی هوا», then choose the saved plate ending `67`. | `/saved-plates` | S-257, all four plate fields for synthetic `plate-1` (`** ب *** ایران 67`), and «خودم» selected. This is the inquiry boundary before any fee is shown. | **Pass.** The question waited at S-256. S-257 matched all four saved fields, with «خودم» checked and the inquiry button untouched. |
| Loan installment. «قسط بعدی وام شخصیمو از سپرده کوتاه‌مدت بده». | `/loans`, `/accounts` | S-331, short-term deposit `****3231`, personal loan file `****1287`, guarantee account `****4131`, no withdrawal ID, installment 2 and total 5,000,000 IRR. | **Pass.** S-331 displayed `****3231`, `****1287`, `****4131`, a dash for withdrawal ID, and 5,000,000 IRR for installment 2 and the total. |

| Fault or follow-up | Observed result |
|---|---|
| Ambiguous card source | The panel offered both eligible debit cards, `****3566` and `****4544`. It stayed at S-174 until the user selected `****3566`. |
| Correction and cancellation | «نه، مبلغ رو ۳۲۰ هزار تومن کن» opened a fresh S-173 at 3,200,000 IRR. **لغو کار** cleared the task and history showed `cancelled`. The corrected review remained open. |
| Provider timeout and retry | Pausing the synthetic provider timed out the top-up's `operators` lookup. History reported `failed` with the banking-service message and the queue had no bridge command. After the provider recovered, repeating the request in the same panel reached S-212 with the four expected fields. |
| Bridge disconnect and reconnect | A loopback fault proxy dropped the replica's bridge connections while the panel remained reachable. The undelivered internal-transfer command expired against observation 2 and the task paused. The user opened the transfer menu while disconnected. On reconnect the replica sent observation 3 at S-193. «ادامه» planned from S-193 and reached S-173 with the original accounts, reason, and 3,000,000 IRR. A backend regression check also covers a delivered command with a lost result. It stops the task and requires a fresh poll before another command. |

The five happy paths stopped at `show_review`. The configured provider uses the same synthetic
records as the replica. The replica renders the loan amount and the inquiry boundary from its
fixtures. All five candidates passed the configured replay and the applicable fault checks. The
replay found no remaining defects in these five scenarios. The local evidence ID is `DEMO-5`.
See [the Android replay runbook](../bridge/try-it.md#verify-the-replica-against-standalone) for setup.

The acceptance commands passed with 174 project checks, 511 Python tests, and both Ruff checks.
The development bridge smoke reached S-176 and refused S-003. The standalone bridge smoke reached
S-173 and verified the rendered account transfer. The Android build passed 178 JVM tests and
reported 0 lint errors and 116 warnings.

Record the selected web features and the first web journey in an issue comment before web UI
implementation begins. [Demo 6/8 issue](https://github.com/Hooman1996/RagBot-Assistant/issues/215)

## Demo 6/8 web client

The owner selected internal transfer first, then account statement and card transfer.
[Owner selection](https://github.com/Hooman1996/RagBot-Assistant/issues/215#issuecomment-7102378696)

Start the provider and the web-configured Assistant in separate terminals from the checkout
in the `assistant` environment.

```bash
python -m uvicorn mock_services.app:create_app --factory --host 127.0.0.1 --port 8110
```

```bash
set -a
source .env
set +a
python -m assistant --graph app_profiles/web.profile.json --services mock_services/web-services.json
```

Open [NegahOrbit on loopback](http://127.0.0.1:8101/), select **Agentic Assistant** and click
**Launch Service**. The registry opens `/banking/` in the same tab. The standalone process serves
the checked-in assets, the existing session/bridge API and the browser's read-only display data
on the same origin. Each tab keeps its session, current screen, masked draft and pending bridge
delivery in session storage. Reloading the page resumes polling with that session. The backend
holds conversations in memory. Start a fresh tab after restarting the backend with a new graph.

For a separate static host, serve the contents of `negahorbit-platform/` at the site root and
forward `/assistant/v1/` to the standalone loopback listener. The checked-in Nginx configuration
includes this location. The proxy and Assistant must share the same loopback network namespace.
For example, with the backend on 8101 and a local Nginx installation:

```bash
mkdir -p local/scratch/web-nginx
cat > local/scratch/web-nginx/nginx.conf <<EOF
daemon off;
pid nginx.pid;
error_log error.log;
events {}
http {
  include /etc/nginx/mime.types;
  access_log off;
  client_body_temp_path client_temp;
  proxy_temp_path proxy_temp;
  fastcgi_temp_path fastcgi_temp;
  uwsgi_temp_path uwsgi_temp;
  scgi_temp_path scgi_temp;
  server {
    listen 127.0.0.1:8120;
    root $PWD/negahorbit-platform;
    location /assistant/v1/ { proxy_pass http://127.0.0.1:8101; }
  }
}
EOF
nginx -p "$PWD/local/scratch/web-nginx/" -c nginx.conf
```

Run Nginx as a user with read access to the static directory and write access to the prefix.
Open the portal on 8120. This check verifies JSON forwarding independently of the page.

```bash
curl -fsS -H 'X-Assistant-Session: check' http://127.0.0.1:8120/assistant/v1/web/profile |
  python -c 'import json,sys; print([f["intent"] for f in json.load(sys.stdin)["features"]])'
```

The web graph controls the offered journeys, routes, forms and required fields. The service
file binds accounts, saved account recipients, source cards, saved card recipients and
`statement` to explicit loopback HTTP endpoints. The statement provider accepts an account key
and inclusive Gregorian ISO dates (`YYYY-MM-DD`). Like the replica, the synthetic provider applies
the transaction fixture's day offsets to today and its balance offsets to the selected account.
A successful empty range displays an empty-state message. A failed lookup ends with the provider
error. All amounts and balances are signed integer IRR.

### Browser replay

With the project emulator stopped, run from the checkout using Node, Playwright Core and a
compatible headless Chromium. The recorded run used Playwright Core 1.49.1 and Chromium 131.

```bash
export PLAYWRIGHT_CORE="$PWD/local/toolchain/playwright-core"
export CHROMIUM_PATH="$PWD/local/toolchain/web-browser/chromium"
mkdir -p local/scratch/web-browser
export TMPDIR="$PWD/local/scratch/web-browser"
local/toolchain/node/bin/node tests/browser/web-demo.cjs
python -m pytest tests/backend/test_web_statement.py
```

For the configuration replay, copy the graph, service file, and the alternate provider's
`accounts.json` and `transactions.json` under `local/scratch/demo6/`. In the graph copy
`graph-b.json`, append the edge
`{"from":"S-026","to":"S-192","action":"transfer-internal"}`. In `provider-b/transactions.json`,
set the first statement row's amount to `2100000`. In `services-b.json`, change just the
statement endpoint to `http://127.0.0.1:8111/statement`. Use these commands in separate terminals:

```bash
python -c 'from pathlib import Path; import uvicorn; from mock_services.app import create_app; uvicorn.run(create_app(Path("local/scratch/demo6/provider-b")),host="127.0.0.1",port=8111)'
python -m assistant --port 8102 --graph local/scratch/demo6/graph-b.json --services local/scratch/demo6/services-b.json
```

Point the local static host's proxy at port 8102 and restart it. The same client checks the
alternate route and compares the rendered rows with that provider. To check failure, copy
`services-b.json` to `services-fault.json`, change its statement endpoint path to `/unavailable`
and run the third command in a separate terminal with the same model settings.

```bash
DEMO_URL=http://127.0.0.1:8120 PROVIDER_URL=http://127.0.0.1:8111 SHORT_ROUTE=1 \
  local/toolchain/node/bin/node tests/browser/web-demo.cjs
python -m assistant --port 8103 --graph app_profiles/web.profile.json --services local/scratch/demo6/services-fault.json
DEMO_URL=http://127.0.0.1:8103 EXPECT_FAILURE=1 STATEMENT_ONLY=1 \
  local/toolchain/node/bin/node tests/browser/web-demo.cjs
```

### Evidence for Demo 7/8

Implementation `e18847caad34663ea2b6818ca72520389ac050e3`, graph blob
`64d0e2874c64aa972aaa83c78bce0a0638077c85`, bindings blob
`9241d52ea574304acf2acf1e32c22b76474c6f25`. The 23 November 2026 replay used the development
model, the workflow agent, Chromium and the synthetic providers. Local evidence ID is `DEMO-6`.

| Journey | Browser observation and comparison |
|---|---|
| Internal transfer | S-026 → S-193 → S-192, then S-173. Source `****0579`, recipient `****7800`, 3,000,000 IRR. Correcting the amount displayed 3,200,000 IRR. Cancel ended the task. Graph B went directly from S-026 to S-192. |
| Account statement | «صورتحساب حسابم رو نشون بده» requested the account and range. For savings `****0579`, 16–23 November, W-statement showed 2,000,000, −750,000 and 1,000,000 IRR. Dates, descriptions and balances matched the provider. The pending message was «در حال دریافت صورتحساب» and the rendered result reported «صورتحساب نمایش داده شد». Provider B changed only the first amount to 2,100,000 IRR. A missing endpoint displayed the banking lookup error and no table. |
| Card transfer | The user selected source `****3566`. S-176 displayed that source, recipient `****8880` and exactly 2,500,000 IRR. |

For short-term account `****3231` on 21 November, reloading during a pending statement read
recovered one row with a −750,000 IRR amount and 10,500,000 IRR balance. On 23 November, the
same account showed the empty-state message. The first replay exposed an aborted browser fetch
being recorded as a provider failure on unload. The client now retains the delivered command
through unload and completes it after reload.

The registry upgrade retained an existing service override and eight entries. The browser switched
to the stone theme, retained it and the clarification across reload, and fit a 380 px viewport.
The acceptance run passed 176 project checks, 517 Python tests and both Ruff checks. The Android
debug build, JVM tests and lint passed. Lint reported 0 errors and 116 warnings.

### Interactive banking and personal confirmation (#224)

Launch **Agentic Assistant** from NegahOrbit. In **بانک من**, expand **حساب‌ها و کارت‌های من**
to inspect the configured accounts, linked cards and available IRR balances. Select card transfer,
internal transfer or statement to use the form yourself, or use **گفت‌وگو با دستیار** to prepare
the same review by conversation. A review receives keyboard focus and moves into view on mobile.
Use **ویرایش انتقال** to change a prepared draft.

**تأیید انتقال آزمایشی** posts a synthetic debit only on a direct user interaction with an eligible
review. Insufficient or unavailable funds, an unknown recipient and a cancelled draft prevent
posting. The receipt shows the recipient, masked endpoints, date, amount, balance and reference.
Open the source account's statement for today to inspect its debit. The tab stores its receipts
and ledger in session storage alongside the existing bridge session. Reload retains them.
A new independent browser session starts from the configured provider balances. The provider
continues serving read-only fixtures. Mock confirmation uses a personal button press with no
bank credentials. The bridge still supports observe, navigate, fill_form and show_review.

With the same browser environment and services as the Demo 6 replay, run:

```bash
local/toolchain/node/bin/node tests/browser/interactive-bank.cjs
local/toolchain/node/bin/node tests/browser/web-demo.cjs
```

The interactive replay checks the 500,000 toman request from card ending 4544 against its
3,750,000 IRR linked account balance. At 380 × 780 the review takes focus, the 5,000,000 IRR amount
is in the viewport, and the 1,250,000 IRR shortfall prevents confirmation. It also checks the
desktop manual forms, editing and cancellation, two direct user confirmations, receipt reload,
the resulting balance and both statement debits.
