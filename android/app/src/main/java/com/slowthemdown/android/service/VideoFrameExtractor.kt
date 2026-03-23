package com.slowthemdown.android.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.media.Image
import android.media.ImageReader
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoFrameExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class VideoInfo(
        val durationMs: Long,
        val width: Int,
        val height: Int,
    )

    suspend fun getVideoInfo(uri: Uri): VideoInfo = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val duration = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L
            val rawWidth = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
            )?.toIntOrNull() ?: 0
            val rawHeight = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
            )?.toIntOrNull() ?: 0
            val rotation = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
            )?.toIntOrNull() ?: 0
            // Swap dimensions for 90°/270° rotation so callers see the displayed size
            val (width, height) = if (rotation == 90 || rotation == 270) {
                rawHeight to rawWidth
            } else {
                rawWidth to rawHeight
            }
            VideoInfo(duration, width, height)
        } finally {
            retriever.release()
        }
    }

    /**
     * Extract a frame at the given timestamp using MediaCodec for precise seeking.
     *
     * MediaMetadataRetriever.OPTION_CLOSEST is unreliable on many devices — it
     * often returns the nearest keyframe instead of the actual closest frame,
     * causing both frame 1 and frame 2 to show the same image.
     *
     * This implementation seeks the MediaExtractor to the sync sample before the
     * target time, then decodes forward frame-by-frame until we pass the target
     * timestamp, capturing the closest frame.
     */
    suspend fun extractFrame(uri: Uri, timeSeconds: Double): Bitmap? = withContext(Dispatchers.IO) {
        val targetUs = (timeSeconds * 1_000_000).toLong()
        val rotation = getVideoRotation(uri)

        val extractor = MediaExtractor()
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                extractor.setDataSource(pfd.fileDescriptor)
            } ?: return@withContext fallbackExtract(uri, targetUs)?.applyRotation(rotation)

            val trackIndex = selectVideoTrack(extractor) ?: return@withContext fallbackExtract(uri, targetUs)?.applyRotation(rotation)
            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return@withContext fallbackExtract(uri, targetUs)?.applyRotation(rotation)
            val width = format.getInteger(MediaFormat.KEY_WIDTH)
            val height = format.getInteger(MediaFormat.KEY_HEIGHT)

            (decodeFrameAt(extractor, mime, width, height, targetUs)
                ?: fallbackExtract(uri, targetUs))?.applyRotation(rotation)
        } catch (_: Exception) {
            fallbackExtract(uri, targetUs)?.applyRotation(rotation)
        } finally {
            extractor.release()
        }
    }

    private fun getVideoRotation(uri: Uri): Int {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toIntOrNull() ?: 0
        } catch (_: Exception) {
            0
        } finally {
            retriever.release()
        }
    }

    private fun Bitmap.applyRotation(degrees: Int): Bitmap {
        if (degrees == 0) return this
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
        if (rotated !== this) recycle()
        return rotated
    }

    private fun selectVideoTrack(extractor: MediaExtractor): Int? {
        for (i in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("video/")) return i
        }
        return null
    }

    private fun decodeFrameAt(
        extractor: MediaExtractor,
        mime: String,
        width: Int,
        height: Int,
        targetUs: Long,
    ): Bitmap? {
        // Seek to the sync sample at or before targetUs
        extractor.seekTo(targetUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

        val imageReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 2)
        val handlerThread = HandlerThread("frame-decode").apply { start() }
        val handler = Handler(handlerThread.looper)

        val codec = MediaCodec.createDecoderByType(mime)
        try {
            val format = extractor.getTrackFormat(extractor.sampleTrackIndex.coerceAtLeast(0))
            codec.configure(format, imageReader.surface, null, 0)
            codec.start()

            val bufferInfo = MediaCodec.BufferInfo()
            var outputBitmap: Bitmap? = null
            var closestTimeDiff = Long.MAX_VALUE
            var inputDone = false
            var outputDone = false
            // Decode past the target by up to one frame duration to find the closest
            val overshootUs = 100_000L // 100ms overshoot to ensure we capture the target frame

            while (!outputDone) {
                // Feed input
                if (!inputDone) {
                    val inputIndex = codec.dequeueInputBuffer(10_000)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex)!!
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            val sampleTime = extractor.sampleTime
                            codec.queueInputBuffer(inputIndex, 0, sampleSize, sampleTime, 0)
                            extractor.advance()
                            // Stop feeding once we're well past the target
                            if (sampleTime > targetUs + overshootUs) {
                                inputDone = true
                            }
                        }
                    }
                }

                // Drain output
                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                if (outputIndex >= 0) {
                    val presentationTimeUs = bufferInfo.presentationTimeUs
                    val timeDiff = kotlin.math.abs(presentationTimeUs - targetUs)

                    if (timeDiff < closestTimeDiff) {
                        closestTimeDiff = timeDiff
                        // Render to ImageReader surface
                        codec.releaseOutputBuffer(outputIndex, true)
                        // Read the image from ImageReader
                        val image = imageReader.acquireLatestImage()
                        if (image != null) {
                            outputBitmap?.recycle()
                            outputBitmap = imageToBitmap(image, width, height)
                            image.close()
                        }
                    } else {
                        codec.releaseOutputBuffer(outputIndex, false)
                        // We've moved past the closest frame, stop
                        if (presentationTimeUs > targetUs) {
                            outputDone = true
                        }
                    }

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        outputDone = true
                    }
                } else if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER && inputDone) {
                    // No more output coming
                    outputDone = true
                }
            }

            return outputBitmap
        } finally {
            try { codec.stop() } catch (_: Exception) {}
            try { codec.release() } catch (_: Exception) {}
            imageReader.close()
            handlerThread.quitSafely()
        }
    }

    private fun imageToBitmap(image: Image, width: Int, height: Int): Bitmap {
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val yRowStride = yPlane.rowStride
        val uvRowStride = uPlane.rowStride
        val uvPixelStride = uPlane.pixelStride

        val argb = IntArray(width * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val yIndex = y * yRowStride + x
                val uvIndex = (y / 2) * uvRowStride + (x / 2) * uvPixelStride

                val yVal = (yBuffer.get(yIndex).toInt() and 0xFF)
                val uVal = (uBuffer.get(uvIndex).toInt() and 0xFF) - 128
                val vVal = (vBuffer.get(uvIndex).toInt() and 0xFF) - 128

                var r = yVal + (1.370705f * vVal).toInt()
                var g = yVal - (0.337633f * uVal).toInt() - (0.698001f * vVal).toInt()
                var b = yVal + (1.732446f * uVal).toInt()

                r = r.coerceIn(0, 255)
                g = g.coerceIn(0, 255)
                b = b.coerceIn(0, 255)

                argb[y * width + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        return Bitmap.createBitmap(argb, width, height, Bitmap.Config.ARGB_8888)
    }

    /** Fallback to MediaMetadataRetriever for devices where MediaCodec fails. */
    private fun fallbackExtract(uri: Uri, targetUs: Long): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.getFrameAtTime(targetUs, MediaMetadataRetriever.OPTION_CLOSEST)
                ?: retriever.getFrameAtTime(targetUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } finally {
            retriever.release()
        }
    }
}
