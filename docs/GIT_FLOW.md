# GIT_FLOW.md

## Purpose

This document defines the Git workflow and collaboration rules for the project. The goal is to keep history clean, reviews efficient, and changes easy to understand.

---

## Branch Strategy

Primary branches:

- main
- develop

Working branches:

- feature/<name>
- fix/<name>
- hotfix/<name>
- release/<version>

Examples:

- feature/login
- feature/payment-flow
- fix/crash-home
- hotfix/startup-crash

---

## Development Flow

1. Create a branch from the correct base branch.
2. Keep commits focused.
3. Sync with the latest changes regularly.
4. Resolve conflicts carefully.
5. Self-review before opening a Pull Request.
6. Merge only after review and approval.

---

## Commit Convention

Use conventional commit prefixes:

- feat
- fix
- refactor
- docs
- style
- test
- chore
- perf
- build

Examples:

- feat: add login validation
- fix: prevent duplicate requests
- refactor: simplify user mapper
- docs: update architecture guide

---

## Commit Rules

- One logical change per commit.
- Keep commits small.
- Write imperative commit messages.
- Avoid mixing unrelated changes.
- Never commit generated files unless required.
- Remove debug code before committing.

---

## Pull Request Guidelines

A Pull Request should:

- Solve one problem.
- Be easy to review.
- Avoid unrelated refactoring.
- Follow all engineering documents.
- Include a clear description when necessary.

---

## Before Opening a Pull Request

- Build passes.
- Run the full quality gate: `./gradlew check`.
- For published-library changes, run `./scripts/verify-publication.sh --quick`.
- No merge conflicts.
- Self-review completed.
- No duplicated logic.
- Architecture respected.
- Coding standards followed.
- No temporary logs or TODOs.

---

## Code Review Mindset

When reviewing code, prioritize:

1. Correctness
2. Architecture
3. Maintainability
4. Readability
5. Simplicity
6. Performance

Review code, not the developer.

---

## Merge Rules

Prefer:

- Small Pull Requests.
- Frequent integration.
- Clean commit history.

Avoid:

- Large feature branches.
- Long-lived branches.
- Mixing multiple features in one PR.

---

## Conflict Resolution

When resolving conflicts:

- Understand both changes.
- Preserve intended behavior.
- Follow current architecture.
- Prefer consistency over personal preference.
- Test affected functionality after resolving.

---

## Final Checklist

- [ ] Correct branch used.
- [ ] Commit messages follow convention.
- [ ] One logical change per commit.
- [ ] No debug code.
- [ ] No unnecessary files.
- [ ] Architecture respected.
- [ ] Coding standards followed.
- [ ] Self-review completed.
- [ ] Ready for review.
