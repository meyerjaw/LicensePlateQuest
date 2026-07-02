# Camera plate → state recognition (plan / spike)

Status: **exploratory plan, not built.** A new *input modality* that auto-marks states, feeding the
existing domain (recognition → `SpottingRepository.markState(code)`). Phone-first; a future Android
XR
glasses HUD would be a thin front-end on the same engine.

## Goal

Point the phone camera at a passing car → recognize the plate's **issuing state** (not the number) →
glanceably confirm and mark it found.

## Principles

- **State only.** Never read/store the plate number or the image — only the resulting `stateCode`
  (+ timestamp) lands in the DB, exactly like a manual mark.
- **On-device first.** Offline, free, real-time (ML Kit). Gemini is an optional, opt-in fallback for
  the hard cases — never the primary/continuous engine.
- **Passenger-only.** Framed for the navigator, not the driver (fits the players/attribution model).
- **Reuse the domain.** Recognition ends at `markState`; no gameplay changes.

## Architecture — one seam

`PlateRecognizer` interface (same pattern as `ReminderScheduler` / `CityLocator`):

```kotlin
interface PlateRecognizer {
    suspend fun recognize(frame: PlateImage): PlateResult?   // null = no confident match
}
data class PlateResult(val stateCode: String, val confidence: Float, val source: Source)
enum class Source { ON_DEVICE, CLOUD }
```

Implementations:

- `MlKitPlateRecognizer` (default) — ML Kit text recognition → match text to a state, on-device.
- `GeminiPlateRecognizer` (optional) — Firebase AI Logic one-shot for low-confidence frames.
- `CompositePlateRecognizer` — try on-device; escalate to cloud only when
  `confidence < threshold && online && userOptedIn`.

The seam keeps the ViewModel/UI oblivious to which engine ran, and makes it fake-able in tests.

## On-device pipeline (the core)

1. **CameraX** `ImageAnalysis` use case → frames, throttled (~2–4 fps; we don't need every frame).
2. **ML Kit Text Recognition v2** → text blocks + positions.
3. **Matcher** (pure, unit-testable): normalize tokens and fuzzy-match against a bundled dictionary
   of
   the 50 states —
    - full names (`OHIO`), 2-letter codes, and **state slogans/mottos** printed on plates
      (`SUNSHINE STATE`→FL, `EMPIRE STATE`→NY, `GARDEN STATE`→NJ, `GRAND CANYON STATE`→AZ, …).
    - score by best match; require a minimum score to accept. (Slogan data can be bundled or derived
      from the existing `PlateRegion` fields.)
4. **Stabilize** — require the same state across N consecutive frames / a short window before
   firing,
   to kill flicker and one-frame false positives.
5. On accept → `PlateResult` → confirm/auto-mark → `markState`.

## UX (phone)

- A **Scan** entry (FAB on the Active Trip map, or a small camera tab).
- Live preview with a subtle reticle; on a hit, a chip: **"New! Ohio — add"** (or auto-add with
  undo,
  gated by a setting).
- Credit the scanning **passenger** (attribution). Reuse the existing find haptic/sound/celebration.
- **CAMERA** runtime permission + a pre-permission primer (reuse the notification-primer pattern).

## Privacy & safety

- No image or plate-number persistence; frames processed in memory and discarded.
- Settings: enable camera scanning (off by default); enable **cloud fallback** (off by default).
- In-app note about what's captured. Explicitly not a driver tool.

## Gemini fallback (phase 3, optional)

- Firebase AI Logic SDK; **one-shot** multimodal call on the cropped plate region:
  *"Which US state issued this plate? Reply with the 2-letter code or UNKNOWN."*
- Only when on-device is unsure, connectivity exists, and the user opted in. Rate-limit + cost
  guard;
  never a continuous stream.

## Dependencies

- CameraX (`androidx.camera:camera-core/camera2/lifecycle/view`).
- ML Kit text recognition (`com.google.mlkit:text-recognition`), on-device.
- (Phase 3) Firebase AI Logic SDK + a Firebase project.

## Testing

- **Matcher** unit tests (token/slogan/fuzzy → state) — pure JVM, no device.
- Fake `PlateRecognizer` for ViewModel tests.
- Offline harness: a folder of sample plate photos → run the matcher, measure hit rate.

## Phasing

- **Phase 0 — spike (go/no-go):** CameraX preview + ML Kit OCR + matcher, just *log* the recognized
  state (no DB writes). Answers the one real question: **does reading the state name off real plates
  actually work** at distance/angle/motion?
- **Phase 1:** wire recognition → confirm chip → `markState`; CAMERA permission + primer; settings
  toggle; matcher tests.
- **Phase 2:** stabilization/debounce polish; attribution; auto-mark with undo.
- **Phase 3 (optional):** Gemini fallback behind a flag.
- **Phase 4 (later, gated):** Android XR glasses HUD + voice on the same engine (needs Catalyst
  hardware / glasses dev access).

## Open questions / risks

- **On-device OCR accuracy** at distance/angle/motion/glare — the main risk; **Phase 0 answers it.**
- Specialty/vanity plates and plates where the state name isn't legibly printed — the Gemini niche.
- If OCR proves too weak, the fallback is a **visual state classifier** (custom TFLite on plate
  crops)
  — bigger effort (labeled dataset), deferred unless needed.
