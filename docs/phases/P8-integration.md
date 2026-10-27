# P8: After-Presentation Integration

**Goal:** if the app team accepts, the same accepted scenarios run against their real app and
services.
**Gate (plan §17):** the same scenarios are verified on the team's staging build, and
compatibility differences are resolved before any production claim.

| Task | What it will do |
|---|---|
| P8-01 | Responsibility map with the app team: trusted user context, real services, executor integration |
| P8-02 | Real service adapters behind the same feature interfaces, with contract tests against both mock and real |
| P8-03 | Executor integration into their codebase, on their navigation and lifecycle |
| P8-04 | Run the accepted scenarios on their staging build |

P8 is scoped separately after the demo. Nothing here starts before the presentation.
