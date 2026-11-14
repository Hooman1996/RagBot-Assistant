# The assistant panel

The panel is where the user talks to the agent ([the agent](agent.md)) from inside the app. It is
plan §8's "Chatbot and assistant UI": open, minimize, reopen, cancel, clarification choices and
progress. Built in #144 for the replica. **In production the bank's team builds the same panel in
their app** ([the handover](../app-team-handover.md#deliverable-5-the-assistant-panel)), the way they
build the four bridge operations; the backend side is ours and stays as it is.

Nothing is stored: a conversation lives in the backend's memory and in the app's memory, and is
gone when either restarts. There are no RAG answers yet: a knowledge question gets the fixed
message (P6-06).

## What the user sees

| Control | What it does |
|---|---|
| **دستیار** in the support menu, beside **چت بات** | Opens the panel. The chatbot entry stays the chatbot. Shown only when the bridge loop is on (a debug build with the flag file). |
| The text box and **ارسال** | Sends a request, an answer or a correction. |
| An option button | Answers the pending question with that option: a card, an account, a choice's label, or «لغو». |
| The state line | The task state in Persian («منتظر پاسخ شما», «در حال آماده‌سازی در برنامه», «آماده بازبینی شما»…). While the agent works it says «در حال انجام…» and, after a run, lists each command, its screen and how it ended. |
| **کوچک کردن** (minimize) | Hides the panel. The conversation and any pending question stay; the agent keeps working while the app is open. A **دستیار** bubble with the task state stays on screen. |
| The bubble | Reopens the panel and reloads the conversation from the backend. |
| **لغو کار** (cancel task) | A separate, red control, shown only while the task can be cancelled. It stops a running task before its next command, or cancels a question, a review or a pause. A command already sent is never undone. |

The panel never confirms, submits or pays. The agent stops on the app's review; only the user's
own tap on the app's confirm goes further.

## Why it is built this way

The executor reads and taps the screen through the semantics tree of the app's **top window**, and
treats any second window as an open dialog (`modal`). So the panel:

- **sits inside the app's own window**, over the screen, never in a dialog, a popup or a bottom
  sheet window, so the executor still sees and taps the screen behind it;
- **catches no Back press**, because the executor itself presses Back to move between pages;
- **shows no spinner**: an indeterminate progress indicator reads as `loading`, which the executor
  waits out;
- uses tags that start with `assistant-`, which no profile edge or field uses, so the executor never
  taps the panel.

`AssistantPanelTest` proves the first three on the emulator: with the panel open, the observation has
no dialog and no loading, and the executor reaches the card-transfer review behind it.

## The calls

All three are in `contracts/openapi/assistant_mobile.json`; the contract did not change.

| Call | Body or query | Answer |
|---|---|---|
| `POST /assistant/v1/messages` | `conversation_request.schema.json`: the app's conversation ID, the session and device, the synthetic user, **exactly one of** `message` or `selection` (`question_id` and the option's `ref`), and the app's current observation | A `status` response at once. The turn runs on a background thread. 409 `turn_in_progress`, `run_in_progress`, `unknown_option` or `stale_question`; 422 for a body outside the contract |
| `GET /assistant/v1/history?conversation_id=` | — | `{conversation_id, busy, messages: [{from, text}], current, steps: [{operation, screen, outcome}]}`. `current` is a contract response: `status` (the live task state while a turn runs), `clarification` (question ID, text, options) or `null` |
| `POST /assistant/v1/sessions/{session_id}/cancel` | — | The task's `status`. 409 `nothing_to_cancel` |

The contract leaves the history's body open, so its shape is written here. The observation in a
message is checked against its schema but not stored: the bridge loop owns the device's
observation, and its version must stay the loop's own. The panel takes it from
`CommandPoller.lastObservation`.

The graph page's chat box, every panel conversation and a graph-page Run share one device, so only
one of them runs at a time (`agent_chat.Chats.busy`).

## Try it

Follow [try it](try-it.md) to step 6, then on the replica: open the support menu, tap **دستیار**,
and write «۲۵۰ هزار تومان به رضا کارت به کارت کن». Pick a card from the options, tap **کوچک کردن**
while the replica moves, and reopen it from the bubble once it says «آماده بازبینی شما». Then write
«نه، مبلغ رو ۳۰۰ هزار تومن کن» for a fresh review, or tap **لغو کار**.

Checked on 14 Nov 2026 on the emulator against the live backend, Gemma and TEI: the question came
with the 7 cards and «لغو»; minimize and reopen kept it; the option prepared review S-176 while the
panel was minimized; the correction prepared a second review with 3,000,000 rials; cancel ended
the task.
