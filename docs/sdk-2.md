# SDK 2.0 in this example

The build imports the published `io.fluxzero:fluxzero-bom:2.0.0-rc.15` from Fluxzero Packages. The Maven Compiler also runs the matching SDK annotation processor to generate model and type indexes. The application, tests and local runtime use the same SDK version; no local SDK build is required.

| SDK capability | Concrete use |
| --- | --- |
| Independent `@Model` boundaries | Homes, spaces, devices, residents, zones, scenes, routines and automations have their own lifecycles. |
| Cohesive details | Domain-specific details values with validated command input and focused updates that preserve other fields. |
| Typed `Id<T>` | Each ID type's namespace prevents collisions, for example between a room and a lamp with the same name. |
| Automatic command handling | Commands carry their own `@Apply`. Execution commands handle functional rejection by pausing; the Home Assistant link fetches API capabilities in its own handler before its Model commit. |
| `@Parent` and recursive relationships | `Space.parentId` selects one Home or Space through `PlaceId<?>`; the declared parent types and `spaces` path preserve the flexible tree. |
| Lazy, consistent `Graph<T>` | Home boundaries, zone selection, scene targets and deletion conditions read only the relationships they need. |
| Interception and atomic changes across Models | `ActivateScene` expands an intention into ordinary device commands and the activation itself within one commit. |
| Updating existing parents | `RemoveDevice` deletes a device and corrects its existing room's primary light without including the room ID in the command. `@Association("devices")` distinguishes that room from enclosing spaces. |
| Input validation before Model checks | Jakarta constraints and pure `@AssertTrue` methods validate names, roles, capabilities, cooldowns, triggers, timing patterns and measurements. `@Valid` includes concrete nested values. `@AssertLegal` then enforces model- and relationship-dependent rules. |
| Delegated Model assertions | `@AssertLegal` on `DefineAutomation.trigger` lets the concrete `MeasurementCrosses` check its sensor and measurement capability through the pinned home Graph. |
| Explicit event publication | `ActivateScene` uses `@Apply(eventPublication = ALWAYS)` and returns the existing scene: activation history without copied audit fields. |
| Purposeful storage and history | All Models use plain `@Model`. Device observations also need history to recognize threshold crossings. |
| Creation compatibility and relationships | The SDK rejects duplicate creation; `AddSpace` requires one existing destination. `MoveSpace` preserves the original home boundary. |
| Complete Graph-change handlers | `RoutineSchedules` reads the current routine with `eventGraph.current()` and reconciles status and deadlines, including after an older event. |
| Schedules with `@Parent` | `RunRoutine.routineId` ties an execution to its Routine's lifetime. Direct and cascading deletion cancel stored executions without application cleanup. |
| Relationship-aware search | `FindDevices` selects through `whereAncestor(homeId)` and the requested capability. |
| One identity and parent | `DeviceStatus.deviceId` carries both `@EntityId(prefix = "devicestatus:")` and `@Parent`; reporting requires only the device ID. |
| Alternative identity | An optional device label is an `@Alias`; the actual device ID remains stable. |
| Current and event-bound Graphs | Consumers can read state at a change; schedule reconciliation explicitly selects the current routine. |
| Versioned defaults | `2026.09.10` enables automatic Model routing; conflict retry is the SDK default independently of this marker. |
| Self-handling integration messages and WebRequestGateway | `GetHomeAssistantStates` and `CallHomeAssistantService` send the external web request from their own handlers. TestFixture dispatches those messages and replaces only the HTTP responses. |
| Deterministic scheduling and TestFixture | Deadlines, generations, pausing, repetition, daylight-saving transitions and stale delivery are covered by behavior tests. |

The routine patterns `Once` and `Weekly` are concrete values behind the `RoutineTiming` contract. They receive an explicit reference instant and the home's time zone; the application does not use the system clock for this. `@Valid` on `PlanRoutine.timing` includes the selected pattern's field constraints. `Weekly` owns the local calendar calculation, while the existing routine consumer synchronizes the calculated deadline with the SDK scheduler. The polymorphic JSON names remain `once` and `weekly`.

