import Foundation
import SwiftUI
import UIKit

@Observable
final class CalibrationViewModel {
    var calibration: Calibration
    var selectedImage: UIImage?
    var markerPoints: [CGPoint] = []
    var referenceDistanceText: String = ""
    var imageSize: CGSize = .zero

    var isCalibrated: Bool { calibration.isValid }

    var pixelDistance: Double {
        guard markerPoints.count == 2 else { return 0 }
        return CoordinateMapper.pixelDistance(from: markerPoints[0], to: markerPoints[1])
    }

    var referenceDistanceFeet: Double {
        Double(referenceDistanceText) ?? 0
    }

    var canSave: Bool {
        pixelDistance > 0 && referenceDistanceFeet > 0
    }

    init() {
        self.calibration = Calibration.load() ?? Calibration()
    }

    func addMarker(at viewPoint: CGPoint, viewSize: CGSize) {
        let imagePoint = CoordinateMapper.viewToImage(
            viewPoint: viewPoint,
            viewSize: viewSize,
            imageSize: imageSize
        )
        if markerPoints.count >= 2 {
            markerPoints = [imagePoint]
        } else {
            markerPoints.append(imagePoint)
        }
        HapticManager.impact(.light)
    }

    func resetMarkers() {
        markerPoints = []
    }

    func saveCalibration() {
        let ppf = SpeedCalculator.pixelsPerFoot(
            pixelDistance: pixelDistance,
            referenceFeet: referenceDistanceFeet
        )
        calibration = Calibration(
            method: .manualDistance,
            pixelsPerFoot: ppf,
            referenceDistanceFeet: referenceDistanceFeet,
            pixelDistance: pixelDistance,
            timestamp: .now
        )
        calibration.save()
        HapticManager.notification(.success)
    }

    func clearCalibration() {
        calibration = Calibration()
        UserDefaults.standard.removeObject(forKey: Calibration.storageKey)
        selectedImage = nil
        markerPoints = []
        referenceDistanceText = ""
    }
}
