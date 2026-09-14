#!/usr/bin/env python3
"""
scripts/test_init_project.py

Comprehensive test suite for init_project.py.
Covers:
1. Destination collision detection & data loss prevention.
2. Clean samples synchronization (MainActivity, MainActivityTest, CriticalJourney, drawables, strings).
3. Android XML & string resource escaping (special characters, quotes, ampersands).
4. Full scope artifact and publication/consumer consistency.
5. Strict package and core-package validation (Kotlin/Java reserved keywords).
6. End-to-end full refactoring run on a simulated project tree.
"""

from __future__ import annotations

import argparse
import tempfile
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path
from unittest.mock import patch

from init_project import (
    CollisionError,
    InitConfig,
    apply_text_replacements,
    build_replacements_table,
    check_move_collisions,
    clean_sample_code,
    escape_android_string_resource,
    get_source_moves,
    interactive_prompt,
    run_git_or_shutil_move,
    should_skip_file,
    validate_config,
    validate_package,
    validate_project_name,
)


class TestInitProject(unittest.TestCase):

    def test_validate_project_name(self) -> None:
        self.assertTrue(validate_project_name("AcmeShop"))
        self.assertTrue(validate_project_name("MyAwesomeApp"))
        self.assertTrue(validate_project_name("App123"))
        self.assertTrue(validate_project_name("Super_App"))

        self.assertFalse(validate_project_name("acmeShop"))  # lowercase start
        self.assertFalse(validate_project_name("123App"))    # digit start
        self.assertFalse(validate_project_name("Acme-Shop")) # hyphen
        self.assertFalse(validate_project_name(""))          # empty

    def test_validate_package(self) -> None:
        self.assertTrue(validate_package("com.acme.shop"))
        self.assertTrue(validate_package("vn.edu.app"))
        self.assertTrue(validate_package("org.cool.mobile.tool"))

        self.assertFalse(validate_package("singleword"))
        self.assertFalse(validate_package("com.acme..shop"))
        self.assertFalse(validate_package("Com.Acme.Shop"))
        self.assertFalse(validate_package("com.acme-shop.app"))

    def test_validate_package_rejects_keywords(self) -> None:
        # Kotlin & Java reserved keywords
        self.assertFalse(validate_package("com.when.shop"))      # 'when' Kotlin keyword
        self.assertFalse(validate_package("com.fun.shop"))       # 'fun'
        self.assertFalse(validate_package("com.is.shop"))        # 'is'
        self.assertFalse(validate_package("com.in.shop"))        # 'in'
        self.assertFalse(validate_package("com.val.app"))        # 'val'
        self.assertFalse(validate_package("com.var.app"))        # 'var'
        self.assertFalse(validate_package("com.class.app"))      # 'class'
        self.assertFalse(validate_package("com.object.app"))     # 'object'
        self.assertFalse(validate_package("com.interface.app"))  # 'interface'
        self.assertFalse(validate_package("com.null.app"))       # 'null'
        self.assertFalse(validate_package("com.true.app"))       # 'true'
        self.assertFalse(validate_package("com.false.app"))      # 'false'
        self.assertFalse(validate_package("com._.app"))          # lone underscore

    def test_escape_android_string_resource(self) -> None:
        # Basic & XML entity escaping
        self.assertEqual(escape_android_string_resource("Acme & Shop"), "Acme &amp; Shop")
        self.assertEqual(escape_android_string_resource("<New App>"), "&lt;New App&gt;")

        # Quotes and apostrophes
        self.assertEqual(
            escape_android_string_resource("Acme's \"Super\" Shop"),
            "Acme\\'s \\\"Super\\\" Shop",
        )

        # Leading reference symbols
        self.assertEqual(escape_android_string_resource("@Special"), "\\@Special")
        self.assertEqual(escape_android_string_resource("?AttrApp"), "\\?AttrApp")

        # XML validity
        sample_names = [
            "Acme & Shop",
            "Acme's \"Super\" & <Best> Shop",
            "@AppName",
            "?AppTheme",
            "Plain App",
        ]
        for name in sample_names:
            escaped = escape_android_string_resource(name)
            xml_str = f'<resources><string name="app_name">{escaped}</string></resources>'
            elem = ET.fromstring(xml_str)
            self.assertIsNotNone(elem.find("string"))

    def test_check_move_collisions_detects_existing_destination(self) -> None:
        with tempfile.TemporaryDirectory() as tmpdir:
            root = Path(tmpdir)
            src = root / "src_file.txt"
            dst = root / "dst_file.txt"
            src.write_text("source content", encoding="utf-8")
            dst.write_text("existing pre-existing content", encoding="utf-8")

            # Check collision detector
            conflicts = check_move_collisions([(src, dst)])
            self.assertTrue(len(conflicts) > 0)
            self.assertIn("destination already exists on disk", conflicts[0])

            # Check that run_git_or_shutil_move raises CollisionError and NEVER deletes dst
            with self.assertRaises(CollisionError):
                run_git_or_shutil_move(src, dst, root, dry_run=False)

            # Destination content must be preserved intact
            self.assertEqual(dst.read_text(encoding="utf-8"), "existing pre-existing content")
            self.assertEqual(src.read_text(encoding="utf-8"), "source content")

    def test_check_move_collisions_detects_duplicate_destinations(self) -> None:
        with tempfile.TemporaryDirectory() as tmpdir:
            root = Path(tmpdir)
            src1 = root / "src1.txt"
            src2 = root / "src2.txt"
            dst = root / "dst.txt"
            src1.touch()
            src2.touch()

            conflicts = check_move_collisions([(src1, dst), (src2, dst)])
            self.assertTrue(len(conflicts) > 0)
            self.assertIn("multiple sources mapped to same destination", conflicts[0])

    def test_should_skip_file_committed_baseline_profile(self) -> None:
        root = Path("/mock/root")
        committed_prof = root / "app" / "src" / "release" / "generated" / "baselineProfiles" / "baseline-prof.txt"
        build_prof = root / "app" / "build" / "intermediates" / "baselineProfiles" / "baseline-prof.txt"
        build_gen = root / "app" / "build" / "generated" / "source" / "BuildConfig.kt"
        git_file = root / ".git" / "config"

        self.assertFalse(should_skip_file(committed_prof, root))
        self.assertTrue(should_skip_file(build_prof, root))
        self.assertTrue(should_skip_file(build_gen, root))
        self.assertTrue(should_skip_file(git_file, root))

    def test_build_replacements_table_full(self) -> None:
        config = InitConfig(
            root_dir=Path("/mock/root"),
            project_name="AcmeShop",
            app_name="Acme & Shop",
            app_package="com.acme.shop",
            core_package="com.acme.shop.core",
            scope="full",
            clean_samples=False,
            dry_run=False,
            skip_build_check=True,
            force=True,
        )
        table = dict(build_replacements_table(config))

        self.assertEqual(table["com.example.androidcorebase"], "com.acme.shop")
        self.assertEqual(table["AndroidCoreBaseApplication"], "AcmeShopApplication")
        self.assertEqual(table['rootProject.name = "AndroidCoreBase"'], 'rootProject.name = "AcmeShop"')
        self.assertEqual(table["com.thanhng224.androidcorebase.core"], "com.acme.shop.core")
        self.assertEqual(table["androidcorebase.buildlogic"], "acmeshop.buildlogic")
        self.assertEqual(table["AndroidCoreBaseTheme"], "AcmeShopTheme")
        self.assertEqual(table["Theme.AndroidCoreBase"], "Theme.AcmeShop")

        # Escaped app_name
        self.assertEqual(
            table['<string name="app_name">XML Base</string>'],
            '<string name="app_name">Acme &amp; Shop</string>',
        )

        # Publication verification replacements
        self.assertEqual(table["AndroidCoreBase-*.pom"], "AcmeShop-*.pom")
        self.assertEqual(table["AndroidCoreBase-ui-compose-*.pom"], "AcmeShop-ui-compose-*.pom")
        self.assertEqual(table["AndroidCoreBase-*.aar"], "AcmeShop-*.aar")
        self.assertEqual(table["AndroidCoreBase-ui-compose-*.aar"], "AcmeShop-ui-compose-*.aar")

        # Consumer integration replacements
        self.assertEqual(
            table['implementation("com.github.ThanhNg224:AndroidCoreBase:$androidCoreBaseVersion")'],
            'implementation("com.github.ThanhNg224:AcmeShop:$androidCoreBaseVersion")',
        )
        self.assertEqual(
            table['implementation("com.github.ThanhNg224:AndroidCoreBase-ui-compose:$androidCoreBaseVersion")'],
            'implementation("com.github.ThanhNg224:AcmeShop-ui-compose:$androidCoreBaseVersion")',
        )
        self.assertEqual(
            table['rootProject.name = "androidcorebase-consumer"'],
            'rootProject.name = "acmeshop-consumer"',
        )

    def test_build_replacements_table_app_only(self) -> None:
        config = InitConfig(
            root_dir=Path("/mock/root"),
            project_name="AcmeShop",
            app_name="Acme Shop",
            app_package="com.acme.shop",
            core_package="com.thanhng224.androidcorebase.core",
            scope="app-only",
            clean_samples=False,
            dry_run=False,
            skip_build_check=True,
            force=True,
        )
        table = dict(build_replacements_table(config))

        self.assertEqual(table["com.example.androidcorebase"], "com.acme.shop")
        self.assertEqual(table["AndroidCoreBaseApplication"], "AcmeShopApplication")
        self.assertNotIn("com.thanhng224.androidcorebase.core", table)
        self.assertNotIn("AndroidCoreBase-*.pom", table)

    def test_source_moves_planning(self) -> None:
        with tempfile.TemporaryDirectory() as tmpdir:
            root = Path(tmpdir)
            app_dir = root / "app" / "src" / "main" / "java" / "com" / "example" / "androidcorebase"
            app_dir.mkdir(parents=True)
            (app_dir / "AndroidCoreBaseApplication.kt").touch()

            core_dir = root / "core" / "src" / "main" / "java" / "com" / "thanhng224" / "androidcorebase" / "core"
            core_dir.mkdir(parents=True)

            config = InitConfig(
                root_dir=root,
                project_name="AcmeShop",
                app_name="Acme Shop",
                app_package="com.acme.shop",
                core_package="com.acme.shop.core",
                scope="full",
                clean_samples=False,
                dry_run=False,
                skip_build_check=True,
                force=True,
            )

            moves = get_source_moves(config)
            move_srcs = [src for src, _ in moves]

            self.assertIn(app_dir, move_srcs)
            self.assertIn(core_dir, move_srcs)
            self.assertIn(app_dir / "AndroidCoreBaseApplication.kt", move_srcs)

    def test_clean_samples_sync(self) -> None:
        with tempfile.TemporaryDirectory() as tmpdir:
            root = Path(tmpdir)
            app_pkg = Path("com/acme/shop")
            bp_pkg = Path("com/acme/shop/baselineprofile")

            # Setup sample directories and files
            sample_dir = root / "app" / "src" / "main" / "java" / app_pkg / "sample"
            sample_dir.mkdir(parents=True)
            (sample_dir / "Demo.kt").touch()

            # Layouts & drawables
            layout_dir = root / "app" / "src" / "main" / "res" / "layout"
            layout_dir.mkdir(parents=True)
            (layout_dir / "fragment_demo.xml").touch()
            (layout_dir / "fragment_design_system.xml").touch()

            drawable_dir = root / "app" / "src" / "main" / "res" / "drawable"
            drawable_dir.mkdir(parents=True)
            (drawable_dir / "ic_nav_demo.xml").touch()
            (drawable_dir / "ic_nav_ui_kit.xml").touch()

            # Strings XML
            values_dir = root / "app" / "src" / "main" / "res" / "values"
            values_dir.mkdir(parents=True)
            strings_xml = values_dir / "strings.xml"
            strings_xml.write_text(
                '<resources>\n'
                '    <string name="app_name">Acme Shop</string>\n'
                '    <string name="demo_title">Demo</string>\n'
                '    <string name="design_system_title">UI Kit</string>\n'
                '</resources>\n',
                encoding="utf-8",
            )

            # MainActivity.kt
            main_act_file = root / "app" / "src" / "main" / "java" / app_pkg / "MainActivity.kt"
            main_act_file.parent.mkdir(parents=True, exist_ok=True)
            main_act_file.write_text(
                'package com.acme.shop\n'
                'class MainActivity {\n'
                '    fun onReady() {\n'
                '        appBarConfiguration = AppBarConfiguration(setOf(R.id.homeFragment, R.id.demoFragment, R.id.designSystemFragment))\n'
                '    }\n'
                '}\n',
                encoding="utf-8",
            )

            # MainActivityTest.kt
            test_file = root / "app" / "src" / "androidTest" / "java" / app_pkg / "MainActivityTest.kt"
            test_file.parent.mkdir(parents=True, exist_ok=True)
            test_file.write_text(
                '    @Test\n'
                '    fun bottomNavigation_switchesBetweenTopLevelDestinations() {\n'
                '        onView(withId(R.id.demoFragment)).perform(click())\n'
                '        onView(withId(R.id.tvCount)).check(matches(isDisplayed()))\n\n'
                '        onView(withId(R.id.designSystemFragment)).perform(click())\n'
                '        onView(withText(R.string.design_system_headline_sample)).check(matches(isDisplayed()))\n\n'
                '        onView(withId(R.id.demoFragment)).perform(click())\n'
                '        onView(withId(R.id.tvCount)).check(matches(isDisplayed()))\n'
                '    }\n',
                encoding="utf-8",
            )

            # CriticalJourney.kt
            journey_file = root / "baselineprofile" / "src" / "main" / "java" / bp_pkg / "CriticalJourney.kt"
            journey_file.parent.mkdir(parents=True, exist_ok=True)
            journey_file.write_text(
                'import androidx.test.uiautomator.By\n'
                'import androidx.test.uiautomator.Direction\n'
                'import androidx.test.uiautomator.UiDevice\n\n'
                '/** Keeps the scroll gesture clear of the system back-gesture edges. */\n'
                'private const val GESTURE_MARGIN_DIVISOR = 5\n\n'
                'fun execute(device: UiDevice, packageName: String) {\n'
                '        device.clickOrFail(By.res(packageName, "demoFragment"), "the Demo tab")\n'
                '        device.clickOrFail(By.res(packageName, "designSystemFragment"), "the UI Kit tab")\n'
                '        check(device.wait(Until.hasObject(By.res(packageName, "composeInteropDemo")), WAIT_TIMEOUT_MS)) {\n'
                '            "UI Kit tab never showed composeInteropDemo after scrolling to the bottom"\n'
                '        }\n'
                '        device.clickOrFail(By.res(packageName, "actionSettings"), "the Settings menu item")\n'
                '        check(device.wait(Until.hasObject(By.res(packageName, "rowLanguage")), WAIT_TIMEOUT_MS))\n'
                '}\n',
                encoding="utf-8",
            )

            # app/build.gradle.kts with Kover configuration
            app_build_file = root / "app" / "build.gradle.kts"
            app_build_file.write_text(
                'kover {\n'
                '    reports {\n'
                '        filters {\n'
                '            includes {\n'
                '                classes(\n'
                '                    "*.sample.demo.data.mapper.*",\n'
                '                    "*.sample.demo.domain.usecase.FetchDemoWeatherUseCase",\n'
                '                    "*.sample.designsystem.presentation.viewmodel.DesignSystemViewModel",\n'
                '                )\n'
                '            }\n'
                '        }\n'
                '    }\n'
                '}\n',
                encoding="utf-8",
            )

            # baseline-prof.txt with sample descriptors
            prof_dir = root / "app" / "src" / "release" / "generated" / "baselineProfiles"
            prof_dir.mkdir(parents=True)
            prof_file = prof_dir / "baseline-prof.txt"
            prof_file.write_text(
                "Lcom/example/androidcorebase/MainActivity;\n"
                "Lcom/example/androidcorebase/sample/demo/DemoFragment;\n"
                "Lcom/example/androidcorebase/sample/designsystem/DesignSystemFragment;\n"
                "Lcom/example/androidcorebase/feature/settings/SettingsFragment;\n",
                encoding="utf-8",
            )

            config = InitConfig(
                root_dir=root,
                project_name="AcmeShop",
                app_name="Acme Shop",
                app_package="com.acme.shop",
                core_package="com.acme.shop.core",
                scope="full",
                clean_samples=True,
                dry_run=False,
                skip_build_check=True,
                force=True,
            )

            clean_sample_code(config)

            # Assert sample dir, layouts, and drawables removed
            self.assertFalse(sample_dir.exists())
            self.assertFalse((layout_dir / "fragment_demo.xml").exists())
            self.assertFalse((layout_dir / "fragment_design_system.xml").exists())
            self.assertFalse((drawable_dir / "ic_nav_demo.xml").exists())
            self.assertFalse((drawable_dir / "ic_nav_ui_kit.xml").exists())

            # Assert strings.xml cleaned
            strings_content = strings_xml.read_text(encoding="utf-8")
            self.assertNotIn("demo_title", strings_content)
            self.assertNotIn("design_system_title", strings_content)
            self.assertIn("app_name", strings_content)

            # Assert MainActivity.kt updated
            main_content = main_act_file.read_text(encoding="utf-8")
            self.assertNotIn("demoFragment", main_content)
            self.assertNotIn("designSystemFragment", main_content)
            self.assertIn("setOf(R.id.homeFragment, R.id.settingsFragment)", main_content)

            # Assert MainActivityTest.kt updated
            test_content = test_file.read_text(encoding="utf-8")
            self.assertNotIn("demoFragment", test_content)
            self.assertNotIn("designSystemFragment", test_content)
            self.assertIn("settingsFragment", test_content)

            # Assert CriticalJourney.kt updated
            journey_content = journey_file.read_text(encoding="utf-8")
            self.assertNotIn("demoFragment", journey_content)
            self.assertNotIn("designSystemFragment", journey_content)
            self.assertNotIn("Direction", journey_content)
            self.assertNotIn("GESTURE_MARGIN_DIVISOR", journey_content)
            self.assertIn("settingsFragment", journey_content)

            # Assert Kover includes updated to SettingsViewModel without empty classes()
            kover_content = app_build_file.read_text(encoding="utf-8")
            self.assertNotIn("sample.demo", kover_content)
            self.assertNotIn("sample.designsystem", kover_content)
            self.assertIn("*.feature.settings.presentation.viewmodel.SettingsViewModel", kover_content)
            self.assertNotIn("classes()", kover_content)

            # Assert baseline-prof.txt has sample descriptors pruned
            prof_content = prof_file.read_text(encoding="utf-8")
            self.assertNotIn("/sample/", prof_content)
            self.assertIn("MainActivity", prof_content)
            self.assertIn("SettingsFragment", prof_content)

    @patch("builtins.input", side_effect=["", "", "", "", "", "", ""])
    def test_interactive_prompt_inherits_cli_flags(self, mock_input) -> None:
        cli_args = argparse.Namespace(
            project_name="AcmeShop",
            app_name="Acme Shop",
            package="com.acme.shop",
            core_package=None,
            scope="full",
            clean_samples=True,
            dry_run=True,
            skip_build_check=True,
            force=True,
            non_interactive=False,
        )
        config = interactive_prompt(Path("/mock/root"), cli_args=cli_args)
        self.assertEqual(config.project_name, "AcmeShop")
        self.assertEqual(config.app_name, "Acme Shop")
        self.assertEqual(config.app_package, "com.acme.shop")
        self.assertEqual(config.core_package, "com.acme.shop.core")
        self.assertEqual(config.scope, "full")
        self.assertTrue(config.clean_samples)
        self.assertTrue(config.dry_run)
        self.assertTrue(config.skip_build_check)
        self.assertTrue(config.force)

    @patch("builtins.input", side_effect=["AcmeShop", "", "", "1", "", "n", "n"])
    def test_interactive_prompt_can_override_cli_flags(self, mock_input) -> None:
        cli_args = argparse.Namespace(
            project_name=None,
            app_name=None,
            package=None,
            core_package=None,
            scope="full",
            clean_samples=True,
            dry_run=True,
            skip_build_check=False,
            force=False,
            non_interactive=False,
        )
        config = interactive_prompt(Path("/mock/root"), cli_args=cli_args)
        self.assertEqual(config.project_name, "AcmeShop")
        self.assertFalse(config.clean_samples)
        self.assertFalse(config.dry_run)

    def test_e2e_full_run_on_repo_copy(self) -> None:
        """Runs the entire refactoring workflow on a complete synthetic project tree."""
        with tempfile.TemporaryDirectory() as tmpdir:
            root = Path(tmpdir)

            # 1. Setup simulated repository files
            # settings.gradle.kts
            (root / "settings.gradle.kts").write_text('rootProject.name = "AndroidCoreBase"\n', encoding="utf-8")

            # app/
            app_main = root / "app" / "src" / "main" / "java" / "com" / "example" / "androidcorebase"
            app_main.mkdir(parents=True)
            (app_main / "AndroidCoreBaseApplication.kt").write_text(
                'package com.example.androidcorebase\nclass AndroidCoreBaseApplication\n',
                encoding="utf-8",
            )
            (app_main / "MainActivity.kt").write_text(
                'package com.example.androidcorebase\n'
                'import com.example.androidcorebase.AndroidCoreBaseApplication\n'
                'class MainActivity {\n'
                '    fun setup() {\n'
                '        appBarConfiguration = AppBarConfiguration(setOf(R.id.homeFragment, R.id.demoFragment, R.id.designSystemFragment))\n'
                '    }\n'
                '}\n',
                encoding="utf-8",
            )

            # app/src/androidTest
            app_test = root / "app" / "src" / "androidTest" / "java" / "com" / "example" / "androidcorebase"
            app_test.mkdir(parents=True)
            (app_test / "MainActivityTest.kt").write_text(
                '    @Test\n'
                '    fun bottomNavigation_switchesBetweenTopLevelDestinations() {\n'
                '        onView(withId(R.id.demoFragment)).perform(click())\n'
                '        onView(withId(R.id.tvCount)).check(matches(isDisplayed()))\n\n'
                '        onView(withId(R.id.designSystemFragment)).perform(click())\n'
                '        onView(withText(R.string.design_system_headline_sample)).check(matches(isDisplayed()))\n\n'
                '        onView(withId(R.id.demoFragment)).perform(click())\n'
                '        onView(withId(R.id.tvCount)).check(matches(isDisplayed()))\n'
                '    }\n',
                encoding="utf-8",
            )

            # app/src/main/res/values/strings.xml
            res_val = root / "app" / "src" / "main" / "res" / "values"
            res_val.mkdir(parents=True)
            (res_val / "strings.xml").write_text(
                '<resources>\n'
                '    <string name="app_name">XML Base</string>\n'
                '    <string name="home_title">Home</string>\n'
                '    <string name="demo_title">Demo</string>\n'
                '</resources>\n',
                encoding="utf-8",
            )

            # baselineprofile/
            bp_dir = root / "baselineprofile" / "src" / "main" / "java" / "com" / "example" / "androidcorebase" / "baselineprofile"
            bp_dir.mkdir(parents=True)
            (bp_dir / "CriticalJourney.kt").write_text(
                'package com.example.androidcorebase.baselineprofile\n'
                'import androidx.test.uiautomator.By\n'
                'import androidx.test.uiautomator.Direction\n'
                'import androidx.test.uiautomator.UiDevice\n'
                '/** Keeps the scroll gesture clear of the system back-gesture edges. */\n'
                'private const val GESTURE_MARGIN_DIVISOR = 5\n'
                'fun execute(device: UiDevice, packageName: String) {\n'
                '        device.clickOrFail(By.res(packageName, "demoFragment"), "the Demo tab")\n'
                '        check(device.wait(Until.hasObject(By.res(packageName, "composeInteropDemo")), WAIT_TIMEOUT_MS)) {\n'
                '            "UI Kit tab never showed composeInteropDemo after scrolling to the bottom"\n'
                '        }\n'
                '        device.clickOrFail(By.res(packageName, "actionSettings"), "the Settings menu item")\n'
                '        check(device.wait(Until.hasObject(By.res(packageName, "rowLanguage")), WAIT_TIMEOUT_MS))\n'
                '}\n',
                encoding="utf-8",
            )

            # core/
            core_dir = root / "core" / "src" / "main" / "java" / "com" / "thanhng224" / "androidcorebase" / "core"
            core_dir.mkdir(parents=True)
            (core_dir / "CoreClass.kt").write_text(
                'package com.thanhng224.androidcorebase.core\n'
                'class CoreClass\n',
                encoding="utf-8",
            )
            (root / "core" / "build.gradle.kts").write_text(
                'publishedLibrary {\n    artifactId.set("AndroidCoreBase")\n}\n',
                encoding="utf-8",
            )

            # scripts/verify-publication.sh
            scripts_dir = root / "scripts"
            scripts_dir.mkdir(parents=True)
            (scripts_dir / "verify-publication.sh").write_text(
                'CORE_POM="$(find "$TEMP_REPO_DIR" -name \'AndroidCoreBase-*.pom\' ! -name \'*ui-compose*\')"\n',
                encoding="utf-8",
            )

            # integration/consumer/
            consumer_dir = root / "integration" / "consumer" / "app"
            consumer_dir.mkdir(parents=True)
            (consumer_dir / "build.gradle.kts").write_text(
                'dependencies {\n    implementation("com.github.ThanhNg224:AndroidCoreBase:$androidCoreBaseVersion")\n}\n',
                encoding="utf-8",
            )

            # baseline-prof.txt
            prof_dir = root / "app" / "src" / "release" / "generated" / "baselineProfiles"
            prof_dir.mkdir(parents=True)
            (prof_dir / "baseline-prof.txt").write_text(
                "Landroidx/activity/ComponentActivity;\n"
                "Lcom/example/androidcorebase/MainActivity;\n"
                "Lcom/example/androidcorebase/sample/demo/DemoFragment;\n"
                "Lcom/thanhng224/androidcorebase/core/CoreClass;\n",
                encoding="utf-8",
            )

            # Configure and validate
            config = InitConfig(
                root_dir=root,
                project_name="AcmeShop",
                app_name="Acme & Shop",
                app_package="com.acme.shop",
                core_package="com.acme.shop.core",
                scope="full",
                clean_samples=True,
                dry_run=False,
                skip_build_check=True,
                force=True,
            )

            # Run pre-flight checks
            validate_config(config)

            # Execute moves
            moves = get_source_moves(config)
            for src, dst in moves:
                if src.exists():
                    run_git_or_shutil_move(src, dst, root, dry_run=False)

            # Execute text replacements
            table = build_replacements_table(config)
            apply_text_replacements(root, table, dry_run=False)

            # Clean sample code
            clean_sample_code(config)

            # Assertions
            # 1. New application file exists, old does not
            new_app_file = root / "app" / "src" / "main" / "java" / "com" / "acme" / "shop" / "AcmeShopApplication.kt"
            self.assertTrue(new_app_file.exists())
            self.assertFalse((app_main / "AndroidCoreBaseApplication.kt").exists())

            # 2. XML string is correctly escaped and parseable
            new_strings = (res_val / "strings.xml").read_text(encoding="utf-8")
            self.assertIn('<string name="app_name">Acme &amp; Shop</string>', new_strings)
            tree = ET.fromstring(new_strings)
            app_name_elem = tree.find("./string[@name='app_name']")
            self.assertIsNotNone(app_name_elem)
            self.assertEqual(app_name_elem.text, "Acme & Shop")

            # 3. MainActivity updated cleanly
            new_main_act = root / "app" / "src" / "main" / "java" / "com" / "acme" / "shop" / "MainActivity.kt"
            main_text = new_main_act.read_text(encoding="utf-8")
            self.assertIn("package com.acme.shop", main_text)
            self.assertIn("AcmeShopApplication", main_text)
            self.assertIn("setOf(R.id.homeFragment, R.id.settingsFragment)", main_text)
            self.assertNotIn("demoFragment", main_text)

            # 4. CriticalJourney updated cleanly
            new_journey = root / "baselineprofile" / "src" / "main" / "java" / "com" / "acme" / "shop" / "baselineprofile" / "CriticalJourney.kt"
            journey_text = new_journey.read_text(encoding="utf-8")
            self.assertIn("package com.acme.shop.baselineprofile", journey_text)
            self.assertNotIn("demoFragment", journey_text)
            self.assertNotIn("Direction", journey_text)
            self.assertNotIn("GESTURE_MARGIN_DIVISOR", journey_text)

            # 5. Publication and consumer scripts updated
            verify_script = (scripts_dir / "verify-publication.sh").read_text(encoding="utf-8")
            self.assertIn("AcmeShop-*.pom", verify_script)
            self.assertNotIn("AndroidCoreBase-*.pom", verify_script)

            consumer_build = (consumer_dir / "build.gradle.kts").read_text(encoding="utf-8")
            self.assertIn("com.github.ThanhNg224:AcmeShop:$androidCoreBaseVersion", consumer_build)
            self.assertNotIn("AndroidCoreBase", consumer_build)

            # 6. Baseline Profile descriptors updated and sample classes pruned
            new_prof = (prof_dir / "baseline-prof.txt").read_text(encoding="utf-8")
            self.assertNotIn("Lcom/example/androidcorebase/", new_prof)
            self.assertNotIn("Lcom/thanhng224/androidcorebase/core/", new_prof)
            self.assertNotIn("/sample/", new_prof)
            self.assertIn("Lcom/acme/shop/MainActivity;", new_prof)
            self.assertIn("Lcom/acme/shop/core/CoreClass;", new_prof)


if __name__ == "__main__":
    unittest.main()

