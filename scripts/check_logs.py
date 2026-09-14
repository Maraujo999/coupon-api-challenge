"""Validate operational logs without printing matching sensitive content."""
import argparse
import json
import os
from pathlib import Path
import re


def check(path):
    text = Path(path).read_text(encoding="utf-8", errors="replace")
    problems = []
    if re.search(r"\bERROR\b|Application run failed", text):
        problems.append("Unexpected ERROR in application logs")
    if re.search(r"eyJ[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+", text):
        problems.append("JWT found in application logs")
    password = os.environ.get("COUPON_SECURITY_PASSWORD", "")
    if password and password in text:
        problems.append("Configured password found in application logs")
    if "runtime-validation-marker" in text:
        problems.append("Request description found in application logs")
    if "coupon_created" not in text or "coupon_deleted" not in text:
        problems.append("Missing committed business events")
    if "http_request" not in text:
        problems.append("Missing request diagnostics")
    if problems:
        raise RuntimeError("; ".join(problems))
    warnings = [line for line in text.splitlines() if " WARN " in line or re.search(r'"(?:log\.)?level"\s*:\s*"WARN"', line)]
    result = {"status": "passed", "warnings": len(warnings), "errors": 0, "sensitiveData": False}
    print(json.dumps(result))
    return result


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("path")
    check(parser.parse_args().path)
