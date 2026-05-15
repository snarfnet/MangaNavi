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
PRIVACY_URL = "https://snarfnet.github.io/privacy.html"
COPYRIGHT = "2026 Tokyo Nasu"

# Keep the storefront text plain Japanese. The earlier constants are overwritten
# here because some terminals display the old source text as mojibake.
DESC_JA = """まとめ・よみきりは、次に読みたい漫画を探すための読み物ナビアプリです。

注目作品、ランキング、ジャンル別の作品一覧から、気になる漫画をすばやく見つけられます。作品のメモや評価も残せるので、あとで読みたい作品の整理にも使えます。

主な機能:
- 注目作品のチェック
- ランキング表示
- ジャンル別の作品探し
- 読書リスト
- 作品メモと評価

漫画選びで迷ったとき、次の一冊を探す入口として使ってください。"""

KEYWORDS = "漫画,マンガ,おすすめ,ランキング,読書リスト,ジャンル,読み切り,まとめ,comic,manga,book"
REVIEW_DETAIL = {
    "contactFirstName": "Tokyo",
    "contactLastName": "Nasu",
    "contactEmail": "tokyonasu@yahoo.co.jp",
    "contactPhone": "+81 80-2368-9194",
    "demoAccountRequired": False,
    "demoAccountName": "",
    "demoAccountPassword": "",
    "notes": (
        "No sign-in is required. Advertising remains enabled. "
        "This build shows the App Tracking Transparency permission request on first launch before Google Mobile Ads starts. "
        "The screenshots were updated to remove non-iOS status bar artwork."
    ),
}


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
        print(response.text[:1600])
    return response


def body(response: requests.Response) -> dict[str, Any]:
    try:
        return response.json()
    except ValueError:
        return {}


def must(response: requests.Response, message: str) -> dict[str, Any]:
    if response.status_code >= 400:
        print(message)
        sys.exit(1)
    return body(response)


def latest_build() -> dict[str, Any]:
    builds = must(request("GET", f"/apps/{APP_ID}/builds?limit=20"), "Build lookup failed.").get("data", [])
    if BUILD_NUMBER:
        for build in builds:
            if build["attributes"].get("version") == BUILD_NUMBER:
                return build

    candidates = [
        build for build in builds
        if build["attributes"].get("processingState") in {"VALID", "PROCESSING"}
    ]
    if not candidates:
        print("No valid or processing builds were found.")
        sys.exit(1)
    return sorted(candidates, key=lambda build: build["attributes"].get("uploadedDate", ""), reverse=True)[0]


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


def current_version() -> dict[str, Any]:
    states = "PREPARE_FOR_SUBMISSION,DEVELOPER_REJECTED,REJECTED,READY_FOR_REVIEW,WAITING_FOR_REVIEW,IN_REVIEW"
    path = f"/apps/{APP_ID}/appStoreVersions?limit=10&filter[platform]=IOS&filter[appStoreState]={states}"
    versions = must(request("GET", path), "Version lookup failed.").get("data", [])
    for version in versions:
        if version["attributes"].get("versionString") == VERSION_STRING:
            return version

    payload = {
        "data": {
            "type": "appStoreVersions",
            "attributes": {"platform": "IOS", "versionString": VERSION_STRING},
            "relationships": {"app": {"data": {"type": "apps", "id": APP_ID}}},
        }
    }
    return must(request("POST", "/appStoreVersions", json=payload), "Version creation failed.")["data"]


def version_state(version_id: str) -> str:
    data = must(request("GET", f"/appStoreVersions/{version_id}"), "Version state lookup failed.")
    return data["data"]["attributes"].get("appStoreState", "")


def assign_build(version_id: str, build_id: str) -> None:
    payload = {"data": {"type": "builds", "id": build_id}}
    response = request("PATCH", f"/appStoreVersions/{version_id}/relationships/build", json=payload)
    must(response, "Could not attach the build to the App Store version.")


