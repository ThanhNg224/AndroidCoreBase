# Verification

Choose the smallest level covering the change. Escalate when scope or uncertainty requires it.

| Level | Change | Evidence |
|---|---|---|
| 0 | Documentation, agent rules, ignore patterns, attributes | Diff, affected links/claims, ignore and attribute probes; no build |
| 1 | Local implementation without public-contract/build changes | Focused formatting/static checks and relevant existing tests |
| 2 | State, public contract, build, or dependency changes | Project gates plus applicable API/changelog steps |
| 3 | Initializer, scaffolds, validation scripts, or CI | Tooling tests, archive smoke, and existing gates affected by the change |

A level is a scope decision, not a requirement to run every listed command. Do not add tests for styling alone or repeat broad gates without a new failure or source change.

## Commands

| Purpose | Command |
|---|---|
| Focused module gate | `./gradlew :<module>:check` (replace `<module>` with the affected Gradle project path) |
| Project gate | `./gradlew check` |
| App release build | `./gradlew :app:assembleRelease` |
| Format explicitly | `./gradlew ktlintFormat` (rewrites Kotlin files) |
| Python tooling tests | `python3 -B -m unittest discover -s scripts -p 'test_*.py' --verbose` |

For public library changes, review the API dump diff and run `./gradlew :core:apiCheck :core:ui-compose:apiCheck`. Publication and consumer instructions remain in [Core modules](CORE_MODULES.md); their existing gates and profiles are unchanged.

`check` retains Android lint, ktlint, detekt, and Kover. The existing deterministic-core coverage gate (`verifyDeterministicCoreCoverage`, an alias for `koverVerify`) requires 80%+ line coverage on the positively selected non-UI surface in `core/build.gradle.kts`.

## Archive initializer smoke

Use Python 3.12+ for archive tooling.

`python3 scripts/smoke_init_project.py` reads committed `HEAD` via `git archive` and checks initialization in temporary copies. Use `--repo <path> --revision <ref>` to check another committed input. Uncommitted edits are not archive proof; validate intended edits in an isolated snapshot repository before committing to the source repository.

One full-scope rename removes demo samples and retains the XML app shell. Dry-run content checks run where the initializer supports dry-run. No fake Gradle launcher is used.

Every PR runs this rename smoke without a native build. `python3 scripts/smoke_init_project.py --build` adds one real build/check in the temporary clone. It is also available through the existing CI workflow's manual `initializer_build` input, disabled by default. Existing CI jobs remain unchanged in name and coverage.

## Hygiene checks and proof boundaries

For level 0, run `git diff --check`, resolve changed document links, and probe relevant patterns with `git -c core.excludesFile=/dev/null check-ignore --no-index -v <path>` and `git check-attr text eol -- <path>`. Ignore probes may return no match for intentionally trackable templates.

A successful rename smoke proves the asserted identities and structure only. Build/check mode additionally proves the listed compiler/build tasks, not installation or runtime/device behavior. Local success does not prove remote CI or branch-protection enforcement. Report exact commands, results, and skipped gates with their reason.
