#!/usr/bin/env python3
"""
scripts/init_project.py

Automated project initialization and refactoring script for AndroidXmlBase.
Refactors project name, application ID, packages, themes, convention plugins,
build-logic tasks, and baseline profile configurations.

Usage:
  # Interactive wizard:
  python3 scripts/init_project.py

  # Automated CLI execution:
  python3 scripts/init_project.py \\
    --project-name "AcmeShop" \\
    --app-name "Acme Shop" \\
    --package "com.acme.shop"

Options:
  --scope [full|app-only]  Full standalone fork (default) or app-only rebrand.
  --clean-samples          Remove sample features (weather demo & UI kit demo).
  --dry-run                Simulate file moves and modifications without writing.
  --skip-build-check       Skip Gradle verification after refactoring.
  --force                  Ignore uncommitted git changes warning.
"""

from __future__ import annotations

import argparse
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path
from typing import List, NamedTuple, Tuple

# Terminal color styling
USE_COLOR = sys.stdout.isatty() and os.environ.get("TERM") != "dumb"


def style(text: str, color_code: str) -> str:
    return f"\033[{color_code}m{text}\033[0m" if USE_COLOR else text


def log_info(msg: str) -> None:
    print(f"{style('==>', '34;1')} {msg}")


def log_step(msg: str) -> None:
    print(f"  {style('->', '36')} {msg}")


def log_success(msg: str) -> None:
    print(f"{style('✔', '32;1')} {style(msg, '32')}")


def log_warn(msg: str) -> None:
    print(f"{style('⚠', '33;1')} {style(msg, '33')}")


def log_error(msg: str) -> None:
    print(f"{style('✖ ERROR:', '31;1')} {style(msg, '31')}")


# Validation regular expressions
PROJECT_NAME_REGEX = re.compile(r"^[A-Z][a-zA-Z0-9_]*$")
PACKAGE_REGEX = re.compile(r"^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$")

RESERVED_KEYWORDS = {
    # Java keywords
    "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
    "class", "const", "continue", "default", "do", "double", "else", "enum",
    "extends", "final", "finally", "float", "for", "goto", "if", "implements",
    "import", "instanceof", "int", "interface", "long", "native", "new",
    "package", "private", "protected", "public", "return", "short", "static",
    "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
    "transient", "try", "void", "volatile", "while",
    # Kotlin keywords
    "as", "fun", "in", "is", "null", "object", "typealias", "typeof", "val",
    "var", "when", "true", "false",
}


class CollisionError(Exception):
    """Raised when a file or directory move destination already exists or collides."""
    pass


class InitConfig(NamedTuple):
    root_dir: Path
    project_name: str
    app_name: str
    app_package: str
    core_package: str
    scope: str
    clean_samples: bool
    dry_run: bool
    skip_build_check: bool
    force: bool


def validate_project_name(name: str) -> bool:
    return bool(PROJECT_NAME_REGEX.match(name))


def validate_package(pkg: str) -> bool:
    if not isinstance(pkg, str) or not PACKAGE_REGEX.match(pkg):
        return False
    parts = pkg.split(".")
    for part in parts:
        if part in RESERVED_KEYWORDS:
            return False
        if part.startswith("_") and len(part) == 1:
            return False
    return True


def escape_android_string_resource(value: str) -> str:
    """Escapes special characters in string value for Android strings.xml compliance."""
    # 1. Backslashes
    res = value.replace("\\", "\\\\")
    # 2. XML standard markup entities
    res = res.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    # 3. Android quotes
    res = res.replace("'", "\\'").replace('"', '\\"')
    # 4. Resource / theme attribute references at the start
    if res.startswith("@") or res.startswith("?"):
        res = "\\" + res
    return res


def check_git_clean(root_dir: Path) -> bool:
    if not (root_dir / ".git").exists():
        return True
    try:
        res = subprocess.run(
            ["git", "status", "--porcelain"],
            cwd=root_dir,
            capture_output=True,
            text=True,
            check=True,
        )
        return len(res.stdout.strip()) == 0
    except (subprocess.SubprocessError, FileNotFoundError):
        return True


def check_move_collisions(moves: List[Tuple[Path, Path]]) -> List[str]:
    """Pre-flight check: verifies that no destination already exists or collides with another move."""
    conflicts: List[str] = []
    seen_dsts: Dict[Path, Path] = {}

    for src, dst in moves:
        if src.resolve() == dst.resolve():
            continue

        # Check for multiple sources mapped to same destination
        if dst in seen_dsts:
            conflicts.append(
                f"Collision: multiple sources mapped to same destination '{dst}' "
                f"('{src}' and '{seen_dsts[dst]}')"
            )
        else:
            seen_dsts[dst] = src

        # Check if destination already exists on disk
        if dst.exists():
            conflicts.append(
                f"Collision: destination already exists on disk: '{dst}' (source: '{src}')"
            )

    return conflicts


