package com.paradox.app.feature.capture.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class ReceiptImageOcrHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun processUri(uri: Uri): Result<List<String>> = suspendCancellableCoroutine { cont ->
        try {
            val image = InputImage.fromFilePath(context, uri)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val lines = visionText.textBlocks.flatMap { block ->
                        block.lines.map { it.text }
                    }
                    cont.resume(Result.success(lines))
                }
                .addOnFailureListener { e ->
                    cont.resume(Result.failure(e))
                }
        } catch (e: Exception) {
            cont.resume(Result.failure(e))
        }
    }

    suspend fun processBitmap(bitmap: Bitmap): Result<List<String>> = suspendCancellableCoroutine { cont ->
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val lines = visionText.textBlocks.flatMap { block ->
                        block.lines.map { it.text }
                    }
                    cont.resume(Result.success(lines))
                }
                .addOnFailureListener { e ->
                    cont.resume(Result.failure(e))
                }
        } catch (e: Exception) {
            cont.resume(Result.failure(e))
        }
    }
}
