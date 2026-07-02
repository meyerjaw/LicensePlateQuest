# Android XR (experimental)

Status: **experimental side project** on branch `experiment/android-xr`. Not part of the shipping
MVP and intentionally not tracked in `SPEC.md`. This file is the spec/record for the XR exploration.

Goal: let License Plate Quest run on Android XR headsets (e.g. in Full Space) by reusing the
existing
2D Compose UI as a spatial panel — no separate XR app, no forked screens.

## SDK

- Jetpack Compose for XR: `androidx.xr.compose:compose:1.0.0-alpha12`
  (version catalog: `xrCompose`; `libs.androidx.xr.compose`).
- Alpha API — signatures shift between releases; expect occasional adjustments on SDK bumps.

## What's implemented

1. **Spatialized app shell** — `MainActivity`
    - The whole shell (onboarding or `AppRoot`) is one composable, reused in both presentations.
    - When `LocalSpatialCapabilities.current.isSpatialUiEnabled`, it's wrapped in
      `Subspace { SpatialPanel(1024×768) { … } }`; otherwise it renders flat exactly as before.
    - `AndroidManifest.xml`:
      `PROPERTY_XR_ACTIVITY_START_MODE = XR_ACTIVITY_START_MODE_FULL_SPACE_MANAGED`
      so it launches into Full Space on a headset (ignored on phones/tablets).

2. **Spatial state picker** — `ui/components/RegionPicker.kt`
    - Problem: window-hosted overlays (`ModalBottomSheet`, `Dialog`, `Popup`, `DropdownMenu`) don't
      render/receive input inside a `SpatialPanel`.
    - Fix: `RegionPickerSheet` branches on `isSpatialUiEnabled` — `SpatialDialog` (a real spatial
      surface, in a sized `Surface`) in spatial mode, `ModalBottomSheet` otherwise. Both share the
      extracted `RegionPickerContent`, so there's one picker body, two containers.

3. **Curved map cockpit** — `MainActivity` + `ui/xr/XrMapPanel.kt`
    - In spatial mode the single panel is replaced by a `SpatialCurvedRow(curveRadius = 1400.dp)`
      with
      two `SpatialPanel`s: the interactive app shell (820×720) and a big dedicated map panel
      (1100×720) curving alongside.
    - `XrMapPanel` loads the bundled `UsMapShapes` and renders the in-app `UsMap` (display-only,
      `interactive = false`) filled with the active trip's found codes
      (`observeFoundCodesForActiveTrip`), so it updates live as plates are marked.

4. **Trophy shelf** — `MainActivity` + `ui/xr/XrTrophyShelf.kt`
    - The cockpit is wrapped in a `SpatialColumn`; below it, `XrTrophyShelf` floats each **earned**
      achievement as its own small `SpatialPanel` (badge icon + title), in a grid of 4 per row,
      driven by `achievementRepository.observeEarned()`. Renders nothing when none are earned.
    - **Panels, not meshes.** Upgrade path: swap each `SpatialPanel` for a real glTF trophy mesh
      loaded via **SceneCore** (`androidx.xr.scenecore`, a `.glb` in `assets/`, placed through a
      `Volume`/SceneCore entity) once a model exists — the data + grid layout here stay the same.

5. **Spatial confetti** — `MainActivity` + `ui/xr/XrCelebrationOverlay.kt`
    - The whole spatial scene is wrapped in a `SpatialBox`; a transparent `SpatialPanel` (1600×1100)
      pulled toward the viewer (`offset(z = 250.dp)`) renders the in-app `Confetti` Canvas so the
      burst rains in FRONT of the cockpit panels (coplanar, it was occluded by the opaque panels).
      `Confetti` gained a `particleScale` knob (default 1; the XR overlay uses 5× + 320 particles +
      2.8s) so particles stay big/dense on a large surface. (If the burst shows behind the windows,
      flip the z-offset sign.)
    - Trigger: collect the active trip's found set **directly** and bump the counter only when it
      grows
      *after* the first (baseline) emission — so it never fires on app load (the artificial
      `collectAsState` emptySet would otherwise read as growth). Reuses the existing `Confetti`.
    - The panel is only **mounted during the burst** (`confettiActive`, ~2.9s): an XR panel captures
      input across its whole quad, so leaving it up permanently blocked taps on the app behind it.
    - Future tweak: distinct bursts for 50/50 vs rare plates (currently any find fires it), and a
      true
      3D particle system via SceneCore.

