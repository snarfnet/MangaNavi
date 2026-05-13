#!/usr/bin/env python3
"""ASC submission script for MangaNavi."""
import os, sys, time

try:
    import jwt
    import requests
except ImportError:
    os.system("pip install PyJWT cryptography requests")
    import jwt, requests

KEY_ID      = os.environ["ASC_KEY_ID"]
ISSUER_ID   = os.environ["ASC_ISSUER_ID"]
PRIVATE_KEY = os.environ["ASC_PRIVATE_KEY"]
APP_ID      = "6767800620"

BASE = "https://api.appstoreconnect.apple.com"

def token():
    now = int(time.time())
    return jwt.encode({"iss": ISSUER_ID, "iat": now, "exp": now+1200,
                       "aud": "appstoreconnect-v1"},
                      PRIVATE_KEY, algorithm="ES256",
                      headers={"kid": KEY_ID, "typ": "JWT"})

def api(method, path, **kw):
    r = requests.request(method, BASE+"/v1"+path,
                         headers={"Authorization": f"Bearer {token()}",
                                  "Content-Type": "application/json"}, **kw)
    if r.status_code >= 400:
        print(f"  ERR {r.status_code}: {r.text[:300]}")
    return r

def wait_build(build_id, attempts=80):
    for i in range(attempts):
        r = api("GET", f"/builds/{build_id}")
        state = r.json()["data"]["attributes"]["processingState"]
        print(f"  Build state: {state} ({i+1}/{attempts})")
        if state == "VALID": return True
        if state in ["INVALID", "FAILED"]: return False
        time.sleep(30)
    return False

def main():
    print("=== MangaNavi submit ===")
    r = api("GET", f"/apps/{APP_ID}/builds?limit=1&filter[processingState]=VALID,PROCESSING")
    builds = r.json().get("data", [])
    if not builds:
        print("No builds found"); sys.exit(1)
    build_id = builds[0]["id"]
    build_ver = builds[0]["attributes"]["version"]
    print(f"Build: {build_ver} ({build_id})")

    if builds[0]["attributes"]["processingState"] != "VALID":
        if not wait_build(build_id):
            print("Build not valid"); sys.exit(1)

    r = api("GET", f"/apps/{APP_ID}/appStoreVersions?filter[platform]=IOS&filter[appStoreState]=PREPARE_FOR_SUBMISSION,READY_FOR_REVIEW,DEVELOPER_REJECTED")
    versions = r.json().get("data", [])
    if versions:
        version_id = versions[0]["id"]
        print(f"Existing version: {version_id}")
    else:
        r = api("POST", "/appStoreVersions", json={"data": {"type": "appStoreVersions",
            "attributes": {"platform": "IOS", "versionString": "1.0"},
            "relationships": {"app": {"data": {"type": "apps", "id": APP_ID}}}}})
        version_id = r.json()["data"]["id"]
        print(f"Created version: {version_id}")

    api("PATCH", f"/appStoreVersions/{version_id}/relationships/build",
        json={"data": {"type": "builds", "id": build_id}})

    r = api("GET", f"/appStoreVersions/{version_id}/appStoreVersionLocalizations")
    locs = r.json().get("data", [])
    loc_id = locs[0]["id"] if locs else None
    if not loc_id:
        r = api("POST", "/appStoreVersionLocalizations", json={"data": {"type": "appStoreVersionLocalizations",
            "attributes": {"locale": "ja", "description": DESC_JA, "keywords": KEYWORDS,
                "marketingUrl": "https://snarfnet.github.io/",
                "supportUrl": "https://snarfnet.github.io/"},
            "relationships": {"appStoreVersion": {"data": {"type": "appStoreVersions", "id": version_id}}}}})
    else:
        api("PATCH", f"/appStoreVersionLocalizations/{loc_id}", json={"data": {"type": "appStoreVersionLocalizations",
            "id": loc_id, "attributes": {"description": DESC_JA, "keywords": KEYWORDS,
                "marketingUrl": "https://snarfnet.github.io/",
                "supportUrl": "https://snarfnet.github.io/"}}})

    r = api("POST", "/reviewSubmissions", json={"data": {"type": "reviewSubmissions",
        "attributes": {"platform": "IOS"},
        "relationships": {"app": {"data": {"type": "apps", "id": APP_ID}}}}})
    sub_id = r.json()["data"]["id"]
    api("POST", "/reviewSubmissionItems", json={"data": {"type": "reviewSubmissionItems",
        "relationships": {"reviewSubmission": {"data": {"type": "reviewSubmissions", "id": sub_id}},
            "appStoreVersion": {"data": {"type": "appStoreVersions", "id": version_id}}}}})
    api("PATCH", f"/reviewSubmissions/{sub_id}", json={"data": {"type": "reviewSubmissions",
        "id": sub_id, "attributes": {"submitted": True}}})
    print("Submitted for review!")

DESC_JA = """好きな漫画が必ず見つかる、マンガ探しの相棒アプリ。

AniListの膨大なデータベースから、あなたにぴったりの漫画を提案します。

【機能】
・ホーム：今話題の作品・殿堂入り名作・高評価ランキング
・ランキング：スコア順・人気順・トレンドで絞り込み
・ジャンル別：14ジャンルから探せる
・今日のラッキー漫画：日替わりおすすめ
・作品詳細：スコア・巻数・話数・ジャンル・評価分析

気になった作品はAmazonですぐに購入できます。"""

KEYWORDS = "漫画,マンガ,おすすめ,ランキング,アニメ,コミック,manga,comic,AniList,おすすめ漫画"

if __name__ == "__main__":
    main()
