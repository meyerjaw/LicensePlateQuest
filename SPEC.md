# License Plate Quest — MVP Specification

**Project:** License Plate Quest (launcher label "LP Quest")
**Platform:** Android (Native, Kotlin + Jetpack Compose)
**Document version:** 1.4
**Status:** MVP shipped; in active post-MVP iteration

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
- Trip creation: name, origin (city + state), destination (city + state), start date, selected players
- Manual trip end (no GPS-based auto-end)
- State detail screen with bundled state info (bird, motto, flower, state flag, fun fact)
- Mark a state immediately on the explicit "Mark as found" tap; unmark requires confirmation
- Per-state firework-style confetti celebration with haptic feedback (celebration sound asset deferred)
- 50/50 celebration with full stats screen (does not end trip)
- Manual-end celebration with stats screen
- Trip list with three sections: Active, In Progress, Completed
- Special visual treatment for completed (50/50) trips in the list
- Active Trip view with **Map** and **List** top tabs (selection remembered across sessions); the List tab shows found states — sortable (order found, alphabetical), searchable, with a toggle to also show unfound states
- Found states fill the map in a graph-colored vibrant palette (no two bordering states share a color), with 2-letter abbreviations on unfound states and check marks at each state's visual center
- A bottom stats strip under the map (found X/50, percent, last find, day of trip, found today)
- Delete trips; swipe-to-delete with an in-place 3-second undo on the Trip List and Player roster
- Manage an in-progress trip's players — add existing, add brand-new, and remove — from the Active Trip overflow menu
- **Per-player attribution:** each player has a chosen color; a find can be credited to one or more players; the summary shows a leaderboard ranking players (crown for the lead) plus a "Family Find" line for unattributed plates
- **Share a finished trip** as an image (filled map + stats with app watermark) via the system share sheet
- **Settings screen** (reached from a top-right icon): theme (light / dark / system) and a vibration toggle
- **Default home location** that pre-fills the New Trip origin

### Explicitly out of scope (deferred to future phases)

- Accounts, cloud sync, multi-device, login
- Photo capture
- GPS permission flow and GPS-based features (trip auto-end, per-spotting location, distance-from-current-location)
- Other road trip games (slug bug, alphabet, padiddle, trivia, etc.)
- Canadian provinces, DC, U.S. territories, and the full-screen multi-country region selector
- Achievements and badges
- Editing spotting details after the fact beyond who's credited (note, photo, time — only unmark and editing attribution are supported)
- Trip start/end dates beyond a single start date, and overdue-trip reminders
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
| 2 | Active Trip View | Standalone full-screen view (no bottom nav) with a Back button and two top tabs — **Map** and **List**. Trip name + overflow menu (Manage players, End trip). Entry point if an active trip exists. |
| 3 | State Detail | Shown on map tap. Displays state info; "Mark as found" or "Unmark" depending on status. |
| 4 | New Trip Creation | Form: trip name (prefilled), origin city + state, destination city + state, start date, players. |
| 5 | Players Management | Full CRUD for player roster, accessible from bottom nav. |
| 6 | 50/50 Celebration | Big celebration + stats screen when the 50th state is found. Trip continues afterward. |
| 7 | Manual-End Celebration | "You made it home!" celebration + stats screen when user manually ends a trip. |
| 8 | Manage Players | Add/remove the current trip's players — pick existing roster members or create a brand-new player. Reached from the Active Trip overflow menu. |

### Navigation

- Bottom navigation with two tabs: **Trips** and **Players**. The bottom nav is hidden while the Active Trip view is showing, so it reads as a standalone full-screen view.
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

- **Top bar:** Back button (returns to the Trip List), trip name, and an overflow (⋯) menu with **Manage players** and **End trip** (the latter with a confirmation dialog).
- **Top tabs:** **Map** and **List**. The selected tab is persisted (via `UiPreferences`) and restored the next time the user opens a trip.
- **Map tab:** Bundled vector U.S. map. Unfound states show outline only; found states are filled with a per-state color drawn from a vibrant palette (stable per state code), so the map fills in as a colorful mosaic; a newly found state animates its fill. Found states also carry a check mark (color-blind-safe cue). Pinch-to-zoom and pan supported. Tapping a state opens State Detail.
- **List tab:** Header with the persistent **X/50 counter** and sort chips ("Order found" — default, newest first — or "Alphabetical"); a **search box** (filter by name); a **"Show unfound states"** toggle; then the scrolling list. Each row shows the state flag thumbnail and name; unfound rows are dimmed and labelled "Not found yet." Tapping a row opens State Detail.

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
- **Origin:** City (text input) + State (dropdown). Both required.
- **Destination:** City (text input) + State (dropdown). Both required.
- **Start date:** Date picker. Defaults to today.
- **Players:** Multi-select from roster. At least one required. "+ Add new player" quick-add inline.

Save creates the trip, makes it active, navigates to Active Trip View.

### Players Management

This is the **global roster** (Players tab):

- List of all players with name.
- "+" to add a new player (name only).
- Tap a player to edit name.
- Swipe or long-press to delete (with confirmation, warning if player is on any trips). Delete is a soft-delete.

### Manage Trip Players

Per-trip roster editing, reached from the Active Trip overflow menu (separate from the global roster above):

- **On this trip:** lists the trip's current players (join order); each has a remove (✕) action. Removing only unlinks the player from this trip — it never deletes them from the global roster.
- **Add from your players:** lists active roster members not yet on the trip; each has an add (＋) action. Hidden when everyone is already on the trip.
- **Add a new player:** an inline name field that creates a brand-new roster player and adds them to the trip in one step, with the same blank/duplicate-name validation as the global Add Player flow.
- Changes apply live (no separate save); removal is immediate (no confirmation, since re-adding is trivial).

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
- `status` (enum: `active`, `in_progress`, `completed`)
- `ended_at` (timestamp, nullable)
- `created_at` (timestamp)
- `updated_at` (timestamp)

**TripPlayer** (junction)
- `id` (UUID, PK)
- `trip_id` (UUID, FK → Trip)
- `player_id` (UUID, FK → Player)
- `joined_at` (timestamp) — supports "add player mid-trip"

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

- New trip requires: name (auto-prefilled), origin city + state, destination city + state, start date (defaults today), at least one player.
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

