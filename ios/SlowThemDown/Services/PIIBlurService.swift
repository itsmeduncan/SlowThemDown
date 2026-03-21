import CoreImage
import UIKit
import Vision

enum PIIBlurService {

    static func blurFaces(in image: UIImage) async -> UIImage {
        guard let cgImage = image.cgImage else { return image }

        let request = VNDetectFaceRectanglesRequest()
        let handler = VNImageRequestHandler(cgImage: cgImage, options: [:])

        do {
            try handler.perform([request])
        } catch {
            return image
        }

        guard let results = request.results, !results.isEmpty else {
            return image
        }

        let imageWidth = CGFloat(cgImage.width)
        let imageHeight = CGFloat(cgImage.height)
        let padding: CGFloat = 0.2

        let ciImage = CIImage(cgImage: cgImage)
        let context = CIContext()

        // Create a blurred version of the full image
        guard let blurFilter = CIFilter(name: "CIGaussianBlur") else { return image }
        blurFilter.setValue(ciImage, forKey: kCIInputImageKey)
        blurFilter.setValue(30.0, forKey: kCIInputRadiusKey)
        guard let blurredImage = blurFilter.outputImage else { return image }

        // Build a mask from all face rects
        var maskImage: CIImage?
        for face in results {
            let box = face.boundingBox
            // Vision coords are normalized, bottom-left origin
            let w = box.width * imageWidth
            let h = box.height * imageHeight
            let x = box.origin.x * imageWidth
            let y = box.origin.y * imageHeight

            // Pad by 20%
            let padW = w * padding
            let padH = h * padding
            let rect = CGRect(
                x: max(0, x - padW),
                y: max(0, y - padH),
                width: min(imageWidth, w + 2 * padW),
                height: min(imageHeight, h + 2 * padH)
            )

            let white = CIImage(color: CIColor.white).cropped(to: rect)
            maskImage = maskImage.map { $0.composited(over: white) } ?? white
        }

        guard let mask = maskImage else { return image }

        // Fill the rest of the mask with black (unblurred areas)
        let fullExtent = ciImage.extent
        let blackBackground = CIImage(color: CIColor.black).cropped(to: fullExtent)
        let completeMask = mask.composited(over: blackBackground)

        // Blend: where mask is white use blurred, where black use original
        guard let blendFilter = CIFilter(name: "CIBlendWithMask") else { return image }
        blendFilter.setValue(blurredImage, forKey: kCIInputImageKey)
        blendFilter.setValue(ciImage, forKey: kCIInputBackgroundImageKey)
        blendFilter.setValue(completeMask, forKey: kCIInputMaskImageKey)
        guard let outputCI = blendFilter.outputImage else { return image }

        guard let outputCG = context.createCGImage(outputCI, from: fullExtent) else { return image }
        return UIImage(cgImage: outputCG, scale: image.scale, orientation: image.imageOrientation)
    }
}