def run_git_or_shutil_move(src: Path, dst: Path, root_dir: Path, dry_run: bool) -> None:
    if src.resolve() == dst.resolve():
        return

    if dry_run:
        log_step(f"[DRY-RUN] Move: {src.relative_to(root_dir)} -> {dst.relative_to(root_dir)}")
        return

    # Safety: NEVER overwrite an existing destination
    if dst.exists():
        raise CollisionError(f"Refusing to overwrite existing destination: {dst}")

    dst.parent.mkdir(parents=True, exist_ok=True)
    has_git = (root_dir / ".git").exists()
    moved_with_git = False

    if has_git:
        try:
            subprocess.run(
                ["git", "mv", str(src), str(dst)],
                cwd=root_dir,
                capture_output=True,
                check=True,
            )
            moved_with_git = True
        except (subprocess.SubprocessError, FileNotFoundError):
            moved_with_git = False

    if not moved_with_git:
        if dst.exists():
            raise CollisionError(f"Destination appeared before move: {dst}")
        shutil.move(str(src), str(dst))


def prune_empty_dirs(path: Path, stop_at: Path) -> None:
    curr = path
    while curr != stop_at and curr.exists() and curr.is_dir():
        try:
            if not any(curr.iterdir()):
                curr.rmdir()
                curr = curr.parent
            else:
                break
        except OSError:
            break


def get_source_moves(config: InitConfig) -> List[Tuple[Path, Path]]:
    root = config.root_dir
    moves: List[Tuple[Path, Path]] = []

    old_app_path = Path("com/example/androidcorebase")
    new_app_path = Path(*config.app_package.split("."))

    # 1. Rename Application class file in place first
    old_app_file = root / "app" / "src" / "main" / "java" / old_app_path / "AndroidCoreBaseApplication.kt"
    new_app_file = root / "app" / "src" / "main" / "java" / old_app_path / f"{config.project_name}Application.kt"
    if old_app_file.exists():
        moves.append((old_app_file, new_app_file))

    # 2. :app module sources (moves the directory including the renamed application file)
    if old_app_path != new_app_path:
        for source_set in ["main", "test", "androidTest"]:
            src_dir = root / "app" / "src" / source_set / "java" / old_app_path
            dst_dir = root / "app" / "src" / source_set / "java" / new_app_path
            if src_dir.exists():
                moves.append((src_dir, dst_dir))

        # 3. :baselineprofile module sources
        bp_src = root / "baselineprofile" / "src" / "main" / "java" / old_app_path / "baselineprofile"
        bp_dst = root / "baselineprofile" / "src" / "main" / "java" / new_app_path / "baselineprofile"
        if bp_src.exists():
            moves.append((bp_src, bp_dst))

    # 4. :core, :core:ui-compose, build-logic (if full scope)
    if config.scope == "full":
        old_core_path = Path("com/thanhng224/androidcorebase/core")
        new_core_path = Path(*config.core_package.split("."))

        if old_core_path != new_core_path:
            for source_set in ["main", "test", "testFixtures", "androidTest"]:
                src_dir = root / "core" / "src" / source_set / "java" / old_core_path
                dst_dir = root / "core" / "src" / source_set / "java" / new_core_path
                legacy_dir = root / "core" / "src" / source_set / "java" / Path("com/thanhng224/androidxmlbase/core")
                if src_dir.exists():
                    moves.append((src_dir, dst_dir))
                elif legacy_dir.exists():
                    moves.append((legacy_dir, dst_dir))

            for source_set in ["main", "androidTest"]:
                src_dir = root / "core" / "ui-compose" / "src" / source_set / "java" / old_core_path
                dst_dir = root / "core" / "ui-compose" / "src" / source_set / "java" / new_core_path
                if src_dir.exists():
                    moves.append((src_dir, dst_dir))

        # build-logic package move
        new_plugin_prefix = re.sub(r"[^a-zA-Z0-9]", "", config.project_name).lower()
        bl_src = root / "build-logic" / "src" / "main" / "kotlin" / "androidcorebase"
        bl_dst = root / "build-logic" / "src" / "main" / "kotlin" / new_plugin_prefix
        if bl_src.exists() and bl_src != bl_dst:
            moves.append((bl_src, bl_dst))

        # build-logic convention plugin files
        bl_root = root / "build-logic" / "src" / "main" / "kotlin"
        plugins = ["android-library", "published-library", "quality"]
        for p in plugins:
            p_src = bl_root / f"androidcorebase.{p}.gradle.kts"
            p_dst = bl_root / f"{new_plugin_prefix}.{p}.gradle.kts"
            if p_src.exists() and p_src != p_dst:
                moves.append((p_src, p_dst))

    return moves