def update_version_prerequisites(version_id: str) -> None:
    app_payload = {
        "data": {
            "type": "apps",
            "id": APP_ID,
            "attributes": {"contentRightsDeclaration": "DOES_NOT_USE_THIRD_PARTY_CONTENT"},
        }
    }
    response = request("PATCH", f"/apps/{APP_ID}", json=app_payload)
    if response.status_code >= 400 and "already" not in response.text.lower():
        print("Content rights update was not accepted.")

    version_payload = {
        "data": {
            "type": "appStoreVersions",
            "id": version_id,
            "attributes": {"copyright": COPYRIGHT, "usesIdfa": True},
        }
    }
    response = request("PATCH", f"/appStoreVersions/{version_id}", json=version_payload)
    if response.status_code >= 400:
        print("Version prerequisite update was not accepted.")

    update_app_info()
    update_review_detail(version_id)


def update_app_info() -> None:
    app_infos = must(request("GET", f"/apps/{APP_ID}/appInfos?limit=10"), "App info lookup failed.").get("data", [])
    if not app_infos:
        print("No app info found.")
        return

    app_info_id = app_infos[0]["id"]
    category_payload = {
        "data": {
            "type": "appInfos",
            "id": app_info_id,
            "relationships": {"primaryCategory": {"data": {"type": "appCategories", "id": "BOOKS"}}},
        }
    }
    response = request("PATCH", f"/appInfos/{app_info_id}", json=category_payload)
    if response.status_code >= 400:
        print("Category update was not accepted.")

    locs = request("GET", f"/appInfos/{app_info_id}/appInfoLocalizations?limit=20")
    if locs.status_code < 400:
        for loc in body(locs).get("data", []):
            attrs = {"privacyPolicyUrl": PRIVACY_URL}
            if loc["attributes"].get("locale") in {"ja", "ja-JP"}:
                attrs["subtitle"] = "次に読む漫画を探す"
            payload = {"data": {"type": "appInfoLocalizations", "id": loc["id"], "attributes": attrs}}
            response = request("PATCH", f"/appInfoLocalizations/{loc['id']}", json=payload)
            if response.status_code >= 400:
                print(f"App info localization update was not accepted for {loc['id']}.")

    update_age_rating(app_info_id)


def update_age_rating(app_info_id: str) -> None:
    response = request("GET", f"/appInfos/{app_info_id}/ageRatingDeclaration")
    if response.status_code >= 400:
        print("Age rating lookup was not accepted.")
        return

    data = body(response).get("data")
    if not data:
        print("No age rating declaration found.")
        return

    attrs = {
        "alcoholTobaccoOrDrugUseOrReferences": "NONE",
        "contests": "NONE",
        "gambling": False,
        "gamblingSimulated": "NONE",
        "gunsOrOtherWeapons": "NONE",
        "horrorOrFearThemes": "NONE",
        "matureOrSuggestiveThemes": "NONE",
        "medicalOrTreatmentInformation": "NONE",
        "profanityOrCrudeHumor": "NONE",
        "sexualContentGraphicAndNudity": "NONE",
        "sexualContentOrNudity": "NONE",
        "violenceCartoonOrFantasy": "NONE",
        "violenceRealistic": "NONE",
        "violenceRealisticProlongedGraphicOrSadistic": "NONE",
        "unrestrictedWebAccess": False,
        "advertising": False,
        "messagingAndChat": False,
        "userGeneratedContent": False,
        "lootBox": False,
        "healthOrWellnessTopics": False,
        "parentalControls": False,
        "ageAssurance": False,
    }
    payload = {"data": {"type": "ageRatingDeclarations", "id": data["id"], "attributes": attrs}}
    response = request("PATCH", f"/ageRatingDeclarations/{data['id']}", json=payload)
    if response.status_code >= 400:
        print("Age rating update was not accepted.")


