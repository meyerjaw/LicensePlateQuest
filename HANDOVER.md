# License Plate Quest — Project Handover

How to move this project to a new computer and pick up where you left off (including continuing with Claude Cowork). Last updated 2026-06-14.

---

## 1. What this project is

**License Plate Quest** (drawer name "LP Quest") is a native **Android** app — a family road-trip game for spotting US license plates across all 50 states. Built offline-first, single-device, no backend.

- **Canonical source of truth:** the Git repository (everything you need is in it).
- **Full product spec & change history:** `SPEC.md`
- **Future ideas / not-yet-done work:** `BACKLOG.md`
- **Analytics design, event catalog & provider setup:** `ANALYTICS.md`
- **This file:** `HANDOVER.md`

Tech stack: Kotlin · Jetpack Compose · Material 3 · Room (SQLite, KSP) · kotlinx-serialization · manual DI (`AppContainer`, no Hilt) · MVVM. Package / applicationId: `com.getmecookies.licenseplatequest`.

---

## 2. Where everything lives

| Thing | Location (old machine) | Travels via |
|---|---|---|
| **Android project (all code + assets)** | `C:\Users\meyer\AndroidStudioProjects\LicensePlateQuest` | **Git** (GitHub) — primary transfer method |
| GitHub remote | `https://github.com/meyerjaw/LicensePlateQuest.git` (branch `main`) | — |
| Original icon artwork | `…\OneDrive\Documents\Claude\Projects\License Plate Game App\app_icon.png` | OneDrive (optional — see §6) |

The generated launcher icons and all 50 **state flag PNGs** (`app/src/main/assets/flags/`) are **committed to Git**, so they come down with a normal clone. You do **not** need the OneDrive folder to build the app.

---

## 3. Before you leave the OLD computer

1. Commit and push everything so nothing is left behind:
   ```
   cd C:\Users\meyer\AndroidStudioProjects\LicensePlateQuest
   git add -A
   git commit -m "Handover: latest changes"
   git push
   ```
2. Confirm `git status` is clean and `git push` reports up to date.
   - (As of writing, the only uncommitted items were `SPEC.md`, `BACKLOG.md`, and a couple of icon XMLs — make sure they're pushed.)

> The repo is the transfer. You do **not** need to copy the project folder by hand — and you shouldn't, because some files are machine-specific (see §7).

---

## 4. Prerequisites on the NEW computer

- **Android Studio** — latest stable version (must support **AGP 9.2** and **Android SDK Platform 37**). AGP 9.2 needs **Android Studio Otter 3 Feature Drop (2025.2.3) or newer** — an older Studio fails the Gradle sync with an "incompatible AGP version" error.
- **Android SDK Platform 37** (install via Android Studio → SDK Manager). `minSdk = 31`, `targetSdk = 36`, `compileSdk = 37`.
- **JDK 17+** to run Gradle — Android Studio's bundled JDK (JBR) is fine; no separate install needed.
- **Git**.
- An **Android emulator** (API 31+) or a physical device with USB debugging for testing.

Build tooling versions (already pinned in `gradle/libs.versions.toml`, listed for reference): Gradle **9.5.1**, AGP **9.2.1**, Kotlin **2.2.10**, KSP **2.3.2**, Compose BOM **2026.05.01**, Room **2.8.4**. Firebase: google-services plugin **4.4.4** + Firebase BoM **34.14.0** (see §11). Note: **AGP 9 has a built-in Kotlin plugin** — do *not* add the `kotlin.android` plugin or Gradle fails with "Cannot add extension with name 'kotlin'"; apply the `google-services` plugin (it's fine alongside the built-in). Coroutines (1.10.2) and kotlinx-serialization (1.8.0) remain pinned to the older line (serialization 1.8.x targets Kotlin 2.1; bump cautiously).

---

## 5. Set up on the NEW computer

1. **Clone the repo:**
   ```
   git clone https://github.com/meyerjaw/LicensePlateQuest.git
   ```
2. **Open in Android Studio** → "Open" → select the cloned `LicensePlateQuest` folder.
3. Let **Gradle sync** run. On first sync Android Studio creates `local.properties` automatically pointing at the new machine's SDK (this file is git-ignored — don't copy it from the old machine).
4. If prompted, install any missing SDK components (Platform 36, build-tools).
5. **Build & run** on an emulator or device (Run ▶). You should see the "LP Quest" icon and the sunny-themed app.

That's it — no signing config is required to build and run the debug app.

> **One exception (Firebase):** `app/google-services.json` is **git-ignored**, so a fresh clone won't have it and the Gradle sync will fail at the google-services config step with "File google-services.json is missing." Copy it from your other machine into `app/` (it's Firebase *client* config — a project identifier, not a real secret; see §11), or download a fresh one from the Firebase console (project `license-plate-quest`, Android app `com.getmecookies.licenseplatequest`).

