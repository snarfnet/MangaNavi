#!/usr/bin/env python3
"""Upload metadata and submit MangaNavi for App Store review."""

from __future__ import annotations

import os
import re
import sys
import time
from typing import Any

import jwt
import requests


KEY_ID = os.environ["ASC_KEY_ID"]
ISSUER_ID = os.environ["ASC_ISSUER_ID"]
PRIVATE_KEY = os.environ["ASC_PRIVATE_KEY"]
APP_ID = "6767800620"
VERSION_STRING = os.environ.get("VERSION_STRING", "1.0")
BUILD_NUMBER = os.environ.get("BUILD_NUMBER")
BASE_URL = "https://api.appstoreconnect.apple.com/v1"

DESC_JA = """MangaNaviは、次に読みたい漫画をすばやく見つけるための漫画ナビアプリです。

ランキング、ジャンル、気分に合うおすすめから、気になる作品を探せます。落ち着いた画面で情報を見比べやすく、読書リストにもすぐ残せます。

主な機能:
- 注目作品のおすすめ
- ランキング表示
- ジャンル別の作品探し
- 読書リスト
- 作品メモと評価の確認

漫画選びに迷ったとき、次の一冊を探す入口として使ってください。"""

KEYWORDS = "漫画,マンガ,おすすめ,ランキング,読書リスト,ジャンル,青年漫画,comic,manga,book"
SUPPORT_URL = "https://snarfnet.github.io/"


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
    response = requests.request(method, f"{BASE_URL}{path}", headers=headers, timeout=60, **kwargs)
    if response.status_code >= 400:
        print(f"{method} {path} -> {response.status_code}")
        print(response.text[:1200])
    return response


def json_data(response: requests.Response) -> dict[str, Any]:
    try:
        return response.json()
    except ValueError:
        return {}


def must(response: requests.Response, message: str) -> dict[str, Any]:
    if response.status_code >= 400:
        print(message)
        sys.exit(1)
    return json_data(response)


def latest_build() -> dict[str, Any]:
    query = f"/apps/{APP_ID}/builds?limit=20"
    builds = must(request("GET", query), "Build lookup failed.").get("data", [])
    if BUILD_NUMBER:
        matching = [build for build in builds if build["attributes"].get("version") == BUILD_NUMBER]
        if matching:
            return matching[0]
    builds = [
        build
        for build in builds
        if build["attributes"].get("processingState") in {"VALID", "PROCESSING"}
    ]
    if not builds:
        print("No valid or processing builds were found.")
        sys.exit(1)
    return sorted(builds, key=lambda build: build["attributes"].get("uploadedDate", ""), reverse=True)[0]


def wait_until_valid(build_id: str) -> None:
    for attempt in range(1, 81):
        data = must(request("GET", f"/builds/{build_id}"), "Build status lookup failed.")
        state = data["data"]["attributes"]["processingState"]
        print(f"Build processing state: {state} ({attempt}/80)")
        if state == "VALID":
            return
        if state in {"FAILED", "INVALID"}:
            print("The build did not pass App Store processing.")
            sys.exit(1)
        time.sleep(30)
    print("Timed out waiting for App Store build processing.")
    sys.exit(1)


def editable_version() -> str:
    states = "PREPARE_FOR_SUBMISSION,DEVELOPER_REJECTED,REJECTED,READY_FOR_REVIEW"
    path = f"/apps/{APP_ID}/appStoreVersions?limit=10&filter[platform]=IOS&filter[appStoreState]={states}"
    versions = must(request("GET", path), "Version lookup failed.").get("data", [])
    for version in versions:
        if version["attributes"].get("versionString") == VERSION_STRING:
            return version["id"]

    payload = {
        "data": {
            "type": "appStoreVersions",
            "attributes": {"platform": "IOS", "versionString": VERSION_STRING},
            "relationships": {"app": {"data": {"type": "apps", "id": APP_ID}}},
        }
    }
    data = must(request("POST", "/appStoreVersions", json=payload), "Version creation failed.")
    return data["data"]["id"]


def assign_build(version_id: str, build_id: str) -> None:
    payload = {"data": {"type": "builds", "id": build_id}}
    response = request("PATCH", f"/appStoreVersions/{version_id}/relationships/build", json=payload)
    must(response, "Could not attach the build to the App Store version.")


def set_export_compliance(build_id: str) -> None:
    payload = {
        "data": {
            "type": "builds",
            "id": build_id,
            "attributes": {"usesNonExemptEncryption": False},
        }
    }
    request("PATCH", f"/builds/{build_id}", json=payload)


