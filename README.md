# AndroidCoreBase

An Android XML + ViewBinding application starter with DI-agnostic core libraries and optional Compose interop.

## Prerequisites

Use Python 3.12+, Android Studio with JDK 21, and Android SDK API 37. The checked-in Gradle wrapper/catalog select the build toolchain; minimum Android API remains 24.

## Initialize

Clone the template into a new app directory, then preview and initialize:

```bash
python3 scripts/init_project.py --project-name AcmeShop --app-name "Acme Shop" --package com.acme.shop --scope full --clean-samples --dry-run
python3 scripts/init_project.py --project-name AcmeShop --app-name "Acme Shop" --package com.acme.shop --scope full --clean-samples
```

The wizard remains available without arguments. `--scope app-only` keeps the reusable core identity, and omitting `--clean-samples` keeps demos. The default initializer formats, records renamed library API baselines, and runs its Gradle check; `--skip-build-check` explicitly skips those steps. See [Git workflow](docs/GIT_FLOW.md) for existing derived-project Gitflow behavior.

## Prepare

Open the initialized directory in Android Studio, select JDK 21, and sync Gradle. The app owns Hilt and feature composition; reusable core libraries own no app features or DI graph.

## Verify

Use [Verification](docs/VERIFICATION.md) for focused/project gates, Python tooling tests, archive rename smoke, and its optional clone build. Existing public-library, publication, and consumer requirements remain in [Core modules](docs/CORE_MODULES.md).

## Run

Run `:app` on an Android emulator or device. Regenerate a baseline profile after adding your own journeys. Add product features using [the feature guide](docs/FEATURE_TEMPLATE.md).

## Further reading

- [Agent workflow](AGENTS.md) and [Git workflow](docs/GIT_FLOW.md)
- [Architecture](docs/ARCHITECTURE.md) and [Standards](docs/STANDARD.md)
- [Core modules and host integration](docs/CORE_MODULES.md)
- [Design system](docs/DESIGN_SYSTEM.md) and [v1 migration](docs/MIGRATION_V1_TO_V2.md)

Clone the starter to build an app, or consume its published libraries from an existing app; the retained wiring and JitPack instructions are in Core modules. Release instructions remain in Git workflow. Licensed under [Apache 2.0](LICENSE).
