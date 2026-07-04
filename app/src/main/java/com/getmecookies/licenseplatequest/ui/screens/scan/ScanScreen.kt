package com.getmecookies.licenseplatequest.ui.screens.scan

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.getmecookies.licenseplatequest.data.plate.MlKitPlateRecognizer
import com.getmecookies.licenseplatequest.data.plate.PlateRecognizer
import com.getmecookies.licenseplatequest.domain.plate.PlateMatch
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val TAG = "PlateScan"

/**
 * **Experimental Phase-0 spike** for camera plate → state recognition (see PLATE_RECOGNITION.md).
 * Shows the live camera, runs [PlateRecognizer] on frames, and just *displays + logs* the recognized
 * state — no DB writes, no marking. The point is to answer "does reading the state off real plates
 * work?" before building the real flow. Device-only; can't be exercised in JVM/Robolectric tests.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onBack: () -> Unit,
    recognizer: PlateRecognizer = remember { MlKitPlateRecognizer() },
) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasCameraPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted = it }

    var lastMatch by remember { mutableStateOf<PlateMatch?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan a plate (experimental)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (granted) {
                CameraPreview(
                    recognizer = recognizer,
                    onMatch = { match ->
                        lastMatch = match
                        Log.d(
                            TAG,
                            "Recognized ${match.stateCode} (${match.confidence}) via '${match.matchedPhrase}'"
                        )
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                // Aiming reticle.
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.8f)
                        .padding(vertical = 120.dp)
                        .border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(40.dp)
                    )
                }

                // Latest recognition, bottom-center. Display-only in the spike.
                lastMatch?.let { match ->
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(24.dp),
                    ) {
                        Text(
                            text = "${match.stateCode} · ${(match.confidence * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        )
                    }
                }
            } else {
                PermissionRationale(
                    onGrant = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun CameraPreview(
    recognizer: PlateRecognizer,
    onMatch: (PlateMatch) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val previewView = remember { PreviewView(context) }

    AndroidView(factory = { previewView }, modifier = modifier) {
        scope.launch {
            val provider = context.awaitCameraProvider()
            val preview = Preview.Builder().build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(
                        ContextCompat.getMainExecutor(context),
                        PlateAnalyzer(recognizer, scope) { match -> match?.let(onMatch) },
                    )
                }
            runCatching {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis,
                )
            }.onFailure { Log.e(TAG, "Camera bind failed", it) }
        }
    }
}

@Composable
private fun PermissionRationale(onGrant: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Camera access is needed to spot license-plate states.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(onClick = onGrant) { Text("Grant camera") }
    }
}

/**
 * Runs the recognizer on frames, one at a time (skips frames while busy — a natural throttle). Uses
 * the [PlateRecognizer] seam so the engine stays swappable. Always closes the [ImageProxy].
 */
private class PlateAnalyzer(
    private val recognizer: PlateRecognizer,
    private val scope: CoroutineScope,
    private val onResult: (PlateMatch?) -> Unit,
) : ImageAnalysis.Analyzer {

    private val busy = AtomicBoolean(false)

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val media = imageProxy.image
        if (media == null || !busy.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }
        val input = InputImage.fromMediaImage(media, imageProxy.imageInfo.rotationDegrees)
        scope.launch {
            try {
                onResult(recognizer.recognize(input))
            } catch (e: Exception) {
                Log.e(TAG, "recognize failed", e)
            } finally {
                imageProxy.close()
                busy.set(false)
            }
        }
    }
}

private fun hasCameraPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

private suspend fun Context.awaitCameraProvider(): ProcessCameraProvider =
    suspendCoroutine { cont ->
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({ cont.resume(future.get()) }, ContextCompat.getMainExecutor(this))
    }
