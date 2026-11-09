# Debugging a task step by step

This page shows how to follow one task, card to رضا, line by line. You can do it in three ways:
read the numbered prints, step through it under a debugger with the tests' fake app, or step
through it with the real replica on the emulator. Everything runs on the server, in
`/root/projects/assistant`, with `conda activate assistant`. You reach it from Windows with VS Code
Remote SSH.

Two environment variables control it. Both are off by default.

| Variable | What it does |
|---|---|
| `ASSISTANT_TRACE=1` | Prints a numbered block for every step: the plan, each value resolved, each command queued, handed over and answered, the field check, and how the run ended. Every run of 12 or more digits is printed as `****` and its last 4 digits. The code is `assistant/steps.py`. |
| `ASSISTANT_DEBUG=1` | Keeps a command alive for one hour instead of 30 s, and makes the backend wait one hour for its result instead of 40 s, so a paused breakpoint does not expire it. Read once, when the backend starts. |

**`debugpy` is not installed in the `assistant` env.** You do not need it for VS Code: the
Python Debugger extension that VS Code Remote SSH installs on the server brings its own copy
(`~/.vscode-server/extensions/ms-python.debugpy-*/bundled/libs/debugpy`). PyCharm uses its own
debugger too. Do not install anything into `faq`.

## 1. Prints only

Without the replica, on the tests' fake app:

```bash
cd /root/projects/assistant
conda activate assistant
ASSISTANT_TRACE=1 python -m pytest tests/backend/test_task_runner.py -k card_to_reza -s
```

`-s` lets the prints through. You see:

```text
── 1 · plan ────────────────────────────────────
   target         S-174
   route          S-026 → S-174 (menu-card-transfer)
   requirements   ["card-transfer-source", "card-transfer-destination", "card-transfer-amount"]
── 2 · resolve value ───────────────────────────
   field    card-transfer-source
   lookup   cards
   said     card-1
   sends    card-1
── 3 · resolve value ───────────────────────────
   field    card-transfer-destination
   lookup   contacts.card
   said     رضا
   sends    ****0091
── 4 · send navigate ───────────────────────────
   arguments   {"destination": "S-174"}
── 5 · answer to navigate ──────────────────────
   outcome   succeeded
   screen    S-174
   fields    []
── 6 · send fill_form ──────────────────────────
   arguments   {"form": "S-174", "values": {"card-transfer-source": "card-1", "card-transfer-destination": "****0091", "card-transfer-amount": {"value": 250000, "unit": "IRR"}}}
   ...
── 11 · run ended ──────────────────────────────
   state   review
```

With the replica, start the backend this way instead of step 2 of [try it](../bridge/try-it.md),
then click `S-174` on the graph page:

```bash
ASSISTANT_TRACE=1 python -m assistant
```

Now the command loop prints as well. One `navigate` looks like this:

```text
── 6 · send navigate ───────────────────────────
   arguments   {"destination": "S-174"}
── 7 · command queued ──────────────────────────
   command_id                     2b4e56ac
   operation                      navigate
   arguments                      {"destination": "S-174"}
   expected_observation_version   2
   expires_at                     2026-11-09T10:52:46Z
── 8 · command handed to the app ───────────────
   command_id            2b4e56ac
   observation_version   2
── 9 · answer to navigate ──────────────────────
   outcome   succeeded
   screen    S-174
   fields    [{"ref": "card-transfer-source", "value": "انتخاب کارت"}, {"ref": "card-transfer-destination", "value": ""}, {"ref": "card-transfer-amount", "value": ""}]
```

When the backend closes a command itself, you see `command closed here` with `rejected_stale`,
`rejected_expired` or `uncertain`. The numbers keep counting up while the backend runs.

## 2. Debugger without the replica

`FakeApp` in `tests/backend/test_task_runner.py` stands in for the replica. It moves where it is
told and shows what it is given, so the whole runner runs with no emulator and no web server.

