# License Plate Quest — MVP Specification

**Project:** License Plate Quest (launcher label "LP Quest")
**Platform:** Android (Native, Kotlin + Jetpack Compose)
**Document version:** 1.6
**Status:** MVP shipped; in active post-MVP iteration
**Development approach:** Test-Driven Development (TDD) — see §9 "Testing & development approach". New behavior starts with a failing test.

---

## 1. Vision and Goals

An Android app that lets families play the License Plate Game on road trips: spot license plates from other vehicles and track which U.S. states you've seen. The MVP focuses on a single-device experience where one phone is passed around or used by a passenger to log finds for the whole car.

The architecture is deliberately built to grow into a multi-game platform — slug bug, alphabet game, state trivia, and others — without rework.

### Success criteria for MVP

- A family can install the app, create players once, create a trip, and start playing within 60 seconds.
- Marking a found state takes no more than two taps.
- The app works fully offline.
- Trip data persists reliably across app restarts and device reboots.
- All 50 U.S. states are supported with full info screens.

---

## 2. Target Users

- **Primary:** Families on road trips, kids through adults.
- **Device user:** Typically a passenger (not the driver). Designed for one-handed use in a moving vehicle.
- **Age range:** App should be approachable for ages 7+ with light parental help. Reading-level appropriate state facts.

---

## 3. MVP Scope

### In scope

- Single-device Android app, fully offline-capable
- U.S. only, 50 states (no DC, territories, or other countries)
- Tappable SVG map of the United States with pinch-to-zoom and pan
- Persistent player roster with full CRUD
- Multiple trips supported, with exactly one "active" trip at a time
- Trip creation: name, an ordered list of **stops** (each city + state; minimum two — start and destination — with optional pit stops in between), start date, optional end date, selected players
- Manual trip end (no GPS-based auto-end)
- State detail screen with bundled state info (bird, motto, flower, state flag, fun fact)
- Mark a state immediately on the explicit "Mark as found" tap; unmark requires confirmation
- Per-state firework-style confetti celebration with haptic feedback (celebration sound asset deferred)
- 50/50 celebration with full stats screen (does not end trip)
- Manual-end celebration with stats screen
- Trip list with three sections: Active, In Progress, Completed
- Special visual treatment for completed (50/50) trips in the list
- Active Trip view with **Map** and **List** top tabs (selection remembered across sessions); the
  List tab is sortable (order found, alphabetical), searchable, with independent **Found** / *
  *Unfound** section filters (both on by default, remembered)
- Found states fill the map in a graph-colored vibrant palette (no two bordering states share a color), with 2-letter abbreviations on unfound states and check marks at each state's visual center
- A bottom stats strip under the map (found X/50, percent, last find, day of trip, found today)
- Delete trips; swipe-to-delete with an in-place 3-second undo on the Trip List and Player roster
- Manage an in-progress trip's players — add existing, add brand-new, and remove — from the Active Trip overflow menu
- **Per-player attribution:** each player has a chosen color; a find can be credited to one or more players; the summary shows a leaderboard ranking players (crown for the lead) plus a "Family Find" line for unattributed plates
- **Share a finished trip** as an image (filled map + stats with app watermark) via the system share sheet
- **Settings screen** (reached from a top-right icon): theme (light / dark / system), a vibration toggle, and a trip-reminders toggle
- **Default home location** that pre-fills the New Trip origin
- **Trip start + optional end date.** Trips can carry an optional end date (end ≥ start); the trip list shows the date range and flags **overdue** trips (past end, not yet ended)
- **Overdue-trip reminders:** local notifications (no server) fire ~1 day after the end date with a +3-day follow-up; the notification has **End trip / Remind later / Extend** action buttons, requests notification permission contextually, and is governed by the Settings toggle
- **Manage trip (edit):** edit a trip's name, dates, origin/destination, and players after creation (from the Active Trip overflow menu); player editing is folded in as a section (it replaces the standalone Manage Players screen). Saving a past end date prompts "End trip now, or keep it active?"
- **Shared region (state) picker:** a searchable bottom-sheet selector (search by name/abbreviation) used for the New Trip and Manage trip stops and the Settings home location, replacing the old dropdowns
- **Multi-leg trips (pit stops):** a trip is an ordered list of stops (first = start, last = destination); New Trip and Manage trip add/remove/reorder stops, and the active-trip map draws the route as a connecting line with numbered pins at each stop. Plate counting stays global to the trip (not per-leg)

### Explicitly out of scope (deferred to future phases)

- Accounts, cloud sync, multi-device, login
- Photo capture
- GPS permission flow and GPS-based features (trip auto-end, per-spotting location, distance-from-current-location)
- Other road trip games (slug bug, alphabet, padiddle, trivia, etc.)
- Canadian provinces, DC, U.S. territories — and the region picker's country-filter chips and per-row flags, which depend on that multi-country data
- Achievements and badges
- Editing spotting details after the fact beyond who's credited (note, photo, time — only unmark and editing attribution are supported)
- Per-stop arrival/departure dates and notes (stops carry only city + state for now); route pins sit at the stop **state's** center, not the actual city; the route on the summary/shared image; one-way vs round-trip handling
- Detailed read-only summary for completed trips beyond the celebration screens
- Per-player score tracking across trips (running tally beyond the per-trip leaderboard)
- Complex rarity calculations based on telemetry
- A sound mute toggle (deferred with the celebration sound itself)
- Push notifications
- Widgets

---

## 4. User Stories

### Players
- As a parent, I want to create player profiles once so I don't have to re-enter family names every trip.
- As a parent, I want to manage (add, edit, delete) player names from a dedicated screen.
- As a user, I want to quickly add a new player while creating a trip without leaving the trip creation flow.
- As a parent, I want to add or remove players on an in-progress trip directly from the active trip screen.

### Trips
- As a family, I want to create a new trip with a name, origin, destination, start date, and the players in the car.
- As a user, I want the trip name auto-filled with something sensible (e.g., "Springboro → Cincinnati, May 2026") so I can just tap accept.
- As a user, I want to see all my trips in one list, grouped by status.
- As a user, I want to switch between trips, with the most recently used one being active.
- As a user, I want to end a trip manually when I get home.
- As a user, I want to delete a trip I no longer want.

### Gameplay
- As a passenger, I want to tap a state on the map when I see its license plate.
- As a player, I want marking a state to be a single deliberate tap (the explicit "Mark as found" button is confirmation enough), while unmarking is confirmed so I don't undo progress by accident.
- As a player, I want to see fun information about each state (bird, motto, flower, fun fact, state flag).
- As a player, I want to unmark a state if I made a mistake.
- As a player, I want to see how many states we've found out of 50.
- As a player, I want to see a list of states we've already found, sorted by order or alphabetically.

### Celebrations
- As a family, we want a fun firework confetti celebration each time we find a new state.
- As a family, we want a big celebration with stats when we find all 50 states.
- As a family, we want a smaller celebration with stats when we manually end a trip.

---

## 5. Screen Inventory

