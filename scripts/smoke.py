"""Exercise the running API without printing credentials or JWTs."""
import argparse
from datetime import datetime, timedelta, timezone
from decimal import Decimal
import json
import os
import time
import urllib.error
import urllib.request


def exercise(base_url, wait_seconds=90, is_alive=None):
    base_url = base_url.rstrip("/")
    password = os.environ.get("COUPON_SECURITY_PASSWORD")
    if not password:
        raise RuntimeError("Set COUPON_SECURITY_PASSWORD before running the smoke test.")
    checks = []

    def request(method, path, payload=None, token=None, expected=200, raw=None):
        headers = {}
        content = raw
        if payload is not None:
            content = json.dumps(payload).encode("utf-8")
        if content is not None:
            headers["Content-Type"] = "application/json"
        if token:
            headers["Authorization"] = "Bearer " + token
        req = urllib.request.Request(base_url + path, data=content, method=method, headers=headers)
        try:
            response = urllib.request.urlopen(req, timeout=10)
        except urllib.error.HTTPError as error:
            response = error
        with response:
            body = response.read()
            assert response.status == expected, f"{method} {path}: expected {expected}, got {response.status}"
            checks.append({"method": method, "path": path, "status": response.status})
            if response.status >= 400:
                problem = json.loads(body)
                assert problem["status"] == expected
                assert problem["code"] and problem["correlationId"]
                assert response.headers["X-Correlation-ID"] == problem["correlationId"]
            return response.headers, body

    deadline = time.monotonic() + wait_seconds
    while True:
        if is_alive is not None and not is_alive():
            raise RuntimeError("Application process exited before becoming ready; inspect its log.")
        try:
            with urllib.request.urlopen(base_url + "/actuator/health/readiness", timeout=3) as response:
                if response.status == 200:
                    break
        except (urllib.error.URLError, TimeoutError):
            pass
        if time.monotonic() >= deadline:
            raise RuntimeError("Application did not become ready before timeout.")
        time.sleep(0.5)

    request("GET", "/actuator/health")
    _, specification = request("GET", "/v3/api-docs")
    spec = json.loads(specification)
    assert spec["components"]["securitySchemes"]["bearerAuth"]["scheme"] == "bearer"
    assert "204" in spec["paths"]["/coupon/{id}"]["delete"]["responses"]
    assert "code" in spec["components"]["schemas"]["ApiProblem"]["properties"]
    assert "409" in spec["paths"]["/coupon/{id}"]["delete"]["responses"]

    _, login = request("POST", "/auth/token", {
        "username": os.environ.get("COUPON_SECURITY_USERNAME", "admin"),
        "password": password,
    })
    token = json.loads(login)["accessToken"]
    data = {
        "code": "ABC-123", "description": "runtime-validation-marker",
        "discountValue": 0.5001,
        "expirationDate": (datetime.now(timezone.utc) + timedelta(days=1)).isoformat().replace("+00:00", "Z"),
    }
    headers, body = request("POST", "/coupon", data, token, 201)
    coupon = json.loads(body, parse_float=Decimal)
    assert set(coupon) == {"id", "code", "description", "discountValue", "expirationDate", "status", "published", "redeemed"}
    assert coupon["code"] == "ABC123" and coupon["discountValue"] == Decimal("0.5001")
    assert coupon["published"] is False and coupon["redeemed"] is False
    path = "/coupon/" + coupon["id"]
    assert headers["Location"].endswith(path)
    _, fetched = request("GET", path, token=token)
    assert json.loads(fetched, parse_float=Decimal) == coupon
    request("GET", path, expected=401)
    request("GET", path, token=token + "broken", expected=401)
    request("GET", "/coupon/not-a-uuid", token=token, expected=400)
    request("POST", "/coupon", token=token, raw=b"{", expected=400)
    request("POST", "/coupon", {**data, "discountValue": 0.4999}, token, 422)
    request("POST", "/coupon", {**data, "code": "123"}, token, 422)
    request("POST", "/coupon", {**data, "description": " "}, token, 422)
    request("POST", "/coupon", token=token, raw=b" " * 65537, expected=413)
    _, deleted = request("DELETE", path, token=token, expected=204)
    assert deleted == b""
    request("GET", path, token=token, expected=404)
    request("DELETE", path, token=token, expected=409)
    result = {"status": "passed", "checks": len(checks), "baseUrl": base_url, "requests": checks}
    print(json.dumps({"status": result["status"], "checks": result["checks"]}))
    return result


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default="http://localhost:8080")
    parser.add_argument("--wait-seconds", type=int, default=90)
    args = parser.parse_args()
    exercise(args.base_url, args.wait_seconds)
