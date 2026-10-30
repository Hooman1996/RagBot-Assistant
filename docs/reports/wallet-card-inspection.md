# FR-04c — Wallet-linked card journeys

Issue [#52](https://github.com/Hooman1996/RagBot-Assistant/issues/52), following FR-04b #50. This is a local frontend batch. No backend, database, assistant, bridge, original-app execution or banking network access was added.

## Scope decisions

The owner approved FR-04c after FR-04b. The FR-04 decisions recorded on #48 apply: valid confirms end in a local result labelled **Designed for mock**, and the OTP is the fixed local code `12345`.

## Implemented content and identity

- **Wallet group on the card tab:** the recovered tab has a wallet card group with its own card face, a wallet **balance** view backed by a separate balance call, and copy/share. FR-03 left this group unconfigured; it is now a **کیف کارت** chip with one synthetic wallet card. The wallet card's balance is its own fixture field, separate from any account balance.
- **First wallet card issue:** the wallet router opens issue container `S-138` for the first-card key (WC-ROUTES). `S-138` has no content of its own and runs a three-step graph, so it gets no replica page or registry entry:
  - `S-140` send type: wallet choice, notice, integer fee and delivery options. In-person availability is a server flag, shown disabled in the fixture.
  - `S-137` address: choosing an address is required; adding one belongs to FR-05 and is unavailable.
  - `S-139` confirm: wallet account, name, fee, postal code and address, then **ثبت درخواست** and the labelled result.
- **Recovered flow details (WC-FLOW):** continuing from send type always goes to the address page, because the chosen delivery type is not carried forward. The address step takes the first available card design automatically, so there is no colour page.
- **Wallet PIN:** the assign/change key opens `S-141`. It shows a wallet card picker and the same four PIN sections, confirm, OTP and result as FR-04b `S-135`, and reuses that implementation.
- **Not built:** a generic wallet-issue key opens the shared operations menu list `S-002`, and the delivered-card confirmation is `S-004`. Both are unassigned FR-45 nodes and are recorded only.

Back steps back within the issue flow and keeps the draft. Cancel and the result button leave the flow and return to the card tab with the wallet group still selected. PINs and OTP codes are never kept in saved state.

## Private evidence and assets

WC-ROUTES, WC-TAB, WC-FLOW and WC-PIN refer to focused static findings under `local/evidence/wallet-card/`. No new private assets were added. The wallet card uses the normal card artwork instead of the original wallet design.

## Original-app fidelity

Original UI fidelity is **unscored**. Known gaps:

- The wallet card artwork, the empty and disabled wallet states, and the original wallet picker sheet are not replicated.
- The notice, fee and in-person availability come from the server in the original; here they are fixtures.
- Failure and pending statuses and the received-card confirmation are not shown.

## Acceptance

Baseline on main `3e4e1e2`: repository checks 131/131, pytest 112, Ruff pass, lint 0 errors/107 warnings, and Android 14 JVM / 28/28 device tests.

Final results are in the PR. Android: 15 JVM tests and 31/31 server device tests (0 skipped); the first device run passed. Rendered review on emulator-5554 covers the wallet tab with its balance shown, all three issue steps, the labelled result and wallet PIN. There are no replica crash records. Captures are private under `local/evidence/wallet-card/qa/`.

## Opening the batch and stopping point

Open **RagBot Replica → ورود به خانه → کارت → کیف کارت**, then **اولین کیف کارت** or **دریافت و تغییر رمز**.

This completes FR-04. FR-05, [profile, settings and saved addresses](profile-inspection.md), followed in #54 and made the add-address control work.
