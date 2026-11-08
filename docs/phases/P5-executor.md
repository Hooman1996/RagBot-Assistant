# P5: Executor and Deterministic Demo

**Goal:** the in-app bridge can observe, navigate and fill the transfer form through the app's
normal handlers, driven by the scripted (non-LLM) task runner in the backend. **This is the first
internal demo.** D-023 moved route finding and requirement checking into Python.
**Gate (plan §17):** a non-LLM script completes home-to-review from several starting screens,
the visible UI and the stored draft agree, and final authorization is unreachable through
assistant operations.

| Task | What it will do |
|---|---|
| P5-01 | Observation reporting: screen, modal and loading state, relevant fields, observation version |
| P5-02 | The four bridge operations: observe, navigate to a screen with its arguments, fill a form by field tags, show the review |
| P5-03 | Route execution in the backend: `assistant/navigator.py` plans the route and its requirements, and `assistant/task_runner.py` runs it, checking each page from the observation and stopping with a reason on any mismatch |
| P5-04 | Backend command loop: issue one command, receive the result plus observation, reconcile |
| P5-05 | Command journal and deduplication. Reject stale, expired and cancelled commands. Handle manual edits. |
| P5-06 | Policy tests: no assistant operation, alias or deep link reaches the user-only confirm handler |
| P5-07 | Deterministic demo script and an Appium black-box check of the visible values |
