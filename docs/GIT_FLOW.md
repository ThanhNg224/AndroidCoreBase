# GIT_FLOW.md

## Purpose

This document defines the Git workflow and collaboration rules for the project. The goal is to keep history clean, reviews efficient, and changes easy to understand.

---

## Branch Strategy

> **Lưu ý về Repository Base Template (Giai đoạn Solo Maintainer):**
> - Đối với repo base template này, maintainer phát triển trực tiếp trên nhánh `main` để giữ quy trình tinh gọn.
> - Khi dự án được khởi tạo thành dự án thực tế qua `scripts/init_project.py`, dự án sẽ vận hành đầy đủ theo mô hình GitFlow dưới đây.

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
- Run the applicable risk level in [Verification](VERIFICATION.md).
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

## Library release recipe

`:core`'s version comes from `VERSION` (set by JitPack from the tag) falling back to
`VERSION_NAME` in `core/gradle.properties`. To release:

```bash
# 1. update CHANGELOG.md and bump VERSION_NAME in core/gradle.properties to match
# 2. verify the gate and that the published artifacts assemble
./gradlew check :core:assembleRelease :core:ui-compose:assembleRelease
./scripts/verify-publication.sh --release
# 3. tag with the same value and push
git tag v2.0.1 && git push origin v2.0.1
```

JitPack builds the tag using `jitpack.yml` (pinned to JDK 21). Semantic versioning applies to the
published modules' **public** API only — `internal` declarations are not part of the contract.

Test a candidate against a real consumer before tagging:

```bash
./gradlew :core:publishToMavenLocal :core:ui-compose:publishToMavenLocal
# then add mavenLocal() in the consumer project, or use scripts/verify-publication.sh directly
```

---
