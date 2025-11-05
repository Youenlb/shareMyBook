package fr.enssat.sharemybook.mitosbooking

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.AndroidEntryPoint
import fr.enssat.sharemybook.mitosbooking.data.remote.ReturnQrCode
import fr.enssat.sharemybook.mitosbooking.data.remote.ShareIdQrCode
import java.util.concurrent.Executors

@AndroidEntryPoint
class ScannerActivity : ComponentActivity() {

    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            setContent {
                CameraPreview { barcode, isQrCode ->
                    if (isQrCode) {
                        handleQrCodeScan(barcode)
                    } else {
                        val resultIntent = Intent().apply {
                            putExtra("scanned_barcode", barcode)
                        }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    }
                }
            }
        } else {
            Toast.makeText(this, "La permission de la caméra est requise", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestCameraPermission()
    }

    private fun requestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                setContent {
                    CameraPreview { barcode, isQrCode ->
                        if (isQrCode) {
                            handleQrCodeScan(barcode)
                        } else {
                            val resultIntent = Intent().apply {
                                putExtra("scanned_barcode", barcode)
                            }
                            setResult(RESULT_OK, resultIntent)
                            finish()
                        }
                    }
                }
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun handleQrCodeScan(qrCodeContent: String) {
        val gson = Gson()
        try {
            // Try to parse as ShareIdQrCode (for both loan and return)
            val shareIdQrCode = gson.fromJson(qrCodeContent, ShareIdQrCode::class.java)
            if (shareIdQrCode?.shareId != null) {
                val intent = Intent(this, AcceptTransactionActivity::class.java).apply {
                    putExtra("shareId", shareIdQrCode.shareId)
                }
                startActivity(intent)
                finish()
                return
            }
        } catch (e: Exception) {
            Log.d("ScannerActivity", "Not a ShareIdQrCode: ${e.localizedMessage}")
        }

        Toast.makeText(this, "QR Code non reconnu ou format invalide.", Toast.LENGTH_SHORT).show()
        setResult(RESULT_CANCELED)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

@Composable
fun CameraPreview(onBarcodeScanned: (String, Boolean) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember { PreviewView(context) }
    val barcodeAnalyzer = remember { BarcodeAnalyzer(onBarcodeScanned) }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize(),
        update = {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(ContextCompat.getMainExecutor(context), barcodeAnalyzer)
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e("ScannerActivity", "Use case binding failed", exc)
            }
        },
        onRelease = {
            barcodeAnalyzer.close()
        }
    )
}

class BarcodeAnalyzer(private val onBarcodeScanned: (String, Boolean) -> Unit) : ImageAnalysis.Analyzer {

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_EAN_13
        )
        .build()

    private val scanner = BarcodeScanning.getClient(options)
    @Volatile
    private var isScanning = true

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null && isScanning) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (isScanning && barcodes.isNotEmpty()) {
                        for (barcode in barcodes) {
                            val rawValue = barcode.rawValue
                            if (rawValue != null) {
                                isScanning = false
                                onBarcodeScanned(rawValue, barcode.format == Barcode.FORMAT_QR_CODE)
                                return@addOnSuccessListener
                            }
                        }
                    }
                }
                .addOnFailureListener { 
                    if (isScanning) {
                        Log.e("BarcodeAnalyzer", "Barcode scanning failed", it)
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    fun close() {
        isScanning = false
        scanner.close()
    }
}
