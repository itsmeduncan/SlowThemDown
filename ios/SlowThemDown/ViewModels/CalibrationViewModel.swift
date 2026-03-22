import Foundation
import SwiftUI
import UIKit

@MainActor
@Observable
final class CalibrationViewModel {
    var calibration: Calibration
    var selectedImage: UIImage?
    var markerPoints: [CGPoint] = []
    var referenceDistanceText: String = ""
    var imageSize: CGSize = .zero

    @ObservationIgnored
    @AppStorage("measurementSystem") var measurementSystemRaw: String = MeasurementSystem.deviceDefault.rawValue

    var measurementSystem: MeasurementSystem {
        MeasurementSystem(rawValue: measurementSystemRaw) ?? .imperial
    }

    var isCalibrated: Bool { calibration.isValid }

    var pixelDistance: Double {
        guard markerPoints.count == 2 else { return 0 }
        return CoordinateMapper.pixelDistance(from: markerPoints[0], to: markerPoints[1])
    }

    var referenceDistanceMeters: Double {
        guard let parsed = Double(referenceDistanceText) else { return 0 }
        return UnitConverter.distanceToMeters(parsed, system: measurementSystem)
    }

    var canSave: Bool {
        pixelDistance > 0 && referenceDistanceMeters > 0
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
        let ppm = SpeedCalculator.pixelsPerMeter(
            pixelDistance: pixelDistance,
            referenceMeters: referenceDistanceMeters
        )
        calibration = Calibration(
            method: .manualDistance,
            pixelsPerMeter: ppm,
            referenceDistanceMeters: referenceDistanceMeters,
            pixelDistance: pixelDistance,
            timestamp: .now
        )
        calibration.save()
        HapticManager.notification(.success)
    }

    func reload() {
        calibration = Calibration.load() ?? Calibration()
    }

    func clearCalibration() {
        calibration = Calibration()
        UserDefaults.standard.removeObject(forKey: Calibration.storageKey)
        selectedImage = nil
        markerPoints = []
        referenceDistanceText = ""
    }
}