---

## 6. App icon (optional)

The launcher icon is already generated and committed (`mipmap-*/ic_launcher*`, adaptive XML in `mipmap-anydpi*`). You only need the original artwork if you want to **regenerate** it:

- Source image: `app_icon.png` in the OneDrive "License Plate Game App" folder (copy it over if you want it).
- It was processed (white background flood-filled to transparent) and exported to all densities. There's no committed script for this step; it was a one-off image edit.
- State flags can be re-fetched if ever needed via `tools/fetch_flags.py` (pure-Python, pulls public-domain flags from Wikimedia) — but they're already committed, so this is rarely necessary.

---

## 7. Do NOT copy these (machine-specific / regenerated)

These are git-ignored and will be recreated on the new machine — copying them over can break the build:

- `local.properties` (points to the local Android SDK path)
- `.gradle/`, `build/`, `app/build/` (build caches/outputs)
- `.idea/` (IDE settings — mostly ignored)

A normal `git clone` already excludes all of these.

---

## 8. Continuing with Claude Cowork on the new machine

The app was built collaboratively using **Claude Cowork** (Claude desktop). To keep going there:

1. Install the **Claude desktop app** on the new computer and sign in.
2. Open **Cowork** and **connect/mount the cloned project folder** (`…\LicensePlateQuest`). Optionally also connect the OneDrive "License Plate Game App" folder if you want the icon artwork handy.
3. Start a new session and point it at the repo.

**Important:** Cowork's local working memory/notes from the old machine do **not** transfer automatically. That's by design here — the durable context lives in the repo:
- `SPEC.md` — the complete spec **and** a dated change log (v1.0 → v1.23) describing every feature and decision.
- `BACKLOG.md` — what's intentionally not done yet.
- `HANDOVER.md` — this file.

A fresh Cowork session that reads those three files will have everything it needs to continue. (`SPEC.md` is at **v1.23** as of this writing; its change log runs v1.0 → v1.23.)

> **Working style:** new work is done **test-first (TDD)** — see `TESTING.md` for the test layers and the standing rule that every Room migration ships with a migration test.

---

## 9. Project map (quick orientation)

```
app/src/main/
  java/com/getmecookies/licenseplatequest/
    di/AppContainer.kt              – manual dependency container (incl. the analytics sink + consent)
    data/                           – Room (entities incl. trip_stop, DAOs, AppDatabase @ DB v6), repositories, seeding
      analytics/FirebaseAnalyticsClient.kt – Firebase impl of the Analytics seam (+ params→Bundle)
    domain/                         – domain models, CelebrationTracker, UiPreferences
      Analytics.kt                  – analytics seam (interface, NoOp, ConsentGated), UserProperties.kt (cohorts)
    notifications/                  – ReminderScheduler (WorkManager), ReminderWorker, ReminderActionReceiver (overdue-trip reminders)
    ui/
      navigation/                   – AppNavHost, Routes, TopDestination (bottom tabs)
      theme/                        – Color/Theme/Shapes (sunny palette, dynamic color OFF)
      screens/                      – trips (incl. New Trip + Manage trip), activetrip, statedetail, players, settings, celebration
      components/                   – Confetti, FlagImage, EmptyState, RegionPicker, SwipeToDeleteRow
      map/                          – UsMap (vector US map, hit-testing, themed multi-color fills, route overlay)
  res/values/strings.xml            – ALL user-facing text (app is i18n-ready)
  assets/flags/<code>.png           – 50 committed state flags
app/schemas/                        – exported Room schemas (used by migration tests)
app/src/test/                       – JVM unit tests (Robolectric + in-memory Room)
app/src/androidTest/                – instrumented tests (Compose UI + Room MigrationTestHelper)
tools/fetch_flags.py                – flag downloader (already run; rarely needed)
SPEC.md · BACKLOG.md · HANDOVER.md · TESTING.md · ANALYTICS.md  – docs
```

---

## 10. Known state / gotchas

