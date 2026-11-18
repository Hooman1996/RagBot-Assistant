# The ReAct agent

The ReAct agent sits beside [the workflow agent](agent.md) and is chosen with
`ASSISTANT_AGENT=react`. The default is `workflow`. The decisions are
[D-032](../decisions/0032-react-agent-beside-the-workflow.md) and
[D-033](../decisions/0033-react-uses-the-workflows-feature-choice.md).

In the workflow, code decides every step, and the model only fills three JSON forms. In the ReAct
agent, code first understands the request with the workflow's own chooser (D-033). Then, for the
chosen task, the model decides the next step itself: on each step it returns a thought and one tool
call, sees the result, and this repeats. Every act still goes through the workflow's own checks,
every question the user sees is code's, and nothing can go past the review: only the user
confirms.

## Run it

| Where | How |
|---|---|
| The backend (panel and graph page) | `set -a; source .env; set +a; ASSISTANT_AGENT=react python -m assistant` |
| One process, stand-in app | `python main_debug.py --agent react "۲۵۰ هزار تومان به رضا کارت به کارت کن" 1` |
| One process, the replica | `python main_debug.py --replica --agent react` |
| The evaluation | `python tools/evaluate_agent.py tests/evaluation/tuning.json --agent react` |

With `ASSISTANT_TRACE=1`, each step prints as `think` (the thought, the tool and its arguments)
and `act` (its result).

## The loop

One LangGraph `StateGraph` (`assistant/react_graph.py`):

```mermaid
flowchart LR
    start([user's words]) --> begin
    begin -->|"a task at its review, or paused"| follow_up
    begin -->|a new request| understand
    follow_up -->|correction or resume| think
    follow_up -->|new request| understand
    follow_up -->|cancel| stop([end])
    understand -->|"greeting, thanks, «چیکار میتونی بکنی؟», knowledge question: fixed reply"| stop
    understand -->|not sure| not_sure
    understand -->|a feature| plan
    not_sure -->|"interrupt(): a picked feature"| plan
    not_sure -->|new words| understand
    not_sure -->|cancel| stop
    plan -->|"a page to see: opened at once"| stop
    plan --> think
    think -->|"{thought, tool, arguments}"| act
    think -->|model failed, or 12 tool calls| stop
    act -->|short result| think
    act -->|"ask_user before the app moved: move_app, then the question"| act
    act -->|"ask_user: interrupt(), then the answer"| think
    act -->|review reached, failed, cancelled or paused| stop
```

- **`begin`** starts each new message with an empty list of steps.
- **`understand`** is code. A whole message that is a greeting, thanks or a question about what the
  assistant does (short fixed lists) gets a fixed reply with example requests. Otherwise the
  workflow's `understand` runs: TEI's closest 8, then the workflow's chooser. A knowledge question
  gets the workflow's fixed reply. It runs only for a new request and for new words at the not-sure
  question, never for an answer to a task's question: «پس‌انداز», typed as an answer, would
  otherwise be read as `account_opening`.
- **`not_sure`** asks «منظورتان کدام است؟» with the 4 closest features and «لغو» (owner, 18 Nov 2026).
- **`plan`** plans the chosen feature with the workflow's `plan`. A feature with nothing for the model
  to set, such as «خانه», is opened at once with no model step.
