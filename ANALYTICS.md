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
3. ✅ Feature events + taps + user properties (test-first). **Done:** `trip_created` (in `NewTripViewModel`),
   `state_marked`/`state_unmarked` (in `StateDetailViewModel`, gated so `state_marked` only fires on
   a brand-new mark), `tab_selected` (in `ActiveTripViewModel`), `achievement_unlocked` (one per
   newly-earned id, in `ActiveTripViewModel`'s achievement collector), `setting_changed` (key + new
   value, on every `SettingsViewModel` toggle; note opt-outs of analytics itself are gated out by
   `ConsentGatedAnalytics`), and taps `tap_new_trip_fab` / `tap_manage_trip` / `tap_settings` (in
   `AppNavHost`). Each has a `FakeAnalytics` test (`NewTripAnalyticsTest`, `StateDetailAnalyticsTest`,
   `SettingsViewModelTest`, `ActiveTripViewModelTest`), plus `attribution_set` (in
   `StateDetailViewModel.onSaveAttribution`, `player_count`) and `share_completed` (in the
   `shareTripImage` util, which now takes an injectable `analytics`; the celebration/share screen
   passes the container's instance). Tests: `StateDetailAnalyticsTest`, `TripShareTest`. Also
   `onboarding_completed`/`onboarding_skipped` (in `OnboardingViewModel.finish()`, distinguished by
   the `step` the user exited on — Ready ⇒ completed, earlier ⇒ skipped; the `step` is the param).
   Tests: `OnboardingViewModelTest`. Also `reminder_action` (`end`/`remind` in
   `ReminderActionReceiver`, `extend` in `MainActivity`'s deep-link branch — both read
   `container.analytics`, since a `BroadcastReceiver`/`Activity` can't take constructor injection).
   The action→label mapping is the pure `TripReminders.actionLabel(...)`, covered by
   `TripRemindersTest`. **User properties** are done too: the four cohorts (`player_count_bucket`,
   `has_completed_trip`, `lifetime_states_bucket`, `theme_pref`) live in the pure `UserProperties`
   (bucketed counts only, no raw values) and are pushed once per launch by
   `LicensePlateQuestApp.syncAnalyticsUserProperties()` (consent-gated; theme_pref reflects the
   launch value). Tests: `UserPropertiesTest`. The `tap_share` tap is wired in `CelebrationScreen`'s
   share button `onClick` (a composable tap, like the `AppNavHost` taps — no unit test).
   **✅ The event catalog (§3) is now fully instrumented.** What's left is the provider + compliance
   (steps 5–6 below).
4. ✅ **Analytics** toggle in Settings (mirrors Sound/Vibration; default on; gates all events).
5. ✅ Firebase wired: `google-services.json` in `app/` (git-ignored), the `com.google.gms.google-services`
   plugin (4.4.4) + `firebase-bom` (34.14.0) / `firebase-analytics`, `FirebaseAnalyticsClient :
   Analytics` (params→Bundle via the testable `analyticsParamsToBundle`; degrades to no-op if no
   `FirebaseApp`), and `AppContainer` now sinks to it through the consent gate. The consent setting
   also drives `setAnalyticsCollectionEnabled` (via `AppContainer.applyAnalyticsConsent`, observed in
   `LicensePlateQuestApp`) so opt-out stops the SDK's automatic events too. Test:
   `FirebaseAnalyticsClientTest`. **Note:** `google-services.json` is git-ignored, so CI/fresh clones
   need it dropped into `app/` before the build will configure.
6. Write the **privacy policy**, complete the Play **Data Safety** form (declare: App activity /
   interactions; not linked to identity; used for analytics; Advertising ID only if not disabled).
7. **⚠️ Before production release — separate analytics environments.** Today *every* build (debug +
   release) reports to the single `license-plate-quest` Firebase project, so dev/QA events
   co-mingle with production data. We want lower envs reporting for testing, so before going live
   split them. Preferred: a debug build type / flavor with an `applicationIdSuffix` (e.g. `.debug`)
   + a separate Firebase app/project + its own `google-services.json` under `app/src/debug/` (the
   plugin merges per-variant; the suffixed package needs its own client entry or the plugin's config
   step fails). Lighter alternative: keep one project and stamp an `env`/`build_type` user property
   from `BuildConfig` (one-line via `UserProperties`) and filter in the console. The `Analytics`
   seam + `FirebaseAnalyticsClient` need no changes either way — this is purely build/config wiring.

---

## 7. Privacy / Play compliance checklist

- [ ] Privacy policy URL published and linked in Play Console.
- [ ] Data Safety form matches what Firebase actually collects (disable Ad ID collection if you
      don't need it — fewer disclosures).
- [ ] Settings toggle present and honored (the `ConsentGatedAnalytics` gate).
- [ ] No PII in any event params (audit the catalog before each release).
- [ ] Re-confirm audience is teen/adult in Play Console (this whole design assumes it).
- [ ] **Analytics environments separated** so lower-env/test events don't pollute production data
      (see §6.7) — required before the production release.
