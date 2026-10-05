#!/usr/bin/env python3
"""Check one real initializer scenario from a committed Git archive.

Default: rename/structure proof. --build adds real checks/builds in the same
throwaway clone. The source checkout, branch, index, and remote are never changed.
"""
from __future__ import annotations

import argparse
import hashlib
import subprocess
import sys
import tarfile
import tempfile
from io import BytesIO
from pathlib import Path


def require(condition: bool, message: str) -> None:
    if not condition:
        raise RuntimeError(message)


def run(command: list[str], root: Path, env: dict[str, str] | None = None) -> None:
    print("RUN " + " ".join(command), flush=True)
    result = subprocess.run(command, cwd=root, env=env, capture_output=True, text=True)
    if result.returncode:
        detail = result.stdout[-4000:] + result.stderr[-1000:]
        raise RuntimeError(f"Command exited {result.returncode}: {' '.join(command)}\n{detail}")


def text(root: Path, path: str) -> str:
    return (root / path).read_text(encoding="utf-8")


def tree_digest(root: Path) -> str:
    digest = hashlib.sha256()
    for path in sorted(root.rglob("*")):
        relative = path.relative_to(root)
        if path.is_file() and ".git" not in relative.parts:
            digest.update(relative.as_posix().encode())
            digest.update(path.read_bytes())
    return digest.hexdigest()


def archive_revision(repo: Path, revision: str, clone: Path) -> None:
    result = subprocess.run(["git", "archive", "--format=tar", revision], cwd=repo, check=True, capture_output=True)
    with tarfile.open(fileobj=BytesIO(result.stdout), mode="r:") as bundle:
        bundle.extractall(clone, filter="data")


def check_clone(clone: Path, build: bool) -> None:
    package = "org.example.smokexml"
    command = [sys.executable, "-B", "scripts/init_project.py", "--non-interactive",
               "--project-name", "SmokeXml", "--app-name", "Smoke & XML", "--package", package,
               "--scope", "full", "--clean-samples", "--skip-build-check"]
    before = tree_digest(clone)
    run([*command, "--dry-run"], clone)
    require(tree_digest(clone) == before, "Dry-run changed archived source files")
    run(command, clone)
    app = clone / "app/src/main/java/org/example/smokexml"
    require((app / "SmokeXmlApplication.kt").is_file(), "Renamed application is missing")
    require((app / "appshell/home/HomeFragment.kt").is_file(), "Home shell was removed")
    require((app / "feature/settings/presentation/ui/SettingsFragment.kt").is_file(), "Settings shell was removed")
    require(not (app / "sample").exists(), "Demo sample sources remain")
    require('rootProject.name = "SmokeXml"' in text(clone, "settings.gradle.kts"), "Project name was not renamed")
    app_build = text(clone, "app/build.gradle.kts")
    require(f'applicationId = "{package}"' in app_build, "Application ID was not renamed")
    require(f'namespace = "{package}.core"' in text(clone, "core/build.gradle.kts"), "Core namespace was not renamed")
    navigation = text(clone, "app/src/main/res/navigation/main_navigation.xml")
    require("homeFragment" in navigation and "settingsFragment" in navigation, "Retained navigation is missing")
    require("demoFragment" not in navigation and "designSystemFragment" not in navigation, "Demo routes remain")
    require('android:name=".SmokeXmlApplication"' in text(clone, "app/src/main/AndroidManifest.xml"), "Manifest application identity is stale")
    if build:
        run(["./gradlew", "ktlintFormat", ":core:apiDump", ":core:ui-compose:apiDump"], clone)
        run(["./gradlew", "check", ":app:assembleDebug"], clone)
        require((clone / "app/build/outputs/apk/debug/app-debug.apk").is_file(), "Debug APK is missing")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", type=Path, default=Path(__file__).resolve().parent.parent)
    parser.add_argument("--revision", default="HEAD")
    parser.add_argument("--build", action="store_true", help="Also verify/build the initialized temporary clone")
    args = parser.parse_args()
    try:
        with tempfile.TemporaryDirectory(prefix="androidxmlbase-init-smoke-") as temp:
            clone = Path(temp)
            archive_revision(args.repo.resolve(), args.revision, clone)
            check_clone(clone, args.build)
    except (OSError, RuntimeError, subprocess.CalledProcessError, tarfile.TarError) as error:
        print(f"FAIL initializer smoke: {error}", file=sys.stderr)
        return 1
    print("PASS archive initializer: rename" + (" + build/check" if args.build else " only (build skipped)"))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
