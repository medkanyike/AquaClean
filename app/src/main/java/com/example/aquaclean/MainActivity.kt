package com.example.aquaclean

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.aquaclean.ui.theme.AquaCleanTheme



import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        setContent {
            Scaffold {
                    innerPadding ->
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

    if (cameraPermissionState.status.isGranted) {
        val previewView = remember { PreviewView(context) }
        AndroidView(factory = { previewView }, )

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Button(onClick = {
                val outputOptions = ImageCapture.OutputFileOptions.Builder(createFile(context)).build()
                imageCapture.takePicture(outputOptions, cameraExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        @RequiresApi(Build.VERSION_CODES.P)
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            val bitmap = outputFileResults.savedUri?.let { loadBitmap(it, context) }
                            result = bitmap?.let { tfliteHelper.classifyImage(it) }.toString()
                        }

                        override fun onError(exception: ImageCaptureException) {
                            result = "Image capture failed: ${exception.message}"
                        }
                    })
            }) {
                Text("Capture Image")
            }

            Text(text = result, style = MaterialTheme.typography.h6)
        }

        LaunchedEffect(Unit) {
            val cameraProvider = ProcessCameraProvider.getInstance(context).get()
            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(context as ComponentActivity, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                // Handle exceptions
            }
        }
    } else {
        LaunchedEffect(Unit) { cameraPermissionState.launchPermissionRequest() }
    }
}

fun createFile(context: Context): File {
    val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
        File(it, context.getString(R.string.app_name)).apply { mkdirs() }
    }
    return File(mediaDir, "${System.currentTimeMillis()}.jpg")
}

@RequiresApi(Build.VERSION_CODES.P)
fun loadBitmap(uri: Uri, context: Context): Bitmap {
    val source = ImageDecoder.createSource(context.contentResolver, uri)
    return ImageDecoder.decodeBitmap(source)
}
