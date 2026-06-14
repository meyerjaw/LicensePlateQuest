# Analytics & Usage Tracking

How License Plate Quest tracks usage, button taps, and feature usage — the design, the event
catalog, how to instrument, the provider, and the privacy obligations. Created 2026-06-13.

**Audience decision:** the app will be listed for **teen/adult** (not child-directed), so standard
Google Analytics / Firebase is permitted with disclosure. Analytics defaults **on**, with a Settings
opt-out. (If the audience ever changes to include children, revisit everything here — Google's
Families/COPPA rules restrict analytics SDKs, ban the Advertising ID, and require an approved-SDK +
consent.)

---

## 1. Architecture — the `Analytics` seam

A thin, provider-agnostic interface, mirroring the project's other seams (`ReminderScheduler`,
`CelebrationSounds`, `CityLocator`) so it's injectable and unit-testable.

- `domain/Analytics.kt`
  - `interface Analytics { screen(name, params); event(name, params); setUserProperty(name, value) }`
  - `object NoOpAnalytics` — drops everything (tests, debug, and until a provider is wired).
  - `class ConsentGatedAnalytics(delegate, isEnabled: () -> Boolean)` — forwards only while consent
    is on; reads the flag **live** so a Settings toggle takes effect immediately.
- `data/repository/SettingsRepository` — `analyticsEnabled: StateFlow<Boolean>` (default **true**) +
  `setAnalyticsEnabled(...)`, SharedPreferences-backed like the other toggles.
- `di/AppContainer` — exposes `analytics: Analytics = ConsentGatedAnalytics(NoOpAnalytics) { settingsRepository.analyticsEnabled.value }`.
  Swap `NoOpAnalytics` for the real client (below) and everything downstream keeps working.

ViewModels receive `analytics` via constructor (assert with `FakeAnalytics` in tests). Compose
screens read it from the container the way they read `soundPlayer`, or via a `LocalAnalytics`
composition local.

**Status (this increment, shipped 2026-06-13):** seam + `NoOpAnalytics` + `ConsentGatedAnalytics` +
`FakeAnalytics` + the `analyticsEnabled` setting + `AppContainer` wiring, all test-first
(`ConsentGatedAnalyticsTest`, `SettingsRepositoryTest`). No provider yet, so nothing leaves the
device. Remaining steps are §6.

---

## 2. Rules for event data

- Names `snake_case`, ≤ 40 chars; ≤ 25 params per event (Firebase limits).
- **Never log PII** — no player names, trip names, or city text. Use counts, enums, buckets, and
  2-letter region codes (a region code is not personal data).
- Prefer derived buckets over raw values (e.g. `player_count_bucket = "3-4"`).

---

## 3. Event catalog

### Screens (one wiring covers all — see §4)
`trip_list`, `active_trip`, `state_detail`, `new_trip`, `manage_trip`, `players`, `passport`,
`settings`, `celebration`, `onboarding`.

### Taps / interactions
| Event | Params |
|---|---|
| `tap_new_trip_fab` | — |
| `tap_trip_row` | `status` (active/in_progress/completed) |
| `tab_selected` | `tab` (map/list) |
| `tap_state` | `source` (map/list), `region` |
| `tap_overflow_menu` | — |
| `tap_manage_trip` | — |
| `tap_end_trip` | — |
| `tap_share` | — |
| `tap_settings` | — |
| `tap_achievement` | `achievement_id` |

### Feature usage / funnels
| Event | Params |
|---|---|
| `trip_created` | `player_count`, `stop_count`, `has_end_date` |
| `state_marked` | `region`, `rarity_bucket` (common/rare), `attributed_player_count`, `source` (map/list/detail) |
| `state_unmarked` | `region` |
| `trip_completed` | `states_found`, `duration_days` |
| `fifty_reached` | — |
| `attribution_set` | `player_count` |
| `share_completed` | — |
| `achievement_unlocked` | `achievement_id` |
| `onboarding_completed` / `onboarding_skipped` | `step` |
| `setting_changed` | `key`, `value` |
| `reminder_action` | `action` (end/extend/remind) |

### User properties (non-PII cohorts)
`player_count_bucket`, `has_completed_trip`, `lifetime_states_bucket`, `theme_pref`.

---

## 4. How to instrument

**Screen views — one place.** In `AppNavHost`, attach a destination-changed listener that logs a
`screen` event per navigation:

```kotlin
DisposableEffect(navController) {
    val l = NavController.OnDestinationChangedListener { _, dest, _ ->
        analytics.screen(dest.route ?: "unknown")
    }
    navController.addOnDestinationChangedListener(l)
    onDispose { navController.removeOnDestinationChangedListener(l) }
}
```
The Map/List top tabs aren't nav destinations — log those with `tab_selected` from `onTabSelected`.

