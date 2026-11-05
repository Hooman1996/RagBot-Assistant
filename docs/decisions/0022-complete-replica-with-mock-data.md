# D-022 — Complete the replica with mock data before the bridge

**Source:** Owner, 5 Nov 2026. Revises D-021's scope and the order of Stages 2 and 3.

## Decision

1. **The replica covers the app's most-used services, not the whole app.** On top of the D-021
   journeys, these are built: Sayad cheques (FR-21), vehicle violations and documents (FR-18),
   credit and bounced-cheque inquiries (FR-24), one generic loan request (FR-36, FR-39), login and
   welcome (FR-41), the personal finance dashboard (FR-26), the account opening application
   (FR-43) and, if time allows, wage advance, BNPL credit and rewards (FR-32, FR-33, FR-34, FR-25).
   Brokerage, crowdfunding, merchant terminals, registration, postal and the unresolved
   destinations stay deferred.
2. **Designed details are labelled.** Where the code does not show a detail, it is designed in the
   app's existing theme and labelled `Designed for mock`. Recovered structure, IDs and hierarchy
   stay as they are. No service is added that is not in the app's feature inventory.
3. **Mock data is editable JSON** under `mock_services/fixtures/`, one file per domain, shared
   later by the mock service and the assistant.
4. **Every service the user triggers runs against a local mock** that checks the inputs and returns
   success or a clear failure. There is no database and no real service. For this stage the mock
   bank lives in the app, in Kotlin; D-006's mock service still owns drafts once it exists.
5. **Batches are no longer one issue per FR.** Work is grouped into the parts of the owner's
   completion brief, with one tracker row per part.
6. **The complete replica comes before the bridge (#93) and the clickable graph (#94).** They start
   only after the owner approves the replica.

## Unchanged

All safety rules stand. The assistant and the bridge never reach a confirm, submit or pay action:
those stay a user tap. Money is exact integers with an explicit unit, and all data is synthetic.
