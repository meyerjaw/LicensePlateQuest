# Backlog

Ideas and improvements captured for later — not yet scheduled. Roughly priority-ordered within each section.

> Many items below were expanded during the **2026-06-02 playtest** brainstorm. Those carry a `[playtest #N]` tag referencing the original note number, and several were merged into pre-existing entries rather than duplicated. Cross-cutting themes that span multiple items are collected at the bottom.

## Features

- **Celebration sound (deferred from MVP).** Add a short, playful celebration sound (Android
  `SoundPool`) for per-find and the 50/50 moment, respecting silent mode and media volume. The
  firework + haptics are already in place; this is the missing half of the celebration. Gate behind
  the planned sound setting. (Explicitly deferred during build.) *Status: shipped 2026-06-11 —
  synthesized chimes bundled in `res/raw` (a bright rising triad on a find, an ascending fanfare for
  50/50), played via a `CelebrationSounds` seam (`SoundPoolCelebrationSounds` on the media stream)
  that self-gates on the new Sound setting, the device ringer (stays quiet when silenced), and media
  volume. Wired to the per-find confetti event and the 50/50 celebration. The two `.wav` files are
  trivially swappable. Repo test for the setting.*
- **Per-player attribution on found plates.** `Spotting.spotter_player_id` is reserved but always null today. `[playtest #17]` Make attribution *optional and multi-select* — a plate can be credited to several players (treat the column as a set; empty = unattributed). Flow: tap still marks found; with 2+ players an attribution sheet appears with a prominent "Skip", remembering the last selection for rapid entry; with 0–1 players no attribution UI shows. Surface credits as small player indicators on the list/map rows, allow editing attribution from a found-state detail sheet, and keep attribution records even if a player is later soft-deleted ("Deleted player"). Feeds the summary leaderboard `[playtest #18]` and depends on player colors `[playtest #19]`.
- **Player favorite color.** `[playtest #19]` Give each player a color chosen from a curated 8–12 swatch palette (not a free-form picker) on the add/edit player screen, with light/dark variants and a live sample-chip preview. Auto-pick the first unused color by default; dim already-taken swatches to encourage uniqueness. Store as a palette token (`"teal"`), not raw hex, and resolve via the theme. Powers attribution indicators, summary chips, and celebration accents — centralize in a `getPlayerColor(playerId)` helper. Always pair color with initials/name so colorblind users aren't lost.
- **Summary screen player leaderboard.** `[playtest #18]` Replace the plain player list on the celebration/summary screens with sorted chips (color dot · name · score, crown 👑 on the leader). Sort by score desc, alphabetical tiebreak; all tied leaders get crowns; no crown when everyone's at zero. Show unattributed plates as a separate muted row excluded from crown logic. Depends on attribution `[playtest #17]` and colors `[playtest #19]`.
- **Manage trip flow / edit trip.** The trip name is currently read-only ("tap to view details — read-only in MVP"). Build a single **Manage trip** screen that lets the user edit a trip's name, start/end dates, origin/destination, and players after creation. `[playtest #14]` Approach: extend or replace the existing Manage players screen (`ManagePlayersScreen`) into this broader manage-trip screen so the player add/remove UI is reused rather than rebuilt — manage-players becomes one section of it. Reuse existing pieces: the New Trip form fields and validation (name, From/To region selectors, start/end date pickers — end dates landed 2026-06-04 via `[playtest #12]`) and the trip-player add/remove logic (`addPlayerToTrip`/`removePlayerFromTrip`). Surface trip actions from the active trip overflow menu: edit details, end trip (→ summary), extend trip, add a stop mid-trip, delete (in overflow, with confirm), and share progress. Editing stops must not remove already-found plates (plates belong to the trip, not a stop); changing dates re-validates end ≥ start and reschedules any overdue reminder `[playtest #13]`; if an edited end date is in the past, prompt "End trip now?"; deleting an active trip gets an extra confirm.
- **Trip start & end dates.** `[playtest #12]` Add an optional end date alongside the existing start date (all-day granularity). Validate end ≥ start (push end forward if start moves past it). Derive status from dates — `upcoming` / `active` / `overdue` (past end, not ended) / `completed` — and show the range on the summary ("June 15 – June 22, 2026 · 7 days"). Pairs with overdue notifications `[playtest #13]`.
- **Pit stops / multi-leg trips.** `[playtest #11]` Replace the single destination with an ordered `stops` array (first = start, last = final destination), each stop `{ id, city, regionCode, arrivalDate?, departureDate?, notes? }`; migrate existing trips to a 2-item array. UI: reorderable stop list with "+ Add stop" (opens the shared region selector), first/last visually distinguished. Map view draws stops in order with thin straight connecting lines and numbered pins (no real routing). Plate counting stays **global to the trip** for v1 (per-leg later). *Status: shipped 2026-06-08 — `trip_stop` table (DB v4, with migration tests), unified ordered stops, add/remove/up-down reorder in New Trip + Manage trip, and a route overlay (line + numbered pins at each stop state's visual-center) on the active-trip map. Still open: per-stop arrival/departure dates + notes; route on the summary/shared map; the city-pins item below.*
- **Actual city pins for the route.** Today the route pins sit at each stop **state's**
  visual-center (the state we know from `region_code`), not the actual city the user typed. Upgrade
  to pin the **real city location**: resolve each stop's city to lat/lng (bundled city gazetteer, or
  on-device geocoding behind availability checks) and project geo → map viewBox to place the pin
  precisely (the geo↔viewBox projection doesn't exist yet — it'd be the first such mapping; the
  state visual-centers are viewBox-native). Falls back to the state center when a city can't be
  resolved. Improves accuracy for multi-stop routes within a single state and makes the route line
  follow the actual path. Pairs with multi-country expansion (same projection need). *Status:
  shipped 2026-06-10 — a pure `AlbersUsaProjection` (reimplements the bundled map's `geoAlbersUsa`
  composite incl. AK/HI insets + the asset's crop shift; validated offline that known cities land in
  the right state polygon; unit-tested with golden values) plus a `CityLocator`
  seam (`AndroidCityLocator` over the platform Geocoder, on IO, graceful when absent). The Active
  Trip VM geocodes each stop's city, projects it, and pins it (cached per city; falls back to the
  state center on a miss). Still open: persisting resolved coords on the stop (currently re-geocoded
  per session) and the route on the summary/shared image.*