An invalid input value or empty timing pattern is rejected as a `ValidationException` with a field path, even if the specified home does not exist. `AddDevice` does not load a space just to check whether the input contains capabilities or measurements. Its `@Apply(Space)` still lets the SDK enforce creation in an existing space. Fluxzero supplies collection defaults during deserialization; commands do not add constructors for this.

Field constraints run before method constraints. For example, `hasNonNegativeCooldown()` checks only `!cooldown.isNegative()`: the field's `@NotNull` has already rejected missing input. The same applies to required collection elements. Checks on deliberately optional data, such as a device label or media when playback is stopped, do express a separate domain rule.

For automations, `@Valid` cascades the concrete trigger's input constraints; `@AssertLegal` then delegates its Model checks. `DefineAutomation` checks the definition's and scene's home boundaries without knowing the trigger implementations. `HomeBecomes` and `MeasurementCrosses` own their transition detection. `HomeChange` distinguishes mode changes from device observations through concrete values; both interfaces contain contracts only. Search selection uses the same transition rule as the final command handling, which always checks the loaded automation definition.

`ReportDeviceStatus` compares observation time with the message's original publication time. This context-dependent check uses `@AssertLegal(Message)` without a Model parameter. `@PastOrPresent` against the processing clock would select a different time boundary. Missing observation time, device identity, availability and measurement values are validated declaratively beforehand.

The application does not use legacy Aggregates for new state. Scene steps are values without independent lifecycles, so `@Member` is not added merely to demonstrate a feature. A complete home projection is not materialized on every sensor reading because that would add unnecessary work.

`Graph.revisionStateIndex()` is one node's revision, not necessarily the same value for every node in an atomic commit with several events. The scene test therefore checks the complete durably committed state through an explicitly current Graph during event handling, and rollback after a later rejected action. An event-bound Graph deliberately retains the historical boundary of that individual event.

## Storage and search in this home

`Home`, `Space`, `Device`, `Zone`, `Resident`, `Scene`, `Routine`, `Automation` and `DeviceStatus` use plain `@Model`. Their own event streams remain the source for loading and reloading. `DeviceStatus` contains only the current report; the event-bound Graph supplies the previous observation through `previous()`. This comparison remains available after clearing caches and after later reports, without a duplicate `previousReadings` field.

| Application question | Route used | Why this storage is sufficient |
| --- | --- | --- |
| How is this known home arranged? | `GetHome` loads `Graph<Home>` by HomeId. | Loading by identity and navigating relationships require no direct document projection on `Home`. |
| Which devices in this home can dim? | `FindDevices` searches `Device` through `whereAncestor(homeId)` and filters on `capabilities`. | The existing `devices` composition path maintains indexed internal device documents. The known home needs no document of its own for this. |
| Which automations match this change? | `HomeReactions` searches through `whereParent(homeId)`. | The existing `automations` composition path maintains internal documents for this bounded selection. |
| What did the device actually report? | `GetDeviceStatus` loads with `loadModel(deviceId, DeviceStatus.class)`. | The independent observation history reconstructs the current report; the relationship to the device stays the same. |

`Fluxzero.search(Device.class)` without home selection returns no devices with this configuration. A global device list is not a current application query. Supporting it would require a deliberate direct document projection justified by that new requirement. Search visibility is not access control.

`GetHome` uses a relationship Graph loaded by identity. This differs from `searchGraph(Home.class)`, for which the current plain home root has no document. A searchable projection of complete homes should be added only when a concrete query needs one; the application does not materialize an entire home for every sensor reading.