def build_replacements_table(config: InitConfig) -> List[Tuple[str, str]]:
    proj = config.project_name
    app_pkg = config.app_package
    core_pkg = config.core_package
    core_compose_pkg = f"{core_pkg}.compose"
    baseline_pkg = f"{app_pkg}.baselineprofile"
    plugin_prefix = re.sub(r"[^a-zA-Z0-9]", "", proj).lower()
    app_class = f"{proj}Application"

    replacements: List[Tuple[str, str]] = []

    escaped_app_name = escape_android_string_resource(config.app_name)

    # App-level replacements (always applied)
    replacements.extend([
        ("com.example.androidcorebase.baselineprofile", baseline_pkg),
        ("com.example.androidcorebase", app_pkg),
        ("Lcom/example/androidcorebase/", f"L{app_pkg.replace('.', '/')}/"),
        ("AndroidCoreBaseApplication", app_class),
        ('rootProject.name = "AndroidCoreBase"', f'rootProject.name = "{proj}"'),
        ('<string name="app_name">XML Base</string>', f'<string name="app_name">{escaped_app_name}</string>'),
        ('".AndroidCoreBaseApplication"', f'".{app_class}"'),
        ('*.AndroidCoreBaseApplication', f'*.{app_class}'),
    ])

    if config.scope == "full":
        replacements.extend([
            # Core packages & bytecode descriptors
            ("com.thanhng224.androidcorebase.core.compose", core_compose_pkg),
            ("com.thanhng224.androidcorebase.core", core_pkg),
            ("Lcom/thanhng224/androidcorebase/core/", f"L{core_pkg.replace('.', '/')}/"),

            # Build-logic boundary check task paths
            (
                "src/main/java/com/thanhng224/androidcorebase/core/foundation",
                f"src/main/java/{core_pkg.replace('.', '/')}/foundation",
            ),
            ("com.thanhng224.androidcorebase.core.R", f"{core_pkg}.R"),

            # Convention plugins and build-logic package
            ("androidcorebase.buildlogic", f"{plugin_prefix}.buildlogic"),
            ('id("androidcorebase.', f'id("{plugin_prefix}.'),
            ("import androidcorebase.buildlogic", f"import {plugin_prefix}.buildlogic"),
            ("import androidcorebase.", f"import {plugin_prefix}."),

            # Themes and UI Styles
            ("AndroidCoreBaseTheme", f"{proj}Theme"),
            ("Theme.AndroidCoreBase", f"Theme.{proj}"),
            ("Widget.AndroidCoreBase", f"Widget.{proj}"),
            ("TextAppearance.AndroidCoreBase", f"TextAppearance.{proj}"),

            # Published library metadata
            ('artifactId.set("AndroidCoreBase")', f'artifactId.set("{proj}")'),
            ('artifactId.set("AndroidCoreBase-ui-compose")', f'artifactId.set("{proj}-ui-compose")'),
            ('displayName.set("AndroidCoreBase Core")', f'displayName.set("{proj} Core")'),
            ('displayName.set("AndroidCoreBase Compose Interop")', f'displayName.set("{proj} Compose Interop")'),

            # Publication verification (scripts/verify-publication.sh)
            ("AndroidCoreBase-*.pom", f"{proj}-*.pom"),
            ("AndroidCoreBase-ui-compose-*.pom", f"{proj}-ui-compose-*.pom"),
            ("AndroidCoreBase-*.aar", f"{proj}-*.aar"),
            ("AndroidCoreBase-ui-compose-*.aar", f"{proj}-ui-compose-*.aar"),
            ('echo "==> Publishing AndroidCoreBase', f'echo "==> Publishing {proj}'),

            # Consumer integration tests (integration/consumer)
            (
                'implementation("com.github.ThanhNg224:AndroidCoreBase:$androidCoreBaseVersion")',
                f'implementation("com.github.ThanhNg224:{proj}:$androidCoreBaseVersion")',
            ),
            (
                'implementation("com.github.ThanhNg224:AndroidCoreBase-ui-compose:$androidCoreBaseVersion")',
                f'implementation("com.github.ThanhNg224:{proj}-ui-compose:$androidCoreBaseVersion")',
            ),
            ('rootProject.name = "androidcorebase-consumer"', f'rootProject.name = "{proj.lower()}-consumer"'),
            ('resolve com.github.ThanhNg224:AndroidCoreBase*"', f'resolve com.github.ThanhNg224:{proj}*"'),
            ('android:label="AndroidCoreBase Consumer"', f'android:label="{proj} Consumer"'),
        ])

    return replacements


def should_skip_file(path: Path, root_dir: Path) -> bool:
    rel = path.relative_to(root_dir)
    parts = set(rel.parts)

    # Skip any hidden directory except .github
    for part in rel.parts[:-1]:
        if part.startswith(".") and part != ".github":
            return True

    # Special exemption for committed baseline profile in app/src/release/generated/baselineProfiles
    if "src" in rel.parts and "baselineProfiles" in rel.parts and rel.name == "baseline-prof.txt":
        return False

    ignored_dirs = {
        ".git", ".gradle", "build", ".idea", ".cxx", ".kotlin", "reports",
        "test-results", "intermediates", "generated", "outputs",
        "__pycache__",
    }
    if parts & ignored_dirs:
        return True

    if "docs" in rel.parts and "superpowers" in rel.parts:
        return True

    ignored_extensions = {
        ".png", ".jpg", ".jpeg", ".webp", ".jar", ".aar", ".keystore",
        ".so", ".class", ".pyc", ".ico", ".bin",
    }
    if path.suffix.lower() in ignored_extensions:
        return True

    if rel.name in ("gradlew", "gradlew.bat", "init_project.py", "test_init_project.py"):
        return True

    return False