- **Overdue-trip reminders (long-term).** `[playtest #13]` Local notifications (no server): schedule
  on trip create/edit when an end date exists, cancel on end/delete/extend. Request permission
  contextually (first time an end date is set), fire at end-date +1 day with an optional +3 day
  follow-up, friendly copy, deep-link to the trip, and action buttons (End trip / Extend / Remind
  later). No end date → nothing scheduled; use UTC internally. Add a "Trip reminders" settings
  toggle. *Status: shipped 2026-06-05 — per-trip WorkManager scheduling, one-nudge-per-trip dedup,
  tap-to-open, contextual permission, settings toggle, the +3 day follow-up, and the notification
  action buttons (End trip / Remind later / Extend, the last deep-linking into Manage trip).
  Pre-permission priming shipped 2026-06-09 (see below).*
- **Pre-permission priming for notifications.** Before firing Android's system `POST_NOTIFICATIONS`
  dialog, show our own lightweight rationale ("primer") explaining *why* we want to notify — so a
  family doesn't miss the nudge to wrap up an overdue trip. Goal: lift grant rates and avoid the "
  Don't allow" dead-end (once the system dialog is permanently denied, it can't be re-shown, only
  deep-linked to settings). Flow: on the first trigger (currently when an end date is set on New
  Trip — `[playtest #13]`), present a friendly in-app dialog with a clear value statement and "Not
  now" / "Sounds good" actions; only on "Sounds good" launch the real system prompt. If the OS
  reports the request was permanently denied (`shouldShowRequestPermissionRationale` is false after
  a denial), swap the primer for a "turn it on in Settings" variant that deep-links to the app's
  notification settings. Keep copy warm and specific (mention overdue-trip reminders, not generic "
  notifications"). Make the primer reusable so any future notification need (not just reminders) can
  show context first. Pairs with the "Trip reminders" settings toggle and the contextual request
  already in place. *Status: shipped 2026-06-09 —
  reusable `rememberNotificationPermissionPrimer()` (graphic + benefit points), pure unit-tested
  decision/snooze logic, permanently-denied → Settings deep-link, triggers on first end-date set (
  New Trip + Manage trip) and the Settings toggle (which now reflects the real grant outcome). "Not
  now" snoozes for the next 2 end-date picks.*
- **Share a finished trip.** Export a shareable image of the colorful filled-in map plus celebration stats. `[playtest #4]` "Long screenshot" = render the full scrollable summary to a `Bitmap` at full content height and hand to the Android share sheet (`Intent.ACTION_SEND`); bake in a title ("My License Plate Quest — [trip name]") and an app-name + date footer; verify the map, fonts, and dark mode render before capture; no personal data beyond what the user entered. Depends on the filled summary map `[playtest #3]`.
- **Lifetime "Plate Passport" (cross-trip collection).** `[2026-06-09]` Everything is per-trip
  today; add a persistent, cross-trip record of every state ever spotted, so the family keeps a
  long-term collection that grows across trips. A lifetime map (reuse the non-interactive map mode)
  plus an all-time counter ("43 / 50 collected"), each state's **first-spotted date** and which trip
  first caught it. Derived from existing `spotting` rows across all game instances — a
  DISTINCT-by-region read, no new writes. Reachable from a top-level "Passport"/"Collection" entry.
  Optionally a gentle "new for your collection!" accent the first time a state is added lifetime (
  distinct from the normal per-trip find). Foundation for achievements below. *Status: v1 shipped
  2026-06-09 — a **Passport** bottom tab with a lifetime filled map, an all-time "X of 50 collected"
  counter (+ "N to go"), and the collected states listed with first-spotted dates (empty state when
  none). Reads `observeLifetimeStates()` (DISTINCT region across all trips, MIN timestamp); no
  schema
  change; repo + VM tests. Follow-ups shipped 2026-06-09: which-trip-first attribution ("First
  spotted <date> · <trip>"), a **New!** badge on states first caught on the active trip, and tapping
  a collected state to open its State Detail. **At-catch "new for your collection!" flourish shipped
  2026-06-10** — a gentle toast fires when a state brand-new to the lifetime collection is caught
  (`isNewToCollection`; off-pipeline event → toast; repo + VM tests). Still open: a richer on-map /
  celebration treatment for the moment (beyond the toast).*
- **Achievements / badges.** `[2026-06-09]` Award playful milestones: first plate, a regional
  sweep (all of New England / the West Coast / a custom region), neighbor chains, a full 50 (per
  trip *and* lifetime), a rare catch (see rare-plate moments), and quantity/time fun ("5 before
  lunch"). Store earned achievements with an `earnedAt`; show a locked/unlocked grid and a small
  celebration on unlock. Define each rule as a **pure, testable predicate** over the found set +
  timestamps so they're easy to unit-test. Pairs with the Plate Passport (lifetime data) and the
  existing celebration system. *Status: v1 shipped 2026-06-09 — a 16-achievement catalog (pure,
  unit-tested predicates over an `AchievementStats` snapshot): collection milestones (10/25/40/50 +
  first plate), single-trip feats (first trip, 50/50, 10-in-a-day), rarity (rare catch, treasure
  hunter), geography sweeps (New England / West Coast / Four Corners / good-neighbors via the
  state-adjacency graph), and social/time (team effort, early bird). New `achievement` table (DB
  v5→v6 + migration test); `AchievementRepository` evaluates on each find / trip-end, persists
  earned-once, emits an "unlocked!" toast; an **Achievements section** on the Passport (earned vs
  locked badges + earned/total). Still open: per-achievement detail/share, more milestones, and
  richer unlock animation.*
- **Rare-plate moments.** `[2026-06-09]` `PlateRegion.rarity_score` is bundled but unused. When a
  high-rarity state is marked, layer a distinct "Rare!" flourish on top of the normal find
  celebration (different accent / sound / animation, gated on a threshold), show a small "rare"
  badge on that state's list/detail row and in the summary, and feed an achievement. Tune the
  threshold from the bundled data; keep it subtle so common finds still feel good. Cheap,
  high-delight, and makes existing data earn its keep. *Status: v1 shipped 2026-06-09 — pure
  `isRarePlate(rarityScore)` (threshold 0.6 → ~6 states) + unit test; a **Rare** badge on the Active
  Trip list + Passport rows, a **✦ Rare find** chip on State Detail, and a **Rare plate!** toast
  when
  a rare plate is marked. **Distinct rare sound shipped 2026-06-11** — a sparkly twinkle
  (`sfx_rare.wav`) layered over the find chime via `CelebrationSounds.playRare()`, on the rare-find
  event. A rare-catch achievement (`rare_catch`) also already ships with the achievements. Still
  open: a distinct on-screen rare *animation* (the sound + badge are done).*
- **Photo capture for a find.** `[2026-06-09]` `Spotting.photo_path` is reserved but always null.
  Let a player attach a photo of the actual plate when marking a state (system camera intent or
  CameraX), stored in **app-private** storage with the path on the spotting; show a thumbnail on
  State Detail and optionally in the shared summary; allow retake/remove. Entirely optional. Privacy
  note: photos may show real plates and faces — keep them local, never uploaded, and exclude from
  any share unless the user opts in.
- **Home-screen widget.** `[2026-06-09]` A glanceable launcher widget for the active trip — X / 50,
  last state found, day of trip, maybe a tiny filled map — built with **Glance** (Jetpack app
  widgets), refreshed when spottings change; tapping opens the active trip. When no trip is active,
  show a "start a trip" prompt. Handy during a real road trip with the phone on a mount.
- **First-time trip wizard (onboarding).** `[2026-06-10]` Guide a brand-new user from zero state to
  "ready to play" in their first session instead of dropping them on an empty trip list with no idea
  what to do — a friendly, *skippable* multi-step flow covering the essentials (what the game is,
  create at least one player, optionally set home, create the first trip). Should feel like helpful
  structure, not a mandatory tutorial.
  - **Trigger:** first launch after install, gated by a `hasCompletedOnboarding` flag in user prefs.
    Resume from where they left off if force-quit mid-wizard; re-runnable from Settings ("Restart
    setup wizard") for users who skipped the first time.
  - **Proposed steps:** (1) **Welcome** — app name + one-line pitch ("Spot all 50 state license
    plates on your next road trip"), hero illustration, single "Get started" CTA, plus a corner
    "Skip setup" that drops to an empty trip list. (2) **Set home (optional)** — "Where do you
    usually start your trips?" via the full-screen region selector `[playtest #7]`; explains it
    auto-fills the trip origin (changeable in Settings); prominent Skip; writes the home default
    `[playtest #8]`. (3) **Add players** — "Who's playing?" inline add-player form (name + color
    picker `[playtest #19]`) starting with one focused empty row + "+ Add another"; live color-chip
    preview; minimum one player to proceed (or allow zero = solo / no-attribution). (4) **Create
    your first trip** — the trip form with origin prefilled from home, destination, optional dates
    `[playtest #12]`, round-trip/one-way toggle `[playtest #22]`; multi-leg `[playtest #11]` is
    hidden here to keep it simple (add stops later from Manage trip); "Skip — I'll plan later".
    (5) **You're ready** — confirmation ("start spotting whenever you're on the road") + "Let's go"
    → active-trip map, with one or two lightweight, dismissable coachmarks ("Tap a state to mark it
    found"). Keep it to a pointer or two, not a full tour.
  - **Skip behavior:** every step after Welcome can Skip to the next; skipping the whole wizard
    lands
    the user on the trip list's "Plan your first trip" empty state (bypassed, not stuck). The
    `hasCompletedOnboarding` flag flips true on either completion **or** an explicit skip — don't
    keep nagging.
  - **State persistence:** save partial progress per step (return them to the step they left off);
    data created in earlier steps (players, home) is written immediately and real — *not* held in a
    "wizard buffer" — so bailing never loses work.
  - **Edge cases:** reinstall re-runs the wizard (expected); restoring from a backup / cloud sync
    with existing data skips it (not really new); finishing then deleting all players/trips does
    **not** re-trigger it; verify every step works on small/older screens without horizontal scroll
    and with the keyboard not covering inputs.
  - **Tone & a11y:** conversational copy ("Who's playing? Add the folks who'll be on the road with
    you."), the app's own colors/fonts/components (not a separate world), a top progress indicator
    (dots / thin bar); screen-reader navigable with announced transitions ("Step 3 of 5: Add
    players"), keyboard-accessible labeled Skip, no focus trap, Back on every step except Welcome.
  - **Future / implementation:** could later double as a "what's new" feature-discovery surface
    after
    big updates (a separate pattern, not core onboarding); keep the step config **data-driven** so
    variants (3 vs 5 steps, mandatory vs optional players) are A/B-able without rewrites. Build as a
    **stack-based navigation flow** (each step its own screen with its own
    validation/animation/back),
    with wizard-scoped state persisted to disk on each transition. Absorbs/extends the lightweight
    first-run map hint already shipped (the `hasCompletedOnboarding` gate is broader than today's
    one-time map tip).

## UX & polish

- **Filled-in map on the summary screen.** `[playtest #3]` Reuse the map component in a
  non-interactive display mode (no pan/zoom/tap) as the hero element at the top of the summary,
  aspect-ratio locked. Show it even at 0 states (blank) — it's part of the summary's visual
  identity. Must survive the long-screenshot capture `[playtest #4]`. *Status: shipped 2026-06-04 (
  v1.4) — the filled US map is the hero of the summary/celebration screen (non-interactive mode) and
  is baked into the shared image.*
- **States-found counter on the map view.** `[playtest #2]` Add an `X / 50` pill/badge on the map
  tab (top corner, not covering content; match whatever the list counts re: DC/territories). Animate
  on change (count-up or scale bounce). Optional: tap → jump to list or open a quick-stats sheet.
  *Status: shipped 2026-06-04 (v1.4) — an X/50 pill on the map tab.*
- **Clear the search field after marking a state found (list view).** `[playtest #1]` On
  *mark-as-found* (not unmark), clear the search box and dismiss the keyboard so the list
  re-expands (animate it), readying for the next spot. Announce "marked [state], search cleared" for
  screen readers. *Status: shipped 2026-06-04 (v1.4) — the list search clears after a mark.*
- **Make the region selectors more user-friendly.** The origin/destination **State** dropdowns on
  the New Trip screen (`NewTripScreen.kt`) feel clunky. `[playtest #7-selector]` Replace with a
  shared full-screen / sheet selector: a country filter chip row (All · 🇺🇸 · 🇨🇦 · 🇲🇽), a search box
  matching name/abbreviation/ISO code, rows showing flag + name + muted abbreviation, a pinned "
  recently used" section, and an optional "exclude" param so start and destination can't pick the
  same region. Build once, reuse for start, destination, stops, and home. `[playtest #9]` Also add
  per-field quick-clear (✕) icons (visible only when filled, subtle gray, labeled "Clear
  city/state"); clearing an auto-filled start should *not* re-fill from home. *Status: shipped — the
  shared searchable bottom-sheet `RegionPickerSheet` (search by name/abbreviation,
  exclude-the-other-endpoint) on New Trip, Manage trip, and the home dialog (v1.5, 2026-06-05);
  per-field quick-clear (#9) shipped 2026-06-04 (v1.4). Still open: country-filter chips +
  recently-used (deferred until multi-country data exists).*
- **State abbreviations on unfound states.** `[playtest #10]` Show each unfound state's USPS
  abbreviation (CA, NY, TX) at its visual center; on mark-as-found crossfade (~150–200ms) the
  abbreviation out and the check mark in at the same anchor (reverse on unmark). Scale font to the
  state's bounding box; for the small east-coast cluster (RI, DE, CT, NJ, MD, MA, NH, VT) use
  external labels with leader lines. Compute AK/HI label positions *after* the inset transform.
  Decorative only — screen readers still announce name + status. Tightly coupled to visual-center
  positioning `[playtest #5]` and the color palette `[playtest #6]`. *Status: shipped 2026-06-04 (
  v1.4) — USPS abbreviations on unfound states, crossfading to the check mark on find.*
- **Check-mark positioning on irregular state shapes.** `[playtest #5]` Place marks at each state's
  **visual center** (pole of inaccessibility, e.g. via `polylabel`), cached once in the map data,
  rather than the geometric centroid which fails on LA/MI/FL/ID. Scale the mark down (or use a dot)
  for tiny states (RI, DE, CT, NJ); allow per-state `{ code: [dx, dy] }` manual offset overrides.
  Test MI, HI, AK, LA, FL. Shared anchor for check marks, abbreviations `[playtest #10]`, and
  animation `[playtest #20]`. *Status: shipped 2026-06-04 (v1.4) — marks/labels sit at each state's
  visual center (pole of inaccessibility).*
- **Four-color base map (no same-color adjacent states).** `[playtest #6]` Precompute a 4-coloring
  of the 50 states (+DC if shown) at build time — build an adjacency list, run greedy graph coloring
  with backtracking, store as `{ code: colorIndex }`. Pick 4 colorblind-friendly, light/dark-safe
  muted colors (test deuteranopia/protanopia). This is the *base* unfound styling; found states keep
  their overlay treatment. AK/HI have no neighbors — color them for visual balance. *Status: shipped
  2026-06-08 (v1.7) — a 4-colored mosaic base (no two neighbors share a tint), in a pure,
  unit-tested `StateColorData`.*
- **Map view bottom stats strip.** `[playtest #21]` A tight (80–120pt) horizontally-scrolling row of
  stat cards under the map, each tappable to expand. v1 starter set: found count + percent (hero),
  last plate + how long ago, "Day X of Y", and a leader chip if multiplayer else
  closest-unfound-state if solo, plus a delight stat or two. Idea bank (recency/momentum,
  geographic, narrative, personality) captured in the original note. Gracefully hide stats that need
  more data (averages, projections) early in a trip. Some depend on systems not built yet (rarity,
  achievements). *Status: shipped 2026-06-04 (v1.4) — the at-a-glance stats strip under the map (
  found X/50, percent, last find + how long ago, day of trip, found today).*
- **Defer the state-found animation to the next map visit.** `[playtest #20]` When a state is marked
  from anywhere but the active map (e.g. the list), queue the celebration instead of firing it in
  the background; play queued ones in a satisfying batch on the next map visit. Add `celebrated` (
  bool / `animatedAt`) to the spotting; map-on-mount plays uncelebrated finds then flips the flag;
  real-time map marks set it immediately. Batches: 1 = standard, 2–5 = staggered cascade, 6+ =
  rapid "combo" with a "+N states!" overlay. Persist the queue across app close with a 24h expiry →
  silent mark + "Welcome back · N states added" toast. *Status: core shipped 2026-06-08 —
  nullable `spotting.celebrated_at` (DB v5 + migration test); off-map finds (list/State Detail)
  queue and animate on the next map visit, then get stamped celebrated so they never replay; on-map
  finds animate immediately via the same path. **Cascade + combo shipped 2026-06-10** — a 2–5
  staggered cascade (a spatial top-left→bottom-right sweep) and a 6+ fast combo with a "+N states!"
  overlay, driven by a pure, unit-tested `celebrationTiming` helper. Still open: the cross-restart
  "Welcome back · N states added" toast (+ ~24h silent expiry).*
- **Map/List tab icons on the active trip screen.** `[playtest #23]` Give the Map and List tabs
  stacked icon + label (Material Symbols `map` / `list`), outline when inactive, filled + accent
  color when active, ~150ms transition, ≥48dp targets, real accessible labels. Iconify any future
  tabs too (don't half-iconify). *Status: shipped 2026-06-04 (v1.4) — the Map/List tabs carry icon +
  label.*
- **Swipe-to-delete with undo (trip list).** `[playtest #15]` On swipe, animate the row out
  immediately and show a snackbar "Trip deleted · Undo" with a ~2s window; commit to storage after
  timeout/dismiss/background/navigation, restore from an in-memory buffer on undo. A second delete
  commits the first immediately. Light haptics on confirm/undo; "Trip deleted. Double-tap to undo."
  for TalkBack. *Status: shipped 2026-06-04 (v1.4) — swipe-to-delete with an in-place 3-second undo
  on the Trip List, via a shared component.*
- **Swipe-to-delete with undo (manage players).** `[playtest #16]` Same pattern as the trip list,
  refactored into a shared `SwipeToDeleteList` / undoable-delete component (gesture, 2s buffer,
  snackbar, restore, commit-on-background) configured per screen. Player specifics: allow deleting
  down to zero; keep the undo pattern even when a player has progress; snackbar "Player deleted ·
  Undo". *Status: shipped 2026-06-04 (v1.4) — the same shared swipe-to-delete + undo on the Players
  roster.*
- **Playful empty states.** Add friendly illustration + copy to the Trip List and Players empty
  screens (e.g. the van/road art) instead of plain text — reinforces the family-friendly feel.
  *Status: shipped 2026-06-08; illustration added 2026-06-08 — the Trips and Players empty screens
  use the shared `EmptyState` with the road-trip illustration (`ic_empty_roadtrip`) + copy. Done.*
- **More celebratory "completed trip" treatment** in the Trip List (SPEC §6 calls for special styling for 50/50 trips — gold border, star, etc.). Currently minimal. *Status: shipped 2026-06-08 — completed trips get distinct styling in the trip list.*
- **Map visual polish.** Theme the unfound-state/background colors to the new sunny palette (they're
  still hardcoded slate; note the four-color base `[playtest #6]` may supersede this); consider
  state labels when zoomed in `[playtest #10]`; double-check Alaska/Hawaii placement and tap-target
  sizes. *Status: shipped 2026-06-08 — map colors (states, outline, labels, route) now resolve from
  the Material color scheme instead of hardcoded slate. Four-color base `[playtest #6]` shipped (
  v1.7) and the AK/HI placement + tap-target audit is done. Still open: state labels when zoomed
  in.*
- **First-run hint / onboarding.** A one-time tip ("Tap a state on the map when you spot its
  plate!") for new users. *Status: shipped 2026-06-09 — dismissible map overlay, persisted in
  UiPreferences, auto-retired on the first find. Could later grow into a short first-launch
  carousel.*
- **Richer end-of-trip recap.** `[2026-06-09]` Expand the summary/celebration into a short "story":
  a timeline of finds, busiest day, first / last / rarest catch, biggest single-day streak, and
  per-player highlights — beyond the current stat strip. All derivable from existing spottings +
  timestamps. Pairs with the shareable image so the recap is shareable too. Keep it skimmable and
  celebratory, not a data dump. *Status: v1 shipped 2026-06-09 — a one-line narrative recap ("You
  spotted N states over <duration>, covering ~<distance>"), a **"Your journey"** timeline (flag
  chips
  in found order), and a **busiest day** highlight. Backed by `CelebrationStats.timeline` +
  `busiestDayText` (CelebrationRepository; repo test). Still open: per-player highlights, biggest
  single-day streak, and including the journey in the shared image.*
- **Fun facts on the State Detail (and on find).** `[2026-06-09]` The `PlateRegion` bird / motto /
  flower / `fun_facts` fields are bundled but lightly used; surface a kid-friendly fun fact when a
  state is marked (a quick reveal in the find flow) and make the State Detail's facts more playful.
  Purely a content/presentation enhancement over data already on device.

## Settings

- **Settings screen.** Light/dark/system theme toggle, sound on/off, haptics on/off. Ties together
  the now-fixed theme and the (pending) celebration sound. (Out of scope in MVP.) Also hosts the
  toggles introduced by playtest items below. *Status: shipped 2026-06-04 (v1.4) — a Settings screen
  with Light/Dark/System theme + a vibration (haptics) toggle, plus the Developer tools section and
  a **Sound** on/off toggle (shipped 2026-06-11 with the celebration sound).*
- **Default home location.** `[playtest #8]` A "Home location" row that opens the shared region
  selector `[playtest #7-selector]`, plus an "Unset" option. New trips auto-fill the start field
  from home as a *suggestion* (still editable; clearing it doesn't re-fill `[playtest #9]`).
  Optional separate "Use current location" on the trip screen via reverse geocoding behind a
  permission prompt. Store in user prefs, not trip data; make sure the selector's country filter
  includes the user's home country. *Status: shipped 2026-06-04 (v1.4) — a Home location row (region
  selector + Unset) that pre-fills the New Trip origin as an editable suggestion. Still open: the
  optional "Use current location" reverse-geocoding.*
- **Trip reminder notifications toggle.** `[playtest #13]` Let users disable overdue-trip reminders
  without revoking the OS-level notification permission. *Status: shipped 2026-06-04/05 — a "Trip
  reminders" Settings toggle.*

## Accessibility

- **TalkBack + large-font pass** on the tabbed Active Trip screen and the refreshed theme: verify content descriptions read well, the multi-color fills + check marks have adequate contrast, dynamic font scaling doesn't clip, and all touch targets are ≥48dp. New playtest surfaces to cover: search-cleared announcements `[#1]`, attribution/leaderboard semantics `[#17/#18]`, swatch labels `[#19]`, swipe-undo snackbars `[#15/#16]`, and on-map abbreviations as decorative-only `[#10]`.

## Quality & engineering

- **Automated tests.** Unit tests for repositories/ViewModels (one-active-trip rule, 50/50 fires
  once per trip, name validation/duplicates, trip-player add/remove) and a few Compose UI tests for
  the core flows (create trip → mark state → celebrate). New logic worth covering: date-derived trip
  status `[#12]`, four-color computation `[#6]`, attribution sets `[#17]`, and the undoable-delete
  buffer `[#15/#16]`. *Status: established 2026-06-05 (v1.5) and growing — a Robolectric JVM suite (
  repositories / ViewModels / pure domain) + Compose UI tests + Room migration tests; new features
  ship test-first. Ongoing as surfaces are added.*
- **Debug-only sample-data seeding.** *Status: shipped 2026-06-08; **expanded 2026-06-10**. A "
  Developer" section in Settings (gated on `BuildConfig.DEBUG`, stripped from release) with a "Seed
  sample data" button. The seeder now lives in `SampleDataSeeder` and builds a rich, varied dataset:
  a 6-player roster with distinct colors; three **completed** trips (including a full 50/50
  cross-country sweep that lights up the completed-map styling and fills the Passport);
  two **in-progress** trips (one **overdue**); and the **active** multi-stop trip — each with its
  own finds, mixed single/multi-player/unattributed credit, and **back-dated** dates/timestamps so
  durations, date ranges, Passport first-spotted dates, and the recap read like real history.
  Achievements are pre-evaluated so earned badges show immediately. A separate **"Wipe all data"**
  action (confirmed) clears all trips/players/progress while keeping the bundled regions, for a
  clean slate between runs. Robolectric-tested. Eases recovery after the intermittent
  uninstall/reinstall data wipes.*
- **Localization.** The app is now i18n-ready (all strings in `strings.xml`). Add at least one real translation (e.g. Spanish) to validate the setup, and confirm dates/numbers format per locale.
- **Schema cleanup + migration tests.** Eventually drop the unused `plate_image_path` column (needs
  a Room migration) and add migration tests, since the schema will keep evolving (stops array
  `[#11]`, attribution sets `[#17]`, player color `[#19]`, `celebrated` flag `[#20]` all touch it).

## Future / larger bets

- **Expand beyond the US 50.** The schema already keys on `country_code` + `region_code`, so adding DC, US territories, or other countries is mostly new rows + flag assets. `[playtest #7]` Concretely: Canada (13 provinces/territories) and Mexico (32), stored with ISO 3166-2 codes (`US-CA`, `CA-ON`, `MX-JAL`). *Open question — endpoints only or also findable on plates?* Endpoints-only is a small data addition; making them findable ripples into map geometry, the cross-border four-color scheme, the counter (combined vs per-country), and the summary. Recommend endpoints-first.
- **Additional road-trip games** (slug bug, alphabet game, etc.) via the existing `GameType` → `GameInstance` → `Spotting` hierarchy, which was designed for this.
- **Backup / restore (manual export/import) or cloud sync.** Currently fully offline with system
  backup disabled. Add an in-app **manual JSON export/import** so a family can save and restore
  their trip history — and recover from the occasional dev/reinstall data wipe (`[2026-06-09]`, a
  real user feature, not just a dev aid). Export all trips/players/spottings to a shareable file;
  import merges or replaces behind a confirm, validating the schema/version. A lighter step before
  any optional account-based **cloud sync** across devices.

## Cross-cutting concerns

Themes that span multiple items — build the shared piece once:

- **Shared region selector** (`[playtest #7, #8, #11]`): the full-screen/sheet region picker is used for trip start, destination, stops, and home. One component, with a callback and an optional "exclude" param.
- **Shared swipe-to-delete-with-undo** (`[playtest #15, #16]`): one component/hook (gesture, 2s buffer, snackbar, restore-from-buffer, commit-on-background), configured per screen.
- **Player color system** (`[playtest #17, #18, #19]`): palette tokens resolved via the theme, propagated through summary chips, attribution indicators, player rows, and celebration accents — centralize in `getPlayerColor(playerId)`.
- **Visual-center positions for states** (`[playtest #5, #10, #20]`): one computed pole-of-inaccessibility position per state, reused for check marks, abbreviations, and animation anchors.
- **Celebration / animation queue** (`[playtest #2, #18, #20]`): the deferred-celebration pattern could generalize so counter updates and leader changes also defer to the next view.
- **Multi-country support** (`[playtest #7, #11]`): if CA/MX become findable, effects ripple across
  counters, map geometry, summary, and stats.
