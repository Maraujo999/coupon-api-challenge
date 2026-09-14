import contextlib
import io
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from check_logs import check


class LogChecksTest(unittest.TestCase):
    def inspect(self, extra=""):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "application.log"
            path.write_text("coupon_created\ncoupon_deleted\nhttp_request\n" + extra, encoding="utf-8")
            with contextlib.redirect_stdout(io.StringIO()):
                return check(path)

    def test_clean_logs_are_accepted(self):
        with patch.dict(os.environ, {"COUPON_SECURITY_PASSWORD": "test-fixture-password"}):
            self.assertEqual("passed", self.inspect()["status"])

    def test_errors_tokens_passwords_and_request_bodies_are_rejected(self):
        with patch.dict(os.environ, {"COUPON_SECURITY_PASSWORD": "test-fixture-password"}):
            for value in [" ERROR failure", "eyJabcdefghijk.abcdef.signature",
                          "test-fixture-password", "runtime-validation-marker"]:
                with self.subTest(value=value), self.assertRaises(RuntimeError):
                    self.inspect(value)


if __name__ == "__main__":
    unittest.main()