def apply_text_replacements(
    root_dir: Path,
    replacements: List[Tuple[str, str]],
    dry_run: bool,
) -> int:
    modified_count = 0
    # Sort replacements by length of pattern descending to avoid partial prefix collisions
    sorted_replacements = sorted(replacements, key=lambda pair: len(pair[0]), reverse=True)

    for dirpath, _, filenames in os.walk(root_dir):
        for filename in filenames:
            file_path = Path(dirpath) / filename
            if should_skip_file(file_path, root_dir):
                continue

            try:
                with open(file_path, "r", encoding="utf-8") as f:
                    content = f.read()
            except (UnicodeDecodeError, OSError):
                continue

            new_content = content
            for old_str, new_str in sorted_replacements:
                if old_str in new_content:
                    new_content = new_content.replace(old_str, new_str)

            if new_content != content:
                modified_count += 1
                rel_path = file_path.relative_to(root_dir)
                if dry_run:
                    log_step(f"[DRY-RUN] Would update content: {rel_path}")
                else:
                    with open(file_path, "w", encoding="utf-8") as f:
                        f.write(new_content)
                    log_step(f"Updated: {rel_path}")

    return modified_count


def clean_sample_code(config: InitConfig) -> None:
    root = config.root_dir
    app_path = Path(*config.app_package.split("."))

    log_info("Pruning sample code (--clean-samples)...")

    # 1. Remove sample directory from app main and test
    sample_main = root / "app" / "src" / "main" / "java" / app_path / "sample"
    sample_test = root / "app" / "src" / "test" / "java" / app_path / "sample"

    for s_dir in [sample_main, sample_test]:
        if s_dir.exists():
            if config.dry_run:
                log_step(f"[DRY-RUN] Remove sample dir: {s_dir.relative_to(root)}")
            else:
                shutil.rmtree(s_dir)
                log_step(f"Removed: {s_dir.relative_to(root)}")

    # 2. Remove sample XML layouts
    sample_layouts = [
        root / "app" / "src" / "main" / "res" / "layout" / "fragment_demo.xml",
        root / "app" / "src" / "main" / "res" / "layout" / "fragment_design_system.xml",
    ]
    for layout in sample_layouts:
        if layout.exists():
            if config.dry_run:
                log_step(f"[DRY-RUN] Remove sample layout: {layout.relative_to(root)}")
            else:
                layout.unlink()
                log_step(f"Removed: {layout.relative_to(root)}")

    # 3. Clean main_navigation.xml
    nav_file = root / "app" / "src" / "main" / "res" / "navigation" / "main_navigation.xml"
    if nav_file.exists():
        clean_nav = (
            '<?xml version="1.0" encoding="utf-8"?>\n'
            '<navigation xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    xmlns:app="http://schemas.android.com/apk/res-auto"\n'
            '    android:id="@+id/mainNavigation"\n'
            '    app:startDestination="@id/homeFragment">\n\n'
            '    <fragment\n'
            '        android:id="@+id/homeFragment"\n'
            f'        android:name="{config.app_package}.appshell.home.HomeFragment"\n'
            '        android:label="@string/home_title" />\n\n'
            '    <fragment\n'
            '        android:id="@+id/settingsFragment"\n'
            f'        android:name="{config.app_package}.feature.settings.presentation.ui.SettingsFragment"\n'
            '        android:label="@string/settings_title" />\n\n'
            '</navigation>\n'
        )
        if config.dry_run:
            log_step(f"[DRY-RUN] Simplify navigation graph: {nav_file.relative_to(root)}")
        else:
            nav_file.write_text(clean_nav, encoding="utf-8")
            log_step(f"Simplified: {nav_file.relative_to(root)}")

    # 4. Clean bottom_navigation.xml
    bottom_nav = root / "app" / "src" / "main" / "res" / "menu" / "bottom_navigation.xml"
    if bottom_nav.exists():
        clean_menu = (
            '<?xml version="1.0" encoding="utf-8"?>\n'
            '<menu xmlns:android="http://schemas.android.com/apk/res/android">\n\n'
            '    <item\n'
            '        android:id="@+id/homeFragment"\n'
            '        android:icon="@drawable/ic_nav_home"\n'
            '        android:title="@string/home_title" />\n\n'
            '    <item\n'
            '        android:id="@+id/settingsFragment"\n'
            '        android:icon="@drawable/ic_nav_settings"\n'
            '        android:title="@string/settings_title" />\n\n'
            '</menu>\n'
        )
        if config.dry_run:
            log_step(f"[DRY-RUN] Simplify bottom menu: {bottom_nav.relative_to(root)}")
        else:
            bottom_nav.write_text(clean_menu, encoding="utf-8")
            log_step(f"Simplified: {bottom_nav.relative_to(root)}")

    # 5. Remove sample drawables
    sample_drawables = [
        root / "app" / "src" / "main" / "res" / "drawable" / "ic_nav_demo.xml",
        root / "app" / "src" / "main" / "res" / "drawable" / "ic_nav_ui_kit.xml",
    ]
    for drawable in sample_drawables:
        if drawable.exists():
            if config.dry_run:
                log_step(f"[DRY-RUN] Remove sample drawable: {drawable.relative_to(root)}")
            else:
                drawable.unlink()
                log_step(f"Removed: {drawable.relative_to(root)}")

    # 6. Clean sample strings from strings.xml and values-vi/strings.xml
    for strings_file in [
        root / "app" / "src" / "main" / "res" / "values" / "strings.xml",
        root / "app" / "src" / "main" / "res" / "values-vi" / "strings.xml",
    ]:
        if strings_file.exists():
            content = strings_file.read_text(encoding="utf-8")
            cleaned_content = re.sub(
                r'\n\s*<string name="(?:demo|design_system)_[^"]+">.*?</string>',
                '',
                content,
            )
            if cleaned_content != content:
                if config.dry_run:
                    log_step(f"[DRY-RUN] Remove sample strings from {strings_file.relative_to(root)}")
                else:
                    strings_file.write_text(cleaned_content, encoding="utf-8")
                    log_step(f"Cleaned sample strings in {strings_file.relative_to(root)}")

    # 7. Update MainActivity.kt (remove demoFragment & designSystemFragment from AppBarConfiguration)
    main_activity_candidates = [
        root / "app" / "src" / "main" / "java" / app_path / "MainActivity.kt",
        root / "app" / "src" / "main" / "java" / "com" / "example" / "androidcorebase" / "MainActivity.kt",
    ]
    for act in main_activity_candidates:
        if act.exists():
            content = act.read_text(encoding="utf-8")
            old_conf = "appBarConfiguration = AppBarConfiguration(setOf(R.id.homeFragment, R.id.demoFragment, R.id.designSystemFragment))"
            new_conf = "appBarConfiguration = AppBarConfiguration(setOf(R.id.homeFragment, R.id.settingsFragment))"
            if old_conf in content:
                if config.dry_run:
                    log_step(f"[DRY-RUN] Update AppBarConfiguration in {act.relative_to(root)}")
                else:
                    act.write_text(content.replace(old_conf, new_conf), encoding="utf-8")
                    log_step(f"Updated AppBarConfiguration in {act.relative_to(root)}")
            break

    # 8. Update MainActivityTest.kt (switch top-level destinations between Home and Settings)
    test_candidates = [
        root / "app" / "src" / "androidTest" / "java" / app_path / "MainActivityTest.kt",
        root / "app" / "src" / "androidTest" / "java" / "com" / "example" / "androidcorebase" / "MainActivityTest.kt",
    ]
    for test_file in test_candidates:
        if test_file.exists():
            content = test_file.read_text(encoding="utf-8")
            old_test = (
                "    @Test\n"
                "    fun bottomNavigation_switchesBetweenTopLevelDestinations() {\n"
                "        onView(withId(R.id.demoFragment)).perform(click())\n"
                "        onView(withId(R.id.tvCount)).check(matches(isDisplayed()))\n\n"
                "        onView(withId(R.id.designSystemFragment)).perform(click())\n"
                "        onView(withText(R.string.design_system_headline_sample)).check(matches(isDisplayed()))\n\n"
                "        onView(withId(R.id.demoFragment)).perform(click())\n"
                "        onView(withId(R.id.tvCount)).check(matches(isDisplayed()))\n"
                "    }"
            )
            new_test = (
                "    @Test\n"
                "    fun bottomNavigation_switchesBetweenTopLevelDestinations() {\n"
                "        onView(withId(R.id.settingsFragment)).perform(click())\n"
                "        onView(withText(R.string.settings_personalization_section)).check(matches(isDisplayed()))\n\n"
                "        onView(withId(R.id.homeFragment)).perform(click())\n"
                "        onView(withId(R.id.tvGreeting)).check(matches(isDisplayed()))\n"
                "    }"
            )
            if old_test in content:
                if config.dry_run:
                    log_step(f"[DRY-RUN] Update bottom navigation test in {test_file.relative_to(root)}")
                else:
                    test_file.write_text(content.replace(old_test, new_test), encoding="utf-8")
                    log_step(f"Updated bottom navigation test in {test_file.relative_to(root)}")
            break

    # 9. Update CriticalJourney.kt
    bp_path = Path(*f"{config.app_package}.baselineprofile".split("."))
    journey_candidates = [
        root / "baselineprofile" / "src" / "main" / "java" / bp_path / "CriticalJourney.kt",
        root / "baselineprofile" / "src" / "main" / "java" / "com" / "example" / "androidcorebase" / "baselineprofile" / "CriticalJourney.kt",
    ]
    for journey in journey_candidates:
        if journey.exists():
            content = journey.read_text(encoding="utf-8")
            # Remove unused import Direction
            content = content.replace("import androidx.test.uiautomator.Direction\n", "")
            # Remove unused GESTURE_MARGIN_DIVISOR
            content = re.sub(
                r'/\*\* Keeps the scroll gesture clear of the system back-gesture edges\. \*/\s*private const val GESTURE_MARGIN_DIVISOR = \d+\s*',
                '',
                content,
            )
            old_journey_pattern = re.compile(
                r'\s*device\.clickOrFail\(By\.res\(packageName, "demoFragment"\), "the Demo tab"\).*?'
                r'device\.clickOrFail\(By\.res\(packageName, "actionSettings"\), "the Settings menu item"\)',
                re.DOTALL,
            )
            new_journey_snippet = (
                '\n        device.clickOrFail(By.res(packageName, "settingsFragment"), "the Settings tab")'
            )
            if old_journey_pattern.search(content):
                if config.dry_run:
                    log_step(f"[DRY-RUN] Update CriticalJourney.kt in {journey.relative_to(root)}")
                else:
                    content = old_journey_pattern.sub(new_journey_snippet, content)
                    journey.write_text(content, encoding="utf-8")
                    log_step(f"Updated CriticalJourney.kt in {journey.relative_to(root)}")
            break

    # 10. Clean app/build.gradle.kts kover verify sample includes
    app_build = root / "app" / "build.gradle.kts"
    if app_build.exists():
        content = app_build.read_text(encoding="utf-8")
        includes_pattern = re.compile(r'includes\s*\{[^{}]*classes\s*\([^{}]*?\)\s*\}', re.DOTALL)
        new_includes = (
            'includes {\n'
            '                classes(\n'
            '                    "*.feature.settings.presentation.viewmodel.SettingsViewModel",\n'
            '                )\n'
            '            }'
        )
        if includes_pattern.search(content):
            if config.dry_run:
                log_step(f"[DRY-RUN] Update Kover includes in {app_build.relative_to(root)}")
            else:
                content = includes_pattern.sub(new_includes, content)
                app_build.write_text(content, encoding="utf-8")
                log_step(f"Updated Kover bounds in {app_build.relative_to(root)}")

    # 11. Clean sample descriptors from committed baseline-prof.txt
    baseline_prof = root / "app" / "src" / "release" / "generated" / "baselineProfiles" / "baseline-prof.txt"
    if baseline_prof.exists():
        prof_content = baseline_prof.read_text(encoding="utf-8")
        clean_lines = [line for line in prof_content.splitlines() if "/sample/" not in line]
        if len(clean_lines) != len(prof_content.splitlines()):
            if config.dry_run:
                log_step(f"[DRY-RUN] Prune sample descriptors from {baseline_prof.relative_to(root)}")
            else:
                baseline_prof.write_text("\n".join(clean_lines) + "\n", encoding="utf-8")
                log_step(f"Pruned sample descriptors from {baseline_prof.relative_to(root)}")