**VS Code.** Open the folder `/root/projects/assistant` over Remote SSH. Choose the interpreter
`/root/miniconda3/envs/assistant/bin/python` (Ctrl+Shift+P, "Python: Select Interpreter"). Create
`.vscode/launch.json` with the two entries below (`.vscode/` is git-ignored, so it is given here as
text). Pick "Card to رضا on the fake app" in the Run and Debug view and press F5.

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "name": "Card to رضا on the fake app",
      "type": "debugpy",
      "request": "launch",
      "module": "pytest",
      "args": ["tests/backend/test_task_runner.py", "-k", "card_to_reza", "-s"],
      "cwd": "${workspaceFolder}",
      "justMyCode": true,
      "env": { "ASSISTANT_TRACE": "1" }
    },
    {
      "name": "Backend for the replica",
      "type": "debugpy",
      "request": "launch",
      "module": "assistant",
      "cwd": "${workspaceFolder}",
      "justMyCode": true,
      "env": { "ASSISTANT_TRACE": "1", "ASSISTANT_DEBUG": "1" }
    }
  ]
}
```

**PyCharm (Professional, SSH interpreter).** Add the server's
`/root/miniconda3/envs/assistant/bin/python` as an SSH interpreter, with
`/root/projects/assistant` as the remote project folder. Then Run → Edit Configurations → + →
pytest:

- Target: Script path `tests/backend/test_task_runner.py`
- Keywords: `card_to_reza`
- Additional arguments: `-s`
- Environment variables: `ASSISTANT_TRACE=1`
- Working directory: `/root/projects/assistant`

Start it with the Debug button. For the backend, add a Python configuration with Module name
`assistant` and `ASSISTANT_TRACE=1;ASSISTANT_DEBUG=1`.

Start with a breakpoint in `TaskRunner.run` (`assistant/task_runner.py`) and step in from there.

## 3. Debugger with the replica

1. Stop any backend that is already running on port 8100 (Ctrl-C in its terminal).
2. In VS Code, run "Backend for the replica" from section 2. It starts `python -m assistant` with
   both variables, under the debugger.
3. Follow [try it](../bridge/try-it.md) from step 3 (install the replica and turn its loop on) to
   step 6 (open the graph and the replica side by side). Skip its step 2: the debugger now runs the
   backend.
4. Put a breakpoint, for example in `TaskRunner.move`.
5. On the graph page, click `S-174`, press **Fill with demo values**, then **Run**.

While the debugger is paused, **every thread is paused, the web server too**. The app's polls then
fail with "Backend not reachable", and it tries again every 500 ms. The graph page's side panel also
stops updating while you are paused. Because of `ASSISTANT_DEBUG=1`, a command waits up to an hour,
so it is not expired when you come back.

**Known limit (found on 9 Nov 2026, open in issue #134): with the replica connected, keep a backend
pause under 5 seconds.** Each poll the app sends during a pause waits 5 s (`BackendClient`'s read
timeout), then the app gives up on it. The backend still holds those requests. When you press
Continue, it answers them, and one of them can take the next command. The app never receives it,
the backend counts it as delivered, and the run waits until the command expires (an hour with
`ASSISTANT_DEBUG=1`). Tested with a breakpoint in `TaskRunner.move`: a 3 s pause reached the
review; a 70 s pause hung, twice. Restart the backend to clear a hung run. Long pauses work with
the fake app (section 2), where no polls are involved.

Check which version you are running: the graph page shows the revision, and it should equal
`git rev-parse --short HEAD`.

## 4. The Kotlin side

**Logs.** The app writes one line per command it receives and one per result it posts:

```bash
source android/env.sh
adb -s emulator-5554 logcat -s RagBotBridge
```

```text
I RagBotBridge: command 3f9c1a2b: navigate S-174
I RagBotBridge: result 3f9c1a2b: succeeded on S-174 trace S-026→S-174
I RagBotBridge: command 8d02e1f4: fill_form S-174
I RagBotBridge: result 8d02e1f4: succeeded on S-174
```

The lines come from `BackendClient.kt`. They never hold a field's value.

**Debugger.** The emulator runs on the server, so first make it reachable from Windows. In
PowerShell, keep this open (`emulator-5554` answers `adb` on port 5555):

```powershell
ssh -N -L 127.0.0.1:5555:127.0.0.1:5555 YOUR_SSH_HOST
```

Then, in a second window, `adb connect 127.0.0.1:5555`. Open the `android/` folder in Android
Studio, and choose Run → Attach Debugger to Android Process → `org.ragbot.replica`. Good first breakpoints are `CommandPoller.pollOnce` and `Executor.execute`.

`Actions` waits up to 3 s for each control (`patienceMs`). If you pause inside the app for longer,
that step can end as `blocked`, and the backend then stops the run with the app's reason. That is
expected while debugging; run the task again.

## 5. Where to put breakpoints

The card-to-رضا journey, in the order it runs. File paths are under `assistant/` unless they end
in `.kt`, which are in `android/app/src/main/java/org/ragbot/replica/bridge/`.

| # | Step | File and function | What you see there |
|---|---|---|---|
| 1 | You click `S-174` | `graph_page.py` `plan` | The screen ID the page asked about |
| 2 | Pick the feature and plan | `graph_tasks.py` `plan_for` | The current screen, and the feature with a form that ends on `S-174` |
| 3 | Find the route | `navigator.py` `route`, `search` | The edges from Home; the route `S-026 → S-174` |
| 4 | List what it needs | `navigator.py` `legs`, `fields_of` | One leg on `S-174` with three fields: source, destination, amount |
| 5 | Press Run | `graph_tasks.py` `start` | The values you typed; the `TaskRunner` and its `BridgeLink` being made |
| 6 | Prepare every value | `task_runner.py` `prepared`, `prepared_value` | `250000` becoming `{"value": 250000, "unit": "IRR"}` |
| 7 | Find رضا's card | `lookups.py` `resolve` | `رضا` becoming `0000000000000091` |
| 8 | Go to the form | `task_runner.py` `move` | The `navigate` arguments, `{"destination": "S-174"}` |
| 9 | Build the command | `command_queue.py` `queue_command` | The full command, its `expected_observation_version` and `expires_at` |
| 10 | Wait for the answer | `bridge_link.py` `wait_for` | The loop checking `last_result` every 0.1 s |
| 11 | The app polls | `CommandPoller.kt` `pollOnce` | The observation it is about to send |
| 12 | The backend hands it over | `mobile_api.py` `resume`, then `command_queue.py` `next_command` | The app's observation and the command returned to it |
| 13 | The app runs it | `Executor.kt` `execute`, then `Navigator.kt` `navigate` | The checks before the screen is touched, then each edge it walks |
| 14 | The result comes back | `mobile_api.py` `post_result`, then `command_queue.py` `record_result` | The result with its fresh observation |
| 15 | The runner reads the answer | `task_runner.py` `command` | Outcome, screen and fields of the result |
| 16 | Fill and check | `task_runner.py` `fill`, `shows` | The values sent, and what the screen shows (`****0091`, `250,000`) |
| 17 | Open the review and stop | `task_runner.py` `show_review` | The run ending in `review`. The confirm is the user's alone |

`resume` and `post_result` are defined inside `build_mobile_router`; a breakpoint on their first
line works as usual.
