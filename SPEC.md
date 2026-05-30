# Road Trip Games — MVP Specification

**Project:** Family Road Trip Games (working title)
**Platform:** Android (Native, Kotlin + Jetpack Compose)
**Document version:** 1.0
**Status:** MVP scope locked

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
- State detail screen with bundled state info (bird, motto, flower, plate image, fun fact)
- Mark and unmark states with confirmation
- Per-state confetti and sound celebration
- 50/50 celebration with full stats screen (does not end trip)
- Manual-end celebration with stats screen
- Trip list with three sections: Active, In Progress, Completed
- Special visual treatment for completed (50/50) trips in the list
- Bottom sheet on active trip view showing found states (sortable: order found, alphabetical)
- Delete trips
- Add players to an in-progress trip

### Explicitly out of scope (deferred to future phases)

- Accounts, cloud sync, multi-device, login
- Photo capture
- GPS permission flow and GPS-based features (trip auto-end, per-spotting location, distance-from-current-location)
- Other road trip games (slug bug, alphabet, padiddle, trivia, etc.)
- Sharing or exporting trips
- Canadian provinces, DC, U.S. territories
- Achievements and badges
- Editing spotting details after the fact (only unmark is supported)
- Settings screen (including sound mute toggle, theme, etc.)
- Detailed read-only summary for completed trips beyond the celebration screens
- Per-player score tracking (each player's running tally across trips)
- Removing a player from a trip after it starts
- Complex rarity calculations based on telemetry
- Push notifications
- Widgets

---

## 4. User Stories

### Players
- As a parent, I want to create player profiles once so I don't have to re-enter family names every trip.
- As a parent, I want to manage (add, edit, delete) player names from a dedicated screen.
- As a user, I want to quickly add a new player while creating a trip without leaving the trip creation flow.

### Trips
- As a family, I want to create a new trip with a name, origin, destination, start date, and the players in the car.
- As a user, I want the trip name auto-filled with something sensible (e.g., "Springboro → Cincinnati, May 2026") so I can just tap accept.
- As a user, I want to see all my trips in one list, grouped by status.
- As a user, I want to switch between trips, with the most recently used one being active.
- As a user, I want to end a trip manually when I get home.
- As a user, I want to delete a trip I no longer want.

### Gameplay
- As a passenger, I want to tap a state on the map when I see its license plate.
- As a player, I want a confirmation screen before marking so I don't mis-tap.
- As a player, I want to see fun information about each state (bird, motto, flower, fun fact, plate image).
- As a player, I want to unmark a state if I made a mistake.
- As a player, I want to see how many states we've found out of 50.
- As a player, I want to see a list of states we've already found, sorted by order or alphabetically.

### Celebrations
- As a family, we want a fun confetti and sound celebration each time we find a new state.
- As a family, we want a big celebration with stats when we find all 50 states.
- As a family, we want a smaller celebration with stats when we manually end a trip.

---

## 5. Screen Inventory

| # | Screen | Purpose |
|---|--------|---------|
| 1 | Trip List (Home) | List of all trips, grouped into Active / In Progress / Completed. Entry point if no active trip. |
| 2 | Active Trip View | Map, counter, trip name, bottom sheet of found states. Entry point if an active trip exists. |
| 3 | State Detail | Shown on map tap. Displays state info; "Mark as found" or "Unmark" depending on status. |
| 4 | New Trip Creation | Form: trip name (prefilled), origin city + state, destination city + state, start date, players. |
| 5 | Players Management | Full CRUD for player roster, accessible from bottom nav. |
| 6 | 50/50 Celebration | Big celebration + stats screen when the 50th state is found. Trip continues afterward. |
| 7 | Manual-End Celebration | "You made it home!" celebration + stats screen when user manually ends a trip. |

### Navigation

- Bottom navigation with two tabs: **Trips** and **Players**.
- Trips tab default destination: Active Trip View if a trip is active, otherwise Trip List.
- Trip List → Active Trip View on selecting a trip (also makes that trip the active one).
- Active Trip View → State Detail on tapping a state on the map or in the bottom sheet.
- Active Trip View → 50/50 Celebration automatically when the 50th state is marked.
- Active Trip View → Manual-End Celebration → Trip List on ending a trip.

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

- **Top:** Trip name (tap to view trip details — read-only in MVP).
- **Middle:** SVG U.S. map. Unfound states show outline only; found states are filled with a color. Pinch-to-zoom and pan supported. Tapping a state opens State Detail.
- **Counter:** Persistent X/50 display.
- **Bottom sheet:** Collapsible sheet showing found states. Sortable by "Order found" (default, newest first) or "Alphabetical." Each row shows state name and plate image thumbnail. Tapping a row opens State Detail.
- **Menu:** End trip option (with confirmation dialog).

### State Detail

When tapping an **unfound** state:
- State name, bird, motto, flower, plate image, fun fact.
- Primary action: "Mark as found"

When tapping a **found** state:
- Same state info.
- Found timestamp, trip name where it was found.
- Primary action: "Unmark"

Confirmation dialogs for both Mark and Unmark to prevent mis-taps.

### New Trip Creation

Fields:
- **Trip name:** Required. Prefilled with `{Origin City} → {Destination City}, {Month Year}` (updates live as origin/destination/date change).
- **Origin:** City (text input) + State (dropdown). Both required.
- **Destination:** City (text input) + State (dropdown). Both required.
- **Start date:** Date picker. Defaults to today.
- **Players:** Multi-select from roster. At least one required. "+ Add new player" quick-add inline.

Save creates the trip, makes it active, navigates to Active Trip View.

### Players Management

- List of all players with name.
- "+" to add a new player (name only).
- Tap a player to edit name.
- Swipe or long-press to delete (with confirmation, warning if player is on any trips).

### 50/50 Celebration Screen

- Big confetti animation, celebration sound.
- Headline: "All 50! Congratulations [trip name]!"
- Stats:
  - Trip duration
  - Average time between state finds
  - Longest gap between finds
  - Shortest gap (rapid-fire moment)
  - First state found
  - Last state found
  - Estimated distance traveled (sum of consecutive state-center distances)
  - Furthest state from origin (by state-center distance)
  - Rarest state found (by static rarity score)
  - Players on this trip
- "Continue" button returns to Active Trip View. Trip stays active.

### Manual-End Celebration Screen

- Smaller confetti, "Made it home!" headline.
- Same stats as above (no 50/50 framing).
- "Done" returns to Trip List. Trip status moves to Completed.

---

## 7. Data Model

All entity IDs are UUIDs (java.util.UUID). All timestamps are stored as UTC ISO 8601.

### Tables

**Player**
- `id` (UUID, PK)
- `name` (text)
- `created_at` (timestamp)
- `updated_at` (timestamp)

**PlateRegion**
- `id` (UUID, PK)
- `country_code` (text, e.g., "US")
- `region_code` (text, e.g., "OH")
- `name` (text, e.g., "Ohio")
- `bird` (text)
- `motto` (text)
- `flower` (text)
- `fun_facts` (JSON array of strings)
- `plate_image_path` (text, asset path)
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
- `spotter_player_id` (UUID, FK → Player, nullable) — always null in MVP, reserved for future per-player attribution
- `timestamp` (timestamp)
- `note` (text, nullable)
- `photo_path` (text, nullable) — reserved for future photo capture
- `gps_lat` (numeric, nullable) — reserved for future
- `gps_lng` (numeric, nullable) — reserved for future
- `created_at` (timestamp)

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
- Deleting a player is allowed, but their `TripPlayer` rows remain so trip history isn't broken (consider soft-delete on Player).

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
- `plate_image_path` ("plates/oh.png")
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
- **Map:** SVG-based U.S. map (single SVG file with each state as a `<path>` carrying a `region_code` attribute). Pan/zoom via Compose gesture modifiers or a library like `zoomable`.
- **Confetti:** `konfetti` library or similar.
- **Sound:** Android `SoundPool` for short celebration sounds. Respect phone media volume and silent mode.
- **IDs:** `java.util.UUID` for all entity primary keys.
- **No backend.** All data local.

### Architecture

- MVVM with Compose. ViewModels expose UI state as `StateFlow`.
- Repository layer between ViewModels and Room DAOs.
- Use cases for non-trivial business logic (start trip, mark state, compute celebration stats).
- Strict separation of bundled static data (state info, plate images) and user-generated data (trips, spottings).

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
- Marking and unmarking states both require explicit confirmation.
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
5. **Map rendering:** SVG U.S. map, tap detection per state, pinch-zoom and pan, color fill for found states.
6. **State detail and marking:** State detail screen, mark/unmark flows with confirmation, bundled state info display.
7. **Active trip view:** Map + counter + trip name + bottom sheet with sortable found-states list.
8. **Celebrations:** Per-state confetti and sound, 50/50 celebration screen with stats, manual-end celebration screen.
9. **Polish:** Empty states, validation messages, edge cases, animation polish.
10. **Pre-launch:** Internal testing, accessibility pass, Play Store assets and listing.

---

## 12. Open Questions Deferred to Build Phase

These don't block spec lock but should be decided during build:

- Specific confetti library and exact animation timing.
- Specific celebration sound (length, character — playful but not jarring).
- Visual design system: colors, typography, exact found-state fill color.
- Map projection and SVG source.
- Exact "completed trip" visual treatment in the trip list.
- Whether 50/50 celebration re-triggers if user unmarks and re-marks the 50th state (recommendation: no, it only fires once per trip).
- Accessibility: screen reader support, color-blind-safe found-state styling.

---

## 13. Document Change Log

- **v1.0** — Initial MVP spec compiled from planning conversation.

