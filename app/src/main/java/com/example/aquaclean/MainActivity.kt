package com.example.aquaclean

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        setContent {
            Scaffold { innerPadding ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    MyApp(cameraExecutor)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MyApp(cameraExecutor: ExecutorService) {
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    val context = LocalContext.current
    var result by remember { mutableStateOf("") }
    val tfliteHelper = remember { TFLiteHelper(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    var elapsedTime by remember { mutableStateOf("00:00") }
    val timerHandler = remember { Handler() }
    var startTime by remember { mutableStateOf(0L) }

    if (cameraPermissionState.status.isGranted) {
        val previewView = remember { PreviewView(context) }
        AndroidView(factory = { previewView })

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = elapsedTime, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = {
                // Start the timer
                startTime = System.currentTimeMillis()
                timerHandler.post(object : Runnable {
                    override fun run() {
                        val seconds = (System.currentTimeMillis() - startTime) / 1000
                        elapsedTime = String.format("%02d:%02d", seconds / 60, seconds % 60)
                        timerHandler.postDelayed(this, 1000)
                    }
                })

                imageCapture.takePicture(cameraExecutor, object : ImageCapture.OnImageCapturedCallback() {
                    @RequiresApi(Build.VERSION_CODES.P)
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val bitmap = imageProxyToBitmap(image)
                        result = bitmap?.let { tfliteHelper.classifyImage(it) }.toString()

                        // Stop the timer
                        timerHandler.removeCallbacksAndMessages(null)

                        // Close the image to free up resources
                        image.close()
                    }

                    override fun onError(exception: ImageCaptureException) {
                        result = "Image capture failed: ${exception.message}"

                        // Stop the timer
                        timerHandler.removeCallbacksAndMessages(null)
                    }
                })
            }) {
                Text("Capture Image")
            }

            Text(text = result, style = MaterialTheme.typography.bodyLarge)
        }

        LaunchedEffect(Unit) {
            val cameraProvider = ProcessCameraProvider.getInstance(context).get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(context as LifecycleOwner, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                // Handle exceptions
            }
        }
    } else {
        LaunchedEffect(Unit) { cameraPermissionState.launchPermissionRequest() }
    }
}

@RequiresApi(Build.VERSION_CODES.KITKAT)
private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    val planeProxy = image.planes[0]
    val buffer: ByteBuffer = planeProxy.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
}

fun createFile(context: Context): File {
    val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
        File(it, context.getString(R.string.app_name)).apply { mkdirs() }
    }
    return File(mediaDir, "${System.currentTimeMillis()}.jpg")
}
