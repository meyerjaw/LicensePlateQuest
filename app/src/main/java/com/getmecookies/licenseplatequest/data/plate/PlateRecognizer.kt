package com.getmecookies.licenseplatequest.data.plate

import com.getmecookies.licenseplatequest.domain.plate.PlateMatch
import com.getmecookies.licenseplatequest.domain.plate.PlateStateMatcher
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Recognizes the **issuing state** of a license plate from a camera frame. The seam (like
 * `ReminderScheduler` / `CityLocator`) lets the UI stay oblivious to the engine and be faked in
 * tests. On-device [MlKitPlateRecognizer] is the default; a Gemini fallback can implement this later.
 * Never reads or returns the plate number — only a [PlateMatch].
 */
interface PlateRecognizer {
    /** OCR [image] and return the best state match, or null if nothing is confident. */
    suspend fun recognize(image: InputImage): PlateMatch?
}

/**
 * On-device recognizer: ML Kit Latin text recognition → [PlateStateMatcher]. Offline, private (the
 * frame never leaves the device), and free — the default engine.
 */
class MlKitPlateRecognizer : PlateRecognizer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognize(image: InputImage): PlateMatch? {
        val text = recognizeText(image) ?: return null
        val lines = text.textBlocks.flatMap { block -> block.lines }.map { it.text }
        return PlateStateMatcher.match(lines)
    }

    private suspend fun recognizeText(image: InputImage): Text? =
        suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { result -> if (cont.isActive) cont.resume(result) }
                .addOnFailureListener { if (cont.isActive) cont.resume(null) }
        }
}
