"""Provision only the dedicated local demo using Home Assistant's real auth APIs.

Runs inside the pinned HA container, using its aiohttp dependency. Secrets stay
in the private /config directory and are never written to stdout.
"""
import asyncio
import json
import os
from pathlib import Path
import secrets

from aiohttp import ClientSession, ClientTimeout

BASE = "http://127.0.0.1:8123"
CLIENT = BASE + "/"
CREDENTIALS = Path("/config/demo-credentials.json")


def save(credentials):
    temporary = CREDENTIALS.with_suffix(".tmp")
    temporary.write_text(json.dumps(credentials, indent=2) + "\n")
    temporary.chmod(0o600)
    temporary.replace(CREDENTIALS)


async def main():
    os.umask(0o077)
    credentials = (json.loads(CREDENTIALS.read_text()) if CREDENTIALS.exists()
                   else {"username": "demo", "password": secrets.token_urlsafe(32)})
    save(credentials)
    async with ClientSession(timeout=ClientTimeout(total=60)) as session:
        async def request(method, path, *, token=None, **kwargs):
            headers = {"Authorization": "Bearer " + token} if token else {}
            async with session.request(method, BASE + path, headers=headers, **kwargs) as response:
                if response.status >= 400:
                    # Remote response bodies and auth payloads must not enter dev logs.
                    raise RuntimeError(f"Home Assistant {method} {path}: HTTP {response.status}")
                return await response.json() if response.content_type == "application/json" else await response.text()

        # Startup may involve a first-run database setup; this is readiness,
        # not an application API retry implementation.
        for attempt in range(180):
            try:
                async with session.get(BASE + "/api/onboarding") as response:
                    if response.status in (200, 404):
                        steps = await response.json() if response.status == 200 else []
                        break
            except (OSError, asyncio.TimeoutError):
                pass
            await asyncio.sleep(1)
        else:
            raise RuntimeError("Home Assistant did not become ready within three minutes.")

        token = credentials.get("token")
        grant = None
        if not token or any(not step["done"] for step in steps):
            if any(s["step"] == "user" and not s["done"] for s in steps):
                result = await request("POST", "/api/onboarding/users", json={
                    "name": "Demo owner", **{k: credentials[k] for k in ("username", "password")},
                    "client_id": CLIENT, "language": "en"})
                code = result["auth_code"]
            else:
                flow = await request("POST", "/auth/login_flow", json={
                    "client_id": CLIENT, "handler": ["homeassistant", None],
                    "redirect_uri": CLIENT})
                result = await request("POST", "/auth/login_flow/" + flow["flow_id"], json={
                    "client_id": CLIENT, **{k: credentials[k] for k in ("username", "password")}})
                code = result["result"]
            grant = await request("POST", "/auth/token", data={
                "grant_type": "authorization_code", "code": code, "client_id": CLIENT})
            if not token:
                async with session.ws_connect(BASE + "/api/websocket") as socket:
                    assert (await socket.receive_json())["type"] == "auth_required"
                    await socket.send_json({"type": "auth", "access_token": grant["access_token"]})
                    assert (await socket.receive_json())["type"] == "auth_ok"
                    await socket.send_json({"id": 1, "type": "auth/long_lived_access_token",
                                            "client_name": "Fluxzero Home local demo", "lifespan": 365})
                    result = await socket.receive_json()
                    if not result.get("success"):
                        raise RuntimeError("Home Assistant could not issue the demo access token.")
                    token = credentials["token"] = result["result"]
                    save(credentials)

        for step in steps:
            if step["done"] or step["step"] == "user":
                continue
            body = {"client_id": CLIENT, "redirect_uri": CLIENT} if step["step"] == "integration" else {}
            await request("POST", "/api/onboarding/" + step["step"], token=grant["access_token"], json=body)

        if grant:
            await request("POST", "/auth/revoke", data={"token": grant["refresh_token"]})

        required = {"light.bed_light", "sensor.outside_temperature", "binary_sensor.movement_backyard"}
        for attempt in range(60):
            states = await request("GET", "/api/states", token=token)
            if required <= {s["entity_id"] for s in states}:
                print("Home Assistant demo ready: authenticated light, temperature and motion APIs.", flush=True)
                return
            await asyncio.sleep(1)
        raise RuntimeError("Expected demo entities are missing; check the pinned Home Assistant demo integration.")


if __name__ == "__main__":
    asyncio.run(main())
