# Home Assistant as the first integration

This example follows the public [Home Assistant REST API](https://developers.home-assistant.io/docs/api/rest/). It discovers entities, explicitly links them to existing devices, maps committed intentions to service actions, and periodically reads reported state. The application starts and its tests run without Home Assistant or an access token.

Start with `HomeAssistantTest` to understand the flow: ordinary home commands remain the entry point. `HomeAssistantRequestTest` checks request methods, URLs, bearer headers, JSON, error handling and retry policy through `TestFixture`. Both test classes dispatch real commands and queries, replacing only external HTTP responses with `@HandleGet` and `@HandlePost` handlers. Request settings explicitly disable redirects. These are API contract tests, not qualification against physical equipment.

## The smallest complete example

The existing example data contains `example-home` and `example-light`. To connect that reading lamp to an installation, the operator configures two values on the machine running the **Home application**:

| Property | Environment variable | Example |
| --- | --- | --- |
| `home-assistant.demo.url` | `HOME_ASSISTANT_DEMO_URL` | `http://homeassistant.local:8123` |
| `home-assistant.demo.token` | `HOME_ASSISTANT_DEMO_TOKEN` | Your own long-lived access token |

Create the token in the Home Assistant user profile as described in the REST guide. Supply it through deployment configuration or existing encrypted Fluxzero configuration; do not put it in Git or command JSON. Use HTTPS when the connection leaves a trusted local network. Standard `ApplicationProperties` resolution handles configuration and fixture overrides.

The home selects only the name of a preconfigured group. A domain command cannot supply a token or arbitrary destination URL. The adapter sends ordinary Fluxzero web requests with an `Authorization: Bearer ...` header. Requests and responses travel through the forward proxy and are auditable; Auditlog masks standard credential headers in visible records and downloads. The adapter does not copy remote error content into domain errors.

The configured URL must be reachable from the **forward proxy**. An address such as `homeassistant.local` works only when that proxy can reach the local network and resolve the name. The Home application still reads the URL and token from its own configuration.

From a trusted application component:

```java
var connection = new HomeAssistantId("demo");
var home = new HomeId("example-home");
var light = new DeviceId("example-light");

Fluxzero.sendCommandAndWait(new ConnectHomeAssistant(connection, home,
        new HomeAssistantDetails("My Home Assistant", "demo"), Duration.ofSeconds(10)));

var found = Fluxzero.queryAndWait(new DiscoverHomeAssistantDevices(connection));
// Choose an entity from found with POWER and LIGHT_LEVEL; use its actual entityId.
Fluxzero.sendCommandAndWait(new LinkHomeAssistantDevice(light, connection, Set.of("light.reading")));

Fluxzero.sendCommandAndWait(new DimLight(light, new LightLevel(25)));
var observed = Fluxzero.queryAndWait(new GetDeviceStatus(light));
```

Immediately after the command, `observed` may still contain the previous state. Home first commits the intention, then the adapter performs the service action. A subsequent refresh reports the state known to Home Assistant. The REST route `POST /api/states/...` is never used for physical control; the adapter uses `POST /api/services/light/turn_on` with `entity_id` and `brightness_pct`. An HTTP success does not itself become a status report. See the [REST API](https://developers.home-assistant.io/docs/api/rest/) and [light actions](https://www.home-assistant.io/integrations/light/).

Each external interaction has its own message. `GetHomeAssistantStates(connectionId)` reads the REST snapshot; `CallHomeAssistantService(connectionId, action)` performs a service action. Discovery, linking, delivery and periodic refresh use these commands and queries:

```java
var snapshot = Fluxzero.queryAndWait(new GetHomeAssistantStates(connection));
Fluxzero.sendCommandAndWait(new CallHomeAssistantService(connection,
        new HomeAssistantAction("light", "turn_on",
                new HomeAssistantAction.DimEntity("light.reading", 25))));
```

The web request lives in the message's own `@HandleCommand` or `@HandleQuery`. For example, in `CallHomeAssistantService`:

```java
@HandleCommand
void handle() {
    var request = HomeAssistantEndpoint.load(connectionId)
            .post("api/services/" + action.domain() + "/" + action.service(), action.body());
    try {
        requireSuccess(Fluxzero.sendWebRequestAndWait(request, REQUEST_SETTINGS));
    } catch (GatewayException | TimeoutException failure) {
        throw new HomeAssistantUnavailable("Home Assistant could not be reached or did not respond in time.");
    }
}
```

`HomeAssistantEndpoint` reads trusted configuration, builds requests with standard headers and checks HTTP statuses. It sends no HTTP requests itself and is never injected. No `HomeAssistantApi` service or `withBean` in tests is needed.

These internal interactions and the discovery query use local self-handlers: only `@HandleCommand` or `@HandleQuery` is required. The call runs within the existing workflow, without a separate consumer or Runtime message boundary for the command/query. The external web request still uses the normal auditable gateway. `HomeAssistantUnavailable` is an explicit functional failure outcome: callers can show an unreachable installation and retry later. Unexpected programming errors remain technical failures.

Request settings use a five-second timeout, no redirects, and at most two additional attempts spaced 250 ms apart for HTTP 500, 502, 503 and 504. Supported write operations explicitly set state; repeated attempts do not perform a toggle. The adapter owns no separate HTTP client, retry loop or JSON mapper.

A supported entity may offer more capabilities than the selected Home device. The link must still cover every declared device capability and measurement. For example, an RGB lamp can initially be linked as a device with only `POWER` and `LIGHT_LEVEL`; a device that also declares `LIGHT_COLOR` is rejected until that mapping is added.

The same entities cannot be linked to different devices within one connection. A device has one Home Assistant link. Another system exposing the same physical lamp is not a reason to create a second Device. A later second adapter must preserve that single selected route.

A Home Assistant entity does not always represent a complete physical device. The example room sensor can use two entities:

```java
Fluxzero.sendCommandAndWait(new LinkHomeAssistantDevice(
        new DeviceId("example-sensor"), connection,
        Set.of("sensor.room_temperature", "binary_sensor.room_motion")));
```

One snapshot contains both measurements. Two sources for the same measurement or two controllers for the same capability are rejected as ambiguous. Discovery does not create spaces or devices or rename them. The device and connection must belong to the same home.

## Supported mappings

| Home Assistant | Home | Behavior |
| --- | --- | --- |
| `light` | `Power`, optionally `LightLevel` | On/off and brightness percentage; dimming requires a suitable `supported_color_modes`. |
| `switch` | `Power` | Explicit `turn_on` / `turn_off`; no toggle. |
| `sensor`, temperature class | `TEMPERATURE` | °C or °F to °C. |
| humidity / illuminance / battery / carbon_dioxide | Corresponding measurement | Only %, lx, % and ppm. |
| power / energy | `POWER` / `ENERGY` | W/kW to W; Wh/kWh to kWh. |
| `binary_sensor`, motion / occupancy / presence | `MOTION` | `on` = 1; `off` = 0. |
| door / window / opening | `CONTACT_OPEN` | `on` = open. |
| smoke / moisture | `SMOKE` / `WATER_LEAK` | `on` = detected. |

Classification and attributes follow the documentation for [lights](https://developers.home-assistant.io/docs/core/entity/light/), [sensors](https://developers.home-assistant.io/docs/core/entity/sensor/) and [binary sensors](https://developers.home-assistant.io/docs/core/entity/binary-sensor/). Unknown domains, units and unsupported capabilities are not advertised as working support.

For combined lighting intentions, explicit off takes precedence over dimming; zero brightness also means off. The core stores the chosen dimensions independently. An API refresh only observes: manual control is not reversed every ten seconds to restore an old intention that has already been delivered.

Color, climate control, coverings, locks, media, ventilation, irrigation and charging have not yet been mapped to Home Assistant. They remain available in the generic core model. Extending support means adding a concrete mapping and protocol example, including units, valid ranges and the meaning of reported state.

## Scheduling, failures and deletion

A `RefreshHomeAssistant` schedule belongs to its connection through `@Parent`. The refresh interval is configurable from five seconds to one hour. The next refresh is scheduled even after a connection failure; `GetHomeAssistant` shows that current problem. An unreachable gateway says nothing certain about a lamp, so the adapter does not replace its last observation with invented offline readings.

A missing entity or one reported as `unavailable` by Home Assistant does produce an offline device report. `unknown`, missing required values and invalid measurements produce unknown state without invented zero values. The adapter compares complete reports and publishes only changed content. `observedAt` is when the adapter samples the combined API snapshot, not a new physical measurement time for each individual sensor. Different HA `last_updated` fields are not merged into one fictitious original sensor timestamp.

When the short SDK retries do not succeed, the link records a current delivery problem and the adapter schedules `DeliverHomeAssistantSettings`. That schedule contains the device and connection, without copied settings. The retry reads the latest device intention. After success, the delivery problem is cleared and retries stop; physical observations still come from a separate refresh.

Scheduled attempts and ordinary device changes converge on one event consumer for physical execution. This provides one execution order without a custom thread pool or connection manager. It limits this example's throughput: one slow HTTP request can hold up other Home Assistant effects for several seconds. A larger application can deliberately partition this work by installation.

```java
// Remove only this route; the Device remains.
Fluxzero.sendCommandAndWait(new UnlinkHomeAssistantDevice(light));

// Remove the connection and its routes; devices and the home layout remain.
Fluxzero.sendCommandAndWait(new DisconnectHomeAssistant(connection));
```

Deletion cancels stored work through parent ownership. An observation checks its still-selected route before reaching the device model. An HTTP request already sent cannot be recalled. The adapter claims no transaction across physical devices: a scene commits all its intentions atomically, but physical delivery may succeed only partially. There is no extra application bookkeeping for generic duplicate event delivery; the current SDK pin does not promise full durable execution.

## Why this first version polls

The [WebSocket API](https://developers.home-assistant.io/docs/api/websocket/) supports `state_changed`. This first version uses complete REST snapshots and the Fluxzero scheduler. That keeps the initial integration small and demonstrates scheduling, independent Model lifecycles and committed effects without a custom socket lifecycle.

A brief motion or open/close transition between polls can therefore be missed. This example teaches the architecture, controls and measurement-threshold reactions; it does not implement a reliable alarm system. Capturing every transition will require replacing the incoming path with a WebSocket subscription and a recovery snapshot. Commands, scenes and `ReportDeviceStatus` need not change for that.

The [Home interface](interface.md) authenticates browser sessions and enforces household permissions before dispatching core commands. Adapter discovery, configuration and linking remain trusted operator operations; they have no browser-facing routes.
