import http.server
import json
import os
from pathlib import Path
import subprocess
import socketserver
import threading
import unittest


ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "scripts" / "google-places-preflight.py"


class ProviderHandler(http.server.BaseHTTPRequestHandler):
    response_status = 200
    response_body = {"suggestions": [{"placePrediction": {"placeId": "test-place"}}]}
    received_headers = {}

    def do_POST(self):
        type(self).received_headers = dict(self.headers)
        length = int(self.headers.get("Content-Length", "0"))
        self.rfile.read(length)
        body = json.dumps(type(self).response_body).encode()
        self.send_response(type(self).response_status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, _format, *_args):
        return


class LocalThreadingHttpServer(http.server.ThreadingHTTPServer):
    def server_bind(self):
        socketserver.TCPServer.server_bind(self)
        _host, self.server_port = self.server_address
        self.server_name = "127.0.0.1"


class GooglePlacesPreflightTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.server = LocalThreadingHttpServer(("127.0.0.1", 0), ProviderHandler)
        cls.thread = threading.Thread(target=cls.server.serve_forever, daemon=True)
        cls.thread.start()
        cls.endpoint = f"http://127.0.0.1:{cls.server.server_port}/autocomplete"

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()
        cls.thread.join(timeout=5)
        cls.server.server_close()

    def run_preflight(self, key="test-browser-key"):
        environment = os.environ.copy()
        environment["GOOGLE_PLACES_AUTOCOMPLETE_URL"] = self.endpoint
        return subprocess.run(
            ["python3", str(SCRIPT), "https://crowdcam.co.za"],
            input=key,
            text=True,
            capture_output=True,
            env=environment,
            check=False,
        )

    def setUp(self):
        ProviderHandler.response_status = 200
        ProviderHandler.response_body = {
            "suggestions": [{"placePrediction": {"placeId": "test-place"}}]
        }
        ProviderHandler.received_headers = {}

    def test_accepts_predictions_and_sends_production_referrer(self):
        result = self.run_preflight()
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("preflight passed", result.stdout)
        self.assertEqual("https://crowdcam.co.za/", ProviderHandler.received_headers["Referer"])
        self.assertEqual("test-browser-key", ProviderHandler.received_headers["X-Goog-Api-Key"])

    def test_rejects_provider_error_without_exposing_secret_or_body(self):
        ProviderHandler.response_status = 403
        ProviderHandler.response_body = {
            "error": {
                "message": "sensitive provider response",
                "details": [{"reason": "API_KEY_HTTP_REFERRER_BLOCKED"}],
            }
        }
        result = self.run_preflight("do-not-log-this-key")
        self.assertNotEqual(0, result.returncode)
        self.assertIn("HTTP 403 (API_KEY_HTTP_REFERRER_BLOCKED)", result.stderr)
        self.assertNotIn("do-not-log-this-key", result.stdout + result.stderr)
        self.assertNotIn("sensitive provider response", result.stdout + result.stderr)

    def test_rejects_empty_prediction_response(self):
        ProviderHandler.response_body = {"suggestions": []}
        result = self.run_preflight()
        self.assertNotEqual(0, result.returncode)
        self.assertIn("no predictions", result.stderr)


if __name__ == "__main__":
    unittest.main()
