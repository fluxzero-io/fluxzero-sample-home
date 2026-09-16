# Local Home Assistant demo

Use a real Home Assistant API without physical devices or an existing installation. This optional development profile runs the official `2026.9.2` container and [Demo integration](https://www.home-assistant.io/integrations/demo/). It exercises Home's ordinary Fluxzero commands, web gateway, retries and scheduled observations.

## Start

Install Docker with a running Linux container engine and Python 3, then run from this repository:

```sh
fz dev --profile home-assistant
```

If Home is already running, switch profiles with `fz dev restart --profile home-assistant`. Add `--port 60612` to retain that public URL. Vite remains the frontend dev server, so UI edits do not require a backend rebuild.

`fz dev` owns the container, Home backend, tests, IDP and frontend. The first run pulls the image and takes longer. Home starts only after the demo entities and authenticated API are ready. Sign into **Home** as `alex` to manage the house or `sam` to view it.

The profile automatically links:

| Home device | Home Assistant Demo entities |
| --- | --- |
| Reading lamp | `light.bed_light` (on/off and brightness) |
| Bedside lamp | `light.ceiling_lights` (on/off, brightness and color) |
| Pendant lights | `light.kitchen_lights` (on/off and brightness) |
| Path lights | `switch.decorative_lights` (on/off) |
| Heating | `climate.heatpump` (temperature setpoint) |
| Window shades | `cover.hall_window` (percent open) |
| Room sensor | `sensor.outside_temperature` and `binary_sensor.movement_backyard` |

These are synthetic signals: the Demo integration's outside temperature is deliberately used as the example room reading. It does not represent the room's physical temperature. Demo motion and temperature are static; they demonstrate observation, not real sensor transitions. All seven Home devices are linked to distinct demo entities. Their Home names describe the example rooms; Home Assistant retains its demo entity names. Heating changes the thermostat setpoint without changing its HVAC mode. The synthetic measured temperature does not rise when you turn up the heating.

Try power and brightness on the lights, hue and saturation in the Bedside lamp details, the heating temperature and the Window shades slider. Inspect each device’s reported settings after a change. Observations refresh every five seconds. The Home Assistant UI is available at the support-service URL reported by `fz dev status`. Changing the lamp there is also observed by Home; polling does not continually restore an already delivered intention.

The official Demo cover rounds requested positions to steps of 10 and simulates movement over several seconds. Use 0, 10, 20, …, 100 for an exact match in this demo. Other values remain visible as requested versus reported; Home does not invent confirmation. Real covers retain the ordinary 0–100% control.

## Authentication and local data

Bootstrap uses Home Assistant's onboarding flow and [authentication API](https://developers.home-assistant.io/docs/auth_api/) to create a local `demo` user with a random password and issue a real long-lived access token. Authentication is enabled normally. No token is hardcoded in the repository or sent to the Home browser. Onboarding is tied to the pinned HA version; the application adapter uses the public REST API.

Generated data is private to `dev/home-assistant/.state/`, which is ignored by Git:

- `config/demo-credentials.json`: local HA username, password and bearer token. Use this password to sign into the **Home Assistant** UI; it is separate from Home's `alex` identity.
- `home.properties`: URL and token, loaded by the normal Fluxzero `FLUXZERO_CONFIG_LOCATIONS` property source.
- `connection.json`: current local URL and container name, without credentials.
- `config/`: Home Assistant's own database, configuration and authentication state.

The directory is mode `0700`; generated credential files are `0600`. Nothing prints passwords or tokens. Both the API and readiness port bind to `127.0.0.1`. This container has no privileged mode, host networking, hardware access or real household data. The REST-only demo can run in Docker Desktop's Linux VM; it is not a supported full hardware installation on macOS. See [Home Assistant Container installation](https://www.home-assistant.io/installation/linux#install-home-assistant-container) for normal deployments.

Ordinary development restarts reuse HA credentials and data. Home's temporary Fluxzero runtime is seeded again, while hot reloads preserve Home state. Stop with `fz dev stop`, or return to the standalone app with `fz dev restart --profile local`. For a fresh HA installation, first stop the environment and then remove only this demo's `.state` directory. Its credentials and virtual-device history will be lost.

## Verification

The fast `HomeAssistantRequestTest` and `HomeAssistantTest` fixtures remain independent of Docker. They check the adapter's authentication header, API contract, rejected credentials, retries and domain outcomes using Fluxzero's web test handlers.

The running demo additionally supports a real HTTP roundtrip: Home command → Fluxzero web request → HA service → HA state snapshot → Home reported settings. Connection loss must retain the last observation, show a connection problem, and recover on a subsequent scheduled refresh. A pending device intention is retried using its latest settings. Invalid bearer tokens must receive HTTP 401 from HA.

To simulate a temporary network outage, pause the **demo container named in `.state/connection.json`**, wait for a connection problem, change the reading lamp's brightness in Home, then unpause that same container. Always unpause it after the check. Do not stop or pause unrelated containers. No physical equipment is involved.