def post_verification(config: InitConfig) -> None:
    root = config.root_dir
    log_info("Running post-refactor validation...")

    # Check for leftover old app package
    leftover_files: List[Path] = []
    old_target = "com.example.androidcorebase"
    for dirpath, _, filenames in os.walk(root):
        for filename in filenames:
            file_path = Path(dirpath) / filename
            if should_skip_file(file_path, root):
                continue
            try:
                text = file_path.read_text(encoding="utf-8")
                if old_target in text:
                    leftover_files.append(file_path.relative_to(root))
            except (UnicodeDecodeError, OSError):
                continue

    if leftover_files:
        log_warn(f"Found {len(leftover_files)} files with leftover '{old_target}':")
        for f in leftover_files[:10]:
            print(f"    - {f}")
        if len(leftover_files) > 10:
            print(f"    ... and {len(leftover_files) - 10} more.")
    else:
        log_success(f"No leftover '{old_target}' found!")

    # Check build with Gradle
    if config.skip_build_check:
        log_info("Skipping Gradle build check (--skip-build-check requested).")
        return

    gradlew = root / ("gradlew.bat" if sys.platform == "win32" else "gradlew")
    if not gradlew.exists():
        log_warn("gradlew wrapper not found; skipping Gradle build verification.")
        return

    # Auto-format Kotlin files (e.g. import ordering) after package renames
    log_info("Formatting Kotlin files with ktlint (./gradlew ktlintFormat --no-build-cache --rerun-tasks)...")
    try:
        subprocess.run(
            [str(gradlew), "ktlintFormat", "--no-build-cache", "--rerun-tasks", "--quiet"],
            cwd=root,
            check=True,
        )
        log_success("ktlintFormat applied cleanly.")
    except subprocess.SubprocessError as e:
        log_warn(f"ktlintFormat returned non-zero exit or warning: {e}")

    # If core was renamed, dump api first so metalava matches
    if config.scope == "full":
        log_info("Regenerating Metalava API dumps for renamed core...")
        try:
            subprocess.run(
                [str(gradlew), ":core:apiDump", ":core:ui-compose:apiDump", "--stacktrace"],
                cwd=root,
                check=True,
            )
            log_success("Metalava API dumps updated successfully.")
        except subprocess.SubprocessError as e:
            log_warn(f"Could not regenerate API dumps: {e}")

    # Check build with Gradle
    if config.skip_build_check:
        log_info("Skipping Gradle build check (--skip-build-check requested).")
        return

    log_info("Running Gradle verification check (./gradlew check)...")
    try:
        subprocess.run(
            [str(gradlew), "check", "--stacktrace"],
            cwd=root,
            check=True,
        )
        log_success("Gradle check passed! The project builds cleanly with the new naming.")
    except subprocess.SubprocessError as e:
        log_error("Gradle check failed. Review errors above.")
        sys.exit(1)