- **`think`** sends the model one prompt. It holds:
  - the rules;
  - the tools that can be used now;
  - the user's messages, with long numbers masked;
  - the task: its feature, the fields the model may set (each one's Persian question and words, and a
    choice's labels, never its keys), the values held as masked labels, and what is missing;
  - this turn's steps, each as call → result.

  The model answers through `Chat.step`. Its schema has one `anyOf` branch per available tool, so
  the server holds the tool's name and the shape of its arguments. A tool without arguments has no
  `arguments` key, because a required empty object made the model pad the answer with whitespace
  until it ran out of tokens.
- **`act`** runs the tool in code and keeps a short JSON result with the step.
- **`follow_up`** runs for a message after the review or while the task is paused. It uses the
  workflow's own `Lifecycle.follow_up`: cancel, resume, correction or new request. A correction is
  read again by the workflow's reader (`slots.extract`), as in the workflow, and then settled.

The state is the workflow's `AgentState` plus `steps`. The chat box, the panel, `main_debug.py` and
the evaluator read both agents the same way.

## The tools

What is offered depends on the task's state:

| State | Tools |
|---|---|
| Something missing | the read tools, `set_values`, `move_app`, `ask_user` |
| Nothing missing | the read tools, `set_values`, `open_review` |
| The app waits on a question's page | `current_screen`, `ask_user` |

The read tools are `find_records` (only for a task with record fields) and `current_screen`. No
tool searches, picks or changes a feature, and there is no `finish`: the task ends only at its
review, a cancel or a failure.

| Tool | What code does |
|---|---|
| `find_records(ref, words)` | Returns the records the app's picker lists for that field that match the words, as masked labels (at most 8). |
| `current_screen()` | Returns the page by its Persian name (Home, a feature's page or review) and the values given there. Never a screen ID. |
| `set_values(values)` | The workflow's own reader (`slots.extract`) decides every missing field, and what it finds nothing for is asked; the model's value counts only for a value held before (a correction). Then the workflow's `check` and `resolve` run. Returns what was kept, what was refused (and why), and what is missing. |
| `move_app()` | Runs the workflow's `advance`: the app goes as far as the values allow and waits on the page of the first missing value ([D-029](../decisions/0029-asked-on-the-screen.md)). |
| `open_review()` | Runs the workflow's `prepare`, `verify` and `ready_for_user`. The reply is the workflow's list of prepared values. An uncertain command is planned again once, with the workflow's `reconcile`. |
| `ask_user(question)` | The question is the workflow's and the model's words are not used. It holds every missing question of the page the app waits on, with the records (2 to 8) or allowed choices as options, and «لغو». A picked option, or a typed answer naming one listed record, sets that value; other words go back to the model as an answer, never as a new request. |

There is no confirm, submit or pay tool. The app is reached only through `TaskRunner`, so the
only commands are `navigate`, `fill_form` and `show_review`.

## Guards, in code, whatever the model says

- **Values** pass the workflow's own checks:
  - money only as the user wrote it;
  - a choice only as one of its allowed keys;
  - a record only as the app lists it;
  - amount limits, installments and saved plates as today.
- **A record must be the user's.** `set_values` refuses a record whose words are not in the user's
  messages, so the model cannot pick one it only read in `find_records`. A record of generic words
  only («حساب») names nothing and is asked. Only the user picks a record or a saved plate the user
  didn't name.
- **The feature is the workflow's choice,** locked for the task. A vague request gets options, never
  an action.
- **No word of the user is lost.** If the model calls `move_app`, `ask_user` or `open_review`
  before `set_values` has read the newest words, the workflow's reader reads them first.
- **The app moves before a question.** If the model asks while the app has not moved, `act` runs
  `move_app` as a separate step first. When the answer resumes the question, the move is not
  sent again.
- **A task ends only at its review, a cancel or a failure.** There is no `finish`.
- **Nothing is shown in full.** The user's words are masked as in the workflow, and every tool
  result and every prompt goes through `steps.masked`. The model never sees an app key or a
  screen ID.
- **The same call twice in a row is not run again**: the model is told so (`REPEATED`), except `ask_user`.
- **A turn has at most 12 tool calls,** counted from the user's last answer. Then the task fails
  with a plain message and nothing more is sent.
- **A model error ends the task as `failed`,** as in the workflow.

## The steps in the panel

The panel's history and the graph page's chat both carry `agent_steps`, built by
`panel_api.agent_steps`:

- **The ReAct agent:** each step's Persian thought, its tool, and the tool's result as one masked
  line, cut to 160 characters.
- **The workflow:** each node that ran for the request, with a short Persian description and the
  task state after it, read from the checkpointer's history.

Both lists grow while the turn runs. In the panel, «مراحل دستیار (n)» under the messages opens and
closes them; each step shows its thought, then `action → result` left to right. On the graph page
they sit under the chat box in a collapsed «مراحل دستیار» section. The contract leaves the
history's body open (`docs/bridge/panel.md`), so `contracts/` does not change.

## Status

| Step | Issue | What |
|---|---|---|
| RA-0 | #166 | This note and D-032 |
| RA-1 | #167 | The switch and the loop with `ask_user` and `finish` |
| RA-2 | #168 | The read tools |
| RA-3 | #169 | The act tools, to the review |
| RA-4 | #170 | The steps shown in the panel |
| RA-5 | #171 | Both agents evaluated side by side |
| RA-6 | #172 | Native tool calls (blocked) |
| RA-7 | #182 | The workflow's feature choice first (D-033) |
