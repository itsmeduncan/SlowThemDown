import AVFoundation
import CoreGraphics
import FirebaseCrashlytics
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

@MainActor
@Observable
final class CaptureViewModel {
    var state: CaptureFlowState = .selectSource
    var videoURL: URL?
    var videoDuration: Double = 0
    var videoSize: CGSize = .zero
    var errorMessage: String?
    var isExtractingFrames: Bool = false
    var isSaving: Bool = false
    var isLoadingVideo: Bool = false

    // Frame selection
    var frame1Time: Double = 0 {
        didSet { schedulePreview(forFrame: 1) }
    }

    var frame2Time: Double = 0.5 {
        didSet { schedulePreview(forFrame: 2) }
    }

    var frame1Image: UIImage?
    var frame2Image: UIImage?

    // Scrub previews — low-resolution frames shown while choosing timestamps, so the
    // selection isn't blind. These are never persisted and never used for measurement.
    var previewFrame1Image: UIImage?
    var previewFrame2Image: UIImage?
    var isLoadingPreview1: Bool = false
    var isLoadingPreview2: Bool = false

    private static let previewMaxSize = CGSize(width: 640, height: 640)
    private static let previewDebounce = Duration.milliseconds(150)

    private var preview1Task: Task<Void, Never>?
    private var preview2Task: Task<Void, Never>?

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
    var speedLimit: Double = RoadStandards.defaultSpeedLimit
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
        errorMessage = nil
        isLoadingVideo = true
        let ext = VideoFrameExtractor(url: url)
        extractor = ext
        do {
            let dur = try await ext.duration
            videoDuration = CMTimeGetSeconds(dur)
            videoSize = try await ext.naturalSize
            frame1Time = 0
            frame2Time = min(0.5, videoDuration)
            state = .selectFrames
        } catch {
            Crashlytics.crashlytics().record(error: error)
            errorMessage = "Failed to load video. Try a different file."
            state = .selectSource
        }
        isLoadingVideo = false
    }

    func extractFrames() async {
        guard let extractor else { return }
        isExtractingFrames = true
        defer { isExtractingFrames = false }
        do {
            let t1 = CMTime(seconds: frame1Time, preferredTimescale: 600)
            let t2 = CMTime(seconds: frame2Time, preferredTimescale: 600)
            frame1Image = await PIIBlurService.blurPII(in: try await extractor.extractFrame(at: t1))
            frame2Image = await PIIBlurService.blurPII(in: try await extractor.extractFrame(at: t2))
            frame1Markers = []
            frame2Markers = []
            vehicleRefMarkers = []
            state = .markFrame1
        } catch {
            Crashlytics.crashlytics().record(error: error)
            errorMessage = "Failed to extract frames. Try different timestamps."
        }
    }

    // MARK: - Scrub previews

    /// Refresh the preview for one slider, debounced so dragging doesn't queue up a
    /// decode per tick. A newer scrub cancels the one in flight.
    private func schedulePreview(forFrame frame: Int) {
        guard let extractor else { return }
        let time = frame == 1 ? frame1Time : frame2Time

        if frame == 1 {
            preview1Task?.cancel()
            preview1Task = Task { [weak self] in
                await self?.loadPreview(forFrame: 1, at: time, using: extractor)
            }
        } else {
            preview2Task?.cancel()
            preview2Task = Task { [weak self] in
                await self?.loadPreview(forFrame: 2, at: time, using: extractor)
            }
        }
    }

    private func loadPreview(forFrame frame: Int, at time: Double, using extractor: VideoFrameExtractor) async {
        do {
            try await Task.sleep(for: Self.previewDebounce)
        } catch {
            return // superseded while waiting out the debounce
        }

        setPreviewLoading(true, forFrame: frame)
        let image = try? await extractor.extractFrame(
            at: CMTime(seconds: time, preferredTimescale: 600),
            maximumSize: Self.previewMaxSize
        )
        guard !Task.isCancelled else { return }
        setPreviewLoading(false, forFrame: frame)

        // A failed preview leaves the last good frame up rather than blanking the view.
        guard let image else { return }
        if frame == 1 {
            previewFrame1Image = image
        } else {
            previewFrame2Image = image
        }
    }

    private func setPreviewLoading(_ loading: Bool, forFrame frame: Int) {
        if frame == 1 {
            isLoadingPreview1 = loading
        } else {
            isLoadingPreview2 = loading
        }
    }

    // MARK: - Navigation

    var canGoBack: Bool {
        switch state {
        case .selectSource, .recording: false
        case .selectFrames, .markFrame1, .markFrame2, .result: true
        }
    }

    /// Step back one stage, preserving the loaded video and any work already done.
    /// Distinct from `reset()`, which discards the video and starts from scratch.
    func goBack() {
        switch state {
        case .selectSource, .recording:
            break
        case .selectFrames:
            // The only thing behind frame selection is choosing a different video.
            reset()
        case .markFrame1:
            state = .selectFrames
        case .markFrame2:
            state = .markFrame1
        case .result:
            state = .markFrame2
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
        let ppm: Double
        if useVehicleReference, let ref = selectedVehicleRef, vehicleRefMarkers.count == 2 {
            let refPixels = CoordinateMapper.pixelDistance(
                from: vehicleRefMarkers[0],
                to: vehicleRefMarkers[1]
            )
            ppm = SpeedCalculator.pixelsPerMeter(pixelDistance: refPixels, referenceMeters: ref.lengthMeters)
        } else {
            ppm = calibration.scaledPixelsPerMeter(forVideoWidth: videoSize.width)
        }

        calculatedSpeed = SpeedCalculator.calculateSpeed(
            pixelDisplacement: pixelDisplacement,
            pixelsPerMeter: ppm,
            timeDeltaSeconds: timeDelta
        )
        state = .result
        HapticManager.notification(.success)
    }

    func buildEntry(calibration: Calibration, location: LocationManager) -> SpeedEntry {
        let ppm: Double
        let method: CalibrationMethod
        let refDist: Double

        if useVehicleReference, let ref = selectedVehicleRef, vehicleRefMarkers.count == 2 {
            let refPixels = CoordinateMapper.pixelDistance(
                from: vehicleRefMarkers[0],
                to: vehicleRefMarkers[1]
            )
            ppm = SpeedCalculator.pixelsPerMeter(pixelDistance: refPixels, referenceMeters: ref.lengthMeters)
            method = .vehicleReference
            refDist = ref.lengthMeters
        } else {
            ppm = calibration.scaledPixelsPerMeter(forVideoWidth: videoSize.width)
            method = calibration.method
            refDist = calibration.referenceDistanceMeters
        }

        return SpeedEntry(
            speed: calculatedSpeed,
            speedLimit: speedLimit,
            streetName: streetName.isEmpty ? location.streetName : streetName,
            notes: notes,
            vehicleType: vehicleType,
            direction: direction,
            calibrationMethod: method,
            timeDeltaSeconds: timeDelta,
            pixelDisplacement: pixelDisplacement,
            pixelsPerMeter: ppm,
            referenceDistanceMeters: refDist,
            latitude: location.currentLocation?.coordinate.latitude,
            longitude: location.currentLocation?.coordinate.longitude
        )
    }

    func reset() {
        preview1Task?.cancel()
        preview2Task?.cancel()
        preview1Task = nil
        preview2Task = nil

        state = .selectSource
        videoURL = nil
        // Drop the extractor too, so a stray time change can't decode from the old video.
        extractor = nil
        errorMessage = nil
        frame1Image = nil
        frame2Image = nil
        previewFrame1Image = nil
        previewFrame2Image = nil
        isLoadingPreview1 = false
        isLoadingPreview2 = false
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
