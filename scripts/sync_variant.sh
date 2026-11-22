#!/bin/bash
# Copy the shared TC LMS Android source from this (SG) project into a country
# variant project — tertiarycoursesmyapp (MY) or tertiarycoursesghapp (GH).
# Every Kotlin file is shared except each project's own Site.kt. Per-project
# (never synced): Site.kt, app/build.gradle.kts (applicationId, version),
# res/values/strings.xml (app_name), launcher icons, assets/catalog.json,
# play-assets/, store/, README/CHANGELOG, keystore files.
#
#   scripts/sync_variant.sh ../tertiarycoursesmyapp
set -euo pipefail
pkg="app/src/main/java/com/tertiaryinfotech/tertiarycourses"
src="$(cd "$(dirname "$0")/.." && pwd)"
dest="$(cd "${1:?usage: sync_variant.sh <variant-project>}" && pwd)"
[ "$src" != "$dest" ] || { echo "refusing to sync onto itself"; exit 1; }
[ -f "$dest/$pkg/Site.kt" ] || { echo "$dest has no $pkg/Site.kt"; exit 1; }

# HARD RULE (22 Nov 2026): TC LMS is the non-WSQ app (C-prefix courses only). WSQ
# classes, SSG attendance and the TRAQOM survey belong to the TIA app. The API
# enforces the course filter (ai-mms scripts/audit/check-mobileapi-nonwsq.sh);
# this stops a WSQ-only feature being built into the apps.
"$src/scripts/check_nonwsq.sh" "$src/$pkg" || exit 1

for f in "$src/$pkg"/*.kt; do
    name=$(basename "$f")
    [ "$name" = "Site.kt" ] && continue
    cp "$f" "$dest/$pkg/$name"
done
# Drop shared files that were deleted here.
for f in "$dest/$pkg"/*.kt; do
    name=$(basename "$f")
    [ "$name" = "Site.kt" ] || [ -f "$src/$pkg/$name" ] || rm -v "$f"
done
# The document viewer's Download shares files through a FileProvider: its path
# list is shared; the <provider> block in AndroidManifest.xml must exist too.
mkdir -p "$dest/app/src/main/res/xml"
cp "$src/app/src/main/res/xml/file_paths.xml" "$dest/app/src/main/res/xml/"
grep -q 'androidx.core.content.FileProvider' "$dest/app/src/main/AndroidManifest.xml" \
    || echo "WARNING: $dest AndroidManifest.xml has no FileProvider <provider> block; copy it from the SG manifest"
# Admin sign-in with Google returns to the app's own URL scheme: the intent filter
# lives in the manifest and the scheme in app/build.gradle.kts, both per project.
grep -q 'authScheme' "$dest/app/src/main/AndroidManifest.xml" \
    || echo "WARNING: $dest AndroidManifest.xml has no \${authScheme} intent filter on MainActivity; copy it from the SG manifest"
grep -q 'authScheme' "$dest/app/build.gradle.kts" \
    || echo "WARNING: $dest app/build.gradle.kts sets no manifestPlaceholders[\"authScheme\"] (tclms + lower-case Site.code)"
mkdir -p "$dest/scripts"
cp "$src/scripts/play_release.py" "$src/scripts/sync_variant.sh" "$src/scripts/check_nonwsq.sh" "$dest/scripts/"
echo "synced shared source into $dest"
