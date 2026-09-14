"""Launch the packaged JAR, exercise real HTTP, inspect logs and stop this process."""
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import secrets
import socket
import subprocess
import sys

from smoke import exercise
from check_logs import check

root = Path(__file__).resolve().parents[1]
log_dir = root / "tmp" / "runtime"
log_dir.mkdir(parents=True, exist_ok=True)
jar = root / "target" / "coupon-api-1.0.0.jar"
if not jar.is_file():
    raise SystemExit("Build the application with Maven verify first.")
with socket.socket() as port_socket:
    port_socket.bind(("127.0.0.1", 0))
    port = port_socket.getsockname()[1]
environment = os.environ.copy()
environment.update({
    "SPRING_PROFILES_ACTIVE": "demo",
    "COUPON_SECURITY_PASSWORD": secrets.token_urlsafe(24),
    "COUPON_SECURITY_USERNAME": "runtime-check",
    "SERVER_PORT": str(port),
    "LOGGING_STRUCTURED_FORMAT_CONSOLE": "ecs",
})
# The helper functions use this process's environment; changes are confined to this verifier.
os.environ.update({key: environment[key] for key in ("COUPON_SECURITY_PASSWORD", "COUPON_SECURITY_USERNAME")})
log_path = log_dir / "application.log"
with log_path.open("w", encoding="utf-8") as output:
    java = ["java", "-Xmx384m", "-Duser.timezone=UTC"]
    if os.name == "nt":
        # Avoid virtualized AppData socket paths in packaged Windows desktop hosts.
        java.append("-Djdk.net.unixdomain.tmpdir=" + str(root / "tmp"))
    process = subprocess.Popen(java + ["-jar", str(jar)],
                               cwd=root, env=environment, stdout=output, stderr=subprocess.STDOUT)
    try:
        http = exercise(f"http://127.0.0.1:{port}", is_alive=lambda: process.poll() is None)
    finally:
        process.terminate()
        try:
            process.wait(timeout=20)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait(timeout=5)
logs = check(log_path)
report = {"executedAt": datetime.now(timezone.utc).isoformat(), "http": http, "logs": logs}
(log_dir / "result.json").write_text(json.dumps(report, indent=2), encoding="utf-8")
print("Runtime verification passed. Sanitized report: tmp/runtime/result.json")
