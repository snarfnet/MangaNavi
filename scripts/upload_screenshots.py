#!/usr/bin/env python3
"""Upload MangaNavi App Store screenshots."""

from __future__ import annotations

import hashlib
import os
import sys
import time
from pathlib import Path
from typing import Any

import jwt
import requests
from PIL import Image, ImageOps


KEY_ID = os.environ["ASC_KEY_ID"]
ISSUER_ID = os.environ["ASC_ISSUER_ID"]
PRIVATE_KEY = os.environ.get("ASC_PRIVATE_KEY")
APP_ID = "6767800620"
VERSION_STRING = os.environ.get("VERSION_STRING", "1.0")
BASE_URL = "https://api.appstoreconnect.apple.com/v1"
ROOT_DIR = Path(__file__).resolve().parents[1]
SCREENSHOT_DIR = ROOT_DIR / "output" / "ios-screenshots"
GENERATED_DIR = ROOT_DIR / "output" / "generated-screenshots"

if not PRIVATE_KEY:
    key_path = Path(
        os.environ.get(
            "ASC_KEY_PATH",
            Path.home() / ".appstoreconnect" / "private_keys" / f"AuthKey_{KEY_ID}.p8",
        )
    )
    PRIVATE_KEY = key_path.read_text(encoding="utf-8")


def make_token() -> str:
    now = int(time.time())
    return jwt.encode(
        {"iss": ISSUER_ID, "iat": now, "exp": now + 1200, "aud": "appstoreconnect-v1"},
        PRIVATE_KEY,
        algorithm="ES256",
        headers={"kid": KEY_ID, "typ": "JWT"},
    )


def request(method: str, path: str, **kwargs: Any) -> requests.Response:
    headers = kwargs.pop("headers", {})
    headers.update(
        {
            "Authorization": f"Bearer {make_token()}",
            "Content-Type": "application/json",
        }
    )
    response = requests.request(method, f"{BASE_URL}{path}", headers=headers, timeout=90, **kwargs)
    if response.status_code >= 400:
        print(f"{method} {path} -> {response.status_code}")
        print(response.text[:1200])
    return response


def must(response: requests.Response, message: str) -> dict[str, Any]:
    if response.status_code >= 400:
        print(message)
        sys.exit(1)
    return response.json() if response.content else {}


def editable_version() -> dict[str, Any]:
    states = "PREPARE_FOR_SUBMISSION,DEVELOPER_REJECTED,REJECTED,METADATA_REJECTED,READY_FOR_REVIEW"
    data = must(
        request("GET", f"/apps/{APP_ID}/appStoreVersions?limit=10&filter[platform]=IOS&filter[appStoreState]={states}"),
        "Version lookup failed.",
    ).get("data", [])
    for version in data:
        if version["attributes"].get("versionString") == VERSION_STRING:
            return version
    print(f"No editable App Store version found for {VERSION_STRING}.")
    sys.exit(1)


def localizations(version_id: str) -> dict[str, str]:
    data = must(
        request("GET", f"/appStoreVersions/{version_id}/appStoreVersionLocalizations"),
        "Localization lookup failed.",
    ).get("data", [])
    return {item["attributes"]["locale"]: item["id"] for item in data}


def get_or_create_screenshot_set(localization_id: str, device_type: str) -> str:
    sets = must(
        request("GET", f"/appStoreVersionLocalizations/{localization_id}/appScreenshotSets"),
        "Screenshot set lookup failed.",
    ).get("data", [])
    for item in sets:
        if item["attributes"].get("screenshotDisplayType") == device_type:
            return item["id"]

    payload = {
        "data": {
            "type": "appScreenshotSets",
            "attributes": {"screenshotDisplayType": device_type},
            "relationships": {
                "appStoreVersionLocalization": {
                    "data": {"type": "appStoreVersionLocalizations", "id": localization_id}
                }
            },
        }
    }
    return must(request("POST", "/appScreenshotSets", json=payload), "Screenshot set creation failed.")["data"]["id"]


