# Add project specific ProGuard rules here.
# Release builds run with isMinifyEnabled/isShrinkResources = true (see app/build.gradle.kts),
# using this file plus the default Android/R8 optimize rules. Room (KSP-generated, no reflection)
# and Compose need no extra keep rules for this app; add any here if that ever changes.

# ── Gson / Backup serialization ───────────────────────────────────────────────
# Gson uses reflection to read and write field names. R8 renames fields in
# minified builds, which breaks JSON round-trips. Keep all backup data-model
# classes so their field names survive into the release APK.
-keep class com.mystudytracker.app.data.backup.BackupPayload { *; }
-keep class com.mystudytracker.app.data.backup.DailyProgressEntry { *; }
-keep class com.mystudytracker.app.data.backup.DailyTaskStateEntry { *; }
-keep class com.mystudytracker.app.data.backup.DailyAttachmentEntry { *; }

# Also keep Gson itself from being stripped/relocated
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
