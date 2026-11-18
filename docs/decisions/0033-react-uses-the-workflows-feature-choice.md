# D-033: The ReAct agent uses the workflow's feature choice

**Source:** Owner, 18 Nov 2026 (issue #182). The owner approved the issue and took the recommended
choices: "Approve #182, use your recommendations". It revises
[D-032](0032-react-agent-beside-the-workflow.md), items 3 and 4.

## Decision

1. **Code understands a new request before the model's loop.** A message that starts a new
   request, or new words typed at the not-sure question, goes through these steps:
   - **A greeting, thanks, or a question about what the assistant does.** These are whole
     messages from short fixed lists. Each gets one fixed Persian reply, with no model call. The
     greeting and the help text show example requests (choices A1 and D2).
   - **Otherwise, the workflow's own `understand`.** TEI narrows the features to the closest 8, and
     the workflow's chooser picks one of them, `not_sure` or `knowledge_question`.
     - **`not_sure`:** the workflow's question «منظورتان کدام است؟», with the 4 closest features and
       «لغو» (choice B1). A picked feature is planned, new words are understood again, and «لغو»
       cancels.
     - **`knowledge_question`:** the workflow's fixed reply.
     - **A feature:** it is planned with the workflow's `plan`. A feature with nothing for the model
       to set, such as a page to see, is opened at once.
2. **The feature is locked for the task.** `find_features`, `feature_requirements` and `finish` are
   removed. The model gets the task's fields in its prompt and works only on that task, with
   `find_records`, `current_screen`, `set_values`, `move_app`, `ask_user` and `open_review`.
3. **Every question the user sees is code's.** The model never words a question or a reply.
4. **The chooser never sees an answer to a task's question.** It is used only as in item 1.
   «پس‌انداز», typed as an answer, is the account. The chooser alone would call it
   `account_opening`.
5. **A new request in the middle of a question is read as an answer** (choice C1), as in the
   workflow. The user cancels with «لغو» first.
6. **Values: a missing field is the workflow's reader's alone.** `set_values` takes the workflow's
   reading (`slots.extract`) for every missing field, and what it finds nothing for is asked. The
   model's value counts only for a value held before (a correction).
7. **A record must name something.** A record value made only of generic words («حساب») is refused
   and asked. The shared lookup would otherwise match it to the one label that holds the word.

Everything else in D-032 holds: the switch, the step budget, the guards, the branching, and
`workflow` as the default.

## Why

RA-5 found 3 / 145 critical wrong actions for the ReAct agent on held-out. All three were features
the model picked itself. The workflow's chooser had none on the same cases. With the chooser in
code, the user also always gets clear options for a vague request, and a friendly line for a hello.
And the agent makes fewer model calls before the first question.

## The fresh run

See [RA-7 in the report](../reports/react-vs-workflow.md#ra-7-the-workflows-feature-choice).
