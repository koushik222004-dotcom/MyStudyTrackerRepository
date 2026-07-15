# MyStudyTracker

A fully offline Android app for tracking daily NEET 2027 study progress.

- Kotlin, Jetpack Compose, Material 3, MVVM, Room database
- No ads, analytics, or background services. The only network activity is a single,
  user-initiated "sync" tap that reads the current date to keep tracking honest against
  clock tampering - everything else works fully offline
- Tracks 88 daily tasks across 9 sections (Pre-Lecture Revision, Lectures, Notes, DPP,
  Assignments, Post-Lecture Revision, Practice, NCERT Reading, Tests), including nested
  Physics/Chemistry/Botany/Zoology subject groups and a dedicated Tests section covering
  Partial and Full tests across all subjects
- Per-task quantity tracking (e.g. "3 DPP sheets"), a "Doesn't Apply" state for tasks that
  don't apply on a given day, and a backlog report that rolls up every outstanding task
  across your whole tracked history with drill-down to the specific dates it's pending
- Color-coded calendar for 29 June 2026 - 31 May 2027 (no future access)

## Getting the APK

Every push to `main` automatically builds a signed release APK via GitHub Actions:

1. Go to the **Actions** tab of this repository.
2. Open the latest successful **Build APK** run.
3. Download the **MyStudyTracker-release-apk** artifact (a zip containing the `.apk`).
4. Unzip it, transfer the `.apk` to your Android phone, and install it
   (you may need to allow "Install unknown apps" for the app you used to open the file).

No Android Studio, Gradle, or SDK setup required on your device.
