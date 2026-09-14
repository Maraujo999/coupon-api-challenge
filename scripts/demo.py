"""Configure disposable local demo credentials or run the packaged application."""
import argparse
import os
from pathlib import Path
import secrets
import subprocess

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument("action", choices=["configure", "run", "smoke"])
args = parser.parse_args()
env_file = root / ".env"
if args.action == "configure":
    if env_file.exists():
        print(".env already exists; preserved.")
    else:
        # Exclusive creation preserves an existing user configuration.
        with env_file.open("x", encoding="utf-8") as file:
            file.write("COUPON_SECURITY_USERNAME=admin\nCOUPON_SECURITY_PASSWORD=" + secrets.token_urlsafe(24) + "\n")
        print("Created .env with random demo credentials. Keep this file private.")
else:
    if not env_file.exists():
        raise SystemExit("Run: python scripts/demo.py configure")
    env = os.environ.copy()
    for line in env_file.read_text(encoding="utf-8").splitlines():
        if line.strip() and not line.lstrip().startswith("#"):
            key, value = line.split("=", 1)
            env[key.strip()] = value.strip()
    env["SPRING_PROFILES_ACTIVE"] = "demo"
    if args.action == "run":
        jar = root / "target" / "coupon-api-1.0.0.jar"
        if not jar.exists():
            raise SystemExit("Build first with Maven verify.")
        java = ["java", "-Duser.timezone=UTC"]
        if os.name == "nt":
            (root / "tmp").mkdir(exist_ok=True)
            java.append("-Djdk.net.unixdomain.tmpdir=" + str(root / "tmp"))
        raise SystemExit(subprocess.call(java + ["-jar", str(jar)], cwd=root, env=env))
    os.environ.update(env)
    from smoke import exercise
    exercise("http://127.0.0.1:" + env.get("COUPON_PORT", "8080"))
