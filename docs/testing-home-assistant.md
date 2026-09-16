# Testing the Home Assistant integration

This walkthrough exercises Home against a real, local Home Assistant API with virtual equipment. It is useful both when exploring the sample and when changing the adapter with a coding agent. No physical devices, cloud account or existing Home Assistant installation are needed.

There are two complementary ways to test:

| Test | What it proves | Requirements |
| --- | --- | --- |
| Fluxzero `TestFixture` tests | Command/query behavior, HTTP contracts, failure handling and scheduling, with controlled responses. | Java and the normal development environment. |
| This Home Assistant demo | Real bearer authentication, service calls, separate observations and recovery against the official server. | Docker with a running Linux container engine, Python 3 and the normal development environment. |

The demo is the official Home Assistant **2026.9.2** image with its [Demo integration](https://www.home-assistant.io/integrations/demo/), not a mock server that accepts every request. The bootstrap is tied to that version; Home's adapter uses the public REST API.

## 1. Start the environment

From the repository root, after installing the [quickstart prerequisites](../README.md#try-it-locally):

```sh
fz dev --profile home-assistant
```

If Home is already running, use `fz dev restart --profile home-assistant`. A full restart creates a fresh, seeded Home runtime. The Home Assistant container reuses its own local data. The first run downloads the container image and can take several minutes.

Open the Home URL printed by the CLI and sign in as **alex**. The overview should eventually show **7 of 7 linked**. Open Connections if an observation or delivery problem is shown. The separate Home Assistant UI URL is listed by `fz dev status`; it is not Home's frontend URL.

The profile provisions a real HA user and long-lived bearer token, then loads the connection properties into the backend. Home's browser never receives that token. The Home login (`alex` or read-only `sam`) and the HA login (`demo`, with a generated password) are separate identities.

Local files under `dev/home-assistant/.state/` are ignored by Git:

| File | Purpose |
| --- | --- |
| `connection.json` | Current API URL and demo container name; no credentials. |
| `config/demo-credentials.json` | Private HA username, password and token. |
| `home.properties` | Private backend connection configuration. |

Do not paste credentials into chat, screenshots, test fixtures or issue reports. The examples below read the token privately and print only test outcomes. See [demo setup](../dev/home-assistant/README.md) for permissions and lifecycle details.

## 2. Check real authentication

Run this read-only probe from the repository root. It checks missing credentials, invalid credentials and the generated token against the same `/api/states` endpoint. It then prints only the relevant virtual-device settings.

```sh
python3 - <<'PY'
import json
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import Request, urlopen

state_dir = Path("dev/home-assistant/.state")
connection = json.loads((state_dir / "connection.json").read_text())
credentials = json.loads((state_dir / "config/demo-credentials.json").read_text())
url = connection["url"] + "/api/states"
states = []
for label, token, expected in [
    ("No credentials", None, 401),
    ("Invalid credentials", "invalid-demo-token", 401),
    ("Generated credentials", credentials["token"], 200),
]:
    headers = {} if token is None else {"Authorization": "Bearer " + token}
    try:
        with urlopen(Request(url, headers=headers), timeout=15) as response:
            status = response.status
            if expected == 200:
                states = json.load(response)
    except HTTPError as error:
        status = error.code
        error.close()
    assert status == expected, f"{label}: expected {expected}, got {status}"
    print(f"{label}: HTTP {status}")

fields = {
    "light.bed_light": ["brightness"],
    "light.ceiling_lights": ["brightness", "hs_color"],
    "light.kitchen_lights": ["brightness"],
    "switch.decorative_lights": [],
    "climate.heatpump": ["temperature"],
    "cover.hall_window": ["current_position"],
    "sensor.outside_temperature": ["unit_of_measurement"],
    "binary_sensor.movement_backyard": [],
}
by_id = {item["entity_id"]: item for item in states}
for entity_id, names in fields.items():
    item = by_id[entity_id]
    values = {name: item["attributes"].get(name) for name in names}
    print(entity_id, item["state"], values)
PY
```

Expected authentication results: **401, 401, 200**. A successful authenticated read also proves the required demo entities exist. This diagnostic Python code is only a read-only probe; the application itself sends HTTP through Fluxzero's web gateway.

## 3. Control devices and verify observations

Use Home's controls for these actions. Turn a light **on** before changing its brightness or color: brightness is a separate intention and does not imply power on. Some HA entities omit brightness while off, which cannot confirm a saved brightness setting.

| Home action | HA state to inspect with the probe | Home confirmation |
| --- | --- | --- |
| Turn on Reading lamp and set brightness to 40%. | `light.bed_light`: `on`, brightness `102` (HA uses 0–255). | Reported power on and brightness 40%. |
| Open Bedside lamp details; turn it on and set hue 210°, saturation 70%. | `light.ceiling_lights`: `hs_color` approximately `[210, 70]`. | Matching reported color. |
| Set Heating to 21.5 °C. | `climate.heatpump`: `temperature` 21.5 in this Celsius demo. | Matching reported temperature setting. |
| Set Window shades to 50% open. | `cover.hall_window`: `current_position` 50 after movement. | Reported opening 50%. |
| Toggle Path lights. | `switch.decorative_lights`: `on` or `off`. | Matching reported power. |

Home polls every five seconds. A service's HTTP success is only acceptance: wait for the next observation and compare the **reported** settings in device details. A mismatch shows **Syncing** after one second. Do not treat the control position or a success toast alone as evidence that equipment changed.

The Demo cover rounds to steps of ten and simulates movement. Use 0, 10, …, 100 for an exact match. Its temperature and motion sensors are synthetic and static; increasing the thermostat does not warm the reported room. Room sensor deliberately uses the demo's outside-temperature entity. These limitations belong to the demo equipment, not an invented Home confirmation.

Run the read-only probe again to compare actual API values with Home's reported values. To test the reverse direction, sign into the separate HA UI using the private generated password and change a virtual lamp there. Home should observe that change; polling does not continually restore an intention that was already delivered.

## 4. Exercise connection loss and recovery

This exercise temporarily pauses only this repository's demo container and automatically resumes it. It leaves other containers and credentials alone. Run it in another terminal from the repository root, with Home open:

```sh
python3 - <<'PY'
import json
from pathlib import Path
import subprocess
import time

root = Path("dev/home-assistant").resolve()
connection = json.loads((root / ".state/connection.json").read_text())
container = connection["container"]
info = json.loads(subprocess.check_output(["docker", "inspect", container]))[0]
assert info["Config"]["Labels"].get("io.fluxzero.home.demo") == str(root), \
    "This container does not belong to this demo"
assert not info["State"]["Paused"], "The container was already paused"
try:
    subprocess.run(["docker", "pause", container], check=True, stdout=subprocess.DEVNULL)
    print("Demo paused for 60 seconds. Change Reading lamp brightness in Home.", flush=True)
    time.sleep(60)
finally:
    subprocess.run(["docker", "unpause", container], check=True, stdout=subprocess.DEVNULL)
    print("Demo resumed.", flush=True)
PY
```

While paused, change Reading lamp brightness to **60%**. Home should retain the previous observation, save the new intention and show pending synchronization or a connection problem after the request times out. It must not claim that the new brightness was observed. After the script resumes HA, allow the pending request or retry and subsequent observation to complete. Home's reported brightness should become **60%**; the API probe should show `153`.

If you changed it several times during the outage, the latest intention should win. Recovery timing includes request timeouts, backoff and the next observation, so it can take longer than one five-second polling interval. If the terminal is forcibly killed and cannot execute `finally`, unpause the exact container named in `connection.json` before continuing.

## 5. Work with a coding agent

Follow [Fluxzero Get started](https://fluxzero.io/get-started) to set up your coding agent, then open this repository. [AGENTS.md](../AGENTS.md) supplies the project-specific boundaries. A useful task is:

> Follow docs/testing-home-assistant.md against the local demo. Use the existing Fluxzero development environment, verify authentication without revealing credentials, control a virtual light through Home, compare its reported state with HA, and exercise temporary connection loss. Restore the demo and report the observations and any failures.

The agent should select SDK **2.0.0-rc.13** with `docs_start`, inspect `get_status`, and reuse the active environment. For this optional profile, start or switch it with the CLI command above. Follow `wait_for_change` and `get_test_status` after code changes. Do not run a second backend, watcher or Maven/npm verification loop alongside `fz dev`.

Use the browser to drive Home, not a replacement HTTP client that bypasses its domain commands. Read-only API probes are useful for independent confirmation. A probe succeeding is not enough to establish that Home sent the intended request or processed its observation.

## Automated tests and troubleshooting

[HomeAssistantTest](../src/test/java/io/fluxzero/home/homeassistant/HomeAssistantTest.java) covers the integration workflows, [HomeAssistantRequestTest](../src/test/java/io/fluxzero/home/homeassistant/HomeAssistantRequestTest.java) covers message and HTTP contracts, and [HomeAssistantTranslationTest](../src/test/java/io/fluxzero/home/homeassistant/api/model/HomeAssistantTranslationTest.java) covers units and device capability translation. They dispatch commands and queries through `TestFixture`; only external web responses are stubbed. CI runs them without Docker or secrets.

| Symptom | Check |
| --- | --- |
| Container is not ready. | Docker is running, the pinned image can be pulled, and `fz dev status` identifies the demo service. |
| Home shows 0 of 7 linked. | The `home-assistant` profile is selected and its startup commands succeeded. |
| HA rejects the generated token. | The URL and credentials belong to the same `.state` installation. Restart the profile to reload configuration; do not copy in a token from another installation. |
| A control changes but never confirms. | Inspect Connections, reported settings and the read-only API probe. For lights, check power; for demo covers, use multiples of ten. |
| Home login fails. | Use `alex` or `sam` through Home's local IDP. HA's `demo` user is a different account. |

Finish with `fz dev stop`, or return to the standalone sample with `fz dev restart --profile local`. Normal restarts preserve HA credentials. A full reset is optional and destructive to this demo's own data; follow the [setup guide](../dev/home-assistant/README.md#authentication-and-local-data) only when you actually need one.
