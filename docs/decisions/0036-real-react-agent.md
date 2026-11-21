# D-036: A real ReAct agent: the model handles the conversation with tools

**Source:** Owner, 21 Nov 2026 (issue #198). The owner asked: "Lets go with real ReAct so we really
have one, it wouldnt change how the workflow works, so add the tools the ReAct needs, it may have
some wrong actions but it is the nature of the system but we will have to optimize its performance
eitherway." It revises [D-033](0033-react-uses-the-workflows-feature-choice.md) items 1, 2, 3 and 5,
and [D-034](0034-conversation-flow.md) item 7, for the ReAct agent only.

## Decision

1. **The model reads every message.** In the ReAct agent, code no longer works out what a new
   message means. The model gets it with the recent conversation (CF-2's last 8 lines), the open
   task and this turn's steps, and decides what to do. That covers a new message, an answer to a
   question, a message after the review, and a message to a paused task.
2. **Five new tools.**

   | Tool | What it does |
   |---|---|
   | `reply(text)` | The model's own short reply. Code guards it with CF-3's `safe_chat`: the reply is masked, and one with a number, an amount or a claim that something was done is replaced. With a task going on, the reply is said before the task's next question. |
   | `find_features(request)` | TEI's closest features to the user's newest words, then to the model's request sentence, at most 8. |
   | `start_task(feature, request, second)` | Starts one of the features just found. The request goes through CF-2's rewrite guard and `standalone`. If another task is open, code asks the switch question first. |
   | `ask_which(features)` | «منظورتان کدام است؟» with 2 to 4 found features and «لغو». |
   | `cancel_task()` | Cancels the open task. The app stays where it is. |

   A correction after the review, or a message to a paused task, goes through the act tools.
   `set_values`, `move_app` or `open_review` first opens the task again. A task at its review is
   planned again, and the newest message is read on its own by the workflow's reader (the
   workflow's `corrected`). A paused task is planned again from where the app is.
3. **What stays in code.**
   - **The fast path at both doors (D-034):**
     - A typed PIN, password, CVV2 or code is refused, and never reaches the history or a prompt.
     - A typed confirm request is refused.
     - «لغو» or a whole-message cancel phrase cancels.
   - **The value guards of D-032 and D-033 items 6 and 7:**
     - The workflow's reader decides every missing field.
     - Check and resolve run on every value.
     - A record must be named in the user's own words, never only in generic words.
     - Amount limits, installments and plates apply as today.
   - **The questions:** every question shown with options is code's. A secret or confirm request
     typed at one of them is refused at once, and the same question is asked again: the model
     never reads it.
   - **No cancel of a task the model just started:** `cancel_task` is not offered for a task
     started after the user's last words, because the user has not asked since to stop it.
   - **The switch question,** owner decision in CF.
   - **The limits:**
     - At most 12 tool calls a turn.
     - The same call twice in a row is not run again.
     - No tool can confirm, submit or pay.
4. **What goes.**
   - The fixed greeting, thanks and help replies.
   - The workflow's chooser and its not-sure node in the ReAct agent.
   - The ReAct-only follow-up (`Lifecycle.follow_up` and `lifecycle.corrects`). Nothing else used it.
5. **Scoring.** A reply with no task open is recorded as `knowledge_question`, which in the
   evaluator means "answered, nothing started".
6. **Wrong actions are accepted for now (owner).** The ReAct agent's numbers on the frozen sets are
   reported beside the workflow's. They are not a gate here, and tuning is a later issue.
7. **Unchanged:** the workflow agent and its behaviour, `workflow` as the default, the switch,
   native tool calls still blocked (RA-6), and no RAG.

## Why

The owner wants a ReAct agent that reasons about the conversation, and not a workflow with a model
step inside. With D-033 the model had no tool to act on a change of mind, chat or a cancel, so
none of CF-2 and CF-3 could reach it. The safety guards stay in code, because a model's judgement
is not a guarantee. Values are still checked by the workflow's own code, so a wrong step can pick a
wrong feature or miss a turn, but it cannot send a value the user did not give, and it cannot go
past the review.

## The run

See [RA-8 in the report](../reports/react-vs-workflow.md#ra-8-the-model-handles-the-conversation).
