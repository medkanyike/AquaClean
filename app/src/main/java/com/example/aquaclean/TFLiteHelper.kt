package com.example.aquaclean


import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class TFLiteHelper(context: Context) {
    private var interpreter: Interpreter
    private val labels: List<String>

    init {
        // Load the model and labels
        val modelFile = loadModelFile(context, "model.tflite")
        interpreter = Interpreter(modelFile)
        labels = FileUtil.loadLabels(context, "names.txt")
    }

    private fun loadModelFile(context: Context, modelPath: String): ByteBuffer {
        return try {
            val fileDescriptor = context.assets.openFd(modelPath)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength).apply {
                order(ByteOrder.nativeOrder())
            }
        } catch (ex: IOException) {
            Log.e("TFLiteHelper", "Error loading model file", ex)
            throw RuntimeException("Error loading model file", ex)
        }
    }

    fun classifyImage(bitmap: Bitmap): String {
        try {
            // Resize the bitmap to 224x224
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)

            // Convert bitmap to ARGB_8888 format if not already
            val argbBitmap = if (resizedBitmap.config != Bitmap.Config.ARGB_8888) {
                resizedBitmap.copy(Bitmap.Config.ARGB_8888, true)
            } else {
                resizedBitmap
            }

            // Create a TensorImage from the bitmap
            val tensorImage = TensorImage.fromBitmap(argbBitmap)

            // Prepare the output buffer
            val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, labels.size), DataType.UINT8)

            // Run inference
            interpreter.run(tensorImage.buffer, outputBuffer.buffer.rewind())

            // Process the output
            val predictions = outputBuffer.floatArray
            val predictedIndex = predictions.indices.maxByOrNull { predictions[it] } ?: -1

            return labels[predictedIndex]
        } catch (e: IllegalArgumentException) {
            Log.e("ClassifyImage", "Error: Unsupported bitmap configuration", e)
            return "Error: Unsupported bitmap configuration"
        } catch (e: Exception) {
            Log.e("ClassifyImage", "Error during classification", e)
            return "Error: ${e.message}"
        }
    }

}

