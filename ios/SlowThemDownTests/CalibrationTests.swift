import Foundation
import Testing
@testable import SlowThemDown

@Suite("Calibration", .serialized)
struct CalibrationTests {

    // MARK: - isValid

    @Test func isValid_positivePixelsPerMeter_returnsTrue() {
        let cal = Calibration(pixelsPerMeter: 10.0)
        #expect(cal.isValid == true)
    }

    @Test func isValid_zeroPixelsPerMeter_returnsFalse() {
        let cal = Calibration()
        #expect(cal.isValid == false)
    }

    // MARK: - Codable roundtrip

    @Test func codableRoundtrip() throws {
        let original = Calibration(
            method: .vehicleReference,
            pixelsPerMeter: 50.853,
            referenceDistanceMeters: 4.877,
            pixelDistance: 248.0,
            vehicleReferenceName: "Toyota Camry",
            timestamp: Date(timeIntervalSince1970: 1000)
        )
        let data = try JSONEncoder().encode(original)
        let decoded = try JSONDecoder().decode(Calibration.self, from: data)
        #expect(decoded == original)
    }

    // MARK: - Persistence

    @Test func saveAndLoad_roundtrip() {
        // Clean slate
        UserDefaults.standard.removeObject(forKey: Calibration.storageKey)
        UserDefaults.standard.synchronize()

        let original = Calibration(
            method: .manualDistance,
            pixelsPerMeter: 65.617,
            referenceDistanceMeters: 3.048,
            pixelDistance: 200.0
        )
        original.save()
        UserDefaults.standard.synchronize()

        let loaded = Calibration.load()
        #expect(loaded != nil)
        #expect(loaded?.method == .manualDistance)
        #expect(abs((loaded?.pixelsPerMeter ?? 0) - 65.617) < 0.001)
        #expect(abs((loaded?.referenceDistanceMeters ?? 0) - 3.048) < 0.001)

        // Clean up
        UserDefaults.standard.removeObject(forKey: Calibration.storageKey)
        UserDefaults.standard.synchronize()
    }

    @Test func load_noData_returnsNil() {
        UserDefaults.standard.removeObject(forKey: Calibration.storageKey)
        UserDefaults.standard.synchronize()
        #expect(Calibration.load() == nil)
    }

    // MARK: - Enum labels

    @Test func calibrationMethod_labels() {
        #expect(CalibrationMethod.manualDistance.label == "Manual Distance")
        #expect(CalibrationMethod.vehicleReference.label == "Vehicle Reference")
    }

    @Test func vehicleType_allHaveLabelsAndIcons() {
        for type in VehicleType.allCases {
            #expect(!type.label.isEmpty)
            #expect(!type.icon.isEmpty)
        }
    }

    @Test func travelDirection_allHaveLabelsAndIcons() {
        for dir in TravelDirection.allCases {
            #expect(!dir.label.isEmpty)
            #expect(!dir.icon.isEmpty)
        }
    }

    @Test func captureMethod_allHaveLabels() {
        for method in CaptureMethod.allCases {
            #expect(!method.label.isEmpty)
        }
    }
}