def interactive_prompt(root_dir: Path, cli_args: Optional[argparse.Namespace] = None) -> InitConfig:
    print(style("=" * 64, "36;1"))
    print(style("  AndroidCoreBase Project Initialization Wizard", "36;1"))
    print(style("=" * 64, "36;1"))
    print("This tool renames and refactors the base project for your new app.\n")

    # Project Name
    default_p_name = cli_args.project_name if (cli_args and cli_args.project_name) else None
    while True:
        prompt_str = (
            f"Project Name (PascalCase) [Default: \"{default_p_name}\"]:"
            if default_p_name
            else "Project Name (PascalCase, e.g. AcmeShop):"
        )
        p_name = input(f"{style(prompt_str, '32;1')} ").strip()
        if not p_name and default_p_name:
            p_name = default_p_name
        if not p_name:
            print("Project name cannot be empty.")
            continue
        if not validate_project_name(p_name):
            print("Invalid project name. Must start with capital letter and contain only alphanumeric chars.")
            continue
        break

    # App Display Name
    if cli_args and cli_args.app_name:
        default_app_name = cli_args.app_name
    else:
        default_app_name = re.sub(r"([a-z])([A-Z])", r"\1 \2", p_name)
    a_name = input(f"{style(f'App Display Name [Default: \"{default_app_name}\"]:', '32;1')} ").strip()
    if not a_name:
        a_name = default_app_name

    # Package Name
    default_pkg = cli_args.package if (cli_args and cli_args.package) else f"com.example.{p_name.lower()}"
    while True:
        pkg = input(f"{style(f'Application Package / ID [e.g. \"{default_pkg}\"]:', '32;1')} ").strip()
        if not pkg:
            pkg = default_pkg
        if not validate_package(pkg):
            print("Invalid package format. Must be at least two dot-separated lowercase identifiers without keywords.")
            continue
        break

    # Scope
    cli_scope = cli_args.scope if (cli_args and cli_args.scope) else "full"
    default_scope_choice = "2" if cli_scope == "app-only" else "1"
    print(f"\n{style('Choose Refactoring Scope:', '33;1')}")
    print("  1) Full Standalone - Rename app, core, convention plugins, and themes")
    print("  2) App-Only - Keep core library namespace (com.thanhng224.androidcorebase.core) unchanged")
    scope_choice = input(f"{style(f'Select [1/2, default {default_scope_choice}]:', '32;1')} ").strip()
    if not scope_choice:
        scope_choice = default_scope_choice
    scope = "app-only" if scope_choice == "2" else "full"

    if cli_args and cli_args.core_package:
        default_core_pkg = cli_args.core_package
    else:
        default_core_pkg = f"{pkg}.core" if scope == "full" else "com.thanhng224.androidcorebase.core"

    core_pkg = default_core_pkg
    if scope == "full":
        custom_core = input(f"{style(f'Core Library Package [Default: \"{default_core_pkg}\"]:', '32;1')} ").strip()
        if custom_core:
            while not validate_package(custom_core):
                print("Invalid core package format. Must be at least two dot-separated lowercase identifiers without keywords.")
                custom_core = input(f"{style(f'Core Library Package [Default: \"{default_core_pkg}\"]:', '32;1')} ").strip()
                if not custom_core:
                    custom_core = default_core_pkg
                    break
            core_pkg = custom_core

    # Sample code pruning
    default_clean = bool(cli_args.clean_samples) if cli_args else False
    clean_prompt = "Remove demo weather and UI kit sample code? [Y/n]:" if default_clean else "Remove demo weather and UI kit sample code? [y/N]:"
    clean_choice = input(f"\n{style(clean_prompt, '32;1')} ").strip().lower()
    if not clean_choice:
        clean_samples = default_clean
    else:
        clean_samples = clean_choice in ("y", "yes")

    # Dry-run
    default_dry = bool(cli_args.dry_run) if cli_args else False
    dry_prompt = "Run in Dry-Run mode first (simulate without changing)? [Y/n]:" if default_dry else "Run in Dry-Run mode first (simulate without changing)? [y/N]:"
    dry_choice = input(f"{style(dry_prompt, '32;1')} ").strip().lower()
    if not dry_choice:
        dry_run = default_dry
    else:
        dry_run = dry_choice in ("y", "yes")

    skip_build_check = bool(cli_args.skip_build_check) if cli_args else False
    force = bool(cli_args.force) if cli_args else False

    return InitConfig(
        root_dir=root_dir,
        project_name=p_name,
        app_name=a_name,
        app_package=pkg,
        core_package=core_pkg,
        scope=scope,
        clean_samples=clean_samples,
        dry_run=dry_run,
        skip_build_check=skip_build_check,
        force=force,
    )


