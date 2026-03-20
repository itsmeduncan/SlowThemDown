import Foundation
import Testing
@testable import SlowThemDown

@Suite("SpeedEntry")
struct SpeedEntryTests {

    // MARK: - Enum accessors

    @Test func vehicleType_getterAndSetter() {
        let entry = SpeedEntry(speedMPH: 30, vehicleType: .truck)
        #expect(entry.vehicleType == .truck)
        #expect(entry.vehicleTypeRaw == "truck")

        entry.vehicleType = .motorcycle
        #expect(entry.vehicleTypeRaw == "motorcycle")
    }

    @Test func vehicleType_unknownRawDefaultsToCar() {
        let entry = SpeedEntry(speedMPH: 30)
        entry.vehicleTypeRaw = "segway"
        #expect(entry.vehicleType == .car)
    }

    @Test func direction_getterAndSetter() {
        let entry = SpeedEntry(speedMPH: 30, direction: .toward)
        #expect(entry.direction == .toward)
        #expect(entry.directionRaw == "toward")

        entry.direction = .rightToLeft
        #expect(entry.directionRaw == "right_to_left")
    }

    @Test func calibrationMethod_getterAndSetter() {
        let entry = SpeedEntry(speedMPH: 30, calibrationMethod: .vehicleReference)
        #expect(entry.calibrationMethod == .vehicleReference)
        #expect(entry.calibrationMethodRaw == "vehicle_reference")
    }

    // MARK: - isOverLimit

    @Test func isOverLimit_speedAboveLimit_returnsTrue() {
        let entry = SpeedEntry(speedMPH: 30, speedLimit: 25)
        #expect(entry.isOverLimit == true)
    }

    @Test func isOverLimit_speedAtLimit_returnsFalse() {
        let entry = SpeedEntry(speedMPH: 25, speedLimit: 25)
        #expect(entry.isOverLimit == false)
    }

    @Test func isOverLimit_speedBelowLimit_returnsFalse() {
        let entry = SpeedEntry(speedMPH: 20, speedLimit: 25)
        #expect(entry.isOverLimit == false)
    }

    // MARK: - speedCategory

    @Test func speedCategory_underLimit() {
        let entry = SpeedEntry(speedMPH: 20, speedLimit: 25)
        #expect(entry.speedCategory == .underLimit)
    }

    @Test func speedCategory_atLimit_isUnder() {
        let entry = SpeedEntry(speedMPH: 25, speedLimit: 25)
        #expect(entry.speedCategory == .underLimit)
    }

    @Test func speedCategory_marginal() {
        let entry = SpeedEntry(speedMPH: 28, speedLimit: 25)
        #expect(entry.speedCategory == .marginal)
    }

    @Test func speedCategory_overLimit() {
        let entry = SpeedEntry(speedMPH: 35, speedLimit: 25)
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
        let entry = SpeedEntry(speedMPH: 30)
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
