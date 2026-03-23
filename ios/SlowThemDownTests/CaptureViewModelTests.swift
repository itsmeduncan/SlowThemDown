import CoreGraphics
import Foundation
import Testing
@testable import SlowThemDown

@Suite("CaptureViewModel")
@MainActor
struct CaptureViewModelTests {

    // MARK: - Initial State

    @Test func initialState_isSelectSource() {
        let vm = CaptureViewModel()
        #expect(vm.state == .selectSource)
    }

    @Test func initialState_fieldsAreDefaults() {
        let vm = CaptureViewModel()
        #expect(vm.videoURL == nil)
        #expect(vm.calculatedSpeed == 0)
        #expect(vm.streetName.isEmpty)
        #expect(vm.notes.isEmpty)
        #expect(vm.vehicleType == .car)
        #expect(vm.direction == .leftToRight)
        #expect(vm.frame1Markers.isEmpty)
        #expect(vm.frame2Markers.isEmpty)
        #expect(vm.vehicleRefMarkers.isEmpty)
        #expect(vm.useVehicleReference == false)
        #expect(vm.selectedVehicleRef == nil)
        #expect(vm.isSaving == false)
        #expect(vm.isLoadingVideo == false)
        #expect(vm.isExtractingFrames == false)
    }

    // MARK: - Computed Properties

    @Test func timeDelta_returnsAbsoluteDifference() {
        let vm = CaptureViewModel()
        vm.frame1Time = 1.0
        vm.frame2Time = 1.5
        #expect(abs(vm.timeDelta - 0.5) < 0.001)
    }

    @Test func timeDelta_handlesReversedOrder() {
        let vm = CaptureViewModel()
        vm.frame1Time = 2.0
        vm.frame2Time = 1.0
        #expect(abs(vm.timeDelta - 1.0) < 0.001)
    }

    @Test func pixelDisplacement_withNoMarkers_returnsZero() {
        let vm = CaptureViewModel()
        #expect(vm.pixelDisplacement == 0)
    }

    @Test func pixelDisplacement_withOneMarkerEach_returnsDistance() {
        let vm = CaptureViewModel()
        vm.frame1Markers = [CGPoint(x: 0, y: 0)]
        vm.frame2Markers = [CGPoint(x: 3, y: 4)]
        #expect(abs(vm.pixelDisplacement - 5.0) < 0.001)
    }

    // MARK: - Markers

    @Test func addMarkerFrame1_replacesExistingMarker() {
        let vm = CaptureViewModel()
        vm.videoSize = CGSize(width: 1920, height: 1080)
        vm.addMarkerFrame1(at: CGPoint(x: 100, y: 200), viewSize: CGSize(width: 390, height: 844))
        #expect(vm.frame1Markers.count == 1)

        vm.addMarkerFrame1(at: CGPoint(x: 200, y: 300), viewSize: CGSize(width: 390, height: 844))
        #expect(vm.frame1Markers.count == 1)
    }

    @Test func addMarkerFrame2_replacesExistingMarker() {
        let vm = CaptureViewModel()
        vm.videoSize = CGSize(width: 1920, height: 1080)
        vm.addMarkerFrame2(at: CGPoint(x: 100, y: 200), viewSize: CGSize(width: 390, height: 844))
        #expect(vm.frame2Markers.count == 1)

        vm.addMarkerFrame2(at: CGPoint(x: 200, y: 300), viewSize: CGSize(width: 390, height: 844))
        #expect(vm.frame2Markers.count == 1)
    }

    @Test func addVehicleRefMarker_appendsUpToTwo() {
        let vm = CaptureViewModel()
        vm.videoSize = CGSize(width: 1920, height: 1080)
        let viewSize = CGSize(width: 390, height: 844)

        vm.addVehicleRefMarker(at: CGPoint(x: 10, y: 10), viewSize: viewSize)
        #expect(vm.vehicleRefMarkers.count == 1)

        vm.addVehicleRefMarker(at: CGPoint(x: 20, y: 20), viewSize: viewSize)
        #expect(vm.vehicleRefMarkers.count == 2)
    }

    @Test func addVehicleRefMarker_replacesWhenAlreadyTwo() {
        let vm = CaptureViewModel()
        vm.videoSize = CGSize(width: 1920, height: 1080)
        let viewSize = CGSize(width: 390, height: 844)

        vm.addVehicleRefMarker(at: CGPoint(x: 10, y: 10), viewSize: viewSize)
        vm.addVehicleRefMarker(at: CGPoint(x: 20, y: 20), viewSize: viewSize)
        #expect(vm.vehicleRefMarkers.count == 2)

        vm.addVehicleRefMarker(at: CGPoint(x: 30, y: 30), viewSize: viewSize)
        #expect(vm.vehicleRefMarkers.count == 1)
    }

    // MARK: - calculateSpeed

    @Test func calculateSpeed_withCalibration_scalesForVideoWidth() {
        let vm = CaptureViewModel()
        vm.videoSize = CGSize(width: 1920, height: 1080)
        vm.frame1Markers = [CGPoint(x: 0, y: 0)]
        vm.frame2Markers = [CGPoint(x: 100, y: 0)]
        vm.frame1Time = 0
        vm.frame2Time = 1.0

        // ppm=200 at calWidth=4032, video=1920 → scaled ppm ≈ 95.238
        let cal = Calibration(pixelsPerMeter: 200, referenceDistanceMeters: 6.096, calibrationImageWidth: 4032)
        vm.calculateSpeed(calibration: cal)

        #expect(vm.state == .result)
        // speed = 100 / 95.238 / 1.0 ≈ 1.05 m/s
        #expect(abs(vm.calculatedSpeed - 1.05) < 0.01)
    }

