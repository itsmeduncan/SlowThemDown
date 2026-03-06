package com.slowthemdown.shared.calculator

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoordinateMapperTest {

    @Test
    fun viewToImage_imageToView_roundtrip() {
        val viewSize = Size(390.0, 844.0)
        val imageSize = Size(1920.0, 1080.0)
        val original = Point(195.0, 422.0)

        val imagePoint = CoordinateMapper.viewToImage(
            viewPoint = original, viewSize = viewSize, imageSize = imageSize
        )
        val roundtripped = CoordinateMapper.imageToView(
            imagePoint = imagePoint, viewSize = viewSize, imageSize = imageSize
        )

        assertTrue(abs(roundtripped.x - original.x) < 0.001)
        assertTrue(abs(roundtripped.y - original.y) < 0.001)
    }

    @Test
    fun viewToImage_zeroViewSize_returnsZero() {
        val result = CoordinateMapper.viewToImage(
            viewPoint = Point(10.0, 10.0),
            viewSize = Size.ZERO,
            imageSize = Size(1920.0, 1080.0)
        )
        assertEquals(Point.ZERO, result)
    }

    @Test
    fun imageToView_zeroImageSize_returnsZero() {
        val result = CoordinateMapper.imageToView(
            imagePoint = Point(10.0, 10.0),
            viewSize = Size(390.0, 844.0),
            imageSize = Size.ZERO
        )
        assertEquals(Point.ZERO, result)
    }

    @Test
    fun pixelDistance_knownValues() {
        val d = CoordinateMapper.pixelDistance(
            from = Point(0.0, 0.0),
            to = Point(3.0, 4.0)
        )
        assertTrue(abs(d - 5.0) < 0.001)
    }

    @Test
    fun pixelDistance_samePoint_isZero() {
        val p = Point(100.0, 200.0)
        assertEquals(0.0, CoordinateMapper.pixelDistance(from = p, to = p))
    }
}