def update_review_detail(version_id: str) -> None:
    response = request("GET", f"/appStoreVersions/{version_id}/appStoreReviewDetail")
    if response.status_code == 200 and body(response).get("data"):
        detail_id = body(response)["data"]["id"]
        payload = {"data": {"type": "appStoreReviewDetails", "id": detail_id, "attributes": REVIEW_DETAIL}}
        must(request("PATCH", f"/appStoreReviewDetails/{detail_id}", json=payload), "Review detail update failed.")
        return

    payload = {
        "data": {
            "type": "appStoreReviewDetails",
            "attributes": REVIEW_DETAIL,
            "relationships": {"appStoreVersion": {"data": {"type": "appStoreVersions", "id": version_id}}},
        }
    }
    response = request("POST", "/appStoreReviewDetails", json=payload)
    if response.status_code not in {200, 201}:
        must(response, "Review detail creation failed.")


def update_version_settings(version_id: str) -> None:
    payload = {
        "data": {
            "type": "appStoreVersions",
            "id": version_id,
            "attributes": {
                "copyright": "2026 Tokyo Nasu",
                "usesIdfa": True,
                "releaseType": "AFTER_APPROVAL",
            },
        }
    }
    response = request("PATCH", f"/appStoreVersions/{version_id}", json=payload)
    must(response, "Version settings update failed.")


def set_export_compliance(build_id: str) -> None:
    payload = {
        "data": {
            "type": "builds",
            "id": build_id,
            "attributes": {"usesNonExemptEncryption": False},
        }
    }
    response = request("PATCH", f"/builds/{build_id}", json=payload)
    if response.status_code >= 400 and "already set" not in response.text:
        print("Could not update export compliance.")
        sys.exit(1)


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
    submissions = must(
        request("GET", f"/apps/{APP_ID}/reviewSubmissions?limit=10"),
        "Review submission lookup failed.",
    ).get("data", [])
    for submission in submissions:
        state = submission["attributes"].get("state")
        if state in {"READY_FOR_REVIEW", "WAITING_FOR_REVIEW", "IN_REVIEW"}:
            return submission["id"]
    return None


def cancel_submission(submission_id: str) -> bool:
    response = request(
        "PATCH",
        f"/reviewSubmissions/{submission_id}",
        json={
            "data": {
                "type": "reviewSubmissions",
                "id": submission_id,
                "attributes": {"canceled": True},
            }
        },
    )
    if response.status_code < 400:
        print(f"Canceled review submission {submission_id}.")
        time.sleep(10)
        return True
    print(f"Could not cancel review submission {submission_id}.")
    return False


def cancel_blocking_submissions() -> None:
    submissions = must(
        request("GET", f"/apps/{APP_ID}/reviewSubmissions?limit=20"),
        "Review submission lookup failed.",
    ).get("data", [])
    for submission in submissions:
        state = submission["attributes"].get("state")
        if state in {"UNRESOLVED_ISSUES", "READY_FOR_REVIEW"}:
            cancel_submission(submission["id"])


def submission_items(submission_id: str) -> list[dict[str, Any]]:
    data = must(
        request("GET", f"/reviewSubmissions/{submission_id}/items?include=appStoreVersion&limit=50"),
        f"Review submission item lookup failed for {submission_id}.",
    )
    return data.get("data", [])


def delete_stale_submission_item(submission_id: str, version_id: str) -> str:
    for item in submission_items(submission_id):
        relationship = item.get("relationships", {}).get("appStoreVersion", {}).get("data")
        if not relationship or relationship.get("id") != version_id:
            continue

        item_id = item["id"]
        print(f"Removing stale review submission item {item_id} from {submission_id}.")
        response = request("DELETE", f"/reviewSubmissionItems/{item_id}")
        if response.status_code in {200, 202, 204}:
            return "removed"
        if response.status_code == 409 and "already submitted" in response.text:
            return "already_submitted"

        print("Could not remove the stale review submission item.")
        return "failed"

    print(f"No matching stale review submission item found in {submission_id}.")
    return "missing"


