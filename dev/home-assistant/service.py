"""fz dev support service: isolated HA container, private credentials, readiness.

Only this project's labelled container is owned here. fz dev stops this process
and the container together; Home Assistant data survives normal dev restarts.
"""
import argparse
import hashlib
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import os
from pathlib import Path
import shutil
import signal
import subprocess
import threading

IMAGE = "ghcr.io/home-assistant/home-assistant:2026.9.2"
ROOT = Path(__file__).resolve().parent
STATE = ROOT / ".state"
NAME = "fluxzero-home-ha-" + hashlib.sha256(str(ROOT).encode()).hexdigest()[:10] + "-" + str(os.getpid())


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", required=True, type=int)
    parser.add_argument("--ready-port", required=True, type=int)
    args = parser.parse_args()
    os.umask(0o077)
    config = STATE / "config"
    config.mkdir(parents=True, exist_ok=True)
    STATE.chmod(0o700)
    shutil.copyfile(ROOT / "configuration.yaml", config / "configuration.yaml")
    ready = threading.Event()
    stopped = threading.Event()

    class Health(BaseHTTPRequestHandler):
        def do_GET(self):
            self.send_response(200 if ready.is_set() and self.path == "/ready" else 503)
            self.end_headers()
        def log_message(self, *_):
            pass

    health = ThreadingHTTPServer(("127.0.0.1", args.ready_port), Health)
    threading.Thread(target=health.serve_forever, daemon=True).start()

    def stop(*_):
        stopped.set()
        ready.clear()
        subprocess.run(["docker", "stop", "--time", "10", NAME], stdout=subprocess.DEVNULL,
                       stderr=subprocess.DEVNULL, timeout=25)

    signal.signal(signal.SIGTERM, stop)
    signal.signal(signal.SIGINT, stop)
    container = subprocess.Popen([
        "docker", "run", "--rm", "--name", NAME,
        "--label", "io.fluxzero.home.demo=" + str(ROOT),
        "--publish", f"127.0.0.1:{args.port}:8123",
        "--volume", f"{config}:/config", "--volume", f"{ROOT}:/demo:ro", IMAGE])
    try:
        # The container can take time to pull on its first run. Wait for our own
        # container before executing bootstrap; never attach to another project.
        for attempt in range(300):
            if stopped.wait(1) or container.poll() is not None:
                raise RuntimeError("Home Assistant exited before startup completed.")
            result = subprocess.run(["docker", "inspect", "--format", "{{.State.Running}}", NAME],
                                    capture_output=True, text=True)
            if result.returncode == 0 and result.stdout.strip() == "true":
                break
        else:
            raise RuntimeError("Home Assistant image did not start within five minutes.")
        subprocess.run(["docker", "exec", NAME, "python", "/demo/bootstrap.py"], check=True)
        credentials = json.loads((config / "demo-credentials.json").read_text())
        temporary = STATE / "home.properties.tmp"
        temporary.write_text(f"home-assistant.demo.url=http://127.0.0.1:{args.port}\n"
                             f"home-assistant.demo.token={credentials['token']}\n")
        temporary.chmod(0o600)
        temporary.replace(STATE / "home.properties")
        (STATE / "connection.json").write_text(json.dumps({"url": f"http://127.0.0.1:{args.port}",
                                                          "container": NAME}, indent=2) + "\n")
        ready.set()
        print(f"Demo UI: http://127.0.0.1:{args.port} — credentials in {config / 'demo-credentials.json'}", flush=True)
        return container.wait()
    finally:
        stop()
        container.wait(timeout=30)
        health.shutdown()


if __name__ == "__main__":
    raise SystemExit(main())