- **App version:** `versionName 1.0`, `versionCode 1` (debug builds only so far; no release signing config set up yet).
- **Database is at v6** with explicit, sequential migrations and **no destructive fallback**; exported schemas (`2.json … 6.json`) live in `app/schemas/` and every migration has a `MigrationTestHelper` test. (Earlier addition: the `trip_stop` table for multi-leg trips.)
- **Automated tests exist** and new work is TDD: JVM unit tests (Robolectric + in-memory Room) in `app/src/test/`, instrumented Compose UI + migration tests in `app/src/androidTest/`. See `TESTING.md`. Run unit tests from Android Studio or `./gradlew testDebugUnitTest`.
- **Debug-only "Seed sample data"** lives in Settings → Developer (gated on `BuildConfig.DEBUG`, absent from release builds). One tap seeds a few players and a multi-stop trip with finds.
- **Celebration sound is not implemented** (deferred — see `BACKLOG.md`); per-find feedback uses confetti + haptics.
- The DB column `plate_image_path` is **retained but unused** (state flags are derived from the state code) — left in place to avoid a Room migration. See `BACKLOG.md` for the eventual cleanup.
- **Line-ending churn:** the working tree often shows the whole repo as "modified" due to CRLF/LF normalization; `git diff --ignore-all-space` reveals the real changes. Stage real files explicitly (or sort out `.gitattributes`/`core.autocrlf`) rather than a blanket `git add -A`.
- **Intermittent dev data loss:** on a physical device, Android Studio sometimes does an uninstall→reinstall (e.g. signature mismatch) that wipes app data. This is the IDE/device, **not** a migration bug (migrations are additive and verified). The debug seed above exists to recover quickly.
- App data (trips/players) lives only on the device; it does **not** transfer with the source. Moving the *project* to a new computer does not move any *gameplay data* off a phone.

---

## 11. Analytics & Firebase

Product analytics (usage, taps, feature funnels) was added in mid-2026. Full design, the event catalog, and provider setup live in **`ANALYTICS.md`** — read that first. The short version:

- **Audience:** the app is listed for **teen/adult** (not child-directed), so standard Firebase/Google Analytics is permitted with disclosure. Analytics defaults **on** with a Settings opt-out. If the audience ever changes to include children, revisit everything (COPPA/Families rules).
- **Architecture:** a provider-agnostic `Analytics` seam (`domain/Analytics.kt`) with `NoOpAnalytics`, a `ConsentGatedAnalytics` decorator (reads the opt-out **live**), and `FirebaseAnalyticsClient` (`data/analytics/`) as the real sink. ViewModels take `analytics` with a `NoOpAnalytics` default and get the real one from `AppContainer` via the factory; receivers/activities read `container.analytics`.
- **Consent:** the Settings "Anonymous usage data" toggle gates explicit events via `ConsentGatedAnalytics`, **and** drives Firebase `setAnalyticsCollectionEnabled` (via `AppContainer.applyAnalyticsConsent`, observed in `LicensePlateQuestApp`) so opting out also stops the SDK's automatic events.
- **No PII** ever leaves the device — events use counts, enums, region codes, and bucketed cohorts only. The whole catalog (§3 of ANALYTICS.md) is instrumented and test-first (`FakeAnalytics`).
- **Firebase config:** google-services plugin **4.4.4** + Firebase BoM **34.14.0** / `firebase-analytics`. `app/google-services.json` (project `license-plate-quest`) is **git-ignored** — see §5. `FirebaseAnalyticsClient` no-ops if no `FirebaseApp` is initialized (so JVM unit tests don't need it).
- **Verify events:** `adb shell setprop debug.firebase.analytics.app com.getmecookies.licenseplatequest` enables Firebase **DebugView** (live event stream) for QA builds.

**⚠️ Before a production release** (also tracked in ANALYTICS.md §6.7 / §7):
- **Separate analytics environments.** Today *every* build (debug + release) reports to the single `license-plate-quest` project, so dev/QA events co-mingle with production data. Split before launch — preferred: a debug build type / flavor with an `applicationIdSuffix` + its own Firebase app + `google-services.json` in `app/src/debug/`; lighter: one project tagged by an `env`/`build_type` user property. Seam/client need no changes.
- **Privacy policy** URL published + linked in Play Console, and the Play **Data Safety** form completed (App activity / interactions; not linked to identity; used for analytics). Consider removing the `com.google.android.gms.permission.AD_ID` permission (`tools:node="remove"`) if you don't need the Advertising ID — fewer disclosures.

---

## 12. New-machine checklist

- [ ] Old machine: `git add -A && git commit && git push`, working tree clean
- [ ] New machine: Android Studio **Otter 3 Feature Drop (2025.2.3)+** with SDK Platform 37 installed
- [ ] `git clone https://github.com/meyerjaw/LicensePlateQuest.git`
- [ ] Put `google-services.json` in `app/` (git-ignored — copy over or re-download; see §5/§11)
- [ ] Open in Android Studio, Gradle sync succeeds (creates `local.properties`)
- [ ] App builds and runs on emulator/device
- [ ] Launcher shows the "LP Quest" icon; app opens with the sunny theme
- [ ] (Optional) Claude desktop installed, Cowork connected to the cloned folder
