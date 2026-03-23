package com.slowthemdown.shared.calculator

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

    // isWithinImageBounds

    @Test
    fun isWithinImageBounds_centerOfImage_returnsTrue() {
        val viewSize = Size(390.0, 844.0)
        val imageSize = Size(1920.0, 1080.0)
        val center = Point(195.0, 422.0)

        assertTrue(CoordinateMapper.isWithinImageBounds(center, viewSize, imageSize))
    }

    @Test
    fun isWithinImageBounds_outsideLetterbox_returnsFalse() {
        // Wide image in tall view → letterboxed top/bottom
        val viewSize = Size(390.0, 844.0)
        val imageSize = Size(1920.0, 1080.0)

        // Tap in the top letterbox area
        assertFalse(CoordinateMapper.isWithinImageBounds(Point(195.0, 5.0), viewSize, imageSize))

        // Tap in the bottom letterbox area
        assertFalse(CoordinateMapper.isWithinImageBounds(Point(195.0, 840.0), viewSize, imageSize))
    }

    @Test
    fun isWithinImageBounds_outsidePillarbox_returnsFalse() {
        // Tall image in wide view → pillarboxed left/right
        val viewSize = Size(844.0, 390.0)
        val imageSize = Size(1080.0, 1920.0)

        // Tap in left pillarbox
        assertFalse(CoordinateMapper.isWithinImageBounds(Point(5.0, 195.0), viewSize, imageSize))

        // Tap in right pillarbox
        assertFalse(CoordinateMapper.isWithinImageBounds(Point(840.0, 195.0), viewSize, imageSize))
    }

    @Test
    fun isWithinImageBounds_exactFit_allPointsValid() {
        val viewSize = Size(400.0, 300.0)
        val imageSize = Size(800.0, 600.0)

        // Origin corner
        assertTrue(CoordinateMapper.isWithinImageBounds(Point(0.0, 0.0), viewSize, imageSize))
        // Far corner (Kotlin range is inclusive)
        assertTrue(CoordinateMapper.isWithinImageBounds(Point(400.0, 300.0), viewSize, imageSize))
        // Just inside
        assertTrue(CoordinateMapper.isWithinImageBounds(Point(399.0, 299.0), viewSize, imageSize))
    }

    @Test
    fun isWithinImageBounds_zeroSizes_returnsFalse() {
        assertFalse(CoordinateMapper.isWithinImageBounds(Point(10.0, 10.0), Size.ZERO, Size(100.0, 100.0)))
        assertFalse(CoordinateMapper.isWithinImageBounds(Point(10.0, 10.0), Size(100.0, 100.0), Size.ZERO))
    }

    // pixelDistance

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