**Feature events — in ViewModels** (most reliable + testable). Fire inside the handler that already
does the work, e.g. in the create-trip path:
```kotlin
analytics.event("trip_created", mapOf(
    "player_count" to playerIds.size,
    "stop_count" to stops.size,
    "has_end_date" to (endDate != null),
))
```
Test (red→green) with `FakeAnalytics`: assert the event + params fire, and that nothing fires when
consent is off.

**Taps — in the Compose `onClick`**, alongside the action:
```kotlin
FloatingActionButton(onClick = { analytics.event("tap_new_trip_fab"); onNewTrip() }) {
    /* FAB icon */
}
```

**Existing `EventLog` table:** the app already records domain events (`state_found`, `trip_started`,
…) locally. Option A: mirror those to `analytics` from the single `logEvent` call site (sanitize
payloads first). Option B: keep `EventLog` local-only and add a debug "Usage" screen reading it —
zero network, handy during the 12-tester phase.

---

## 5. Provider — Firebase (the teen/adult baseline)

Free, unlimited events, screen + custom events, funnels (Explorations), retention, DebugView, and
BigQuery export.

**Setup (requires a Firebase project — your action):**
1. Create a Firebase project, add an Android app with applicationId `com.getmecookies.licenseplatequest`.
2. Download `google-services.json` into `app/` (git-ignore it; it's project config, not a secret, but keep it out of public repos by preference).
3. Add the Google Services Gradle plugin + `com.google.firebase:firebase-analytics` (via the Firebase BoM).
   - Note the toolchain caveat already in `HANDOVER.md`: AGP 9 has a built-in Kotlin plugin — apply the google-services plugin, not anything that re-adds Kotlin.
4. Implement `data/analytics/FirebaseAnalyticsClient : Analytics` mapping `params` → `Bundle` and
   calling `FirebaseAnalytics.logEvent` / `setUserProperty`. On consent change also call
   `setAnalyticsCollectionEnabled(...)` so nothing is buffered while off.
5. In `AppContainer`, swap `NoOpAnalytics` → `FirebaseAnalyticsClient(context)`.

**Verify events:** `adb shell setprop debug.firebase.analytics.app com.getmecookies.licenseplatequest`
turns on Firebase **DebugView** (live event stream) for QA builds.

**Reading "what's used most":** the Events dashboard ranks event counts; Explorations build funnels
(open → trip_created → state_marked → fifty_reached) and retention.

**Alternative providers** (kept here for the record): **Aptabase** (open-source, privacy-first,
minimal), **PostHog** (funnels/paths/retention, self-host option), **Countly** (self-host). The seam
means switching providers is a one-file change.

---

## 6. Remaining build order

1. ✅ Seam + consent gate + setting + DI + tests (done 2026-06-13).
2. ✅ Auto screen tracking in `AppNavHost` (logs `screen` on route change, with the active_trip /
   trip_list split) plus `tab_selected` for the Map/List tabs inside the active trip.
3. ◑ Feature events + taps (test-first). **Done:** `trip_created` (in `NewTripViewModel`),
   `state_marked`/`state_unmarked` (in `StateDetailViewModel`, gated so `state_marked` only fires on
   a brand-new mark), `tab_selected` (in `ActiveTripViewModel`), and taps `tap_new_trip_fab` /
   `tap_manage_trip` / `tap_settings` (in `AppNavHost`). Each has a `FakeAnalytics` test
   (`NewTripAnalyticsTest`, `StateDetailAnalyticsTest`, `ActiveTripViewModelTest.onTabSelected_*`).
   **Remaining:** `attribution_set`, `share_completed`, `achievement_unlocked`,
   `onboarding_completed`/`_skipped`, `setting_changed`, `reminder_action`, and the user
   properties — all follow the same pattern (inject `analytics` with a `NoOpAnalytics` default so
   existing tests/constructors keep compiling; wire the real one in the factory; assert with
   `FakeAnalytics`).
4. ✅ **Analytics** toggle in Settings (mirrors Sound/Vibration; default on; gates all events).
5. Create the Firebase project, add `google-services.json` + deps, implement `FirebaseAnalyticsClient`,
   swap `NoOpAnalytics` → it in `AppContainer`.
6. Write the **privacy policy**, complete the Play **Data Safety** form (declare: App activity /
   interactions; not linked to identity; used for analytics; Advertising ID only if not disabled).

---

## 7. Privacy / Play compliance checklist

- [ ] Privacy policy URL published and linked in Play Console.
- [ ] Data Safety form matches what Firebase actually collects (disable Ad ID collection if you
      don't need it — fewer disclosures).
- [ ] Settings toggle present and honored (the `ConsentGatedAnalytics` gate).
- [ ] No PII in any event params (audit the catalog before each release).
- [ ] Re-confirm audience is teen/adult in Play Console (this whole design assumes it).