Search results show committed current documents, not historical event state or transactional read dependencies. Domain rules therefore continue to use injected Models/Graphs. The scheduler deliberately requests the current routine through `eventGraph.current()`. During that call, the SDK establishes a fresh storage boundary for the same Model identity; the original Graph retains its event-bound state. `loadCurrentGraph` remains appropriate when reconciling from an ID alone. Domain assertions stay on the injected Graph to preserve transactional read dependencies.

Versioned agent documentation is supplied by the Fluxzero plugin from the published rc.15 archive. `AGENTS.md` records the version and source commit; the SDK documentation contains complete contracts and executable modeling recipes.

## Commands, relationships and workflow execution

A scene remains a composition of ordinary device commands. This produces recognizable events and lets each command apply its own rules. `SceneAction` and `SceneTarget` define contracts without implementations. Concrete actions create commands; concrete selections read their part of the home Graph. The application contains no Model simulator or alternative dynamic-Model route to retest the SDK contract.

The SDK rejects duplicate `CreateHome`, `AddSpace`, `AddDevice` and `AddResident` commands as functional failures. The application needs no extra `requireNew` assertions for them. Commands intended to redefine state, such as `DefineScene` and `PlanRoutine`, explicitly accept a nullable current Model. The application still enforces its own business rules. Input uses declarative constraints; relationship-based domain rejections use `IllegalCommandException` directly. There is no generic Rules helper or custom exception wrapper. `PlanRoutine` expresses the home boundary, scene selection and future execution in separate assertions.

`Home` and `Space` implement the pure `Place` contract with only `id()`. `HomeId` and `SpaceId` form the sealed `PlaceId` family. `Space.parentId` declares both concrete Models with `@Parent(types = {Home.class, Space.class}, pathInParent = "spaces")`; the owning home is not stored twice. `MoveSpace` reads the existing space's Graph and finds its destination within the original home, including the home root itself.

`Place` is a shared domain contract, not an independent `@Model`. `AddSpace` checks the required parent in a small `@AssertLegal` through `Fluxzero.loadModel(parentId)`, which uses the concrete ID type. The apply only creates the space. There is no custom resolver or execution layer.

A polymorphic `parentId` uses JSON `["home", "example-home"]` or `["space", "example-floor"]`. Ordinary `HomeId` and `SpaceId` fields remain scalars. `PlaceId` lets Jackson choose the concrete subtype constructor through type information and `@JsonCreator`; otherwise the pinned SDK's default ID deserializer tries to construct the abstract ID base itself.

`ReactToHome` and `RunRoutine` have a small `@HandleCommand` that calls `Fluxzero.assertAndApply(this)`. This executes the complete Model commit without resending the command. Their `@InterceptApply` returns the scene activation and workflow progress as one combined intention. There is no scene preflight: assertions and applies belong to the same commit attempt.

A `FunctionalException` rolls back that attempt, after which a separate `PauseFailedRoutine` or `PauseFailedAutomation` command follows. Technical failures propagate. Successful device settings and workflow progress remain one commit; rejection and the subsequent pause are two transactions. The application makes no crash-safe workflow guarantee for this sequence. The pause command rechecks the routine generation/deadline or whether the automation definition still applies.

Routine and Automation have no execution counters or copied last-executed timestamps. `ReactToHome` explicitly publishes successful reactions with `eventPublication = ALWAYS`, even when a zero cooldown and the same timestamp produce no new value. `Automation.cooldownEndsAt` stores the current cooldown boundary. `effectiveFrom` marks the start of the current definition. The cooldown rule therefore has constant cost instead of scanning history for every sensor reading.

The routine consumer reconciles current state on one tracker, including after a historical event. Its handler with a sole Graph parameter receives both direct changes and cascade deletion. For an absent routine, the consumer does nothing: `@Parent` on `RunRoutine.routineId` lets the SDK cancel the stored execution. Pausing, completion and rescheduling remain explicit schedule effects of the current routine. Deadlines and generations still protect against old or already delivered commands.

