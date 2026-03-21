package com.slowthemdown.android.service

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.text.TextRecognizer
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

@Singleton
class PIIBlurService @Inject constructor(
    private val faceDetector: FaceDetector,
    private val textRecognizer: TextRecognizer,
) {

    suspend fun blurPII(bitmap: Bitmap): Bitmap {
        // Timeout after 5s — ML Kit model downloads can hang on emulators
        return withTimeoutOrNull(5_000L) { blurPIIInternal(bitmap) } ?: bitmap
    }

    private suspend fun blurPIIInternal(bitmap: Bitmap): Bitmap {
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        val blurRects = mutableListOf<Rect>()

        coroutineScope {
            val facesDeferred = async {
                try {
                    faceDetector.process(inputImage).await()
                } catch (_: Exception) {
                    emptyList()
                }
            }
            val textDeferred = async {
                try {
                    textRecognizer.process(inputImage).await()
                } catch (_: Exception) {
                    null
                }
            }

            // Collect face rects with 20% padding
            val faces = facesDeferred.await()
            val facePadding = 0.2f
            for (face in faces) {
                val box = face.boundingBox
                val padW = (box.width() * facePadding).toInt()
                val padH = (box.height() * facePadding).toInt()
                blurRects.add(Rect(
                    max(0, box.left - padW),
                    max(0, box.top - padH),
                    min(bitmap.width, box.right + padW),
                    min(bitmap.height, box.bottom + padH),
                ))
            }

            // Collect license plate rects with 10% padding
            val textResult = textDeferred.await()
            if (textResult != null) {
                val platePadding = 0.1f
                for (block in textResult.textBlocks) {
                    for (line in block.lines) {
                        val box = line.boundingBox ?: continue
                        val w = box.width()
                        val h = box.height()
                        if (isLikelyPlate(line.text, w, h, bitmap.width)) {
                            val padW = (w * platePadding).toInt()
                            val padH = (h * platePadding).toInt()
                            blurRects.add(Rect(
                                max(0, box.left - padW),
                                max(0, box.top - padH),
                                min(bitmap.width, box.right + padW),
                                min(bitmap.height, box.bottom + padH),
                            ))
                        }
                    }
                }
            }
        }

        if (blurRects.isEmpty()) return bitmap

        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        for (rect in blurRects) {
            val left = rect.left
            val top = rect.top
            val width = rect.width()
            val height = rect.height()

            if (width <= 0 || height <= 0) continue

            // Pixelate: downscale to ~8px wide, then upscale back
            val smallWidth = max(1, 8)
            val smallHeight = max(1, (8.0 * height / width).toInt())

            val region = Bitmap.createBitmap(result, left, top, width, height)
            val small = Bitmap.createScaledBitmap(region, smallWidth, smallHeight, false)
            val pixelated = Bitmap.createScaledBitmap(small, width, height, false)

            canvas.drawBitmap(pixelated, null, rect, null)

            region.recycle()
            small.recycle()
            pixelated.recycle()
        }

        return result
    }

    companion object {
        internal fun isLikelyPlate(text: String, bboxWidth: Int, bboxHeight: Int, imageWidth: Int): Boolean {
            val cleaned = text.replace(Regex("[\\s\\-]"), "")
            if (cleaned.length < 5 || cleaned.length > 8) return false
            if (!cleaned.all { it.isLetterOrDigit() }) return false
            if (!cleaned.any { it.isDigit() } || !cleaned.any { it.isLetter() }) return false
            if (bboxHeight <= 0) return false
            val aspectRatio = bboxWidth.toDouble() / bboxHeight
            if (aspectRatio < 1.5 || aspectRatio > 3.5) return false
            if (bboxWidth < imageWidth * 0.01) return false
            return true
        }
    }
}