def delete_existing_screenshots(set_id: str) -> None:
    screenshots = must(
        request("GET", f"/appScreenshotSets/{set_id}/appScreenshots?limit=50"),
        "Screenshot lookup failed.",
    ).get("data", [])
    for screenshot in screenshots:
        response = request("DELETE", f"/appScreenshots/{screenshot['id']}")
        if response.status_code not in {200, 202, 204, 404}:
            print("Could not delete an existing screenshot.")
            sys.exit(1)
        print(f"Deleted old screenshot {screenshot['id']}")


def clear_all_screenshot_sets(localization_id: str) -> None:
    sets = must(
        request("GET", f"/appStoreVersionLocalizations/{localization_id}/appScreenshotSets?limit=200"),
        "Screenshot set lookup failed.",
    ).get("data", [])
    for item in sets:
        display_type = item["attributes"].get("screenshotDisplayType", "unknown")
        print(f"Clearing old screenshots for {display_type}")
        delete_existing_screenshots(item["id"])


def upload_screenshot(set_id: str, path: Path) -> None:
    data = path.read_bytes()
    checksum = hashlib.md5(data).hexdigest()
    payload = {
        "data": {
            "type": "appScreenshots",
            "attributes": {"fileSize": len(data), "fileName": path.name},
            "relationships": {"appScreenshotSet": {"data": {"type": "appScreenshotSets", "id": set_id}}},
        }
    }
    screenshot = must(request("POST", "/appScreenshots", json=payload), f"Could not reserve {path.name}.")["data"]
    screenshot_id = screenshot["id"]

    for operation in screenshot["attributes"]["uploadOperations"]:
        headers = {header["name"]: header["value"] for header in operation.get("requestHeaders", [])}
        offset = operation.get("offset", 0)
        length = operation.get("length", len(data))
        upload = requests.request(
            operation["method"],
            operation["url"],
            headers=headers,
            data=data[offset : offset + length],
            timeout=90,
        )
        if upload.status_code >= 400:
            print(f"Upload failed for {path.name}: {upload.status_code}")
            print(upload.text[:800])
            sys.exit(1)

    commit_payload = {
        "data": {
            "type": "appScreenshots",
            "id": screenshot_id,
            "attributes": {"uploaded": True, "sourceFileChecksum": checksum},
        }
    }
    must(request("PATCH", f"/appScreenshots/{screenshot_id}", json=commit_payload), f"Could not finish {path.name}.")
    print(f"Uploaded {path.name}")


def resized_sources(sources: list[Path], device_type: str, size: tuple[int, int]) -> list[Path]:
    if device_type == "APP_IPHONE_67":
        return sources

    GENERATED_DIR.mkdir(parents=True, exist_ok=True)
    resized: list[Path] = []
    for source in sources:
        target = GENERATED_DIR / source.name.replace("iphone-6-7", device_type.lower())
        with Image.open(source) as image:
            rgb = image.convert("RGB")
            fitted = ImageOps.fit(rgb, size, method=Image.Resampling.LANCZOS, centering=(0.5, 0.5))
            fitted.save(target, "PNG", optimize=True)
        resized.append(target)
    return resized


def main() -> None:
    print("=== MangaNavi screenshot upload ===")
    screenshots = sorted(SCREENSHOT_DIR.glob("*-iphone-6-7.png"))
    if not screenshots:
        print(f"No screenshots found in {SCREENSHOT_DIR}.")
        sys.exit(1)

    version = editable_version()
    version_id = version["id"]
    print(f"Using App Store version {VERSION_STRING} ({version_id})")

    locs = localizations(version_id)
    localization_id = locs.get("ja") or locs.get("ja-JP")
    if not localization_id:
        print("No Japanese localization found. Run submit.py once before uploading screenshots.")
        sys.exit(1)

    clear_all_screenshot_sets(localization_id)

    device_sets = {
        "APP_IPHONE_67": (1290, 2796),
        "APP_IPHONE_65": (1242, 2688),
        "APP_IPHONE_55": (1242, 2208),
        "APP_IPAD_PRO_3GEN_129": (2048, 2732),
    }
    for device_type, size in device_sets.items():
        set_screenshots = resized_sources(screenshots, device_type, size)
        print(f"Uploading {len(set_screenshots)} screenshots for {device_type}")
        set_id = get_or_create_screenshot_set(localization_id, device_type)
        for screenshot in set_screenshots:
            upload_screenshot(set_id, screenshot)
    print("Screenshot upload finished.")


if __name__ == "__main__":
    main()
