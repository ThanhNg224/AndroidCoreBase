# Engineering Guidelines

An Android XML/ViewBinding application template with reusable `:core` and optional `:core:ui-compose` libraries. The app owns features and Hilt; see [Architecture](docs/ARCHITECTURE.md).

## Workflow

- Read only the owning documents relevant to the task, then inspect current implementation, callers, and existing tests.
- Make the smallest complete change. Add infrastructure or abstractions only for a concrete requirement or failure mode.
- Work in the current branch and checkout. Do not create a branch or worktree unless explicitly requested; preserve other contributors' edits.
- Ask when unresolved intent or a tradeoff changes the work. Continue independent work while waiting.
- Handle small and tightly coupled changes directly. Delegate only when independent tracks reduce total effort; use a reviewer for high-risk changes or when requested.
- Use the smallest level in [Verification](docs/VERIFICATION.md). Test behavior, state, persistence, security, and concurrency when affected; do not add tests that merely mirror layout or styling.
- Report exact commands and results. Keep local checks, archive checks, builds, remote CI, and device evidence distinct.
- Keep rules in their owning documents and link elsewhere. Preserve active plans and decision records; keep handoff plans local under `docs/plans/` and delete them when completed.
- Follow [Git workflow](docs/GIT_FLOW.md). Do not commit, push, tag, publish, or deploy unless explicitly requested.

## Documents

- [Architecture](docs/ARCHITECTURE.md): ownership, dependencies, and data flow.
- [Verification](docs/VERIFICATION.md): risk levels, commands, side effects, and proof boundaries.
- [Git workflow](docs/GIT_FLOW.md): existing branch, commit, and release conventions.
- [Standards](docs/STANDARD.md): coding, API, error, and security contracts.
- [Core modules](docs/CORE_MODULES.md): reusable inventory and integration.
- [Feature guide](docs/FEATURE_TEMPLATE.md): adding application features.

## Repository invariants

- Keep XML/ViewBinding as the application UI; Compose interop belongs in the optional library. Never bring app features or Hilt into reusable libraries.
- Keep domain contracts independent of data/presentation implementations and Android framework types. Promote shared code only after demonstrated reuse.
- Preserve public APIs and behavior unless a change is explicitly requested. Review API dump changes before updating baselines.
- Propagate cancellation; never use `GlobalScope`, log credentials, personal data, or payloads, or expose raw technical exceptions to users.
- Reference projects are read-only. Port generic concepts only; exclude product-specific behavior and legacy infrastructure.
