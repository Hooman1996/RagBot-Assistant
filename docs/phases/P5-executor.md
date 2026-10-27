# P5: Executor and Deterministic Demo

**Goal:** the in-app executor can observe, navigate and fill the transfer form through the
app's normal handlers, driven by a scripted (non-LLM) command sequence. **This is the first
internal demo.**
**Gate (plan §17):** a non-LLM script completes home-to-review from several starting screens,
the visible UI and the stored draft agree, and final authorization is unreachable through
assistant operations.

| Task | What it will do |
|---|---|
| P5-01 | Observation reporting: screen, modal and loading state, relevant fields, observation version |
| P5-02 | Semantic operations: navigate to a registered destination, fill supported fields, select a candidate, show the review |
| P5-03 | Route execution from the versioned graph, with per-edge verification and a stop when user input is needed |
| P5-04 | Backend command loop: issue one command, receive the result plus observation, reconcile |
| P5-05 | Command journal and deduplication. Reject stale, expired and cancelled commands. Handle manual edits. |
| P5-06 | Policy tests: no assistant operation, alias or deep link reaches the user-only confirm handler |
| P5-07 | Deterministic demo script and an Appium black-box check of the visible values |
