# D-023 — Python navigator, thin bridge

**Source:** Owner, 7 Nov 2026. Revises where D-020's stage 3 and runtime loop put route finding.

## Decision

1. **Route finding and requirement checking live in Python.** A deterministic navigator in the
   backend plans the route from the current screen to the target over the profile's edges, and
   collects every requirement along it.
2. **The graph carries each screen's requirements.** Each required field has a type, a short
   description and an `ask` question in Persian. Each edge that needs a selection first (an
   account, a card or a record) lists that as a requirement.
3. **The assistant asks for every missing value on the route before moving**, ideally in one
   question. It checks each page from the observation after filling it, and comes back to the user
   only when something fails or is ambiguous. **The user verifies once, on the final review, and
   only the user confirms.**
4. **The app's bridge offers four operations only:** `observe`; `navigate` to a screen with the
   arguments that screen needs (how it gets there inside the app is the app's business: taps,
   direct open or deep link); `fill_form`, one generic command for every form, driven by field tags;
   and `show_review`. It reports the observation after each, including an unexpected screen. It
   does not plan routes or decide what is missing. `select_candidate` is removed: a row choice is
   now one of `navigate`'s arguments.
5. **Intent selection (stage 6, plan only, not built now):** narrow the profile's features to the
   closest few with semantic search (the existing TEI embeddings), then let the model choose among
   them in a fixed JSON shape (an intent, "knowledge question" or "not sure", plus the slot values).
   Code checks every answer. The model never sees the graph, the forms or the user-only list; those
   are used by code only. See `docs/bridge/intent-selection.md`.
6. **What production looks like:** we keep the assistant, the tools, the adapter, the command loop
   and the navigator. The app team provides the four bridge operations in their app, field tags on
   their forms, the real graph with each screen's requirements, and the real lookup APIs, and keeps
   confirm and every money-moving service.

## Why

The route and the requirements are the same for every app build, so they belong with the assistant,
where they can be read, tested and asked about before anything moves. A thin bridge is also a much
smaller thing to ask of the app team.

## In the replica

The replica's `navigate` walks the declared edges, because it has no direct open. That walk is the
replica's private way of reaching a screen. It decides nothing: the plan, the requirements and the
check of each page are Python's. An app team may open screens directly instead.

## Unchanged

All safety rules, the pull-based command loop, the evidence labels and the stage order stand.
