# The integration boundary

The first phase provides an executable domain core. A device's brand or protocol is not a reason to create a different home, space or scene model.

The first phase-2 adapter uses the [Home Assistant REST API](home-assistant.md). [Matter and KNX](standards.md) are references for the generic model. The adapter supports an explicit initial subset; a broad core model does not mean every API feature has already been mapped.

The existing separation is:

1. A resident expresses an intention through a command or scene.
2. The core checks the home boundary and supported capabilities, then commits the desired settings.
3. An adapter maps committed intentions to the connected system.
4. The adapter reports observed state through `ReportDeviceStatus`, with the time at which it observed that state. The REST adapter combines several entities into one snapshot; it uses the sampling time rather than presenting it as a new physical sensor measurement.

An adapter independently manages its connection, external device identity, secrets, delivery attempts and physical acknowledgements. It must not report a desired setting as a confirmed observation. Repeated intentions and connection recovery require reconciliation with the current desired state; older messages must not undo a newer intention.

Devices have different limits. The current capabilities provide a broad starting point for lighting, comfort, access, media, gardens and energy. Additional domain features receive named, typed settings with their own rules. Residents are not expected to understand a universal bag of protocol fields.

An external control layer will connect the signed-in user to the household and enforce permissions for each action. Sensor reports will have a separate adapter identity. `HouseholdRole` does not yet implement that authentication boundary. There are no public control endpoints or default live device connections. Operators connecting Home Assistant supply the configuration group and explicitly choose device links.