def parse_arguments(root_dir: Path) -> InitConfig:
    parser = argparse.ArgumentParser(
        description="Initialize and refactor AndroidCoreBase for a new application.",
        formatter_class=argparse.ArgumentDefaultsHelpFormatter,
    )
    parser.add_argument("--project-name", type=str, help="PascalCase project name (e.g. AcmeShop)")
    parser.add_argument("--app-name", type=str, help="Human-readable application name (e.g. 'Acme Shop')")
    parser.add_argument("--package", type=str, help="Application ID & package (e.g. com.acme.shop)")
    parser.add_argument("--core-package", type=str, help="Core library package (default: <package>.core)")
    parser.add_argument(
        "--scope",
        choices=["full", "app-only"],
        default="full",
        help="Refactoring scope: 'full' renames both app and core; 'app-only' keeps core intact",
    )
    parser.add_argument(
        "--clean-samples",
        action="store_true",
        help="Remove weather demo and UI kit sample code",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Simulate the refactoring without making any filesystem modifications",
    )
    parser.add_argument(
        "--skip-build-check",
        action="store_true",
        help="Skip Gradle verification check after refactoring",
    )
    parser.add_argument(
        "--force",
        action="store_true",
        help="Ignore uncommitted git working tree check",
    )
    parser.add_argument(
        "--non-interactive",
        action="store_true",
        help="Fail if required parameters are missing instead of prompting",
    )

    args = parser.parse_args()

    # If any mandatory arg is missing and not non-interactive, launch wizard
    if not (args.project_name and args.package) and not args.non_interactive:
        return interactive_prompt(root_dir, cli_args=args)

    if not args.project_name:
        parser.error("--project-name is required in non-interactive mode")
    if not args.package:
        parser.error("--package is required in non-interactive mode")

    if not validate_project_name(args.project_name):
        parser.error(f"Invalid --project-name '{args.project_name}'. Must be PascalCase alphanumeric.")
    if not validate_package(args.package):
        parser.error(f"Invalid --package '{args.package}'. Must be valid lowercase dot-separated package without reserved keywords.")

    core_pkg = args.core_package or (f"{args.package}.core" if args.scope == "full" else "com.thanhng224.androidcorebase.core")
    if not validate_package(core_pkg):
        parser.error(f"Invalid --core-package '{core_pkg}'. Must be valid lowercase dot-separated package without reserved keywords.")

    app_name = args.app_name or re.sub(r"([a-z])([A-Z])", r"\1 \2", args.project_name)

    return InitConfig(
        root_dir=root_dir,
        project_name=args.project_name,
        app_name=app_name,
        app_package=args.package,
        core_package=core_pkg,
        scope=args.scope,
        clean_samples=args.clean_samples,
        dry_run=args.dry_run,
        skip_build_check=args.skip_build_check,
        force=args.force,
    )


