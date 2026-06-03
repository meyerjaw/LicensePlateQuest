# License Plate Quest — Project Handover

How to move this project to a new computer and pick up where you left off (including continuing with Claude Cowork). Last updated 2026-06-01.

---

## 1. What this project is

**License Plate Quest** (drawer name "LP Quest") is a native **Android** app — a family road-trip game for spotting US license plates across all 50 states. Built offline-first, single-device, no backend.

- **Canonical source of truth:** the Git repository (everything you need is in it).
- **Full product spec & change history:** `SPEC.md`
- **Future ideas / not-yet-done work:** `BACKLOG.md`
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

- **Android Studio** — latest stable version (must support **AGP 8.13** and **Android SDK Platform 36**).
- **Android SDK Platform 36** (install via Android Studio → SDK Manager). `minSdk = 31`, `targetSdk = 36`, `compileSdk = 36`.
- **JDK 17+** to run Gradle — Android Studio's bundled JDK (JBR) is fine; no separate install needed.
- **Git**.
- An **Android emulator** (API 31+) or a physical device with USB debugging for testing.

Build tooling versions (already pinned in the repo, listed for reference): Gradle **8.13**, AGP **8.13.2**, Kotlin **2.0.21**, Compose BOM **2024.12.01**.

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

That's it — no secrets, API keys, or signing config are required to build and run the debug app.

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
- `SPEC.md` — the complete spec **and** a dated change log (v1.0 → v1.3) describing every feature and decision.
- `BACKLOG.md` — what's intentionally not done yet.
- `HANDOVER.md` — this file.

A fresh Cowork session that reads those three files will have everything it needs to continue.

---

## 9. Project map (quick orientation)

```
app/src/main/
  java/com/getmecookies/licenseplatequest/
    di/AppContainer.kt              – manual dependency container
    data/                           – Room (entities, DAOs, AppDatabase), repositories, seeding
    domain/                         – domain models, CelebrationTracker, UiPreferences
    ui/
      navigation/                   – AppNavHost, Routes, TopDestination (bottom tabs)
      theme/                        – Color/Theme/Shapes (sunny palette, dynamic color OFF)
      screens/                      – trips, activetrip, statedetail, players, manageplayers, celebration
      components/                   – Confetti (firework), FlagImage
      map/                          – UsMap (vector US map, hit-testing, multi-color fills)
  res/values/strings.xml            – ALL user-facing text (app is i18n-ready)
  assets/flags/<code>.png           – 50 committed state flags
tools/fetch_flags.py                – flag downloader (already run; rarely needed)
SPEC.md · BACKLOG.md · HANDOVER.md  – docs
```

---

## 10. Known state / gotchas

- **App version:** `versionName 1.0`, `versionCode 1` (debug builds only so far; no release signing config set up yet).
- **Celebration sound is not implemented** (deferred — see `BACKLOG.md`); per-find feedback uses confetti + haptics.
- The DB column `plate_image_path` is **retained but unused** (state flags are derived from the state code) — left in place to avoid a Room migration. See `BACKLOG.md` for the eventual cleanup.
- No automated tests yet (also in the backlog).
- App data (trips/players) lives only on the device; it does **not** transfer with the source. Moving the *project* to a new computer does not move any *gameplay data* off a phone.

---

## 11. New-machine checklist

- [ ] Old machine: `git add -A && git commit && git push`, working tree clean
- [ ] New machine: Android Studio (latest) + SDK Platform 36 installed
- [ ] `git clone https://github.com/meyerjaw/LicensePlateQuest.git`
- [ ] Open in Android Studio, Gradle sync succeeds (creates `local.properties`)
- [ ] App builds and runs on emulator/device
- [ ] Launcher shows the "LP Quest" icon; app opens with the sunny theme
- [ ] (Optional) Claude desktop installed, Cowork connected to the cloned folder
