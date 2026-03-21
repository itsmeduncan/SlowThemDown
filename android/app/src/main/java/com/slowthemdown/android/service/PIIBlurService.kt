package com.slowthemdown.android.service

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetector
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

@Singleton
class PIIBlurService @Inject constructor(
    private val faceDetector: FaceDetector,
) {

    suspend fun blurFaces(bitmap: Bitmap): Bitmap {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val faces = try {
            faceDetector.process(inputImage).await()
        } catch (_: Exception) {
            return bitmap
        }

        if (faces.isEmpty()) return bitmap

        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val padding = 0.2f

        for (face in faces) {
            val box = face.boundingBox
            val padW = (box.width() * padding).toInt()
            val padH = (box.height() * padding).toInt()

            val left = max(0, box.left - padW)
            val top = max(0, box.top - padH)
            val right = min(bitmap.width, box.right + padW)
            val bottom = min(bitmap.height, box.bottom + padH)
            val width = right - left
            val height = bottom - top

            if (width <= 0 || height <= 0) continue

            // Pixelate: downscale to ~8px wide, then upscale back
            val smallWidth = max(1, 8)
            val smallHeight = max(1, (8.0 * height / width).toInt())

            val region = Bitmap.createBitmap(result, left, top, width, height)
            val small = Bitmap.createScaledBitmap(region, smallWidth, smallHeight, false)
            val pixelated = Bitmap.createScaledBitmap(small, width, height, false)

            canvas.drawBitmap(pixelated, null, Rect(left, top, right, bottom), null)

            region.recycle()
            small.recycle()
            pixelated.recycle()
        }

        return result
    }
}
