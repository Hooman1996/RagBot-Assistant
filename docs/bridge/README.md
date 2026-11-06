# The bridge

The bridge lets a backend move the app and fill its forms, the way a person would. It can open a
screen, fill a form and show the review page. It can never confirm, submit or pay: those stay a
tap by the user. Inside the app, a small executor takes one command at a time and works the real
screens through their own tap and typing handlers, so the app validates everything as usual. After
each command it reports what the screen now shows.

## The parts

```mermaid
flowchart LR
    subgraph backend [Python backend]
        A[Assistant, later] --> L[Command loop]
        G[Graph page] --> L
        T[Lookup tools, later] --> D[Adapter, later]
        D --> M[Mock service, later]
    end
    subgraph app [Replica app]
        P[CommandPoller] --> E[Executor]
        E --> X[Policy and Actions]
        X --> C[ComposeControls]
        C --> S[Screens]
        S --> B[Mock bank in the app]
    end
    L <-->|results and commands| P
```

The app always starts the conversation. It polls the backend, so the backend never needs to reach
into the device. The parts marked "later" are not built yet.

## Read the code in this order

All Kotlin files are in `android/app/src/main/java/org/ragbot/replica/bridge/`.

| File | What it does |
|---|---|
| `Contract.kt` | The command, observation and result of `contracts/schemas/`, as Kotlin data |
| `Controls.kt` | The executor's only view of the screen, as an interface |
| `Policy.kt` | Decides what may be touched. This is rule 4 in code |
| `Actions.kt` | The one place the executor taps or types. It asks Policy every time |
| `Routes.kt` | Finds a path over the exported edges of the profile |
| `Navigator.kt` | `navigate` and `select_candidate`: moves one edge at a time and checks each step |
| `FormFiller.kt` | `fill_form` and `show_review`, and the list of review pages |
| `Observer.kt` | Builds the observation and tracks its version and the draft's version |
| `Executor.kt` | Runs one command: rejects bad ones, runs the operation, keeps the journal |
| `ComposeControls.kt` | Reads and drives the real Compose screens |
| `ContractJson.kt` | Reads commands and writes observations and results |
| `BackendClient.kt` | The two HTTP calls the loop makes |
| `CommandPoller.kt` | The pull loop on its own background thread |
| `BridgeConfig.kt` | The debug setting and the device ID |
| `BridgeState.kt` | The current page, as `ReplicaApp` publishes it |

The tests show each rule at work:

| Test | Where | Proves |
|---|---|---|
| `PolicyTest` | `android/app/src/test/.../bridge/` | Rule 4, see [safety](safety.md) |
| `ExecutorTest` | same | The command lifecycle |
| `ContractJsonTest`, `CommandPollerTest`, `BridgeConfigTest` | same | The wire format, the loop, rule 3 |
| `BridgeJourneysTest` | `android/app/src/androidTest/.../bridge/` | The demo walk's journeys, on the device |
| `BridgeServicesTest` | same | One journey per service group, on the device |
| `BridgeReachTest` | same | Every screen is reached, or stops for a stated reason |

## More pages

- [One command, end to end](command-lifecycle.md)
- [How rule 4 is enforced](safety.md)
- [What changes for production](production.md)