def validate_config(config: InitConfig) -> None:
    """Performs strict pre-flight checks on inputs and checks for directory/file collision."""
    errors: List[str] = []

    if not validate_project_name(config.project_name):
        errors.append(f"Invalid project name '{config.project_name}'. Must be PascalCase alphanumeric.")

    if not validate_package(config.app_package):
        errors.append(f"Invalid application package '{config.app_package}'. Must not contain reserved keywords or invalid characters.")

    if not validate_package(config.core_package):
        errors.append(f"Invalid core package '{config.core_package}'. Must not contain reserved keywords or invalid characters.")

    moves = get_source_moves(config)
    collisions = check_move_collisions(moves)
    if collisions:
        errors.extend(collisions)

    if errors:
        for err in errors:
            log_error(err)
        raise ValueError(f"Configuration pre-flight check failed with {len(errors)} error(s).")


def main() -> None:
    root_dir = Path(__file__).resolve().parent.parent
    config = parse_arguments(root_dir)

    log_info("Starting AndroidCoreBase initialization...")
    print(f"  Project Name: {style(config.project_name, '32;1')}")
    print(f"  App Name:     {style(config.app_name, '32;1')}")
    print(f"  App Package:  {style(config.app_package, '32;1')}")
    print(f"  Core Package: {style(config.core_package, '32;1')}")
    print(f"  Scope:        {style(config.scope, '33;1')}")
    print(f"  Clean Demo:   {style(str(config.clean_samples), '33')}")
    print(f"  Dry Run:      {style(str(config.dry_run), '33')}\n")

    # 0. Pre-flight input validation & collision checks
    try:
        validate_config(config)
    except ValueError as e:
        log_error(str(e))
        sys.exit(1)

    # 0b. Check git clean status
    if not config.force and not config.dry_run:
        if not check_git_clean(root_dir):
            log_error(
                "Git working directory contains uncommitted changes. "
                "Commit or stash your changes before running this script, or use --force."
            )
            sys.exit(1)

    # 1. Directory & File moves
    log_info("Phase 1: Reorganizing source directories...")
    moves = get_source_moves(config)
    dirs_to_prune: List[Tuple[Path, Path]] = []

    for src, dst in moves:
        if src.exists():
            log_step(f"Moving: {src.relative_to(root_dir)} -> {dst.relative_to(root_dir)}")
            run_git_or_shutil_move(src, dst, root_dir, config.dry_run)
            if src.is_dir():
                dirs_to_prune.append((src.parent, src.parents[3]))

    # Prune empty parent directories
    if not config.dry_run:
        for p, stop in dirs_to_prune:
            prune_empty_dirs(p, stop)

    # 2. Text replacements
    log_info("Phase 2: Updating package names, manifests, and build configs...")
    replacements = build_replacements_table(config)
    mod_count = apply_text_replacements(root_dir, replacements, config.dry_run)
    log_success(f"Applied replacements across {mod_count} files.")

    # 3. Optional sample code pruning
    if config.clean_samples:
        log_info("Phase 3: Pruning sample code...")
        clean_sample_code(config)

    # 4. Post-verification and build check
    if not config.dry_run:
        log_info("Phase 4: Running verification...")
        post_verification(config)
    else:
        log_info("Dry-run completed. No files were modified on disk.")

    log_success(f"Project initialized successfully for '{config.project_name}'!")
    if not config.dry_run:
        print(
            f"\n{style('Tip:', '36;1')} Baseline Profile was updated for {config.app_package}. "
            "To re-generate an optimal profile after creating your own user journeys, run:\n"
            f"      {style('./gradlew :baselineprofile:generateBaselineProfile', '32')}\n"
        )


if __name__ == "__main__":
    main()
