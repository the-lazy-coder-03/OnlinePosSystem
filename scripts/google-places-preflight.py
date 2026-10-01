#!/usr/bin/env python3
"""Verify a browser-restricted Google Places key without exposing it."""

from __future__ import annotations

import json
import os
import re
import sys
import urllib.error
import urllib.request
from urllib.parse import urlparse


DEFAULT_ENDPOINT = "https://places.googleapis.com/v1/places:autocomplete"
SAFE_REASON = re.compile(r"^[A-Z0-9_]+$")


def fail(message: str) -> int:
    print(message, file=sys.stderr)
    return 1


def provider_reason(payload: bytes) -> str | None:
    try:
        details = json.loads(payload).get("error", {}).get("details", [])
    except (AttributeError, json.JSONDecodeError, UnicodeDecodeError):
        return None
    for detail in details:
        reason = detail.get("reason") if isinstance(detail, dict) else None
        if isinstance(reason, str) and SAFE_REASON.fullmatch(reason):
            return reason
    return None


def main() -> int:
    api_key = sys.stdin.read().strip()
    if not api_key:
        return fail("Google Places preflight failed: GOOGLE_MAPS_API_KEY is empty")

    origin = (sys.argv[1] if len(sys.argv) > 1 else "https://crowdcam.co.za").rstrip("/")
    parsed_origin = urlparse(origin)
    if parsed_origin.scheme not in {"http", "https"} or not parsed_origin.netloc:
        return fail("Google Places preflight failed: invalid public origin")

    endpoint = os.environ.get("GOOGLE_PLACES_AUTOCOMPLETE_URL", DEFAULT_ENDPOINT)
    payload = json.dumps({
        "input": "12 Main Road Cape Town",
        "includedRegionCodes": ["za"],
        "languageCode": "en",
        "regionCode": "za",
    }).encode("utf-8")
    request = urllib.request.Request(
        endpoint,
        data=payload,
        method="POST",
        headers={
            "Content-Type": "application/json",
            "X-Goog-Api-Key": api_key,
            "X-Goog-FieldMask": "suggestions.placePrediction.placeId",
            "Origin": origin,
            "Referer": f"{origin}/",
            "User-Agent": "OnlinePosDeploymentCheck/1.0",
        },
    )

    try:
        with urllib.request.urlopen(request, timeout=20) as response:
            response_body = response.read()
    except urllib.error.HTTPError as error:
        reason = provider_reason(error.read())
        suffix = f" ({reason})" if reason else ""
        return fail(f"Google Places preflight failed with HTTP {error.code}{suffix}")
    except (urllib.error.URLError, TimeoutError):
        return fail("Google Places preflight failed: provider could not be reached")

    try:
        suggestions = json.loads(response_body).get("suggestions", [])
    except (AttributeError, json.JSONDecodeError, UnicodeDecodeError):
        return fail("Google Places preflight failed: provider returned an invalid response")

    predictions = [
        suggestion.get("placePrediction")
        for suggestion in suggestions
        if isinstance(suggestion, dict) and suggestion.get("placePrediction")
    ]
    if not predictions:
        return fail("Google Places preflight failed: provider returned no predictions")

    print(f"Google Places preflight passed with {len(predictions)} prediction(s)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
