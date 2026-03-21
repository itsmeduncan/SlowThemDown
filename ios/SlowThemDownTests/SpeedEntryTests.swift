import Foundation
import Testing
@testable import SlowThemDown

@Suite("SpeedEntry")
struct SpeedEntryTests {

    // MARK: - Enum accessors

    @Test func vehicleType_getterAndSetter() {
        let entry = SpeedEntry(speed: 13.41, vehicleType: .truck)
        #expect(entry.vehicleType == .truck)
        #expect(entry.vehicleTypeRaw == "truck")

        entry.vehicleType = .motorcycle
        #expect(entry.vehicleTypeRaw == "motorcycle")
    }

    @Test func vehicleType_unknownRawDefaultsToCar() {
        let entry = SpeedEntry(speed: 13.41)
        entry.vehicleTypeRaw = "segway"
        #expect(entry.vehicleType == .car)
    }

    @Test func direction_getterAndSetter() {
        let entry = SpeedEntry(speed: 13.41, direction: .toward)
        #expect(entry.direction == .toward)
        #expect(entry.directionRaw == "toward")

        entry.direction = .rightToLeft
        #expect(entry.directionRaw == "right_to_left")
    }

    @Test func calibrationMethod_getterAndSetter() {
        let entry = SpeedEntry(speed: 13.41, calibrationMethod: .vehicleReference)
        #expect(entry.calibrationMethod == .vehicleReference)
        #expect(entry.calibrationMethodRaw == "vehicle_reference")
    }

    // MARK: - isOverLimit

    @Test func isOverLimit_speedAboveLimit_returnsTrue() {
        let entry = SpeedEntry(speed: 13.41, speedLimit: 11.176)
        #expect(entry.isOverLimit == true)
    }

    @Test func isOverLimit_speedAtLimit_returnsFalse() {
        let entry = SpeedEntry(speed: 11.176, speedLimit: 11.176)
        #expect(entry.isOverLimit == false)
    }

    @Test func isOverLimit_speedBelowLimit_returnsFalse() {
        let entry = SpeedEntry(speed: 8.94, speedLimit: 11.176)
        #expect(entry.isOverLimit == false)
    }

    // MARK: - speedCategory

    @Test func speedCategory_underLimit() {
        let entry = SpeedEntry(speed: 8.94, speedLimit: 11.176)
        #expect(entry.speedCategory == .underLimit)
    }

    @Test func speedCategory_atLimit_isUnder() {
        let entry = SpeedEntry(speed: 11.176, speedLimit: 11.176)
        #expect(entry.speedCategory == .underLimit)
    }

    @Test func speedCategory_marginal() {
        // 12.52 / 11.176 = 1.12, which is <= 1.2 -> marginal
        let entry = SpeedEntry(speed: 12.52, speedLimit: 11.176)
        #expect(entry.speedCategory == .marginal)
    }

    @Test func speedCategory_overLimit() {
        // 15.65 / 11.176 = 1.40 -> over limit
        let entry = SpeedEntry(speed: 15.65, speedLimit: 11.176)
        #expect(entry.speedCategory == .overLimit)
    }

    // MARK: - SpeedCategory labels

    @Test func speedCategory_labelsAreNotEmpty() {
        #expect(!SpeedCategory.underLimit.label.isEmpty)
        #expect(!SpeedCategory.marginal.label.isEmpty)
        #expect(!SpeedCategory.overLimit.label.isEmpty)
    }

    // MARK: - Default values

    @Test func init_defaultValues() {
        let entry = SpeedEntry(speed: 13.41)
        #expect(entry.speedLimit == RoadStandards.defaultSpeedLimit)
        #expect(entry.streetName.isEmpty)
        #expect(entry.notes.isEmpty)
        #expect(entry.vehicleType == .car)
        #expect(entry.direction == .leftToRight)
        #expect(entry.calibrationMethod == .manualDistance)
        #expect(entry.latitude == nil)
        #expect(entry.longitude == nil)
    }
}
