import CoreGraphics
import UIKit

enum CoordinateMapper {
    /// Convert a tap point in the displayed image view to coordinates in the original image
    static func viewToImage(
        viewPoint: CGPoint,
        viewSize: CGSize,
        imageSize: CGSize
    ) -> CGPoint {
        guard viewSize.width > 0, viewSize.height > 0 else { return .zero }

        // Compute aspect-fit scaling
        let scaleX = imageSize.width / viewSize.width
        let scaleY = imageSize.height / viewSize.height
        let scale = max(scaleX, scaleY)

        // Compute offset from aspect-fit centering
        let scaledImageWidth = imageSize.width / scale
        let scaledImageHeight = imageSize.height / scale
        let offsetX = (viewSize.width - scaledImageWidth) / 2
        let offsetY = (viewSize.height - scaledImageHeight) / 2

        // Map view coordinates to image coordinates
        let imageX = (viewPoint.x - offsetX) * scale
        let imageY = (viewPoint.y - offsetY) * scale

        return CGPoint(x: imageX, y: imageY)
    }

    /// Convert an image coordinate back to the displayed view coordinate
    static func imageToView(
        imagePoint: CGPoint,
        viewSize: CGSize,
        imageSize: CGSize
    ) -> CGPoint {
        guard imageSize.width > 0, imageSize.height > 0 else { return .zero }

        let scaleX = imageSize.width / viewSize.width
        let scaleY = imageSize.height / viewSize.height
        let scale = max(scaleX, scaleY)

        let scaledImageWidth = imageSize.width / scale
        let scaledImageHeight = imageSize.height / scale
        let offsetX = (viewSize.width - scaledImageWidth) / 2
        let offsetY = (viewSize.height - scaledImageHeight) / 2

        let viewX = imagePoint.x / scale + offsetX
        let viewY = imagePoint.y / scale + offsetY

        return CGPoint(x: viewX, y: viewY)
    }

    /// Check whether a view-space tap point falls within the displayed image bounds
    static func isWithinImageBounds(
        viewPoint: CGPoint,
        viewSize: CGSize,
        imageSize: CGSize
    ) -> Bool {
        guard viewSize.width > 0, viewSize.height > 0,
              imageSize.width > 0, imageSize.height > 0 else { return false }

        let scaleX = imageSize.width / viewSize.width
        let scaleY = imageSize.height / viewSize.height
        let scale = max(scaleX, scaleY)

        let scaledW = imageSize.width / scale
        let scaledH = imageSize.height / scale
        let originX = (viewSize.width - scaledW) / 2
        let originY = (viewSize.height - scaledH) / 2

        let imageRect = CGRect(x: originX, y: originY, width: scaledW, height: scaledH)
        return imageRect.contains(viewPoint)
    }

    /// Calculate the pixel distance between two points
    static func pixelDistance(from: CGPoint, to: CGPoint) -> Double {
        let dx = to.x - from.x
        let dy = to.y - from.y
        return sqrt(dx * dx + dy * dy)
    }
}
