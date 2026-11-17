# D-032: A bounded ReAct agent beside the workflow, chosen by a switch

**Source:** Owner, 17 Nov 2026 (issue #166). The owner asked for a ReAct agent next to the current
one, "adding the ReAct system without changing or messing up what we already have". The owner
approved #166 to #171 on 17 Nov 2026.

## Decision

1. **Two agents and one switch.** `ASSISTANT_AGENT=workflow` (the default) runs today's agent,
   which is a fixed LangGraph workflow ([D-024](0024-agent-loop-first.md)). `ASSISTANT_AGENT=react`
   runs the ReAct agent. The backend, `main_debug.py --agent react` and
   `tools/evaluate_agent.py --agent react` all read the same switch. The panel and the graph page
   keep the same API, so the app needs no change.
2. **The ReAct agent's model picks the next step.** On each step the model returns one JSON object,
   `{"thought", "tool", "arguments"}`. Code runs the tool and gives the model a short result. This
   repeats until the model asks the user, reaches the review, or finishes. In the workflow, the
   model only fills three JSON forms and code decides every step.
3. **The tools.** There are nine:

   | Tool | What it does |
   |---|---|
   | `find_features(text)` | Returns the closest features. |
   | `feature_requirements(intent)` | Plans one feature from the app's current screen and lists the values it needs. |
   | `find_records(ref, words)` | Returns the records the app lists for a field that match the words. |
   | `current_screen()` | Says where the app is and what is filled. |
   | `set_values(values)` | Sets the planned task's values. |
   | `move_app()` | Takes the app to the screen of the next missing value. |
   | `open_review()` | Opens the review. |
   | `ask_user(question)` | Asks the user and waits for the answer. |
   | `finish(message)` | Replies to the user and ends the turn. |

   The act tools work on the task that `feature_requirements` planned last, so they take no
   intent. No tool can confirm, submit or pay. Every act goes through `TaskRunner`, so the only
   commands stay `navigate`, `fill_form` and `show_review`.
4. **Code guards every act, whatever the model says.** The guards are the workflow's own code,
   called and not changed:
   - Values go through the workflow's check and resolve. Money is accepted only as the user wrote
     it. A choice is accepted only as one of its allowed keys. A record is accepted only as the
     app lists it. Amount limits and saved plates are applied as they are today.
   - `move_app` is the workflow's `advance`.
   - `open_review` works only when nothing is missing. It then runs the workflow's `prepare` and
     `verify`.
   - When a task has missing values, the question shown to the user and its options are code's.
     The options are the same as the workflow's, plus «لغو».
   - After the review, a message is handled by the workflow's follow-up: cancel, resume,
     correction or new request.
5. **What the model sees.** It sees Persian titles, questions and option labels. It never sees an
   app key, a screen ID or a full card, account or IBAN number. Long numbers are masked as they
   are today.
6. **Limits.** A turn has at most 12 tool calls; then it ends without acting further. Tool results
   are short, because the model's context is 6,144 tokens.
7. **Tool calls are emulated.** The answer is held to a JSON schema with one branch per tool
   (`anyOf`), and the tool name is held to the tools available in the task's state. vLLM 0.26
   ships the `gemma4` tool-call parser, but enabling it means restarting RagBot's container, which
   rule 1 forbids. Native tool calls are a separate, blocked step (RA-6, #172).
8. **Branching.** The work goes on `sub/react-agent`, from `main`. Each step is a `feature/`
   branch into it. The sub-main merges to `main` once the whole agent works, with `workflow`
   still the default.
9. **Before `react` can become the demo default,** both agents run the D-025 gates side by side on
   held-out and tuning (RA-5, #171). The owner picks the default from that report.

## Why

The model decides the steps itself, and the demo can show its reasoning. Because the code guards
are shared, a wrong step from the model cannot send a wrong value to the app or pass the review.
The workflow stays the default and its files are not touched, so today's demo flows cannot
regress.

## Design

See [the ReAct agent](../bridge/react-agent.md).