## The pattern (reuse this)

For any window-hosted overlay, keep the normal Compose component and swap only the host:

```kotlin
if (LocalSpatialCapabilities.current.isSpatialUiEnabled) {
    SpatialDialog(onDismissRequest = onDismiss) {
        Surface(/* shape + size */) { SharedContent() }
    }
} else {
    ModalBottomSheet(onDismissRequest = onDismiss) { SharedContent() }
}
```

## Remaining / ideas

- **Other window-hosted overlays** to route through `SpatialDialog`/`SpatialPopup` the same way:
    - ~~Achievement detail sheet (`ModalBottomSheet` in `PassportScreen`).~~ **Done** — branches to
      `SpatialDialog`, sharing `AchievementDetailContent` (same recipe as the region picker).
    - Any `AlertDialog`/`Dialog` (e.g. the end-trip prompt, home-location dialog).
    - The celebration screen, if it ever uses a popup/sheet.
- **Spatial enhancements** — see the **Ideas backlog** below.
- **Verify on device/emulator:** panel sizing (1024×768 is a guess), `SpatialDialog` size/elevation,
  and whether any alpha-API signatures need updating on the next SDK bump.
- **Decision deferred:** whether XR ever graduates from experiment → supported (would then enter
  `SPEC.md` and need its own QA matrix).

## Ideas backlog

Effort: 🟢 Jetpack XR Compose only · 🟡 needs SceneCore + glTF/environment asset · 🔴 ambitious /
uncertain on alpha SDK.

Spatial layout (rearrange existing UI):

- 🟢 ~~**Curved map panel** — pull the US map into a large curved `SpatialPanel`, controls on a side
  panel.~~ **Done** (curved cockpit; see "What's implemented" #3).
- 🟢 **Cockpit layout** — map + found/list + leaderboard panels arranged in an arc, like a road-trip
  dashboard.
- 🟢 **Orbiter controls** — float the tab bar / trip switcher as an `Orbiter` so it's always
  reachable.
- 🟢 **Spatialize remaining sheets** — achievement detail + dialogs via the `SpatialDialog` branch (
  parity cleanup).

3D objects (collection as physical things):

- 🟢 ~~**Trophy shelf** — earned achievements floating on a shelf.~~ **Done** as spatial-panel
  tiles (see "What's implemented" #4); 🟡 glTF-mesh upgrade still open.
- 🟡 **Floating 3D license plates** — each found state as a 3D plate you can grab and inspect.
- 🟡 **3D car on the route** — a model car that advances along the trip route as you log states.
- 🔴 **3D relief map** — the US extruded in 3D, found states raised/glowing; tabletop version to lean
  over.

Immersion & celebration:

- 🟡 **Themed environment / skybox** — open-highway or national-park backdrop in Full Space.
- 🟡 **Spatial confetti / fireworks** — 50/50 and rare-plate celebrations burst into the room.
- 🟡 **Spatial audio** — the find chime / rare sparkle comes from where the state sits on the map.

XR-native interaction:

- 🔴 **Gaze + pinch to mark** — look at a state on the big map and pinch to mark it found.
- 🔴 **Reach out and grab** — direct hand-touch the map states / floating plates.

Social (speculative):

- 🔴 **Shared space** — family in one XR session marking plates on a shared map (needs platform
  multiuser).

## How to run

Android Studio → Device Manager → create an **Android XR** emulator → run the
`experiment/android-xr`
branch. On a normal phone/tablet emulator the app runs flat and unchanged.
