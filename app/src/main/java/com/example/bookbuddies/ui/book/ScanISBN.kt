package com.example.bookbuddies.ui.book

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.bookbuddies.navigation.NavigationActions
import com.example.bookbuddies.navigation.Route
import com.example.bookbuddies.system.CameraPermissionStatus
import com.example.bookbuddies.system.cameraPermission
import com.example.bookbuddies.system.checkPermission
import com.example.bookbuddies.ui.LoadingPage

import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.common.InputImage


@OptIn(ExperimentalGetImage::class)
@Composable
fun ScanISBN( navigationActions: NavigationActions) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasScanned by remember { mutableStateOf(false) }

    // camera and permission
    var permissionState by remember { mutableStateOf(CameraPermissionStatus.CHECKING) }

    val requestCameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()) { isGranted ->
        permissionState = if (isGranted) {
            CameraPermissionStatus.GRANTED
        } else {
            CameraPermissionStatus.DENIED
        }
    }

    // on composition, first check permission
    LaunchedEffect(Unit) {
        checkPermission(context, cameraPermission, requestCameraPermissionLauncher) {
           permissionState = CameraPermissionStatus.GRANTED
        }
    }

    when (permissionState) {
        CameraPermissionStatus.CHECKING -> {
            LoadingPage()
        }
        CameraPermissionStatus.GRANTED -> {
            // scanning
            val scanner = remember { BarcodeScanning.getClient(
                BarcodeScannerOptions.Builder().setBarcodeFormats(
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                ).build()
            ) }

            // CameraX and ML Kit integration
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    // display live camera preview
                    val previewView = PreviewView(ctx)

                    // async get camera access
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    // listen to when camera is ready
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        // display camera preview
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        // use ML kit to analyse camera frames
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        analysis.setAnalyzer(
                            ContextCompat.getMainExecutor(ctx)
                        ) { imageProxy ->
                            // to avoid scanning same barcode multiple times
                            if (hasScanned) {
                                imageProxy.close()
                                return@setAnalyzer
                            }

                            // convert CameraX frame to ML Kit image object
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val inputImage = InputImage.fromMediaImage(
                                    mediaImage, imageProxy.imageInfo.rotationDegrees
                                )

                                // use scanner to detect EAN barcodes on image
                                scanner.process(inputImage)
                                    .addOnSuccessListener { barcodes ->
                                        // get EAN-13 or EAN-8 barcode value
                                        val isbn = barcodes.mapNotNull { it.rawValue }
                                            .firstOrNull { value -> value.length == 13 || value.length == 8 }

                                        // successfully get barcode -> stop scanning and navigate to BookEdit
                                        if (!isbn.isNullOrBlank()) {
                                            hasScanned = true
                                            navigationActions.navigateTo("${Route.BOOK_CREATE}?isbn=${isbn}")
                                        }
                                        imageProxy.close()
                                    }
                                    .addOnFailureListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }

                        // bind camera to lifecycle so that it opens and closes properly
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis
                        )
                    }, ContextCompat.getMainExecutor(ctx))

                    // return view
                    previewView
                }
            )
        }
        CameraPermissionStatus.DENIED -> {
            navigationActions.goBack()
        }
    }
}