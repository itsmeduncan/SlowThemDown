import Foundation
import Testing
@testable import SlowThemDown

@Suite("Calibration", .serialized)
struct CalibrationTests {

    // MARK: - isValid

    @Test func isValid_positivePixelsPerFoot_returnsTrue() {
        let cal = Calibration(pixelsPerFoot: 10.0)
        #expect(cal.isValid == true)
    }

    @Test func isValid_zeroPixelsPerFoot_returnsFalse() {
        let cal = Calibration()
        #expect(cal.isValid == false)
    }

    // MARK: - Codable roundtrip

    @Test func codableRoundtrip() throws {
        let original = Calibration(
            method: .vehicleReference,
            pixelsPerFoot: 15.5,
            referenceDistanceFeet: 16.0,
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
            pixelsPerFoot: 20.0,
            referenceDistanceFeet: 10.0,
            pixelDistance: 200.0
        )
        original.save()
        UserDefaults.standard.synchronize()

        let loaded = Calibration.load()
        #expect(loaded != nil)
        #expect(loaded?.method == .manualDistance)
        #expect(loaded?.pixelsPerFoot == 20.0)
        #expect(loaded?.referenceDistanceFeet == 10.0)

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
