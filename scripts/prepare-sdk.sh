#!/usr/bin/env bash
set -euo pipefail

# Build the requested, unpublished SDK commit without changing its source checkout.
sdk_commit=d88696f27d26c03c29785c6fbf1c32cee270ae67
sdk_version=2.0.0-rc.11-local.d88696f27d26
project_dir=$(cd "$(dirname "$0")/.." && pwd)
sdk_source=${1:-"$project_dir/../fluxzero-sdk-java"}
resolved_commit=$(git -C "$sdk_source" rev-parse "$sdk_commit^{commit}")
test "$resolved_commit" = "$sdk_commit"
sdk_build=$(mktemp -d "${TMPDIR:-/tmp}/home-sdk.XXXXXXXX")
trap 'rm -rf "$sdk_build"' EXIT
git -C "$sdk_source" archive "$sdk_commit" | tar -x -C "$sdk_build"
cd "$sdk_build"
./mvnw -B -ntp versions:set -DnewVersion="$sdk_version" -DprocessAllModules=true -DgenerateBackupPoms=false
./mvnw -B -ntp -DskipTests -Dgpg.skip -Dagent-docs.sourceCommit="$sdk_commit" install
printf 'Installed local SDK %s from %s\n' "$sdk_version" "$sdk_commit"
