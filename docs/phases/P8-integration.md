# P8: After-Presentation Integration

**Goal:** if the app team accepts, the same accepted scenarios run against their real app and
services.
**Gate (plan §17):** the same scenarios are verified on the team's staging build, and
compatibility differences are resolved before any production claim.

| Task | What it will do |
|---|---|
| P8-01 | Responsibility map with the app team: trusted user context, real services, the bridge |
| P8-02 | The app team's real lookup APIs behind `assistant/lookups.py`'s interface, with contract tests against both mock and real |
| P8-03 | The app team's four bridge operations (`observe`, `navigate` with arguments, `fill_form`, `show_review`) and field tags in their codebase, on their navigation and lifecycle |
| P8-04 | The app team's real graph, with each screen's requirements, as a profile the navigator reads |
| P8-05 | Run the accepted scenarios on their staging build |

**We keep:** the assistant, the tools, the adapter, the command loop and the navigator.
**The app team provides:** the four bridge operations in their app, field tags on their forms, the
real graph with each screen's requirements, and the real lookup APIs. **The app team keeps:** confirm
and every money-moving service. See [production](../bridge/production.md).

P8 is scoped separately after the demo. Nothing here starts before the presentation.
