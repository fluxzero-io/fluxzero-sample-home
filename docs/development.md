# Developing Fluxzero Home

Start with [Fluxzero Get started](https://fluxzero.io/get-started) and the
[local walkthrough](../README.md#try-it-locally). Coding agents should read
[AGENTS.md](../AGENTS.md) and use the installed plugin for guidance matching the
SDK version in `pom.xml`.

## The development loop

Use `/fluxzero:devboard` in your coding agent to open the project's Devboard.
Keep **App preview** open while changing Home, inspect results in **Tests**, and
use **Startup**, **Progress** and the monitoring pages to follow the environment.
The development environment owns compilation, application reloads and affected
backend tests. Do not run another Maven build, test loop or backend alongside it.

The frontend runs through Vite. React and CSS edits hot-reload without rebuilding
Java. Initial frontend setup installs locked dependencies and runs `npm run check`
(Node behavior tests and a production build). That setup result does not mean
frontend tests rerun on every edit: ask the agent to verify relevant changes through
the managed workflow and check browser behavior in App preview.

### Profiles and demo data

| Profile | What starts | Additional requirements |
| --- | --- | --- |
| `local` | SDK runtime, Java backend, local sign-in, Vite and the example home | None beyond the normal setup |
| `home-assistant` | The same app, plus the official Home Assistant Demo integration and links for all seven devices | Docker with a running Linux container engine and Python 3 |

Select a profile in Devboard, or use `fz dev restart --profile local` /
`fz dev restart --profile home-assistant` from the repository root. Use `fz dev`
for a first start and `fz dev stop` to stop. The checked-in configuration is
[`.fluxzero/dev.yaml`](../.fluxzero/dev.yaml).

An ordinary backend or frontend reload keeps the current Home runtime. A full
environment restart creates a fresh temporary runtime and loads the
[example commands](../examples/README.md). New rooms and control changes from the
previous runtime are therefore not a persistent installation. Home Assistant
retains its own container data and private credentials across normal restarts;
see its [lifecycle guide](../dev/home-assistant/README.md).

Local sign-in accepts any username and grants new demo users management access
to the example home. **alex** is a manager; **sam** retains read-only access.
This policy is explicitly limited to the local demo. The
[interface guide](interface.md#identity-and-permissions) explains household
permissions, production provisioning, browser sessions and logout.

## Follow a product change

Start with the behavior and the domain that owns it. For example, a new device
control belongs with `devices`; a new way to select a scene's targets belongs with
`scenes`. Keep integrations responsible for translating those intentions to their
provider, and endpoints responsible for authentication and dispatch.

For a change to physical-control behavior, read
[`DeviceRequestTest`](../src/test/java/io/fluxzero/home/devices/DeviceRequestTest.java):
it requests light settings, confirms them with an observation, then reports a
change made elsewhere. The last report becomes current without reviving the
completed request. Its synchronous and asynchronous variants use the same
application commands and Models through `TestFixture`.

For an adapter change, add controlled external responses to the relevant fixture
and exercise the real commands and queries. Then use the
[Home Assistant walkthrough](testing-home-assistant.md) for a separate check
against the actual API. No second HTTP client or replacement domain implementation
is needed in the application.

## Source layout

Java code is grouped by product domain under `io.fluxzero.home`:

| Domain | Owns |
| --- | --- |
| `household` | Homes, spaces, zones, residents and the household overview. |
| `devices` | Device capabilities, control and observations. |
| `scenes` | Reusable intentions and device selections. |
| `automation` | Scheduled routines and reactions to household changes. |
| `access` | Accounts, household permissions and browser sessions. |
| `homeassistant` | Connection, discovery, delivery and observation through Home Assistant. |

Each domain keeps commands, queries and typed IDs in `api`, and Models, details and other values in `api.model`.
Self-handling messages stay in `api`; separate handlers, endpoints and integration configuration live directly in
their domain. Tests follow the same domains. Application-wide contract tests and the shared `HouseExample` fixture
remain at the test root.

This is a source layout, not a split into services: Home still builds and runs as one application. HTTP routes
remain `/api/homes/...`; `api` in a Java package denotes its message/model contract, not a web transport.

## A short code tour

| Start here | What it demonstrates |
| --- | --- |
| [AddSpace](../src/main/java/io/fluxzero/home/household/api/AddSpace.java) and [Space](../src/main/java/io/fluxzero/home/household/api/model/Space.java) | Typed identities, cohesive details, independent Models and recursive `@Parent` relationships. |
| [FindDevices](../src/main/java/io/fluxzero/home/devices/api/FindDevices.java) | Search within a known home's relationships, without projecting every home. |
| [ActivateScene](../src/main/java/io/fluxzero/home/scenes/api/ActivateScene.java) | Ordinary device commands combined into one atomic Model commit. |
| [RunRoutine](../src/main/java/io/fluxzero/home/automation/api/RunRoutine.java) and [RoutineSchedules](../src/main/java/io/fluxzero/home/automation/RoutineSchedules.java) | Owned schedules, current Graph reconciliation and functional workflow outcomes. |
| [DeviceStatus](../src/main/java/io/fluxzero/home/devices/api/model/DeviceStatus.java) | A separate observation lifecycle, sharing one input ID with its device parent. |
| [CallHomeAssistantService](../src/main/java/io/fluxzero/home/homeassistant/api/CallHomeAssistantService.java) | A local message handler making an auditable external web request. |
| [SceneBehaviorTest](../src/test/java/io/fluxzero/home/scenes/SceneBehaviorTest.java) and [HomeAssistantRequestTest](../src/test/java/io/fluxzero/home/homeassistant/HomeAssistantRequestTest.java) | Product behavior and API contracts through Fluxzero's `TestFixture`. |

Read [SDK 2.0 in this example](sdk-2.md) for the modeling and execution choices, then the [domain guide](domain.md), [scenes and time](scenes-and-time.md), [interface guide](interface.md) and [Home Assistant adapter](home-assistant.md). The [example commands](../examples/README.md) are another executable entry point.

## Verification

The tests below are useful starting points for the corresponding product rules.
Backend tests exercise the actual application handlers through Fluxzero's
`TestFixture`; controlled HTTP responses stand in for external services. Key scene,
routine, automation and integration scenarios also run with asynchronous handling.
The frontend tests use Node's test runner to check display and control decisions.

| Product rule | Existing scenarios |
| --- | --- |
| A space has one location; moving it cannot cross households or create a cycle | [HomeModelTest](../src/test/java/io/fluxzero/home/household/HomeModelTest.java) |
| Device searches follow moves and removals within the selected home | [HomeQueryTest](../src/test/java/io/fluxzero/home/household/HomeQueryTest.java) |
| Device capabilities and supplied values must be valid | [DeviceBehaviorTest](../src/test/java/io/fluxzero/home/devices/DeviceBehaviorTest.java), [InputValidationTest](../src/test/java/io/fluxzero/home/InputValidationTest.java) |
| An observation confirms only matching pending controls; later physical changes remain in charge | [DeviceRequestTest](../src/test/java/io/fluxzero/home/devices/DeviceRequestTest.java) |
| A scene commits all selected requests together or none; overlapping targets do not duplicate a setting | [SceneBehaviorTest](../src/test/java/io/fluxzero/home/scenes/SceneBehaviorTest.java) |
| Pausing and replanning invalidate old routine work; weekly timing handles clock changes | [RoutineBehaviorTest](../src/test/java/io/fluxzero/home/automation/RoutineBehaviorTest.java), [RoutineReconciliationTest](../src/test/java/io/fluxzero/home/automation/RoutineReconciliationTest.java) |
| Deleting a routine or its home cancels the owned schedule | [RoutineOwnershipTest](../src/test/java/io/fluxzero/home/automation/RoutineOwnershipTest.java) |
| A sensor must cross a threshold; a repeated reading or an active cooldown does not reactivate the scene | [AutomationBehaviorTest](../src/test/java/io/fluxzero/home/automation/AutomationBehaviorTest.java), [AutomationDefinitionTest](../src/test/java/io/fluxzero/home/automation/AutomationDefinitionTest.java) |
| Successful scene and workflow progress appear together; functional refusal pauses without partial device changes | [WorkflowExecutionTest](../src/test/java/io/fluxzero/home/automation/WorkflowExecutionTest.java) |
| An external service acknowledgement is separate from an observation; recovery uses current pending settings | [HomeAssistantTest](../src/test/java/io/fluxzero/home/homeassistant/HomeAssistantTest.java) |
| Provider requests carry authentication and exact REST bodies; units and capabilities translate correctly | [HomeAssistantRequestTest](../src/test/java/io/fluxzero/home/homeassistant/HomeAssistantRequestTest.java), [HomeAssistantTranslationTest](../src/test/java/io/fluxzero/home/homeassistant/api/model/HomeAssistantTranslationTest.java) |
| Household permissions, local demo access, room creation, live updates and logout apply at HTTP boundaries | [HomeWebTest](../src/test/java/io/fluxzero/home/household/HomeWebTest.java) |
| Controls distinguish pending requests, reported settings and unknown values | [Frontend behavior tests](../frontend/src/home.test.js) |
| Models reconstruct from stored history after clearing the fixture cache | [ModelReplayTest](../src/test/java/io/fluxzero/home/ModelReplayTest.java) |

Tests advance fixture time for deadlines and cooldowns. These scenarios demonstrate
application behavior; cache-free reconstruction is not evidence that a production
runtime survives a process restart. Physical delivery is qualified separately by
the real API walkthrough, and still uses virtual demo equipment.

### Clean verification and CI

Outside an active development environment, the full clean checks are:

```sh
(cd frontend && npm ci && npm run check)
./mvnw -B verify
```

Use the Maven wrapper with Java 25 and Node.js 22.12+. The
[Verify workflow](../.github/workflows/verify.yml) runs the frontend checks and
backend verification on pushes and pull requests, without Docker, a Fluxzero
account or repository secrets. Maven packages the built frontend with the Java
application; the [interface guide](interface.md#production-assets-and-verification)
describes serving those assets.

The separate [cloud deployment workflow](../.github/workflows/deploy-to-fluxzero-cloud.yml)
is manual and requires the operator's cloud and identity configuration. Local demo
access is not production account provisioning. This example does not claim a
production deployment, physical hardware qualification or historical-schema
migration support.
