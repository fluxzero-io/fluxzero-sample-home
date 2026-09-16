# The home as a domain

A home provides context rather than one large storage object. A lamp can be replaced without rewriting a room's history. A routine can be deleted while its scene remains.

| Concept | Meaning and lifecycle |
| --- | --- |
| `Home` | Name, local time zone and mode: home, away, sleeping or vacation. |
| `Space` | A building, floor, room, outdoor area or other place; occupies one position in a recursive tree. |
| `Zone` | A named collection of spaces. Overlap is allowed; a zone does not own its spaces. |
| `Resident` | A resident with a name, household role and presence. |
| `Device` | A device in a space, with capabilities, supported measurements and desired settings. |
| `DeviceStatus` | Device observations with a timestamp, availability, settings and measurements. The current value is the latest complete report; its history provides previous readings for threshold detection. |
| `Scene` | A named collection of intentions; activations are recorded in its Model history. |
| `Routine` | A scene with a timing pattern, next execution, pause state and execution history. |
| `Automation` | A scene linked to a meaningful change, with pause state, cooldown and execution history. |

All these concepts are independent Models. Ownership relationships use `@Parent`. Scene actions and settings are values: they have no independent identity or lifecycle, so they are not made into Models or Members merely for demonstration.

## Descriptive data

Each named concept has its own immutable details value: `HomeDetails`, `SpaceDetails`, `DeviceDetails`, `ResidentDetails`, `ZoneDetails`, `SceneDetails`, `RoutineDetails` and `AutomationDetails`. The name belongs there, with the same limit of 1–120 characters. `SpaceDetails` also contains the kind of space; together they describe the place.

Identity and relationships belong on the Model. A device label remains an alternative identity; capabilities, desired settings, household roles and time zones retain their own meaning. Current status, next execution, generations and cooldowns are separate from the description too. New descriptive fields can be added to the appropriate details value.

`CreateHome` and `AddSpace` receive the complete description. `RenameSpace` receives only the new name and preserves the kind, parent and primary light. Input validation also checks nested details and rejects missing values.

## Layout and boundaries

`Home` and `Space` are both a `Place`, with a typed `id`. A space stores one required `parentId`: a `HomeId` or `SpaceId`. `AddSpace` requires only this destination and the description. The owning home follows from the Graph's parents; a separately supplied home cannot contradict the layout. Each space appears at exactly one position in the home Graph.

`MoveSpace` uses the same destination contract: another space or explicitly its own home. Contents move with the space. The SDK rejects concrete cycles; the application rejects destinations outside the original home. A zone is not a Place parent: it groups spaces that keep their own positions.

Devices belong to their space. Moving a device also clears the primary-light preference in its former room. `RemoveDevice` can clear that preference through the existing parent relationship, without a space ID in the command. An explicit device reference in a scene or sensor trigger prevents deletion until the reference is removed.

A space can be deleted only when it is empty and no longer explicitly referenced by a zone or scene. Logically deleting an entire home deletes its owned models and cancels remaining routines. Logical deletion does not physically erase history.

## A device has capabilities

A smart speaker can play audio and control volume. A climate unit can offer temperature and fan-speed control. A simple sensor can provide measurements only. The model does not impose a fixed device category.

Recognizable commands include `TurnOn`, `TurnOff`, `DimLight`, `SetLightColor`, `SetRoomTemperature`, `SetOpening`, `LockDoor`, `UnlockDoor`, `PlayMedia`, `SetVolume`, `SetFanSpeed`, `StartWatering`, `StopWatering`, `EnableCharging` and `PauseCharging`.

`LightLevel`, `LightColor`, `RoomTemperature` and the other concrete settings carry their own declarative input constraints. `Device` checks whether it supports the requested or reported capability. `DeviceCommand` and `DeviceSetting` contain no execution or validation logic.

Percentages range from 0 to 100. Hue ranges from 0 to 359. The current room-comfort setting ranges from 5 to 35 °C. This is an explicit boundary of the comfort feature; specialized installations such as a sauna should receive their own recognizable setting with an appropriate range, rather than silently widening this one.

Measurements have fixed units, such as °C, %, lx, W, kWh or ppm. Binary observations such as motion and water leaks use 0 and 1. `ReportDeviceStatus` requires only the device ID. The observation has its own Model history under that identity, with a separate storage prefix; no second status ID is supplied. Deleting the device also deletes its current observation. An older or equally dated observation does not replace a newer one. A report is a complete current observation; measurements not included in that report are unknown.

## Intent, observation and access

`Device.desiredSettings` describes what was requested. `DeviceStatus.reportedSettings` describes what an adapter observed. `Availability` describes reachability. The core keeps these meanings separate. Both sets of settings use `DeviceSettings`: an immutable collection with at most one value per capability. The capability follows from the value; callers do not supply a second key. In JSON, settings are an array of values with a `kind`, for example `[{"kind":"lightLevel","percent":25}]`. Duplicate capabilities and invalid values are rejected.

A resident role such as owner or guest is a household concept. The [public interface](interface.md) maps a validated identity to a separate `Account` with per-home permissions. It checks that membership and the home boundary before reading or changing household data.
