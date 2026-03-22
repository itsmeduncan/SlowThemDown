import CoreGraphics
import Foundation
import Testing
@testable import SlowThemDown

@Suite("CalibrationViewModel", .serialized)
@MainActor
struct CalibrationViewModelTests {

    // MARK: - Initial State

    @Test func initialState_notCalibrated() {
        // Clear any stored calibration first
        UserDefaults.standard.removeObject(forKey: Calibration.storageKey)
        let vm = CalibrationViewModel()
        vm.reload()
        #expect(vm.isCalibrated == false)
    }

    @Test func initialState_emptyMarkers() {
        let vm = CalibrationViewModel()
        #expect(vm.markerPoints.isEmpty)
        #expect(vm.referenceDistanceText.isEmpty)
    }

    // MARK: - Computed Properties

    @Test func pixelDistance_withNoMarkers_returnsZero() {
        let vm = CalibrationViewModel()
        #expect(vm.pixelDistance == 0)
    }

    @Test func pixelDistance_withTwoMarkers_returnsDistance() {
        let vm = CalibrationViewModel()
        vm.markerPoints = [CGPoint(x: 0, y: 0), CGPoint(x: 3, y: 4)]
        #expect(abs(vm.pixelDistance - 5.0) < 0.001)
    }

    @Test func referenceDistanceMeters_parsesValidNumber() {
        let vm = CalibrationViewModel()
        // In imperial mode (default for US), "20.5" feet -> meters
        vm.measurementSystemRaw = MeasurementSystem.imperial.rawValue
        vm.referenceDistanceText = "20.5"
        #expect(abs(vm.referenceDistanceMeters - 6.248) < 0.01)
    }

    @Test func referenceDistanceMeters_invalidText_returnsZero() {
        let vm = CalibrationViewModel()
        vm.referenceDistanceText = "abc"
        #expect(vm.referenceDistanceMeters == 0)
    }

    @Test func canSave_requiresMarkersAndDistance() {
        let vm = CalibrationViewModel()
        #expect(vm.canSave == false)

        vm.markerPoints = [CGPoint(x: 0, y: 0), CGPoint(x: 100, y: 0)]
        #expect(vm.canSave == false)

        vm.referenceDistanceText = "20"
        #expect(vm.canSave == true)
    }

    @Test func canSave_zeroDistance_isFalse() {
        let vm = CalibrationViewModel()
        vm.markerPoints = [CGPoint(x: 0, y: 0), CGPoint(x: 100, y: 0)]
        vm.referenceDistanceText = "0"
        #expect(vm.canSave == false)
    }

    // MARK: - Markers

    @Test func addMarker_appendsUpToTwo() {
        let vm = CalibrationViewModel()
        vm.imageSize = CGSize(width: 1920, height: 1080)
        let viewSize = CGSize(width: 390, height: 844)

        vm.addMarker(at: CGPoint(x: 100, y: 200), viewSize: viewSize)
        #expect(vm.markerPoints.count == 1)

        vm.addMarker(at: CGPoint(x: 200, y: 300), viewSize: viewSize)
        #expect(vm.markerPoints.count == 2)
    }

    @Test func addMarker_replacesWhenAlreadyTwo() {
        let vm = CalibrationViewModel()
        vm.imageSize = CGSize(width: 1920, height: 1080)
        let viewSize = CGSize(width: 390, height: 844)

        vm.addMarker(at: CGPoint(x: 100, y: 200), viewSize: viewSize)
        vm.addMarker(at: CGPoint(x: 200, y: 300), viewSize: viewSize)
        vm.addMarker(at: CGPoint(x: 300, y: 400), viewSize: viewSize)
        #expect(vm.markerPoints.count == 1)
    }

    @Test func resetMarkers_clearsAll() {
        let vm = CalibrationViewModel()
        vm.markerPoints = [CGPoint(x: 1, y: 1), CGPoint(x: 2, y: 2)]
        vm.resetMarkers()
        #expect(vm.markerPoints.isEmpty)
    }

    // MARK: - Save & Load

    @Test func saveCalibration_updatesInMemoryState() {
        UserDefaults.standard.removeObject(forKey: Calibration.storageKey)
        let vm = CalibrationViewModel()
        vm.measurementSystemRaw = MeasurementSystem.imperial.rawValue
        vm.imageSize = CGSize(width: 4032, height: 3024)
        vm.markerPoints = [CGPoint(x: 0, y: 0), CGPoint(x: 200, y: 0)]
        vm.referenceDistanceText = "10" // 10 feet = 3.048 meters

        vm.saveCalibration()

        #expect(vm.isCalibrated)
        // 200 px / 3.048 m = 65.617 px/m
        #expect(abs(vm.calibration.pixelsPerMeter - 65.617) < 0.1)
        #expect(abs(vm.calibration.referenceDistanceMeters - 3.048) < 0.01)
        #expect(vm.calibration.method == .manualDistance)
        #expect(vm.calibration.calibrationImageWidth == 4032)
        #expect(vm.calibration.needsRecalibration == false)

        // Clean up
        UserDefaults.standard.removeObject(forKey: Calibration.storageKey)
    }

    @Test func clearCalibration_resetsEverything() {
        let vm = CalibrationViewModel()
        vm.markerPoints = [CGPoint(x: 0, y: 0), CGPoint(x: 200, y: 0)]
        vm.referenceDistanceText = "10"
        vm.saveCalibration()

        vm.clearCalibration()

        #expect(vm.isCalibrated == false)
        #expect(vm.markerPoints.isEmpty)
        #expect(vm.referenceDistanceText.isEmpty)
        #expect(vm.selectedImage == nil)

        // Clean up
        UserDefaults.standard.removeObject(forKey: Calibration.storageKey)
    }
}
