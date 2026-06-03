# Backlog

Ideas and improvements captured for later — not yet scheduled. Roughly priority-ordered within each section.

## Features

- **Celebration sound (deferred from MVP).** Add a short, playful celebration sound (Android `SoundPool`) for per-find and the 50/50 moment, respecting silent mode and media volume. The firework + haptics are already in place; this is the missing half of the celebration. (Explicitly deferred during build.)
- **Per-player attribution.** `Spotting.spotter_player_id` is reserved but always null today. Record who spotted each plate, then show per-player tallies and a "who found the most" stat on the celebration screen. Big engagement win for families with kids competing.
- **Trip details / edit trip.** The trip name is currently read-only ("tap to view details — read-only in MVP"). Add a screen to rename a trip and edit origin/destination/start date after creation.
- **Share a finished trip.** Export a shareable image of the colorful filled-in map plus the celebration stats. (Currently out of scope.)

## UX & polish

- **Make the state dropdowns more user-friendly.** The origin/destination **State** selectors on the New Trip screen (`NewTripScreen.kt`, fed by `regionRepository` region options) are plain dropdowns and feel clunky. Improve usability — type-to-search/filter, jump-by-first-letter, showing the state flag next to each name, and/or a more polished picker. (Added 2026-06-01.)
- **Playful empty states.** Add friendly illustration + copy to the Trip List and Players empty screens (e.g. the van/road art) instead of plain text — reinforces the family-friendly feel.
- **More celebratory "completed trip" treatment** in the Trip List (SPEC §6 calls for special styling for 50/50 trips — gold border, star, etc.). Currently minimal.
- **Map visual polish.** Theme the unfound-state/background colors to the new sunny palette (they're still hardcoded slate); consider state labels when zoomed in; double-check Alaska/Hawaii placement and tap-target sizes.
- **First-run hint / onboarding.** A one-time tip ("Tap a state on the map when you spot its plate!") for new users.

## Settings

- **Settings screen.** Light/dark/system theme toggle, sound on/off, haptics on/off. Ties together the now-fixed theme and the (pending) celebration sound. (Out of scope in MVP.)

## Accessibility

- **TalkBack + large-font pass** on the new tabbed Active Trip screen and the refreshed theme: verify content descriptions read well, the multi-color fills + check marks have adequate contrast, dynamic font scaling doesn't clip, and all touch targets are ≥48dp.

## Quality & engineering

- **Automated tests.** Unit tests for repositories/ViewModels (one-active-trip rule, 50/50 fires once per trip, name validation/duplicates, trip-player add/remove) and a few Compose UI tests for the core flows (create trip → mark state → celebrate).
- **Localization.** The app is now i18n-ready (all strings in `strings.xml`). Add at least one real translation (e.g. Spanish) to validate the setup, and confirm dates/numbers format per locale.
- **Schema cleanup + migration tests.** Eventually drop the unused `plate_image_path` column (needs a Room migration) and add migration tests, since the schema will keep evolving.

## Future / larger bets

- **Expand beyond the US 50.** The schema already keys on `country_code` + `region_code`, so adding DC, US territories, or other countries is mostly new rows + flag assets.
- **Additional road-trip games** (slug bug, alphabet game, etc.) via the existing `GameType` → `GameInstance` → `Spotting` hierarchy, which was designed for this.
- **Backup / restore or cloud sync.** Currently fully offline with backup disabled; a manual export/import or optional sync would protect trip history across devices.
