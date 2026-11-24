#!/usr/bin/env python3
"""Publish a TC LMS Android release through the Google Play Developer API.

One edit: store listing text (store/listing.json), icon + feature graphic +
phone screenshots (play-assets/), the signed AAB, and a track release with
notes from the top CHANGELOG.md entry; then commit and read the track back.

  scripts/play_release.py --track production            # full release
  scripts/play_release.py --track production --dry-run  # validate, don't commit
  scripts/play_release.py --status

Uses the shared service account (~/.config/tertiary/play-publisher.json).
The package must already exist in the corporate Play Console account.
"""
import argparse, json, os, re, sys

from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
KEY = os.path.expanduser(os.environ.get("SUPPLY_JSON_KEY", "~/.config/tertiary/play-publisher.json"))


def package_name():
    gradle = open(os.path.join(ROOT, "app/build.gradle.kts")).read()
    return re.search(r'applicationId\s*=\s*"([^"]+)"', gradle).group(1)


def version_code():
    gradle = open(os.path.join(ROOT, "app/build.gradle.kts")).read()
    return int(re.search(r"versionCode\s*=\s*(\d+)", gradle).group(1))


def version_name():
    gradle = open(os.path.join(ROOT, "app/build.gradle.kts")).read()
    return re.search(r'versionName\s*=\s*"([^"]+)"', gradle).group(1)


def release_notes():
    text = open(os.path.join(ROOT, "CHANGELOG.md")).read()
    block = re.split(r"(?m)^##\s+", text)[1]
    bullets = [l.strip()[2:] for l in block.splitlines() if l.strip().startswith("- ")]
    notes = []
    for b in bullets:
        line = "• " + re.sub(r"[*`]", "", b)
        if len("\n".join(notes + [line])) > 495:
            break
        notes.append(line)
    return "\n".join(notes)


def service():
    import google_auth_httplib2, httplib2
    creds = service_account.Credentials.from_service_account_file(
        KEY, scopes=["https://www.googleapis.com/auth/androidpublisher"])
    # Slow uplinks time out on the default socket timeout; allow 10 minutes per chunk.
    raw = httplib2.Http(timeout=600)
    raw.redirect_codes = raw.redirect_codes - {308}   # resumable uploads answer 308 "Resume Incomplete"
    http = google_auth_httplib2.AuthorizedHttp(creds, http=raw)
    return build("androidpublisher", "v3", http=http, cache_discovery=False)


def media(path, mimetype):
    return MediaFileUpload(path, mimetype=mimetype, resumable=True, chunksize=1024 * 1024)


def status(svc, pkg):
    edit = svc.edits().insert(packageName=pkg, body={}).execute()
    for t in svc.edits().tracks().list(packageName=pkg, editId=edit["id"]).execute().get("tracks", []):
        for r in t.get("releases", []):
            print(t["track"], r.get("status"), r.get("versionCodes"), r.get("name"))
    svc.edits().delete(packageName=pkg, editId=edit["id"]).execute()


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--track", default="production")
    p.add_argument("--aab", default=os.path.join(ROOT, "app/build/outputs/bundle/release/app-release.aab"))
    p.add_argument("--dry-run", action="store_true")
    p.add_argument("--status", action="store_true")
    p.add_argument("--skip-listing", action="store_true")
    p.add_argument("--skip-bundle", action="store_true", help="listing only; leave tracks untouched")
    p.add_argument("--release-status", default="completed",
                   help="'draft' for an app that has never been published (Play rejects anything else); "
                        "then roll it out from Play Console")
    a = p.parse_args()

    svc, pkg = service(), package_name()
    if a.status:
        return status(svc, pkg)

    vc = version_code()
    listing = json.load(open(os.path.join(ROOT, "store/listing.json")))
    lang = listing.get("language", "en-US")
    edit = svc.edits().insert(packageName=pkg, body={}).execute()
    eid = edit["id"]
    print(f"{pkg}: edit {eid}")

    if not a.skip_listing:
        svc.edits().details().patch(packageName=pkg, editId=eid, body={
            "defaultLanguage": lang, "contactEmail": listing["contactEmail"],
            "contactWebsite": listing["contactWebsite"]}).execute()
        svc.edits().listings().update(packageName=pkg, editId=eid, language=lang, body={
            "language": lang, "title": listing["title"],
            "shortDescription": listing["shortDescription"],
            "fullDescription": listing["fullDescription"]}).execute()
        print("listing text set")
        # Exactly one listing: drop any other language left over from earlier releases.
        for other in svc.edits().listings().list(packageName=pkg, editId=eid).execute().get("listings", []):
            if other["language"] != lang:
                svc.edits().listings().delete(packageName=pkg, editId=eid, language=other["language"]).execute()
                print(f"removed old {other['language']} listing")
        assets = os.path.join(ROOT, "play-assets")
        for image_type, files in (
                ("icon", ["icon-512.png"]),
                ("featureGraphic", ["feature-graphic.png"]),
                ("phoneScreenshots", sorted(os.listdir(os.path.join(assets, "screenshots"))))):
            svc.edits().images().deleteall(packageName=pkg, editId=eid, language=lang, imageType=image_type).execute()
            for f in files:
                path = os.path.join(assets, "screenshots" if image_type == "phoneScreenshots" else "", f)
                svc.edits().images().upload(packageName=pkg, editId=eid, language=lang, imageType=image_type,
                                            media_body=media(path, "image/png")).execute()
            print(f"{image_type}: {len(files)} uploaded")

    if a.skip_bundle:
        if a.dry_run:
            svc.edits().validate(packageName=pkg, editId=eid).execute()
            svc.edits().delete(packageName=pkg, editId=eid).execute()
            print("dry run: validated, not committed")
            return
        svc.edits().commit(packageName=pkg, editId=eid).execute()
        print("committed (listing only)")
        return status(svc, pkg)

    bundle = svc.edits().bundles().upload(packageName=pkg, editId=eid,
                                          media_body=media(a.aab, "application/octet-stream")).execute()
    if bundle["versionCode"] != vc:
        sys.exit(f"AAB versionCode {bundle['versionCode']} != build.gradle {vc}")
    print(f"bundle uploaded: versionCode {vc}")

    notes = listing.get("releaseNotes") or release_notes()
    svc.edits().tracks().update(packageName=pkg, editId=eid, track=a.track, body={
        "track": a.track,
        "releases": [{"name": f"{version_name()} ({vc})", "versionCodes": [str(vc)], "status": a.release_status,
                      "releaseNotes": [{"language": lang, "text": notes}]}]}).execute()
    print(f"{a.track} release staged:\n{notes}")

    if a.dry_run:
        svc.edits().validate(packageName=pkg, editId=eid).execute()
        svc.edits().delete(packageName=pkg, editId=eid).execute()
        print("dry run: validated, not committed")
        return
    svc.edits().commit(packageName=pkg, editId=eid).execute()
    print("committed")
    status(svc, pkg)


if __name__ == "__main__":
    main()
