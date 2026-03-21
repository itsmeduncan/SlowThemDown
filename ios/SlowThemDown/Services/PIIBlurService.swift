import CoreImage
import UIKit
import Vision

enum PIIBlurService {

    static func blurPII(in image: UIImage) async -> UIImage {
        guard let cgImage = image.cgImage else { return image }

        let imageWidth = CGFloat(cgImage.width)
        let imageHeight = CGFloat(cgImage.height)

        let faceRequest = VNDetectFaceRectanglesRequest()
        let textRequest = VNRecognizeTextRequest()
        textRequest.recognitionLevel = .fast
        textRequest.recognitionLanguages = ["en-US"]

        let handler = VNImageRequestHandler(cgImage: cgImage, options: [:])

        do {
            try handler.perform([faceRequest, textRequest])
        } catch {
            return image
        }

        var blurRects: [CGRect] = []

        // Collect face rects with 20% padding
        if let faces = faceRequest.results {
            let facePadding: CGFloat = 0.2
            for face in faces {
                let box = face.boundingBox
                let w = box.width * imageWidth
                let h = box.height * imageHeight
                let x = box.origin.x * imageWidth
                let y = box.origin.y * imageHeight
                let padW = w * facePadding
                let padH = h * facePadding
                blurRects.append(CGRect(
                    x: max(0, x - padW),
                    y: max(0, y - padH),
                    width: min(imageWidth, w + 2 * padW),
                    height: min(imageHeight, h + 2 * padH)
                ))
            }
        }

        // Collect license plate rects with 10% padding
        if let textObservations = textRequest.results {
            let platePadding: CGFloat = 0.1
            for observation in textObservations {
                guard let candidate = observation.topCandidates(1).first else { continue }
                let box = observation.boundingBox
                let w = box.width * imageWidth
                let h = box.height * imageHeight
                if isLikelyPlate(text: candidate.string, bboxWidth: w, bboxHeight: h, imageWidth: imageWidth) {
                    let x = box.origin.x * imageWidth
                    let y = box.origin.y * imageHeight
                    let padW = w * platePadding
                    let padH = h * platePadding
                    blurRects.append(CGRect(
                        x: max(0, x - padW),
                        y: max(0, y - padH),
                        width: min(imageWidth, w + 2 * padW),
                        height: min(imageHeight, h + 2 * padH)
                    ))
                }
            }
        }

        guard !blurRects.isEmpty else { return image }

        let ciImage = CIImage(cgImage: cgImage)
        let context = CIContext()

        // Create a blurred version of the full image
        guard let blurFilter = CIFilter(name: "CIGaussianBlur") else { return image }
        blurFilter.setValue(ciImage, forKey: kCIInputImageKey)
        blurFilter.setValue(30.0, forKey: kCIInputRadiusKey)
        guard let blurredImage = blurFilter.outputImage else { return image }

        // Build a mask from all blur rects
        var maskImage: CIImage?
        for rect in blurRects {
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

    internal static func isLikelyPlate(text: String, bboxWidth: CGFloat, bboxHeight: CGFloat, imageWidth: CGFloat) -> Bool {
        let cleaned = text.replacingOccurrences(of: "[\\s\\-]", with: "", options: .regularExpression)
        guard cleaned.count >= 5, cleaned.count <= 8 else { return false }
        guard cleaned.allSatisfy(\.isASCII), cleaned.allSatisfy({ $0.isLetter || $0.isNumber }) else { return false }
        let hasDigit = cleaned.contains(where: \.isNumber)
        let hasLetter = cleaned.contains(where: \.isLetter)
        guard hasDigit, hasLetter else { return false }
        guard bboxHeight > 0 else { return false }
        let aspectRatio = bboxWidth / bboxHeight
        guard aspectRatio >= 1.5, aspectRatio <= 3.5 else { return false }
        guard bboxWidth >= imageWidth * 0.01 else { return false }
        return true
    }
}
