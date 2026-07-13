---
name: MyStudyTracker native Android app
description: How to work on the koushik222004-dotcom/MyStudyTrackerRepository project connected into this workspace.
---

The user's real app (calendar/checklist/remark-attachments tracker) lives at `app/` in this
monorepo's git history — a native Kotlin + Jetpack Compose + Room Android app, unrelated to the
pnpm workspace artifacts (`artifacts/api-server`, `artifacts/mockup-sandbox`) that came with the
Replit scaffold. It was merged in via `git merge origin/main --allow-unrelated-histories`.

**Why:** There's no Android SDK/emulator/Gradle in this sandbox, so the app cannot be built,
previewed, or screenshotted here — Screenshot/WorkflowsRestart tools do not apply to it.

**How to apply:** Edit the Kotlin sources directly under `app/src/main/java/com/mystudytracker/app/`,
review carefully by reading (no compiler available to catch mistakes), then `git push origin main`.
The repo's `.github/workflows/build-apk.yml` builds a signed release APK on every push to `main`;
the user downloads it from the GitHub Actions tab. GitHub access uses a manually pasted
`GITHUB_TOKEN` secret wired into the `origin` remote URL (user declined the GitHub OAuth
connector), not the `git-remote` skill's `gitPush`/`gitPull` tools.