The parent must already be committed when an execution is scheduled; the existing post-commit consumer satisfies that requirement. Cancellation is asynchronous and also works without an active application consumer. The local TestServer belongs to SDK rc.15. A separately deployed Runtime must support this ownership feature. `RoutineOwnershipTest` proves cleanup without a routine consumer and verifies that recreating the same ID does not make an old stored execution valid again.

## Example data and publication

This application has not been deployed and starts with the current details schema. Upcasters, explicit schema revisions and migration fixtures are unnecessary. Use a fresh temporary runtime for the example home after an incompatible schema change.

Ordinary event-sourced replay and historical observations remain part of the domain. Automations compare event-bound before/after state without source-revision deduplication or requiring the source to have remained unchanged since the event. Behavior tests check triggers, cooldowns and pausing; technical redelivery guarantees belong to the SDK and Runtime. Full durable execution is not a guarantee of the pinned SDK version. Routine generations distinguish rescheduling and are not schema revisions. Reloading, previous readings and schedule cleanup remain covered. Observation retention is a separate product decision.

Local builds, CI and the deployment workflow resolve rc.15 as a published dependency. The SDK version is defined centrally in `pom.xml`.

## Device requests and observations

`Device.pendingSettings` is operational work, not a permanent household policy. Ordinary device commands stage requests; the Model's shared `@Apply` captures their sampling boundary from the message timestamp. `ReportDeviceStatus` atomically stores the independent observation and finishes matching requests. Completed requests remain in the SDK event history. The UI then follows physical changes, and future adapter delivery uses only outstanding settings.

## Home Assistant: committed intentions and external observations

`HomeAssistantConnection` is a Model under Home. `HomeAssistantDevice` has both Device and connection as parents: it ceases to exist when either is deleted. The device is not deleted when its connection is removed. The combined entity alias includes connection identity and prevents duplicate links within that installation. Both Models remain ordinary event-sourced Models.

`LinkHomeAssistantDevice` has its own `@TrackSelf` command handler: it requests current API capabilities through `GetHomeAssistantStates`, then applies one Model command. Home-boundary and model-existence checks remain in the Model pipeline. Discovery, polling and delivery use the same query; a service action becomes a `CallHomeAssistantService` command. Neither API-bean injection nor a `withBean` fixture is needed.

`GetHomeAssistantStates` and `CallHomeAssistantService` contain their own `@HandleQuery`/`@HandleCommand` with `Fluxzero.sendWebRequestAndWait`. They run locally within the calling workflow, without `@TrackSelf` or an extra consumer. `DiscoverHomeAssistantDevices` is also a local self-handling query. The normal gateway provides the auditable external web request. `HomeAssistantEndpoint` shares only configuration, request construction and status checks. The fixture dispatches ordinary messages and registers only an external HTTP stub. Standard credentials remain in their HTTP header, which Auditlog masks in visible records and downloads.

An expected unreachable installation is mapped by the interaction handler to `HomeAssistantUnavailable`, a recognizable `FunctionalException`. The workflow records the current problem and schedules recovery. The service body is a concrete value within the local call; JSON sent to Home Assistant contains its REST fields: `entity_id` and, as appropriate, `brightness_pct`, `hs_color`, `temperature` or `position`. No internal command type metadata is sent.

The post-commit event consumer delivers device settings. A retry contains only the device and connection and reads current intent through `loadCurrentGraph`. Navigating the Device Graph to its linked Model and status makes a missing or deleted link an ordinary empty relationship. When a link is deleted, `graph.current()` checks directly whether that same Model identity is still absent before cancelling a pending delivery attempt.

`RefreshHomeAssistant` and `DeliverHomeAssistantSettings` demonstrate schedule ownership for external systems. A refresh produces complete, changed `ReportDeviceStatus` observations, allowing existing event-bound automations to work without vendor logic. `AcceptHomeAssistantObservation` checks the still-selected route before applying the report in the same Model pipeline. See the [integration guide](home-assistant.md) for physical delivery, polling and the limits of this SDK version.
