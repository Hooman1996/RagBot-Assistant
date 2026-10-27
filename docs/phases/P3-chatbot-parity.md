# P3: Chatbot Parity

**Goal:** the assistant keeps everything the mobile chatbot does today, in its own sessions,
plus an isolated Chatbot compatibility path for the replica's Chatbot entry.
**Gate (plan §17):** the six mobile-operation behaviors are tested, follow-up and language cases
are tested, and each exception is documented and accepted by the managers.

| Task | What it will do |
|---|---|
| P3-01 | Parity inventory: for talk, history, feedback, comment, session close and satisfaction, plus citations, streaming, attachments and language behaviour, mark each as retained, absent, adapted or deferred |
| P3-02 | Assistant conversation namespace (sessions, messages, feedback) in the `assistant` schema |
| P3-03 | Isolated Chatbot compatibility API: the same behaviour with no tools, and sessions kept separate from Assistant sessions |
| P3-04 | Behaviour tests per operation, follow-up context tests and Persian language cases |
| P3-05 | Parity report with every exception and its reason, for manager acceptance |
