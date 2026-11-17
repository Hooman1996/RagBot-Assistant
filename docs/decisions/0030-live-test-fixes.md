# D-030 — Toman by default, and two show-only features in the held-out set

**Source:** Owner, 17 Nov 2026 (issue #164), after testing the assistant on the replica: "default is
toman, do these". The owner deferred RAG answers: the focus is the agent, tool calling and navigation.

## Decision

1. **An amount with no unit is in tomans.** «۵۰ هزار» means 50,000 toman, as people say it, so it is
   500,000 rials. A bare number typed as the answer to «چند ریال…؟» stays rials. No held-out case
   has an amount with a scale word and no unit, so no expectation changes.
2. **Two show-only features.** «خانه» (`home`, S-026) and «تسهیلات جاری و پرداخت اقساط» (`loans`,
   S-338, the current-loans list) open a page and prepare nothing. A request to see something gets
   a feature that shows it, not one that pays.
3. **Two held-out cases are added**, one per new feature, because D-025 asks for every feature in
   the held-out set: H-144 «منو ببر صفحه اصلی» (`home`) and H-145 «وام‌هایی که دارم رو نشونم بده»
   (`loans`). No other case changes. The gates now count 79 features.
4. **The held-out set is frozen** on 17 Nov 2026 at
   SHA-256 `73fda69572923552436b3f456fcb6536d502ac1a40a4e12c1f5b1d876ce3dbac`. This replaces the hash in
   [D-029](0029-asked-on-the-screen.md). `tests/evaluation/test_cases.py` now reads the hash from
   this file. Everything else in D-025 to D-029 holds.

## Why

The owner's live test showed requests the held-out set did not cover: «برو به خانه» and
«تسهیلات در جریانمو نشون بده» had no feature to land on, and «۵۰ هزار» was read as rials.

## The fresh run

RUN_RESULTS
