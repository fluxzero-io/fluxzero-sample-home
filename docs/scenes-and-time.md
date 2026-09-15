# Scenes and time

## A scene expresses one intention

A scene contains ordered, concrete actions such as `DimLights`, `SetHeating` and `SwitchPower`. An action targets one device, a space and its descendants, a zone or the entire home.

When targeting one specific device, that device must support the setting. For a broader target, only devices with the requested capability are selected. If no suitable device exists, the entire scene is rejected. Zones with overlapping spaces apply the final setting only once. A later action may refine an earlier setting. Invalid actions are still rejected when a later action would overwrite them.

Devices are selected again for each activation. A lamp therefore belongs to its new room after being moved. All checks and changes use one pinned Graph state. Fluxzero executes the individual device commands and the scene activation in one Model transaction. Other application components never see a partially applied scene.

The selections `OneDevice`, `InSpace`, `InZone` and `WholeHome` determine which devices participate. Each concrete scene action creates a recognizable command for each device. `ActivateScene` selects the last action per device and capability, then lets the SDK apply those commands together. It does not build temporary device models to derive commands from them afterwards.

Each successful activation is published and recorded in Model history with `@Apply(eventPublication = ALWAYS)`. `Scene` stores no counter or timestamp for this. When the devices already have the requested settings, only the activation remains as a new event.

This guarantee covers the core. Physical devices may be independently reachable or fail; adapters report that progress separately.

## A routine has a durable next execution

`PlanRoutine` records both the pattern and the concrete next execution. A post-commit consumer reconciles the scheduler with the current routine. A redelivered older event therefore cannot reschedule a paused routine.

```java
new PlanRoutine(appointment, home, new RoutineDetails("One-time comfort"),
        evening, new Once(moment));

new PlanRoutine(weekRhythm, home, new RoutineDetails("Regular evenings"),
        evening, new Weekly(Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), LocalTime.of(20, 0)));
```

`RoutineTiming` defines only the contract for the next execution. `Once` stores one absolute instant and is finished afterwards. `Weekly` selects the next chosen weekday and local time in the home's time zone. That calendar rule belongs to the concrete pattern. Both patterns search strictly after the supplied instant; a one-time appointment for exactly now is rejected when planned. Input constraints live on the concrete values.

Each routine has one stable schedule identity and an increasing generation. An execution checks generation, deadline and pause state. Early delivery, completed intentions and obsolete schedules change nothing. The scene and the routine's completion or advancement are committed together. Executions live in Model history; Routine stores no extra execution count or last-executed timestamp.

- A one-time routine ends after execution.
- A weekly routine selects the next future local date and time.
- A nonexistent time during the spring daylight-saving transition is skipped.
- A repeated time during the autumn clock change runs only at its first occurrence.
- Late delivery executes once and skips missed repetitions.
- Resuming starts at the next future occurrence. An expired one-time appointment must be replanned.
- Replanning replaces the active deadline and invalidates the previous generation.
- Pausing cancels the next execution through the routine consumer.
- `RunRoutine` refers to its routine through `@Parent`. The SDK cancels stored executions when that routine is deleted, including when its home is deleted through a cascade and the routine consumer is inactive.

A new schedule created after the first occurrence of a repeated local time also skips the second occurrence and selects the next matching date. The stored JSON patterns retain the names `once` and `weekly`.

Automatic cancellation is asynchronous and cannot recall commands already delivered. Current-state, generation and deadline checks therefore remain necessary. A new routine with the same ID has a new lifetime: an earlier stored execution cannot be reattached to that new routine.

If a changed home layout makes a scene impossible to execute, the routine is paused with a readable reason in `problem`. None of the device settings are partially applied. After fixing the issue, a repeating routine can be resumed; a missed one-time routine can be replanned.

## Reacting to changes

An automation can react when the home enters a particular mode or when a sensor reading crosses a threshold upwards or downwards. The first sensor reading does not establish a crossing. Values that remain on the same side of the threshold do not reactivate the scene.

```java
new DefineAutomation(leaving, home, new AutomationDetails("When leaving"),
        everythingOff, new HomeBecomes(HomeMode.AWAY), Duration.ZERO);

new DefineAutomation(tooWarm, home, new AutomationDetails("Too warm"),
        cooling, new MeasurementCrosses(sensor, Measurement.TEMPERATURE,
                MeasurementCrosses.Direction.RISES_ABOVE, new BigDecimal("24")),
        Duration.ofMinutes(5));
```

`HomeBecomes` recognizes entry into the chosen mode. `MeasurementCrosses` recognizes a strict threshold crossing by the selected sensor: 24 to 25 counts for *above 24*, while 23 to 24 does not. If a measurement is missing from either report, no crossing has been established. The sensor must belong to the same home and supply the selected measurement; the sensor trigger checks those conditions when the automation is defined.

Concrete triggers contain their own rules. `AutomationTrigger` is only the contract. The reaction consumer creates a `HomeModeChanged` or `DeviceObservationChanged` and uses that same trigger rule to select matching automations. `ReactToHome` checks the change again against the applicable definition and cooldown before executing the scene.

Observations retain their own event history. The reaction compares state before and after the relevant report, even after clearing caches or when newer reports already exist. Previous measurements are not copied into extra fields on the current report.

`effectiveFrom` determines when the current automation definition takes effect; earlier changes do not activate it. Each successful reaction is recorded in Model history without a separate execution count.

The cooldown limits repeated activations. `cooldownEndsAt` stores when another activation is allowed; activation is permitted exactly at that boundary. Pausing and resuming preserve it. Changing the duration recalculates the boundary from the last successful execution. Only a successful scene starts a new cooldown; zero allows the next matching change to react immediately. A later source update does not invalidate an earlier transition merely by introducing a new revision: renaming a home does not erase a departure, and another reading above a threshold does not erase the earlier crossing. Only home-mode changes and reported sensor state trigger reactions; desired settings produced by the scene do not feed back into a loop.

A paused automation does not react. Resuming does not activate the existing situation by itself; a subsequent matching change can activate a scene again. The application keeps no processed source revisions or other technical deduplication bookkeeping. Execution recovery is the responsibility of the SDK and Runtime. Full durable execution is planned but is not part of the pinned SDK version, so this version does not claim exactly-once execution when events are resubmitted.

The application actually executes the scene together with workflow progress. A functional rejection rolls back the entire attempt; a separate command then records the pause and reason. This applies to both routines and automations. There is no duplicate scene preflight. Temporary technical failures remain technical failures, allowing the Fluxzero consumer to retry them.

The routine consumer processes scheduling and status changes on one tracker and always reads current intent. It does nothing for an absent routine: deletion cleanup belongs to the SDK. If a routine has already been recreated, even an old deletion notification reconciles only its new current intent.

## What the tests prove

Tests advance time instead of sleeping. They check active schedules with exact identity, deadline and generation, plus state immediately before and at execution. `RoutineOwnershipTest` registers no routine consumer and checks direct and cascading deletion, the corresponding `ScheduleAutoCancelled` metric, and protection of a recreated routine from old stored executions. Scenarios cover synchronous and asynchronous handling. Reloading after clearing caches proves reconstruction from stored Model history within the fixture; it does not claim that an external production store survived a process restart.