| # | Screen | Purpose |
|---|--------|---------|
| 1 | Trip List (Home) | List of all trips, grouped into Active / In Progress / Completed. Entry point if no active trip. |
| 2 | Active Trip View | Standalone full-screen view (no bottom nav) with a Back button and two top tabs — **Map** and **List**. Trip name + overflow menu (Manage trip, End trip). Entry point if an active trip exists. |
| 3 | State Detail | Shown on map tap. Displays state info; "Mark as found" or "Unmark" depending on status. |
| 4 | New Trip Creation | Form: trip name (prefilled), origin city + state, destination city + state, start date, optional end date, players. |
| 5 | Players Management | Full CRUD for player roster, accessible from bottom nav. |
| 6 | 50/50 Celebration | Big celebration + stats screen when the 50th state is found. Trip continues afterward. |
| 7 | Manual-End Celebration | "You made it home!" celebration + stats screen when user manually ends a trip. |
| 8 | Manage Trip (edit) | Edit an existing trip's name, dates, origin/destination, and players (players as one section). Reached from the Active Trip overflow menu. Replaces the former standalone Manage Players screen. |

### Navigation

- Bottom navigation with three tabs: **Trips**, **Passport** (the lifetime cross-trip collection),
  and **Players**. The bottom nav is hidden while the Active Trip view is showing, so it reads as a
  standalone full-screen view.
- Trips tab default destination: Active Trip View if a trip is active, otherwise Trip List.
- Trip List → Active Trip View on selecting a trip (also makes that trip the active one).
- Active Trip View has two top tabs, **Map** and **List**; the chosen tab is remembered across sessions (restored on re-entry). State Detail opens by tapping a state on the Map tab or a row on the List tab.
- Active Trip View → Manage Players via the overflow menu.
- Active Trip View → 50/50 Celebration automatically when the 50th state is marked.
- Active Trip View → Manual-End Celebration → Trip List on ending a trip.
- Back behavior: from the Active Trip view, the top-bar Back button (and system Back) return to the Trip List; on the Players tab, Back returns to the Trips tab; on the Trip List (the app home), Back requires a second press within 3 seconds to exit (with a toast).

### Empty states

- **Trip List empty:** "No trips yet — tap + to start your first one."
- **Players empty:** "Add your first player to get started."

---

## 6. Detailed Screen Specs

### Trip List (Home)

- Sectioned list: Active (one trip max, pinned at top), In Progress, Completed.
- Each row: trip name, status indicator, X/50 progress, dates.
- Completed (50/50) trips have special visual treatment to stand out (e.g., gold border, star icon, or similar — to be designed).
- "+" FAB to create a new trip.
- Long-press or row swipe → delete (with confirmation).

### Active Trip View

A standalone, full-screen view (the app's bottom navigation is hidden while it's showing).