def delete_existing_version_submission(version_id: str) -> bool:
    response = request("GET", f"/appStoreVersions/{version_id}/appStoreVersionSubmission")
    if response.status_code == 404:
        print("No existing appStoreVersionSubmission found.")
        return False

    data = must(response, "App Store version submission lookup failed.").get("data")
    if not data:
        print("No existing appStoreVersionSubmission found.")
        return False

    submission_id = data["id"]
    print(f"Removing existing appStoreVersionSubmission {submission_id}.")
    delete_response = request("DELETE", f"/appStoreVersionSubmissions/{submission_id}")
    return delete_response.status_code in {200, 202, 204}


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
            old_id = match.group(1)
            print(f"App version is still attached to old review submission {old_id}.")
            stale_result = delete_stale_submission_item(old_id, version_id)
            if stale_result == "removed":
                print("Retrying review submission item creation.")
                retry = request("POST", "/reviewSubmissionItems", json=payload)
                if retry.status_code in {200, 201}:
                    return submission_id
                response = retry
            elif stale_result == "already_submitted":
                if cancel_submission(old_id):
                    print("Retrying review submission item creation after canceling the old review submission.")
                    retry = request("POST", "/reviewSubmissionItems", json=payload)
                    if retry.status_code in {200, 201}:
                        return submission_id
                    response = retry
                elif delete_existing_version_submission(version_id):
                    print("Retrying review submission item creation after deleting appStoreVersionSubmission.")
                    retry = request("POST", "/reviewSubmissionItems", json=payload)
                    if retry.status_code in {200, 201}:
                        return submission_id
                    response = retry
                else:
                    print(f"Using already submitted review submission {old_id}.")
                    return old_id
            else:
                print("Remove that old item in App Store Connect and rerun.")
                sys.exit(1)

    must(response, "Could not add the app version to the review submission.")
    return submission_id


def submit(submission_id: str, version_id: str) -> None:
    payload = {
        "data": {
            "type": "reviewSubmissions",
            "id": submission_id,
            "attributes": {"submitted": True},
        }
    }

    for attempt in range(1, 31):
        response = request("PATCH", f"/reviewSubmissions/{submission_id}", json=payload)
        if response.status_code < 400:
            print("Submitted for App Store review.")
            return

        state = version_state(version_id)
        print(f"Review submission is not ready yet ({attempt}/30). Version state: {state}")
        if state in {"READY_FOR_REVIEW", "WAITING_FOR_REVIEW", "IN_REVIEW"}:
            print("App Store Connect reports the version is now in review flow.")
            return
        time.sleep(30)

    print("Could not submit for review.")
    submit_app_store_version(version_id)


def submit_app_store_version(version_id: str) -> None:
    payload = {
        "data": {
            "type": "appStoreVersionSubmissions",
            "relationships": {
                "appStoreVersion": {"data": {"type": "appStoreVersions", "id": version_id}},
            },
        }
    }
    response = request("POST", "/appStoreVersionSubmissions", json=payload)
    if response.status_code in {200, 201}:
        print("Submitted for App Store review with appStoreVersionSubmissions.")
        return

    print("Could not submit with appStoreVersionSubmissions either.")
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
    version = current_version()
    version_id = version["id"]
    version_status = version["attributes"].get("appStoreState")
    print(f"Using App Store version {VERSION_STRING} ({version_id}), state: {version_status}")
    if version_status in {"WAITING_FOR_REVIEW", "IN_REVIEW"}:
        print("Already submitted for App Store review.")
        return

    update_version_settings(version_id)
    assign_build(version_id, build_id)
    update_version_prerequisites(version_id)
    update_localization(version_id)
    cancel_blocking_submissions()
    submission_id = create_submission()
    print(f"Using review submission {submission_id}")
    submission_id = add_submission_item(submission_id, version_id)
    submit(submission_id, version_id)


if __name__ == "__main__":
    main()
