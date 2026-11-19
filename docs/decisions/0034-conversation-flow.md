# D-034: Every message is understood in the context of the conversation

**Source:** the owner, 18–19 Nov 2026 (issues #185 to #188): "if i change my mind mid request … shouldnt
there be a way that my message again goes through the orchestrator, it sees, even rewrites the users
query based on the history, and then … chooses". The owner approved the plan and these choices on
19 Nov 2026.

## Decision

1. **One entry point for every message.** A button tap, a new message, and an answer to a waiting
   question all go through the same steps:
   - the safety fast path, in code;
   - one turn-router model call, which sees the last 4 turns, the open task and the pending
     question;
   - code, which acts on the router's answer.

   The router's answer is held to a schema. Its kinds are `answer`, `correction`, `new_task`,
   `chat`, `only_looking` and `cancel`, and it returns a standalone request (query rewriting) and
   any second task.
2. **The router decides; code acts.** The rewritten request feeds only the feature detector and the
   new task's first message. A rewrite guard drops it when it holds an amount, a number or a
   record's name the user never wrote. Values still come only from the user's own words, through
   the workflow's reader and checks.
3. **The safety fast path runs before any model.**
   - Cancel words cancel, and the app stays where it is.
   - A typed request to confirm or pay gets a fixed refusal: the final confirm is only by touch, on
     the review page.
   - A typed password, PIN, CVV2 or OTP is refused and replaced in the history, so no model sees it.
4. **Switching tasks is confirmed once.** A new task while another is open, at a question, at its
   review, or paused, asks «… لغو شود و … شروع شود؟». «بله» cancels the old task and starts the new
   one from where the app is.
5. **Chat is the model's own short reply** (owner). Greetings, thanks, small talk, requests the app
   cannot do, and rude words are one model call. Code masks the reply, replaces it when it holds a
   number or claims a payment, and asks the pending question again afterwards.
6. **The rest is today's.** The feature detector, planner, value reader, navigator, question,
   prepare and verify, pause and reconcile are reused unchanged. Not sure now offers the 4 closest
   features. The router replaces the after-the-review follow-up call.
7. **Out of this work:**
   - RAG and knowledge answers: a knowledge question gets the chat reply.
   - Surviving a backend restart: the state stays in memory.
   - The ReAct agent.

   The workflow agent comes first, and `workflow` stays the default.
8. **Tests come first.** A frozen multi-turn conversation set with an expected outcome per turn is
   written before the code changes, and today's workflow is measured on it (CF-1, D-035).

## Why

Today the agent checks what the user wants only at fixed points. A change of mind, small talk in
the middle of a task, a reference to an earlier request, two tasks in one message, or a typed PIN
are all read as an answer, or dropped.

## Design

See [the conversation flow](../bridge/conversation-flow.md), which gives each node's title, inputs,
schema, prompt, fallback and guards, and the state.
