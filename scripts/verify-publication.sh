#!/usr/bin/env bash
# Publishes AndroidCoreBase's two published modules to a throwaway Maven repository, then builds
# an isolated consumer project (integration/consumer) against ONLY that repository (plus
# Google/Maven Central) to prove the artifacts are self-contained and Hilt/Compose-free where they
# should be. See docs/CORE_V2_DESIGN.md's "Published Consumer Verification" section.
#
# Usage: scripts/verify-publication.sh --quick|--release
#   --quick    Compile debug for both consumer modes (main-only, then +Compose). Fast; PR gate.
#   --release  Additionally build both modes' minified release variant. Slow; main/tag/manual gate.
set -euo pipefail

if [[ $# -ne 1 || ( "$1" != "--quick" && "$1" != "--release" ) ]]; then
    echo "Usage: $0 --quick|--release" >&2
    exit 1
fi
MODE="$1"

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONSUMER_DIR="$REPO_ROOT/integration/consumer"
VERIFY_VERSION="0.0.0-verify"

TEMP_REPO_DIR="$(mktemp -d)"
trap 'rm -rf "$TEMP_REPO_DIR"' EXIT

echo "==> Publishing AndroidCoreBase $VERIFY_VERSION to a temporary repository: $TEMP_REPO_DIR"
VERSION="$VERIFY_VERSION" "$REPO_ROOT/gradlew" -p "$REPO_ROOT" \
    :core:publishReleasePublicationToTemporaryRepository \
    :core:ui-compose:publishReleasePublicationToTemporaryRepository \
    -PpublishRepoUrl="file://$TEMP_REPO_DIR"

CORE_POM="$(find "$TEMP_REPO_DIR" -name 'AndroidCoreBase-*.pom' ! -name '*ui-compose*')"
COMPOSE_POM="$(find "$TEMP_REPO_DIR" -name 'AndroidCoreBase-ui-compose-*.pom')"
CORE_AAR="$(find "$TEMP_REPO_DIR" -name 'AndroidCoreBase-*.aar' ! -name '*ui-compose*' ! -name '*test-fixtures*')"
COMPOSE_AAR="$(find "$TEMP_REPO_DIR" -name 'AndroidCoreBase-ui-compose-*.aar' ! -name '*test-fixtures*')"

for path in "$CORE_POM" "$COMPOSE_POM" "$CORE_AAR" "$COMPOSE_AAR"; do
    if [[ -z "$path" || ! -f "$path" ]]; then
        echo "FAIL: expected published artifact not found (looked for one matching: $path)" >&2
        exit 1
    fi
done

echo "==> Asserting the main module's POM has no Compose, Hilt, or test-only dependency"
if grep -Eio '(compose|hilt|>junit<|kotlinx-coroutines-test)' "$CORE_POM"; then
    echo "FAIL: $CORE_POM leaks a Compose/Hilt/test-only dependency" >&2
    exit 1
fi

echo "==> Asserting both AAR manifests are passive (no permission/component nodes)"
for aar in "$CORE_AAR" "$COMPOSE_AAR"; do
    manifest_xml="$(unzip -p "$aar" AndroidManifest.xml | strings)"
    if echo "$manifest_xml" | grep -Eiq 'uses-permission|<provider|<service|<activity'; then
        echo "FAIL: $aar's manifest declares a permission or component:" >&2
        echo "$manifest_xml" >&2
        exit 1
    fi
done

run_consumer_mode() {
    local include_compose="$1"
    local gradle_args=(
        :app:assembleDebug
        -PpublishRepoUrl="file://$TEMP_REPO_DIR"
        -PandroidCoreBaseVersion="$VERIFY_VERSION"
        -PincludeCompose="$include_compose"
    )
    if [[ "$MODE" == "--release" ]]; then
        gradle_args+=(:app:assembleRelease)
    fi

    echo "==> Building the isolated consumer (includeCompose=$include_compose, mode=$MODE)"
    "$CONSUMER_DIR/gradlew" -p "$CONSUMER_DIR" "${gradle_args[@]}"
}

run_consumer_mode false
run_consumer_mode true

echo "==> verify-publication.sh $MODE: PASS"
