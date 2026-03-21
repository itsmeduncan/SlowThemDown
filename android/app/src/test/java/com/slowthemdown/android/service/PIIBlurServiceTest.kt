package com.slowthemdown.android.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PIIBlurServiceTest {

    // Valid plates

    @Test
    fun `valid plate - standard format with space`() {
        assertTrue(PIIBlurService.isLikelyPlate("ABC 1234", 200, 100, 1920))
    }

    @Test
    fun `valid plate - no spaces`() {
        assertTrue(PIIBlurService.isLikelyPlate("7XYZ123", 200, 100, 1920))
    }

    @Test
    fun `valid plate - with hyphen`() {
        assertTrue(PIIBlurService.isLikelyPlate("AB-1234", 200, 100, 1920))
    }

    @Test
    fun `valid plate - five chars`() {
        assertTrue(PIIBlurService.isLikelyPlate("AB123", 200, 100, 1920))
    }

    @Test
    fun `valid plate - eight chars`() {
        assertTrue(PIIBlurService.isLikelyPlate("ABCD1234", 200, 100, 1920))
    }

    // Non-plates

    @Test
    fun `non-plate - all letters`() {
        assertFalse(PIIBlurService.isLikelyPlate("STOP", 200, 100, 1920))
    }

    @Test
    fun `non-plate - all digits`() {
        assertFalse(PIIBlurService.isLikelyPlate("1425", 200, 100, 1920))
    }

    @Test
    fun `non-plate - too short`() {
        assertFalse(PIIBlurService.isLikelyPlate("A1", 200, 100, 1920))
    }

    @Test
    fun `non-plate - too long`() {
        assertFalse(PIIBlurService.isLikelyPlate("ABCDE12345", 200, 100, 1920))
    }

    @Test
    fun `non-plate - street name`() {
        assertFalse(PIIBlurService.isLikelyPlate("MAIN ST", 200, 100, 1920))
    }

    @Test
    fun `non-plate - all letters five chars`() {
        assertFalse(PIIBlurService.isLikelyPlate("ABCDE", 200, 100, 1920))
    }

    // Aspect ratio edge cases

    @Test
    fun `non-plate - aspect ratio too tall`() {
        assertFalse(PIIBlurService.isLikelyPlate("ABC1234", 100, 200, 1920))
    }

    @Test
    fun `non-plate - aspect ratio too wide`() {
        assertFalse(PIIBlurService.isLikelyPlate("ABC1234", 400, 100, 1920))
    }

    // Size edge cases

    @Test
    fun `non-plate - too small relative to image`() {
        assertFalse(PIIBlurService.isLikelyPlate("ABC1234", 10, 5, 1920))
    }

    @Test
    fun `non-plate - zero height`() {
        assertFalse(PIIBlurService.isLikelyPlate("ABC1234", 200, 0, 1920))
    }
}