def update_localization(version_id: str) -> None:
    locs = must(
        request("GET", f"/appStoreVersions/{version_id}/appStoreVersionLocalizations"),
        "Localization lookup failed.",
    ).get("data", [])

    attributes = {
        "description": DESC_JA,
        "keywords": KEYWORDS,
        "marketingUrl": SUPPORT_URL,
        "supportUrl": SUPPORT_URL,
    }

    if locs:
        loc_id = locs[0]["id"]
        payload = {"data": {"type": "appStoreVersionLocalizations", "id": loc_id, "attributes": attributes}}
        must(request("PATCH", f"/appStoreVersionLocalizations/{loc_id}", json=payload), "Localization update failed.")
        return

    payload = {
        "data": {
            "type": "appStoreVersionLocalizations",
            "attributes": {"locale": "ja", **attributes},
            "relationships": {"appStoreVersion": {"data": {"type": "appStoreVersions", "id": version_id}}},
        }
    }
    must(request("POST", "/appStoreVersionLocalizations", json=payload), "Localization creation failed.")


def active_submission() -> str | None:
    data = must(
        request("GET", f"/apps/{APP_ID}/reviewSubmissions?limit=10"),
        "Review submission lookup failed.",
    ).get("data", [])
    for submission in data:
        state = submission["attributes"].get("state")
        if state in {"READY_FOR_REVIEW", "WAITING_FOR_REVIEW", "IN_REVIEW"}:
            return submission["id"]
    return None


def create_submission() -> str:
    existing = active_submission()
    if existing:
        return existing

    payload = {
        "data": {
            "type": "reviewSubmissions",
            "attributes": {"platform": "IOS"},
            "relationships": {"app": {"data": {"type": "apps", "id": APP_ID}}},
        }
    }
    response = request("POST", "/reviewSubmissions", json=payload)
    if response.status_code == 409:
        existing = active_submission()
        if existing:
            return existing
    return must(response, "Review submission creation failed.")["data"]["id"]


def delete_submission_items(submission_id: str) -> bool:
    response = request("GET", f"/reviewSubmissions/{submission_id}/items")
    if response.status_code >= 400:
        return False

    items = json_data(response).get("data", [])
    if not items:
        return False

    for item in items:
        item_id = item["id"]
        delete_response = request("DELETE", f"/reviewSubmissionItems/{item_id}")
        if delete_response.status_code >= 400:
            return False
        print(f"Removed old review submission item {item_id}.")
    return True


def add_submission_item(submission_id: str, version_id: str) -> str:
    payload = {
        "data": {
            "type": "reviewSubmissionItems",
            "relationships": {
                "reviewSubmission": {"data": {"type": "reviewSubmissions", "id": submission_id}},
                "appStoreVersion": {"data": {"type": "appStoreVersions", "id": version_id}},
            },
        }
    }
    response = request("POST", "/reviewSubmissionItems", json=payload)
    if response.status_code in {200, 201}:
        return submission_id
    if response.status_code == 409:
        match = re.search(r"another reviewSubmission with id ([0-9a-f-]+)", response.text)
        if match:
            existing_id = match.group(1)
            print(f"App version is already attached to review submission {existing_id}.")
            if delete_submission_items(existing_id):
                retry = request("POST", "/reviewSubmissionItems", json=payload)
                if retry.status_code in {200, 201}:
                    return submission_id
            return existing_id
        return submission_id
    must(response, "Could not add the app version to the review submission.")
    return submission_id


def submit(submission_id: str) -> None:
    payload = {
        "data": {
            "type": "reviewSubmissions",
            "id": submission_id,
            "attributes": {"submitted": True},
        }
    }

    for attempt in range(1, 21):
        response = request("PATCH", f"/reviewSubmissions/{submission_id}", json=payload)
        if response.status_code < 400:
            print("Submitted for App Store review.")
            return
        print(f"Review submission is not ready yet ({attempt}/20).")
        time.sleep(30)

    print("Could not submit for review.")
    sys.exit(1)


def main() -> None:
    print("=== MangaNavi App Store submission ===")
    build = latest_build()
    build_id = build["id"]
    build_number = build["attributes"].get("version")
    state = build["attributes"].get("processingState")
    print(f"Using build {build_number} ({build_id}), state: {state}")

    if state != "VALID":
        wait_until_valid(build_id)

    set_export_compliance(build_id)
    version_id = editable_version()
    print(f"Using App Store version {VERSION_STRING} ({version_id})")
    assign_build(version_id, build_id)
    update_localization(version_id)
    submission_id = create_submission()
    print(f"Using review submission {submission_id}")
    submission_id = add_submission_item(submission_id, version_id)
    submit(submission_id)


if __name__ == "__main__":
    main()
