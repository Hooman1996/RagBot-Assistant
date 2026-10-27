# P4: Mock Domain and Manual Replica

**Goal:** the replica's transfer journey works by hand, from home to review, against stateful
mock banking services with synthetic data.
**Gate (plan §17):** home-to-review works manually with synthetic data, the draft and version
rules pass, a scoped behaviour and fidelity report exists, and no production banking calls are made.

| Task | What it will do |
|---|---|
| P4-01 | Finalize the contracts with the P1a field mappings, and generate the Python and mobile transport models |
| P4-02 | `mock_bank` migrations and seed fixtures: users, accounts, cards, beneficiaries (one رضا, two رضاs), destinations |
| P4-03 | Mock banking API: cards, recipient search, destinations, draft create/update/read with versions, validate/review, cancel |
| P4-04 | User-only mock submit and receipt API, kept separate from anything the assistant can reach |
| P4-05 | Replica project in the toolkit chosen in P1a, with its own package ID and development endpoints only |
| P4-06 | Replica screens for the journey: Persian, RTL, validation, dialogs, loading, Back |
| P4-07 | Chatbot menu with separate Chatbot and Assistant entries (the assistant panel is a shell for now) |
| P4-08 | Fidelity method: fixed emulator profile, comparison regions, masks and tolerances. The first behaviour and visual report. |
| P4-09 | CI: build the replica and run its instrumentation tests |

These tasks depend on the P1a toolkit decision, so their wording will change once P1a is done.