    @Test func calculateSpeed_withLegacyCalibration_usesUnscaledPPM() {
        let vm = CaptureViewModel()
        vm.videoSize = CGSize(width: 1920, height: 1080)
        vm.frame1Markers = [CGPoint(x: 0, y: 0)]
        vm.frame2Markers = [CGPoint(x: 100, y: 0)]
        vm.frame1Time = 0
        vm.frame2Time = 1.0

        // Legacy calibration without calibrationImageWidth
        let cal = Calibration(pixelsPerMeter: 32.808, referenceDistanceMeters: 6.096)
        vm.calculateSpeed(calibration: cal)

        #expect(vm.state == .result)
        // speed = 100 / 32.808 / 1.0 ≈ 3.048 m/s (unscaled)
        #expect(abs(vm.calculatedSpeed - 3.048) < 0.01)
    }

    @Test func calculateSpeed_withVehicleRef_usesRefCalibration() {
        let vm = CaptureViewModel()
        vm.useVehicleReference = true
        vm.selectedVehicleRef = VehicleReferences.all.first
        vm.vehicleRefMarkers = [CGPoint(x: 0, y: 0), CGPoint(x: 160, y: 0)]
        vm.frame1Markers = [CGPoint(x: 0, y: 0)]
        vm.frame2Markers = [CGPoint(x: 50, y: 0)]
        vm.frame1Time = 0
        vm.frame2Time = 1.0

        vm.calculateSpeed(calibration: Calibration())

        #expect(vm.state == .result)
        #expect(vm.calculatedSpeed > 0)
    }

    @Test func calculateSpeed_zeroCalibration_resultsInZeroSpeed() {
        let vm = CaptureViewModel()
        vm.frame1Markers = [CGPoint(x: 0, y: 0)]
        vm.frame2Markers = [CGPoint(x: 100, y: 0)]
        vm.frame1Time = 0
        vm.frame2Time = 1.0

        vm.calculateSpeed(calibration: Calibration())
        #expect(vm.calculatedSpeed == 0)
    }

    // MARK: - buildEntry

    @Test func buildEntry_usesCalibrationValues() {
        let vm = CaptureViewModel()
        vm.videoSize = CGSize(width: 1920, height: 1080)
        vm.calculatedSpeed = 15.65  // ~35 MPH in m/s
        vm.speedLimit = 11.176      // ~25 MPH in m/s
        vm.streetName = "Oak Ave"
        vm.vehicleType = .truck
        vm.direction = .toward

        let cal = Calibration(
            method: .manualDistance,
            pixelsPerMeter: 32.808,
            referenceDistanceMeters: 6.096,
            calibrationImageWidth: 1920 // same as video width, no scaling
        )
        let loc = LocationManager()
        let entry = vm.buildEntry(calibration: cal, location: loc)

        #expect(entry.speed == 15.65)
        #expect(entry.speedLimit == 11.176)
        #expect(entry.streetName == "Oak Ave")
        #expect(entry.vehicleType == .truck)
        #expect(entry.direction == .toward)
        #expect(entry.calibrationMethod == .manualDistance)
        #expect(entry.pixelsPerMeter == 32.808)
        #expect(entry.referenceDistanceMeters == 6.096)
    }

    @Test func buildEntry_fallsBackToLocationStreetName() {
        let vm = CaptureViewModel()
        vm.streetName = ""

        let loc = LocationManager()
        loc.streetName = "Elm St"

        let entry = vm.buildEntry(calibration: Calibration(), location: loc)
        #expect(entry.streetName == "Elm St")
    }

    @Test func buildEntry_prefersUserStreetName() {
        let vm = CaptureViewModel()
        vm.streetName = "User Input"

        let loc = LocationManager()
        loc.streetName = "GPS Street"

        let entry = vm.buildEntry(calibration: Calibration(), location: loc)
        #expect(entry.streetName == "User Input")
    }

    // MARK: - Reset

    @Test func reset_restoresDefaults() {
        let vm = CaptureViewModel()
        vm.streetName = "Test"
        vm.notes = "Note"
        vm.calculatedSpeed = 20.12
        vm.useVehicleReference = true
        vm.selectedVehicleRef = VehicleReferences.all.first
        vm.frame1Markers = [CGPoint(x: 1, y: 1)]
        vm.frame2Markers = [CGPoint(x: 2, y: 2)]
        vm.vehicleRefMarkers = [CGPoint(x: 3, y: 3)]
        vm.isSaving = true
        vm.isLoadingVideo = true

        vm.reset()

        #expect(vm.state == .selectSource)
        #expect(vm.videoURL == nil)
        #expect(vm.streetName.isEmpty)
        #expect(vm.notes.isEmpty)
        #expect(vm.calculatedSpeed == 0)
        #expect(vm.frame1Markers.isEmpty)
        #expect(vm.frame2Markers.isEmpty)
        #expect(vm.vehicleRefMarkers.isEmpty)
        #expect(vm.useVehicleReference == false)
        #expect(vm.selectedVehicleRef == nil)
    }
}
