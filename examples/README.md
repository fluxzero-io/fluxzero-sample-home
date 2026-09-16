# Example home

The JSON commands in this directory describe a home with a ground floor, living room, kitchen, bedroom and garden, with lighting, heating, shades and a room sensor. The *A pleasant evening* scene dims the lamp and requests 21 °C. A weekly routine activates that scene at 20:00 local time.

Creation and definition commands use `details`. Scene actions have their own `kind`, such as `dimLights` or `setHeating`, with a concrete value (`brightness` or `temperature`) and a target such as `{"kind":"space","spaceId":"example-living"}`. The examples use the current schema without explicit schema revisions or conversion of old payloads.

`AddSpace` selects one parent with `"parentId": ["home", "example-home"]` or `"parentId": ["space", "example-floor"]`. The command does not require a separate home alongside an enclosing space. The type in this reference also distinguishes a home and a space with the same local ID.

The files are numbered in execution order. The local development configuration loads them once per temporary runtime. They contain no real devices or production data. Every fresh runtime starts over; domain commands reject attempts to silently overwrite existing identities.

For more examples, including zone scenes, daylight-saving transitions, presence and sensor reactions, see the Java behavior tests. Open the public URL printed by `fz dev` for the control interface. Sign in as **alex** to manage this example or **sam** for a read-only view. Files 11 and 12 provision these local IDP subjects with application-owned household access.

Changing an example file can cause the development environment to submit it again to the same runtime. An existing identity is then correctly rejected. Use a fresh temporary runtime to rebuild the example home; do not erase an existing installation's data for this purpose.

The optional [Home Assistant guide](../docs/home-assistant.md) builds on this home. The standard example data makes no external connection and contains no credentials. `HomeAssistantTest` also shows an observed temperature controlling a light through an ordinary Home automation.
