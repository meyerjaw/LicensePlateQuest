# Testing & TDD

This project uses fast JVM unit tests (Robolectric, no emulator) plus instrumented Compose UI
tests. Going forward we work test-first: write a failing test for new logic, make it pass, refactor.

## Layout

| Location | Runs on | Use for |
| --- | --- | --- |
| `app/src/test/` | JVM (`./gradlew testDebugUnitTest`) | Pure logic, repositories + ViewModels via Robolectric + in-memory Room |
| `app/src/androidTest/` | Device/emulator | Compose UI flows, anything needing a real device |

Run the unit suite:

```
./gradlew testDebugUnitTest      # or: ./gradlew test
```

## How the layers are tested

- **Pure domain** — plain JUnit, no setup. Examples: `TripListItemTest`, `TripStatusTest`,
  `TripFormValidationTest` (derived `UiState` properties).
- **Repositories** — `@RunWith(RobolectricTestRunner::class)` with an in-memory `AppDatabase`
  (`Room.inMemoryDatabaseBuilder(...).allowMainThreadQueries()`). Seed `plate_region` rows before
  creating trips (FK). See `TripRepositoryTest`.
- **ViewModels** — same Robolectric + in-memory Room setup, plus `MainDispatcherRule` (test util)
  so `viewModelScope` works. Drive the synchronous handlers and assert on `uiState.value`. See
  `NewTripViewModelTest`.

## Compose UI tests (instrumented)

These live in `app/src/androidTest/` and run on an emulator or device:

```
./gradlew connectedDebugAndroidTest      # or run from the gutter with a device attached
```

Patterns in place:

- **Pure component** — host the composable directly and assert on rendered nodes. See
  `RegionPickerSheetTest` (search filtering, exclude, selection callback).
- **Screen + real ViewModel** — build an in-memory `AppDatabase` from the instrumentation
  context, construct the real ViewModel (using `NoopReminderScheduler`), and drive the UI. See
  `NewTripScreenTest`. `createComposeRule()` hosts on a `ComponentActivity`, so screens that
  register an activity-result launcher (the notification permission) compose fine.

Still to add: the full create-trip → mark-state → celebrate navigation flow (the most involved;
left until the harness above is proven on a device).

## Test doubles

- `FakeReminderScheduler` implements the `ReminderScheduler` interface in-memory (records
  schedule/cancel calls, no WorkManager). Pass it to `TripRepository` in tests. The interface seam
  is the pattern to follow: when production code needs an Android/system dependency that's awkward
  in tests, extract a small interface and provide a fake.

## Robolectric SDK pin

`app/src/test/resources/robolectric.properties` pins the emulated SDK to **34**. Robolectric 4.16
supports SDK 36, but SDK 36 requires JDK 21; the project's toolchain is JDK 17, so 34 keeps unit
tests on JDK 17. Revisit if the toolchain moves to JDK 21.

## TDD workflow for new work

1. **Red** — add a failing test that pins the new behavior (a `UiState` rule, a repository
   invariant, a ViewModel transition).
2. **Green** — implement the smallest change to pass it.
3. **Refactor** — clean up with the test as a safety net.

Prefer pushing logic into pure/derived properties (like `TripListItem.isOverdue`) so it can be
tested without Robolectric. Reach for the Robolectric layer when Room or a `Context` is genuinely
involved.