- **Top bar:** Back button (returns to the Trip List), trip name, and an overflow (⋯) menu with **Manage trip** (the full edit screen) and **End trip** (the latter with a confirmation dialog).
- **Top tabs:** **Map** and **List**. The selected tab is persisted (via `UiPreferences`) and restored the next time the user opens a trip.
- **Map tab:** Bundled vector U.S. map. Unfound states show outline only; found states are filled
  with a per-state color drawn from a vibrant palette (stable per state code), so the map fills in
  as a colorful mosaic; a newly found state animates its fill. Found states also carry a check
  mark (color-blind-safe cue). The fill sweep plays when a state is found, and a find made **off the
  map** (from the List tab or State Detail) plays its sweep on the **next map visit** (#20). On
  first run a one-time, dismissible hint tells new players to tap a state when they spot its plate (
  auto-retires on the first find). The trip's **route** is overlaid as a connecting line through the
  stops in order, with numbered pins at each stop's visual center (playtest #11). Pinch-to-zoom and
  pan supported. Tapping a state opens State Detail.
- **List tab:** Header with the persistent **X/50 counter** and sort chips ("Order found" — default,
  newest first — or "Alphabetical"); a **search box** (filter by name); independent **Found** / *
  *Unfound** section filters (both on by default, remembered across sessions; when a search matches
  a state in a switched-off section, a tappable hint offers to reveal it); then the scrolling list.
  Each row is a **collectible card** — the state flag, name, the spotted date (or "Not found yet"),
  a colored accent stripe in the state's map color, and a found check badge; unfound rows are
  dimmed.
  Tapping a row opens State Detail.

### State Detail

The state flag is shown framed at the top; state symbols (bird, flower, motto) and fun facts are grouped into titled section cards. The primary action is **pinned to the bottom** of the screen (above the system navigation bar) so it's always reachable without scrolling.

When tapping an **unfound** state:
- State name, bird, motto, flower, state flag, fun facts.
- Primary action: "Mark as found" — commits immediately on tap (the explicit button press is the confirmation) and returns to the map.
- If no trip is active, the pinned area shows "Start or select a trip to mark states." instead.

When tapping a **found** state:
- Same state info.
- Found timestamp, trip name where it was found.
- Primary action: "Unmark" — requires a confirmation dialog before removing the spotting.

### New Trip Creation

Fields:
- **Trip name:** Required. Prefilled with `{Origin City} → {Destination City}, {Month Year}` (updates live as origin/destination/date change).
- **Stops:** An ordered route — the first stop is the start, the last is the destination, with optional pit stops in between (playtest #11). Each stop is a city (text) + state (shared region picker, a searchable bottom sheet). Add stops, remove them (down to a minimum of two), and reorder via up/down controls. The auto-prefilled name reads "Start to … to Destination - Month Year".
- **Start date:** Date picker. Defaults to today.
- **End date:** Optional date picker (end ≥ start; moving the start past the end pushes the end forward). Setting one requests notification permission and schedules the overdue reminder.
- **Players:** Multi-select from roster. At least one required. "+ Add new player" quick-add inline.

Save creates the trip, makes it active, navigates to Active Trip View.

### Players Management

This is the **global roster** (Players tab):

- List of all players with name.
- "+" to add a new player (name only).
- Tap a player to edit name.
- Swipe or long-press to delete (with confirmation, warning if player is on any trips). Delete is a soft-delete.

### Manage Trip (edit)

A single full-screen editor for an existing trip, reached from the Active Trip overflow menu. It mirrors the New Trip form (same fields, validation, and region picker) but is prefilled from the trip and **commits on Save** (with an unsaved-changes warning if the user backs out). It replaces the former standalone Manage Players screen — player management is now one section of it.

- **Fields:** name, the ordered **stops** list (add/remove/reorder; same editor as New Trip), start date, optional end date, and players (multi-select chips + "+ Add new"). All edits are staged until **Save changes**.
- **Players** are reconciled on save (added/removed as a diff against the trip's current roster); removal only unlinks the player from this trip, never from the global roster.
- **Date edits** re-validate end ≥ start and reschedule (or cancel) the overdue reminder. Saving a **past end date** on a still-running trip prompts **"End date is in the past — end the trip now, or keep it active?"** ("End trip" finalizes it; "Keep active" saves it as overdue).

### 50/50 Celebration Screen

- Big firework confetti animation (celebration sound asset deferred to a later drop-in).
- Headline: "All 50! Congratulations [trip name]!"
- Stats:
  - Trip duration
  - Average time between state finds
  - Longest gap between finds
  - Shortest gap (rapid-fire moment)
  - First state found
  - Last state found
  - Estimated distance traveled (sum of straight-line distances between consecutive found states' centers, in found order; formatted with thousands separators, with an info button explaining the calculation)
  - Furthest state from origin (by state-center distance)
  - Rarest state found (by static rarity score)
  - Players on this trip
- "Continue" button returns to Active Trip View. Trip stays active.

### Manual-End Celebration Screen

- Firework confetti, "Made it home!" headline.
- Same stats as above (no 50/50 framing).
- "Done" returns to Trip List. Trip status moves to Completed.

### Trip Summary (read-only)

- The same stats screen is reused in a third "Summary" mode, opened from a completed trip in the Trip List. It shows the stats without confetti and without changing trip status.

---

## 7. Data Model

All entity IDs are UUIDs (java.util.UUID). All timestamps are stored as UTC ISO 8601.

### Tables

**Player**
- `id` (UUID, PK)
- `name` (text)
- `color` (text, nullable) — chosen palette color token (e.g. "teal"); null falls back to a stable per-id color
- `created_at` (timestamp)
- `updated_at` (timestamp)
- `deleted` (boolean) — soft-delete flag (preserves trip/attribution history)

**PlateRegion**
- `id` (UUID, PK)
- `country_code` (text, e.g., "US")
- `region_code` (text, e.g., "OH")
- `name` (text, e.g., "Ohio")
- `bird` (text)
- `motto` (text)
- `flower` (text)
- `fun_facts` (JSON array of strings)
- `plate_image_path` (text, asset path) — retained in the schema but **unused**; the UI now shows the **state flag**, whose asset path is derived from `region_code` as `flags/<code>.png` (no stored value needed)
- `rarity_score` (numeric)
- `center_lat` (numeric)
- `center_lng` (numeric)
- `display_order` (integer)
- `additional_info` (JSON object, reserved for future expansion — extra facts, historical plate variants, etc.)

**Trip**
- `id` (UUID, PK)
- `name` (text)
- `origin_city` (text)
- `origin_region_id` (UUID, FK → PlateRegion)
- `destination_city` (text)
- `destination_region_id` (UUID, FK → PlateRegion)
- `start_date` (date)
- `end_date` (date, nullable) — optional planned end; drives the trip-list date range, the derived "overdue" flag, and the reminder schedule (added v1.5, DB v3)
- `status` (enum: `active`, `in_progress`, `completed`)
- `ended_at` (timestamp, nullable)
- `created_at` (timestamp)
- `updated_at` (timestamp)

**TripPlayer** (junction)
- `id` (UUID, PK)
- `trip_id` (UUID, FK → Trip)
- `player_id` (UUID, FK → Player)
- `joined_at` (timestamp) — supports "add player mid-trip"

**TripStop** (ordered route — added v1.6, DB v4)
- `id` (UUID, PK)
- `trip_id` (UUID, FK → Trip, cascade delete)
- `position` (integer) — 0-based order; first = start, last = destination
- `region_id` (UUID, FK → PlateRegion)
- `city` (text)
- The canonical route for a trip (playtest #11). The legacy `Trip.origin_*`/`destination_*` columns are kept in sync with the first and last stops so existing stats (furthest-from-origin) and the name prefill keep working unchanged. The v3→v4 migration seeds two stops per existing trip from its origin/destination.

**GameType**
- `id` (UUID, PK)
- `code` (text, e.g., `license_plate`)
- `name` (text)
- `description` (text)

**GameInstance**
- `id` (UUID, PK)
- `trip_id` (UUID, FK → Trip)
- `game_type_id` (UUID, FK → GameType)
- `created_at` (timestamp)

> In MVP, every new trip automatically creates one GameInstance with `game_type = license_plate`. The user never sees this — it's an implementation detail that enables future multi-game support without refactor.

**Spotting**
- `id` (UUID, PK)
- `game_instance_id` (UUID, FK → GameInstance)
- `plate_region_id` (UUID, FK → PlateRegion)
- `spotter_player_id` (UUID, FK → Player, nullable) — retained but unused; per-player attribution now lives in the **SpottingPlayer** junction (a find can credit multiple players)
- `timestamp` (timestamp)
- `note` (text, nullable)
- `photo_path` (text, nullable) — reserved for future photo capture
- `gps_lat` (numeric, nullable) — reserved for future
- `gps_lng` (numeric, nullable) — reserved for future
- `created_at` (timestamp)
- `celebrated_at` (timestamp, nullable — added v1.7, DB v5) — null until the find's map fill
  animation has played; lets off-map finds defer their animation to the next map visit (#20)

**SpottingPlayer** (junction — per-player attribution, added v1.4)
- `id` (UUID, PK)
- `spotting_id` (UUID, FK → Spotting, cascade delete)
- `player_id` (UUID, FK → Player)
- Credits a find to one or more players. Unique on (`spotting_id`, `player_id`); cleared/rewritten when attribution is edited, and removed via cascade when a spotting is unmarked.

**EventLog**
- `id` (UUID, PK)
- `event_type` (text, e.g., `state_found`, `trip_started`, `trip_ended`, `player_added`)
- `payload` (JSON)
- `timestamp` (timestamp)

> Every meaningful action is logged here so future achievements/badges can be computed retroactively.

### Key invariants

- At most one Trip has `status = active` at any time.
- Setting a trip to active automatically demotes the previous active trip to `in_progress`.
- A trip's status moves to `completed` only on manual end (not on reaching 50/50).
- Deleting a player (global roster) is a soft-delete, but their `TripPlayer` rows remain so trip history isn't broken.
- Removing a player from a trip (Manage Trip Players) deletes only that `TripPlayer` link; the player stays in the roster. Players can be added to a trip mid-trip at any time.

---

## 8. State Info Data (Bundled)

State info ships bundled with the app as a JSON file in assets, loaded into the `PlateRegion` table on first run.

### Per-state fields

- `country_code` ("US")
- `region_code` ("OH")
- `name` ("Ohio")
- `bird` ("Northern Cardinal")
- `motto` ("With God, all things are possible")
- `flower` ("Scarlet Carnation")
- `fun_facts` (array of 3-5 short fun facts as strings)
- `plate_image_path` — deprecated/unused (the app shows **state flags**, bundled at `assets/flags/<code>.png` and resolved from `region_code`)
- `rarity_score` (numeric, 0.0–1.0, hand-tuned)
- `center_lat`, `center_lng` (state geographic center)
- `display_order` (numeric)
- `additional_info` (JSON object, reserved — leave as `{}` for now)

### Rarity score guidance

Hand-tuned for MVP. Approximate buckets:
- High rarity (0.8–1.0): Hawaii, Alaska
- Medium-high (0.6–0.8): Distant or low-population states (HI/AK aside) like North Dakota, Wyoming, Vermont, Rhode Island
- Medium (0.4–0.6): States distant from typical road trip routes
- Low (0.0–0.4): Common, high-population, central states

Future phases can replace this with real telemetry from spotting data.

### Versioning

The bundled JSON has a `version` field. On app start, compare bundled version to stored version and apply updates. This enables future content updates (more facts, better images) without forcing schema migrations.

---

## 9. Technical Stack and Architecture

### Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose, Material 3
- **Persistence:** Room (SQLite) with migration framework set up from v1
- **Map:** Bundled vector U.S. map — state path data (geoAlbersUsa boundaries) parsed into `android.graphics.Path` for drawing and `Region` for point-in-polygon hit-testing (handles multi-polygon states). Pan/zoom implemented with custom Compose gesture modifiers (no external library). Found states fill from a per-state color palette.
- **Confetti:** Custom, dependency-free pure-Compose `Canvas` firework effect (no external library).
- **Sound:** Deferred — celebration sound not yet implemented (a `SoundPool` drop-in is planned). Per-find feedback currently uses haptics.
- **Flags:** State flags bundled as PNGs in `assets/flags/<code>.png` (public-domain Wikimedia sources), displayed at true aspect ratio. Fetched via `tools/fetch_flags.py`.
- **IDs:** `java.util.UUID` for all entity primary keys.
- **Strings:** All user-facing text lives in `res/values/strings.xml` and is read via `stringResource` (or `Context.getString` in non-composable spots), ready for localization. ViewModels never hold display strings — validation surfaces a typed error (e.g. `PlayerNameError`) that the screen resolves to a resource.
- **No backend.** All data local.

### Architecture

- MVVM with Compose. ViewModels expose UI state as `StateFlow`.
- Repository layer between ViewModels and Room DAOs.
- Use cases for non-trivial business logic (start trip, mark state, compute celebration stats).
- Strict separation of bundled static data (state info, state flags) and user-generated data (trips, spottings).
- Manual dependency injection via an `AppContainer` (no DI framework in MVP); ViewModels built by a `viewModelFactory`.
- **Notifications/scheduling:** overdue-trip reminders are scheduled with **WorkManager** (per-trip unique work; survives reboot/process death). The scheduler is behind a `ReminderScheduler` **interface** so it can be faked in tests; action buttons route through a `BroadcastReceiver`.

### Testing & development approach

**This project is built test-first (TDD).** New behavior begins with a failing test; we implement the minimum to pass, then refactor with the suite as a safety net. Treat this as a default expectation for all future work, not an afterthought.

- **Layers & where tests live** (full detail in `TESTING.md`):
  - **Pure domain logic** → plain JVM unit tests (no Android), e.g. derived `UiState`/model properties.
  - **Repositories & ViewModels** → JVM unit tests under **Robolectric** with an in-memory Room database (fast loop, no emulator; run via `./gradlew testDebugUnitTest`). The emulated SDK is pinned (see `robolectric.properties`) so tests stay on the project JDK.
  - **Compose UI** → instrumented tests in `androidTest/` (run on a device/emulator).
  - **Database migrations** → instrumented `MigrationTestHelper` tests (`androidTest/`) against the exported schemas. **Every schema change must ship with both a `Migration` and a migration test** that seeds data at the old version, runs the migration, and asserts the data survived (`runMigrationsAndValidate` also catches DDL drift). This is non-negotiable — it's the guard that a migration never silently loses user trip data.
- **Testability seams:** when production code needs an Android/system dependency that's awkward in tests (WorkManager, etc.), extract a small **interface** and provide a fake (e.g. `ReminderScheduler` → `FakeReminderScheduler`). Prefer pushing logic into pure/derived properties so it's testable without Robolectric.
- **Coverage baseline:** the trip lifecycle (one-active-trip invariant, end/delete, update + player diff), spotting/attribution, the once-per-trip 50/50 rule (tracker + ViewModel wiring), players, settings, the edit flow, plus initial Compose UI tests are all under test. Keep this green; extend it as features land.
- **Not unit-testable here:** notification *posting* and action buttons are device-verified (the scheduling/dedup logic around them is unit-tested via the interface seam).

### Forward-compatibility checklist

The following are baked into MVP to avoid rework later:

- ✅ UUIDs everywhere (no auto-increment) — prevents sync collisions when cloud sync is added.
- ✅ Generic `PlateRegion` table with `country_code` + `region_code` — adding DC, Puerto Rico, Canadian provinces is just new rows.
- ✅ `Trip → GameInstance → Spotting` hierarchy — adding new game types is just new `GameType` rows and game-specific UI; no trip refactor.
- ✅ `Spotting.spotter_player_id` reserved as nullable — populated later for per-player attribution.
- ✅ `Spotting.photo_path`, `gps_lat`, `gps_lng` reserved as nullable — populated when those features ship.
- ✅ `PlateRegion.additional_info` JSON field — schema-free space for future per-state attributes.
- ✅ `EventLog` table — captures all meaningful events so future achievements can compute over history.
- ✅ JSON-serializable model — clean export/sharing later.
- ✅ Bundled data versioning — content updates without schema changes.

---

## 10. Validation and Edge Cases

- New trip requires: name (auto-prefilled), origin city + state, destination city + state, start date (defaults today), at least one player. End date is optional but, if set, must be ≥ start (moving start past it pushes it forward).
- A trip is **overdue** when it has an end date in the past and isn't completed (derived, not a stored status). Editing a trip to a past end date prompts "end now, or keep active?".
- Marking a state commits immediately on the explicit "Mark as found" tap (no extra dialog); unmarking requires a confirmation dialog.
- Ending a trip requires confirmation dialog.
- Deleting a trip requires confirmation dialog.
- Deleting a player who is on any trips: warn user but allow (soft-delete or keep on TripPlayer rows for history).
- If app is force-closed mid-trip, the active trip resumes on next open.
- If user marks the 50th state, then later unmarks one, the trip remains in active state (not "uncompleted") and they can keep playing. The 50/50 celebration does not re-trigger if they re-find it; consider this carefully in implementation.

---

## 11. Milestone Breakdown

Suggested build order. Each milestone is independently testable.

1. **Foundation:** Project setup, Room database, navigation skeleton, bundled state data loading, UUID infrastructure.
2. **Player management:** Players tab, CRUD screens, persistence.
3. **Trip creation:** New trip form with validation, prefill logic, "active trip" enforcement.
4. **Trip list:** Sectioned list (Active / In Progress / Completed), delete, navigation.
5. **Map rendering:** Bundled vector U.S. map, tap detection per state, pinch-zoom and pan, multi-color fill for found states.
6. **State detail and marking:** State detail screen, immediate mark / confirmed unmark, bundled state info display, pinned primary action.
7. **Active trip view:** Standalone full-screen view with Back button and Map/List top tabs (persisted); List tab has counter, search, sort, and show-unfound toggle.
8. **Celebrations:** Per-state firework confetti (sound deferred), 50/50 celebration screen with stats, manual-end celebration screen.
9. **Polish:** Empty states, validation messages, edge cases, animation polish.
10. **Pre-launch:** Internal testing, accessibility pass, Play Store assets and listing.

---

## 12. Open Questions Deferred to Build Phase

Several of these were resolved during build (noted inline):

- ~~Specific confetti library and exact animation timing.~~ **Resolved:** custom pure-Compose firework, ~1.5s, multiple staggered bursts.
- Specific celebration sound (length, character — playful but not jarring). **Still open** — sound is deferred.
- ~~Visual design system: exact found-state fill color.~~ **Resolved:** per-state palette of several vibrant colors.
- ~~Map projection and SVG source.~~ **Resolved:** bundled geoAlbersUsa vector path data, parsed to Path/Region.
- Exact "completed trip" visual treatment in the trip list (still as designed in §6).
- ~~Whether 50/50 celebration re-triggers if user unmarks and re-marks the 50th state.~~ **Resolved:** fires once per trip (tracked by `CelebrationTracker`).
- ~~Accessibility: color-blind-safe found-state styling.~~ **Resolved:** found states carry a check mark in addition to color.
- ~~Region selector UX (clunky state dropdowns).~~ **Resolved:** a shared searchable bottom-sheet picker; country-filter/flags wait on multi-country data.

---

## 13. Document Change Log

- **v1.0** — Initial MVP spec compiled from planning conversation.
- **v1.1 (2026-06-01)** — Synced spec to the as-built app after a round of post-MVP polish:
  - State **flags** replace license-plate images everywhere; `plate_image_path` retained but unused (flag path derived from `region_code` → `flags/<code>.png`).
  - **Active Trip View** is now full-screen (bottom nav hidden); found-states sheet gained a **search box**, a **"show unfound states"** toggle, and a collapsed pull-up-handle-only peek.
  - Map found-state fill is now a **multi-color per-state palette** (was a single color).
  - Per-state celebration is a **custom pure-Compose firework** (was planned `konfetti`); **celebration sound deferred** (haptics used for now).
  - **State Detail**: primary action **pinned to the bottom**; marking commits **immediately** on tap, only unmarking is confirmed (was: both confirmed).
  - New **Manage Trip Players** screen (Active Trip overflow menu): add existing roster members, create-and-add new players, and remove players from the trip. "Removing a player from a trip after it starts" moved from out-of-scope to in-scope.
  - **Back navigation** clarified: map→Trip List, Players tab→Trips tab, Trip List double-back-to-exit.
  - **Branding**: custom app icon (white background stripped to transparent) and a short launcher label **"LP Quest"** while the full app name stays "License Plate Quest".
  - Architecture notes: manual DI via `AppContainer`; map uses bundled vector path data with `Path`/`Region` hit-testing and custom gesture pan/zoom.
  - Resolved several §12 build-phase open questions (see inline strikethroughs).
- **v1.2 (2026-06-01)** — Active Trip View reworked into a **standalone tabbed screen**:
  - The bottom-sheet found-states list was **removed**; its content now lives in a dedicated **List** top tab, alongside a **Map** top tab.
  - The top-left "all trips" icon was replaced with a **Back** button (returns to the Trip List).
  - The selected tab (Map vs List) is **remembered across sessions** via a new `UiPreferences` (SharedPreferences) store, restored when re-entering a trip.
- **v1.3 (2026-06-01)** — Visual refresh + full string externalization:
  - New **"sunny road-trip" theme** (sky blue / grass green / sunny orange / coral on warm-cream neutrals), light and dark; Material You **dynamic color disabled** so the brand palette is consistent; rounder corner shapes; warm launch window background.
  - **All user-facing strings externalized** to `res/values/strings.xml` and read via `stringResource` / `Context.getString` (i18n-ready). ViewModel validation now exposes a typed `PlayerNameError` (BLANK / DUPLICATE) that screens resolve to resources; no display strings remain in ViewModels.
- **v1.4 (2026-06-04)** — Large feature drop from a round of play-testing (items tagged with their playtest-note number where applicable):
  - **Per-player attribution (flagship).** Players now have a **color** (curated palette, chosen on add/edit, shown as roster dots and colored chips). A find can be credited to **one or more players** via a multi-select on State Detail (shown for 2+ player trips; a solo trip auto-credits its one player; edits are committed via a top-bar ✓, with a discard-changes warning on back). The summary gained a **leaderboard**: each player's credited-plate count, sorted, with a 👑 crown for the (possibly tied) lead, plus a **"Family Find"** line for unattributed plates. *Schema: added `Player.color` and a new `SpottingPlayer` junction; DB bumped to **v2** with the first Room migration.*
  - **Settings screen** (reached from a top-right gear on the Trip List and Players top bars): **theme** (Light / Dark / System, applied live) and a **vibration** toggle (gates the per-find haptic), backed by a reactive `SettingsRepository` (SharedPreferences). Moved from out-of-scope to in-scope (sound toggle still deferred with the celebration sound).
  - **Default home location** (#8): set a home city + state in Settings; the New Trip "From" field pre-fills from it (a suggestion — editable, and clearing doesn't re-populate).
  - **Share a finished trip** (#4): exports a long-screenshot image of the summary (filled map + stats) with an app-icon + name + date watermark, via the system share sheet (FileProvider). Moved from out-of-scope to in-scope.
  - **Filled summary map** (#3): the colorful filled-in US map is now the hero on the celebration/summary screen (and in the shared image), via a non-interactive map mode.
  - **Map polish:** marks/labels sit at each state's **visual center** (pole of inaccessibility, #5); **2-letter abbreviations** show on unfound states (#10); found-state colors are **graph-colored so no two bordering states share a color** while keeping the vibrant mosaic (#6).
  - **Map stats strip** (#21): a tight row of at-a-glance cards under the map (found X/50, percent, last find + how long ago, day of trip, found today).
  - **Quick wins:** an **X/50 counter** on the map tab (#2); **Map/List tab icons** (#23); the list **search clears after a find** (#1); persistent **clear (✕)** controls on the New Trip From/To and trip-name fields (#9); **swipe-to-delete with an in-place 3-second undo** on both the Trip List (#15) and Player roster (#16) via a shared component.
  - **New Trip form:** From/To moved above the trip-name field; the auto-filled name now reads `<From City> to <Dest City>, <ST> - <Month Year>` (includes the destination state; dash before the date) and stays open as `<From City> to ` until a destination is filled.
  - **Bug fixes:** ending a trip now **persists on confirm** (survives app close before the summary's Done; `endTrip` made idempotent); trip **duration measures from the actual "Start trip" tap** (`createdAt`) rather than the start date's midnight; fixed a one-frame **bottom-nav flash** when leaving State Detail.
  - **Still deferred:** trip end dates (#12) and overdue-trip reminders (#13); the full-screen multi-country region selector (#7); multi-leg pit stops (#11); the celebration sound.
- **v1.5 (2026-06-05)** — Post-MVP feature drop + a shift to test-driven development:
  - **Trip end dates (#12):** trips carry an optional end date (end ≥ start). The trip list shows the date range and an **Overdue** badge (past end, not completed — a derived flag, not a stored status). *Schema: `Trip.end_date`; DB bumped to **v3**.*
  - **Overdue-trip reminders (#13):** local notifications via **WorkManager**, fired ~1 day after the end date with a **+3-day follow-up**, one-nudge-per-end-date dedup, and a **"Trip reminders" Settings toggle**. `POST_NOTIFICATIONS` is requested contextually the first time an end date is set. The notification has **End trip / Remind later / Extend** action buttons (a `BroadcastReceiver` handles End/Remind; Extend deep-links into Manage trip); tapping the body opens the trip. The `ReminderScheduler` is an interface (impl `WorkManagerReminderScheduler`) so it's fakeable in tests.
  - **Manage trip / edit trip (#14):** a single edit screen (from the Active Trip overflow) for name, dates, origin/destination, and players, prefilled and **committed on Save** with an unsaved-changes warning. It **replaces the standalone Manage Players screen** (player editing is now a section, reconciled as a diff on save). Saving a **past end date** prompts "end now, or keep active?". Date edits reschedule/cancel the reminder.
  - **Shared region picker (#7):** the clunky origin/destination **State dropdowns** are replaced by a reusable searchable **bottom-sheet** picker (search by name/abbreviation, exclude-the-other-endpoint), now used on New Trip, Manage trip, and the Settings home dialog. Country-filter chips, per-row flags, and a recently-used section are deferred until multi-country data exists.
  - **Toolchain upgrade (via the IDE):** AGP 9.1, Gradle 9.5, Kotlin 2.1, Compose BOM 2026.05, and other AndroidX bumps; Java 17 toolchain. Kotlin-coupled libraries (coroutines 1.10.2, serialization 1.8.0) pinned to the Kotlin 2.1 line.
  - **Testing & TDD (new discipline):** introduced a **Robolectric**-based JVM test suite plus initial **Compose UI** tests, and adopted **test-first development** going forward (see the new §9 "Testing & development approach" and `TESTING.md`). Baseline coverage spans the trip lifecycle invariants, spotting/attribution, the 50/50-once rule, players, settings, and the edit flow. Extracted the `ReminderScheduler` interface seam to make trip logic testable without WorkManager.
  - **Still deferred:** celebration sound; pre-permission priming for notifications; multi-leg pit stops (#11); one-way trips (#22); deferred-celebration animation queue (#20); broader polish (empty states, completed-trip styling, onboarding); expanding beyond the US 50.
- **v1.6 (2026-06-08)** — Multi-leg trips + a migration-test safety net:
  - **Pit stops / multi-leg trips (#11):** a trip is now an ordered list of **stops** (first = start, last = destination, optional pit stops between). New Trip and Manage trip add/remove/reorder stops (up/down); the name auto-prefill spans N stops. The active-trip **map draws the route** as a connecting line with numbered pins at each stop's visual center. Plate counting stays global to the trip. *Schema: new `trip_stop` table; DB bumped to **v4**. The repository writes ordered stops and keeps the legacy `origin_*`/`destination_*` columns in sync with the first/last stop, so existing stats and name prefill are untouched. Built test-first across the data, repository, and ViewModel layers.*
  - **Database migration tests (new discipline):** added `androidx.room:room-testing` and instrumented `MigrationTestHelper` tests (exported schemas wired into androidTest assets). They prove the v3→v4 migration preserves trip data and validate the full 1→2→3→4 chain against the schemas. **Going forward every migration ships with a test** (see §9). This was prompted by a dev-environment data wipe that an automated migration test would have caught.
  - **Still deferred:** per-stop arrival/departure dates + notes; route on the summary/shared image; **actual city pins** for the route (pins currently sit at the stop state's center, not the city — see backlog); one-way trips (#22); celebration sound; pre-permission priming.
- **v1.7 (2026-06-08)** — UX & polish round:
  - **Deferred find animation (#20):** a find marked **off the map** (from the list or State Detail)
    no longer misses its fill sweep — it's queued and plays on the **next map visit**. Backed by a
    new nullable `Spotting.celebrated_at`; the map animates the pending finds, then stamps them
    celebrated (so they never replay), and on-map finds animate immediately through the same path.
    *Schema: `spotting.celebrated_at`; DB bumped to **v5** (with a v4→v5 migration test).*
  - **Four-color base map (#6):** the **unfound** base is now a subtle 4-colored mosaic (a gentle
    tint over the themed neutral) instead of flat gray — no two neighbors share a tint.
    Color/adjacency data moved to a pure, unit-tested `StateColorData` (a test proves both the found
    mosaic and the base are conflict-free).
  - **Empty-state illustration:** the Trips and Players empty screens show a friendly camper-van
    illustration instead of a plain icon.
  - **Tap targets:** the map's `hitTest` gains a small nearest-anchor tolerance so tiny northeastern
    states are easier to tap (AK/HI placement verified — no change needed).
  - **Still deferred:** the richer #20 batch (staggered cascade, "+N states" combo overlay,
    cross-restart toast); celebration sound; pre-permission priming; first-run onboarding; one-way
    trips (#22).
- **v1.8 (2026-06-09)** — List filters, onboarding hint, and notification priming:
  - **Independent List filters:** the List tab now has separate **Found** and **Unfound** section
    toggles (both on by default, remembered via `UiPreferences`) in place of the single "show
    unfound" switch — so the list shows all states by default. When a search matches a state in a
    switched-off section, a **tappable hint** offers to reveal it.
  - **First-run map hint:** a one-time, dismissible tip on the active-trip map ("Tap a state when
    you spot its plate"), persisted in `UiPreferences` and auto-retired on the first find.
  - **Pre-permission priming for notifications:** before Android's `POST_NOTIFICATIONS` dialog we
    show a friendly in-app rationale (graphic + benefit points), and once permanently denied a
    "turn it on in Settings" deep-link variant. Reusable `rememberNotificationPermissionPrimer()`
    with a pure, unit-tested decision (`notificationPrimerAction` / `resolveSnooze`). Triggers on
    the first end-date set (New Trip + Manage trip) and on the Settings **Trip reminders** toggle
    (which now reflects the real permission outcome — declining leaves it off). "Not now" snoozes
    the primer for the next 2 end-date picks.
  - **Find-animation polish:** slower fill sweep (850 ms) and a fix so a find marked in State
    Detail animates after the return transition instead of behind it.
  - **Debug seed hardening:** the debug "Seed sample data" action ensures regions are seeded first
    (no more silent no-op on a fresh install) and always reports a detailed result.
  - **Still deferred:** the richer #20 batch; celebration sound; per-stop dates/notes; route on the
    summary/shared image; actual city pins; plus the newer ideas captured in `BACKLOG.md` (lifetime
    Plate Passport, achievements, rare-plate moments, photo capture, widget, recap, export/import).
- **v1.9 (2026-06-09)** — Lifetime Plate Passport (v1):
  - A new **Passport** bottom tab (third top-level destination) holding the family's **cross-trip
    collection**: a filled **lifetime map**, an all-time **"X of 50 collected"** counter (with an
    "N to go" line), and the collected states listed with their **first-spotted dates** (friendly
    empty state until the first catch). Read-only.
  - Backed by `SpottingRepository.observeLifetimeStates()` → `SpottingDao.observeLifetimeFound`
    (DISTINCT region across **all** trips' spottings, earliest timestamp via `MIN`). **No schema
    change** — it's a new read over existing data. Repository + ViewModel tests added.
  - **Passport follow-ups (2026-06-09):** each collected state now shows **which trip first caught
    it** ("First spotted <date> · <trip>"), states first caught on the **current active trip** get a
    **New!** badge, and tapping a collected state opens its **State Detail**.
  - **Still open (backlog):** an at-catch "new for your collection!" flourish on the map/celebration
    the moment a brand-new lifetime state is marked.
- **v1.10 (2026-06-09)** — Richer state rows + summary stats:
  - **Collectible state cards:** the Active Trip **List** rows and the **Passport** collection now
    use a shared `StateCard` — flag, bold name, a subtitle (spotted/first-spotted date or "Not found
    yet"), a colored accent stripe in the state's **map color**, and a found check badge (unfound
    states dimmed). The per-state hue is exposed via `stateAccentColor(code)` so lists echo the map.
  - **Summary stat tiles:** the celebration/summary screen leads with a row of **stat tiles** (
    icon +
    big value + label) for the headline numbers — states found, duration, and estimated distance —
    above the detail rows.
- **v1.11 (2026-06-09)** — Rare-plate moments (v1):
  - The bundled `rarity_score` now earns a little fanfare. A pure `isRarePlate(score)` (threshold
    0.6 → ~6 states: HI, AK, ND, WY, VT, SD) drives a **Rare** badge on the Active Trip list and
    Passport rows, a **✦ Rare find** chip on State Detail, and a **"Rare plate!"** toast the moment
    a
    rare plate is spotted. No schema change; classifier unit-tested.
  - **Still open (backlog):** a richer rare flourish (distinct animation/sound) and feeding a
    rare-catch achievement.
- **v1.12 (2026-06-09)** — Achievements (v1):
  - A **16-achievement** catalog earned from existing data: collection milestones (first plate,
    10/25/40/50 lifetime), single-trip feats (first trip, 50/50, 10-in-a-day), rarity (rare catch,
    treasure hunter), geography sweeps (New England / West Coast / Four Corners / "good neighbors" —
    a 5-state connected cluster via the state-adjacency graph), and social/time (team effort, early
    bird). Each is a **pure, unit-tested predicate** over an `AchievementStats` snapshot; titles and
    icons live in the UI layer so the catalog stays Android-free.
  - **Schema:** a new `achievement` table (id + earnedAt); DB bumped to **v6** with a v5→v6
    migration
    test. `AchievementRepository` builds the stats snapshot from spottings/trips, evaluates the
    catalog, and persists newly-earned (earned-once) — no new writes to gameplay tables.
  - **Surfacing:** re-evaluated on each find and on trip-end; newly-unlocked ones fire an
    **"Achievement unlocked"** toast, and the **Passport** gains an **Achievements section** (earned
    vs locked badges with an earned/total count). The state-adjacency data moved to the domain layer
    (`STATE_ADJACENCY`) so achievements and the map test share it.
  - **Still open (backlog):** per-achievement detail/share, more milestones, and a richer unlock
    animation.
- **v1.13 (2026-06-09)** — Richer end-of-trip recap (v1):
  - The celebration/summary screen now opens with a **one-line narrative recap** ("You spotted N
    states over <duration>, covering ~<distance>"), shows a **"Your journey"** timeline (a scrolling
    row of flag chips in the order states were found), and adds a **busiest day** highlight to the
    stats. All derived from existing spottings — `CelebrationStats` gains `timeline` +
    `busiestDayText` (computed in `CelebrationRepository`; repo test). The journey is on-screen only
    (kept out of the shared image to bound its height); the busiest-day stat appears in both.
  - **Still open (backlog):** per-player highlights, a biggest single-day streak, and folding the
    journey into the shared image.
- **v1.14 (2026-06-09)** — Test-stability hardening (no user-facing change):
  - **Notification primer is now context-tolerant:** `rememberNotificationPermissionPrimer()`
    resolves its prefs via a safe cast and **no-ops when the host isn't `LicensePlateQuestApp`** (
    e.g.
    an isolated Compose UI test, or any half-initialized state) instead of crashing composition.
    This
    keeps `NewTripScreen` / Manage trip / Settings renderable in tests without full app startup.
  - **Achievement re-evaluation decoupled from the find pipeline:** on a find the Active Trip
    ViewModel now only signals a **conflated channel**; a single dedicated collector runs
    `evaluateAndPersist()` off the reactive `foundCodes` path, **non-overlapping** and wrapped in
    try/catch, so achievement bookkeeping (and its background DB work) can never run inline within
    or
    disturb the state pipeline. Fixes flaky `ActiveTripViewModelTest` behavior.
  - *(Ops note: a corrupt local git index — `bad index file sha1 signature` — was producing phantom
    "modified" entries and unreliable diffs; rebuilt via `rm .git/index && git reset`, which leaves
    working files untouched.)*
- **v1.15 (2026-06-10)** — "New for your collection!" at-catch flourish (Passport follow-up):
  - Catching a state that's **brand-new to the lifetime collection** (no other trip has ever spotted
    it) now fires a gentle, non-blocking toast — "✨ <State> — new for your collection!" — alongside
    the usual find confetti. It's purely additive (a short toast), so it doesn't interrupt the
    marking flow.
  - Backed by `SpottingRepository.isNewToCollection(code)` (`SpottingDao.countOtherTripsWithRegion`
    == 0). The Active Trip ViewModel detects newly-found codes and routes them through a dedicated
    off-pipeline collector (like the achievement check) that runs the DB lookup and emits
    `newCollectionEvents`. No schema change.
  - Tested: a repository test (first-ever catch true; repeat on a later trip false) and a ViewModel
    test (a first catch emits the flourish).
  - Complements the Passport's existing **New!** badge on states first caught on the active trip.
- **v1.16 (2026-06-10)** — Richer debug sample data (dev-only, no release impact):
    - The debug "Seed sample data" action (Settings → Developer, `BuildConfig.DEBUG` only) now
      builds a
      full, varied dataset via a new `SampleDataSeeder`: a **6-player** roster with distinct colors;
      **3 completed** trips (one a full **50/50** cross-country sweep — completed-map styling + a
      filled
      Passport); **2 in-progress** trips (one **overdue**); and the **active** multi-stop trip. Each
      trip has its own finds with mixed single / multi-player / unattributed credit and **back-dated
      **
      dates + timestamps, so durations, date ranges, Passport first-spotted dates, and the recap all
      read like real history. Achievements are pre-evaluated so earned badges show immediately.
    - Built on the real repositories (create/mark/end), with direct DAO writes only to back-date
      timestamps the public API stamps as "now" (`SpottingDao.backdateSpotting`).
      `SettingsViewModel`
      now delegates seeding to `SampleDataSeeder`. Robolectric-tested. No schema change.
    - Added a separate **"Wipe all data"** developer action (confirmed, destructive-styled) that
      erases
      all trips, players, achievements, and the event log — keeping the bundled regions so the app
      still works — for a clean slate between test runs (`SampleDataSeeder.wipeAllData()` via
      FK-cascading
      `deleteAll()` DAO methods, in a transaction). Also tested.
- **v1.17 (2026-06-10)** — Real city pins on the route (playtest #11 follow-up):
  - The active-trip map route now pins each stop at its **actual city** rather than the state's
    center. Two new pieces: a pure **`AlbersUsaProjection`** that reimplements the bundled map's
    `geoAlbersUsa` composite (lower-48 Albers + Alaska/Hawaii insets) and the asset's crop shift, so
    a latitude/longitude lands in the same viewBox space as the bundled centroids; and a
    **`CityLocator`** seam (`AndroidCityLocator` over the platform `Geocoder`, run on IO, degrading
    to null when no backend is present). The Active Trip ViewModel geocodes each stop's city,
    projects it, and exposes per-stop points (`routeCityPoints`, parallel to `routeStops`); the map
    pins the city when resolved and **falls back to the state center** otherwise. Results are cached
    per city; no schema change.
  - The projection was **validated offline** against the bundled geometry (a spread of known cities
    each projects inside the correct state polygon) and is unit-tested with golden values; a
    ViewModel test covers geocode → project → pin with state-center fallback.
  - Still open: persisting resolved coordinates on the stop (currently re-geocoded per session), and
    drawing the route on the summary/shared image.
- **v1.18 (2026-06-10)** — Deferred-find celebration: cascade + combo (playtest #20 follow-up):
  - A batch of queued finds no longer all flash at once on the next map visit. **2–5** finds now
    play as a **staggered cascade** (a spatial top-left→bottom-right sweep), and **6+** play as a
    fast **combo** with a brief **"+N states!"** overlay banner over the map. A single find is
    unchanged.
  - Timing/thresholds live in a pure, unit-tested `celebrationTiming(count)` (`CelebrationTiming`:
    stagger, fill duration, combo flag) so the feel is tunable without touching the renderer;
    `UsMap`
    drives one master clock and computes each state's own fill progress from its cascade slot. No
    schema change; no new gameplay data.
  - Still open (the remaining #20 piece): the cross-restart "Welcome back · N states added" toast
    for
    finds queued in a previous session, with a ~24h silent expiry.
- **v1.19 (2026-06-11)** — Celebration sound (the deferred MVP item):
  - A short **chime** now plays on each find and an **ascending fanfare** on the 50/50 win, finally
    completing the celebration (firework + haptics + sound). The two sounds are **synthesized**
    (procedurally generated bell-ish tones) and bundled in `res/raw` (`sfx_find.wav`,
    `sfx_fifty.wav`), so they're swappable without code changes.
  - Played through a `CelebrationSounds` seam (`SoundPoolCelebrationSounds`, media stream) that
    **self-gates** on a new **Sound** setting, the device ringer (silent/vibrate → quiet), and media
    volume. Wired to the per-find confetti event and the FIFTY_FIFTY celebration in
    `ActiveTripScreen`.
  - New `SettingsRepository.soundEnabled` (default on) + a **Sound** toggle in Settings (mirrors the
    vibration toggle). Repo test added. No schema change.
  - A distinct **rare-plate sound** (a sparkly twinkle, `sfx_rare.wav`) layers over the find chime
    on
    a rare catch (`CelebrationSounds.playRare()`, fired from the rare-find event). Also fixed a
    latent
    `RegionSeeder` bug surfaced by the seeder test: re-seed when the data-version flag is current
    but
    the region table is empty (flag/table divergence), via a new `PlateRegionDao.count()`.
- **v1.20 (2026-06-11)** — Distinct rare-plate animation:
  - Catching a rare plate now fires a bold gold **`RareSparkle`** overlay — a bright central glow, a
    gold ring bursting outward, a large 8-point "hero" star that pops in with overshoot and lingers,
    and a scatter of twinkling stars drifting up and out — layered over the usual find confetti so a
    rare catch reads as clearly *more*. Pure Compose Canvas (no dependencies), trigger-keyed like
    `Confetti`, fired from the same rare-find event as the rare sound + badge.
  - This completes the rare-plate moment (badge + sound + animation + the `rare_catch` achievement).
- **v1.21 (2026-06-11)** — Recap follow-ups: per-player highlights + longest streak:
  - The end-of-trip recap gains a **"Player highlights"** section — each credited player (color
    dot +
    name) with their plate count and a **✦ <state>** flourish for anyone who caught a rare plate.
    The section only appears when **at least one player caught a rare plate** (otherwise it's just a
    duplicate of the leaderboard). Also a **"Longest streak"** stat (the longest run of consecutive
    days with a find).
  - Backed by `CelebrationStats.playerHighlights` + `longestStreakText`
    (`SpottingPlayerDao.creditedFindsForGame` for per-player finds/rarity; pure, unit-tested
    `longestConsecutiveDayStreak`). Repo tests added. No schema change.
  - Still open on the recap: folding the journey/route into the shared summary image.
- **v1.22 (2026-06-11)** — Achievements v2: progress, more milestones, detail/share, richer unlock:
  - **Six new badges** (catalog now 22): three geography sweeps (**Great Lakes**, **Deep South**,
    **Mountain West**), **Coast to Coast** (a Pacific- and an Atlantic-coast state), **Night Owl**
    (a find after 9 p.m.), and **Weekend Warrior** (finds on both a Saturday and a Sunday). New
    region sets + the supporting stats (`maxStatesOnATrip`, `latestFindHour`,
    `foundOnSaturday/Sunday`)
    are pure and unit-tested.
  - **Progress model:** each achievement now exposes a pure `progress(stats) → AchievementProgress`
    (current / target); `evaluateAchievements` derives "earned" from it. Locked, multi-step badges
    on
    the Passport show a **progress bar + "3 / 6"** so kids can see how close they are. No schema
    change for any of this (still a read-derive over existing data).
  - **Tap-to-detail + share:** tapping any badge opens a bottom sheet with the full description and
    either the **earned date** or live progress; earned badges get a **Share** button (plain-text
    system share). Backed by a new `AchievementDao.observeEarned()` (ids + `earned_at`).
  - **Richer unlock moment:** the plain toast is replaced by a celebratory **badge-reveal banner**
    (`AchievementUnlockBanner`, a bouncy scale/fade pop-in) on the Active Trip screen, with a
    fanfare
    sound; multiple simultaneous unlocks queue and show one at a time.
  - **Passport perf:** the progress snapshot is computed off the critical render path (it no longer
    gates first paint), and `PassportContent` is now a `LazyColumn` so the map, ~22 badges, and up
    to
    50 state cards compose lazily instead of all at once — fixes the slow Passport open.
  - Still open: a richer (image) share card for a badge, and per-country sweeps once CA/MX land.
