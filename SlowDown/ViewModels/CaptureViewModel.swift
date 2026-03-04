import AVFoundation
import CoreGraphics
import Foundation
import UIKit

enum CaptureFlowState: Equatable {
    case selectSource
    case recording
    case selectFrames
    case markFrame1
    case markFrame2
    case result
}

@Observable
final class CaptureViewModel {
    var state: CaptureFlowState = .selectSource
    var videoURL: URL?
    var videoDuration: Double = 0
    var videoSize: CGSize = .zero

    // Frame selection
    var frame1Time: Double = 0
    var frame2Time: Double = 0.5
    var frame1Image: UIImage?
    var frame2Image: UIImage?

    // Marker points (in image coordinates)
    var frame1Markers: [CGPoint] = []
    var frame2Markers: [CGPoint] = []

    // Vehicle reference calibration
    var useVehicleReference: Bool = false
    var selectedVehicleRef: VehicleReference?
    var vehicleRefMarkers: [CGPoint] = []

    // Result
    var calculatedSpeed: Double = 0
    var vehicleType: VehicleType = .car
    var direction: TravelDirection = .leftToRight
    var speedLimit: Int = RoadStandards.defaultSpeedLimit
    var streetName: String = ""
    var notes: String = ""

    var timeDelta: Double {
        abs(frame2Time - frame1Time)
    }

    var pixelDisplacement: Double {
        guard frame1Markers.count == 1, frame2Markers.count == 1 else { return 0 }
        return CoordinateMapper.pixelDistance(from: frame1Markers[0], to: frame2Markers[0])
    }

    private var extractor: VideoFrameExtractor?

    func loadVideo(url: URL) async {
        videoURL = url
        extractor = VideoFrameExtractor(url: url)
        do {
            let dur = try await extractor!.duration
            videoDuration = CMTimeGetSeconds(dur)
            videoSize = try await extractor!.naturalSize
            frame1Time = 0
            frame2Time = min(0.5, videoDuration)
            state = .selectFrames
        } catch {
            state = .selectSource
        }
    }

    func extractFrames() async {
        guard let extractor else { return }
        do {
            let t1 = CMTime(seconds: frame1Time, preferredTimescale: 600)
            let t2 = CMTime(seconds: frame2Time, preferredTimescale: 600)
            frame1Image = try await extractor.extractFrame(at: t1)
            frame2Image = try await extractor.extractFrame(at: t2)
            frame1Markers = []
            frame2Markers = []
            vehicleRefMarkers = []
            state = .markFrame1
        } catch {
            // Stay on frame selection
        }
    }

    func addMarkerFrame1(at viewPoint: CGPoint, viewSize: CGSize) {
        let imagePoint = CoordinateMapper.viewToImage(
            viewPoint: viewPoint,
            viewSize: viewSize,
            imageSize: videoSize
        )
        frame1Markers = [imagePoint]
        HapticManager.impact(.light)
    }

    func addMarkerFrame2(at viewPoint: CGPoint, viewSize: CGSize) {
        let imagePoint = CoordinateMapper.viewToImage(
            viewPoint: viewPoint,
            viewSize: viewSize,
            imageSize: videoSize
        )
        frame2Markers = [imagePoint]
        HapticManager.impact(.light)
    }

    func addVehicleRefMarker(at viewPoint: CGPoint, viewSize: CGSize) {
        let imagePoint = CoordinateMapper.viewToImage(
            viewPoint: viewPoint,
            viewSize: viewSize,
            imageSize: videoSize
        )
        if vehicleRefMarkers.count >= 2 {
            vehicleRefMarkers = [imagePoint]
        } else {
            vehicleRefMarkers.append(imagePoint)
        }
        HapticManager.impact(.light)
    }

    func calculateSpeed(calibration: Calibration) {
        let ppf: Double
        if useVehicleReference, let ref = selectedVehicleRef, vehicleRefMarkers.count == 2 {
            let refPixels = CoordinateMapper.pixelDistance(
                from: vehicleRefMarkers[0],
                to: vehicleRefMarkers[1]
            )
            ppf = SpeedCalculator.pixelsPerFoot(pixelDistance: refPixels, referenceFeet: ref.lengthFeet)
        } else {
            ppf = calibration.pixelsPerFoot
        }

        calculatedSpeed = SpeedCalculator.calculateSpeedMPH(
            pixelDisplacement: pixelDisplacement,
            pixelsPerFoot: ppf,
            timeDeltaSeconds: timeDelta
        )
        state = .result
        HapticManager.notification(.success)
    }

    func buildEntry(calibration: Calibration, location: LocationManager) -> SpeedEntry {
        let ppf: Double
        let method: CalibrationMethod
        let refDist: Double

        if useVehicleReference, let ref = selectedVehicleRef, vehicleRefMarkers.count == 2 {
            let refPixels = CoordinateMapper.pixelDistance(
                from: vehicleRefMarkers[0],
                to: vehicleRefMarkers[1]
            )
            ppf = SpeedCalculator.pixelsPerFoot(pixelDistance: refPixels, referenceFeet: ref.lengthFeet)
            method = .vehicleReference
            refDist = ref.lengthFeet
        } else {
            ppf = calibration.pixelsPerFoot
            method = calibration.method
            refDist = calibration.referenceDistanceFeet
        }

        return SpeedEntry(
            speedMPH: calculatedSpeed,
            speedLimit: speedLimit,
            streetName: streetName.isEmpty ? location.streetName : streetName,
            notes: notes,
            vehicleType: vehicleType,
            direction: direction,
            calibrationMethod: method,
            timeDeltaSeconds: timeDelta,
            pixelDisplacement: pixelDisplacement,
            pixelsPerFoot: ppf,
            referenceDistanceFeet: refDist,
            latitude: location.currentLocation?.coordinate.latitude,
            longitude: location.currentLocation?.coordinate.longitude
        )
    }

    func reset() {
        state = .selectSource
        videoURL = nil
        frame1Image = nil
        frame2Image = nil
        frame1Markers = []
        frame2Markers = []
        vehicleRefMarkers = []
        calculatedSpeed = 0
        notes = ""
        streetName = ""
        useVehicleReference = false
        selectedVehicleRef = nil
    }
}
