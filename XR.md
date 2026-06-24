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
    - Achievement detail sheet (`ModalBottomSheet` in `PassportScreen`).
    - Any `AlertDialog`/`Dialog` (e.g. the end-trip prompt, home-location dialog).
    - The celebration screen, if it ever uses a popup/sheet.
- **Spatial enhancements to play with** (beyond parity):
    - `Orbiter` floating controls (e.g. a floating tab bar or trip switcher).
    - Break the US map out into its own elevated/curved `SpatialPanel`.
    - Multiple panels (map + list side by side).
- **Verify on device/emulator:** panel sizing (1024×768 is a guess), `SpatialDialog` size/elevation,
  and whether any alpha-API signatures need updating on the next SDK bump.
- **Decision deferred:** whether XR ever graduates from experiment → supported (would then enter
  `SPEC.md` and need its own QA matrix).

## How to run

Android Studio → Device Manager → create an **Android XR** emulator → run the
`experiment/android-xr`
branch. On a normal phone/tablet emulator the app runs flat and unchanged.
