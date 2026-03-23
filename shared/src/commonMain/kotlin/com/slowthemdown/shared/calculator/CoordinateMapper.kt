package com.slowthemdown.shared.calculator

import kotlin.math.sqrt

data class Point(val x: Double, val y: Double) {
    companion object {
        val ZERO = Point(0.0, 0.0)
    }
}

data class Size(val width: Double, val height: Double) {
    companion object {
        val ZERO = Size(0.0, 0.0)
    }
}

object CoordinateMapper {
    /** Convert a tap point in the displayed image view to coordinates in the original image */
    fun viewToImage(viewPoint: Point, viewSize: Size, imageSize: Size): Point {
        if (viewSize.width <= 0 || viewSize.height <= 0) return Point.ZERO

        // Compute aspect-fit scaling
        val scaleX = imageSize.width / viewSize.width
        val scaleY = imageSize.height / viewSize.height
        val scale = maxOf(scaleX, scaleY)

        // Compute offset from aspect-fit centering
        val scaledImageWidth = imageSize.width / scale
        val scaledImageHeight = imageSize.height / scale
        val offsetX = (viewSize.width - scaledImageWidth) / 2
        val offsetY = (viewSize.height - scaledImageHeight) / 2

        // Map view coordinates to image coordinates
        val imageX = (viewPoint.x - offsetX) * scale
        val imageY = (viewPoint.y - offsetY) * scale

        return Point(imageX, imageY)
    }

    /** Convert an image coordinate back to the displayed view coordinate */
    fun imageToView(imagePoint: Point, viewSize: Size, imageSize: Size): Point {
        if (imageSize.width <= 0 || imageSize.height <= 0) return Point.ZERO

        val scaleX = imageSize.width / viewSize.width
        val scaleY = imageSize.height / viewSize.height
        val scale = maxOf(scaleX, scaleY)

        val scaledImageWidth = imageSize.width / scale
        val scaledImageHeight = imageSize.height / scale
        val offsetX = (viewSize.width - scaledImageWidth) / 2
        val offsetY = (viewSize.height - scaledImageHeight) / 2

        val viewX = imagePoint.x / scale + offsetX
        val viewY = imagePoint.y / scale + offsetY

        return Point(viewX, viewY)
    }

    /** Check whether a view-space tap point falls within the displayed image bounds */
    fun isWithinImageBounds(viewPoint: Point, viewSize: Size, imageSize: Size): Boolean {
        if (viewSize.width <= 0 || viewSize.height <= 0 ||
            imageSize.width <= 0 || imageSize.height <= 0) return false

        val scaleX = imageSize.width / viewSize.width
        val scaleY = imageSize.height / viewSize.height
        val scale = maxOf(scaleX, scaleY)

        val scaledW = imageSize.width / scale
        val scaledH = imageSize.height / scale
        val originX = (viewSize.width - scaledW) / 2
        val originY = (viewSize.height - scaledH) / 2

        return viewPoint.x in originX..(originX + scaledW) &&
            viewPoint.y in originY..(originY + scaledH)
    }

    /** Calculate the pixel distance between two points */
    fun pixelDistance(from: Point, to: Point): Double {
        val dx = to.x - from.x
        val dy = to.y - from.y
        return sqrt(dx * dx + dy * dy)
    }
}
