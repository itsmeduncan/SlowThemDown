import Foundation
import Testing
@testable import SlowThemDown

@Suite("UnitConverter")
struct UnitConverterTests {

    // MARK: - Speed

    @Test func displaySpeed_imperial_convertsToMPH() {
        // 11.176 m/s = 25 MPH
        let result = UnitConverter.displaySpeed(11.176, system: .imperial)
        #expect(abs(result - 25.0) < 0.01)
    }

    @Test func displaySpeed_metric_convertsToKmh() {
        // 11.176 m/s = 40.2336 km/h
        let result = UnitConverter.displaySpeed(11.176, system: .metric)
        #expect(abs(result - 40.2336) < 0.01)
    }

    @Test func speedToMps_imperial_convertsFromMPH() {
        let result = UnitConverter.speedToMps(25.0, system: .imperial)
        #expect(abs(result - 11.176) < 0.01)
    }

    @Test func speedToMps_metric_convertsFromKmh() {
        let result = UnitConverter.speedToMps(40.2336, system: .metric)
        #expect(abs(result - 11.176) < 0.01)
    }

    @Test func speed_roundTrip_imperial() {
        let original = 25.0
        let mps = UnitConverter.speedToMps(original, system: .imperial)
        let displayed = UnitConverter.displaySpeed(mps, system: .imperial)
        #expect(abs(displayed - original) < 0.001)
    }

    @Test func speed_roundTrip_metric() {
        let original = 50.0
        let mps = UnitConverter.speedToMps(original, system: .metric)
        let displayed = UnitConverter.displaySpeed(mps, system: .metric)
        #expect(abs(displayed - original) < 0.001)
    }

    @Test func speedUnit_imperial() {
        #expect(UnitConverter.speedUnit(.imperial) == "MPH")
    }

    @Test func speedUnit_metric() {
        #expect(UnitConverter.speedUnit(.metric) == "km/h")
    }

    // MARK: - Distance

    @Test func displayDistance_imperial_convertsToFeet() {
        let result = UnitConverter.displayDistance(3.048, system: .imperial)
        #expect(abs(result - 10.0) < 0.01)
    }

    @Test func displayDistance_metric_passesThrough() {
        let result = UnitConverter.displayDistance(3.048, system: .metric)
        #expect(result == 3.048)
    }

    @Test func distanceToMeters_imperial_convertsFromFeet() {
        let result = UnitConverter.distanceToMeters(10.0, system: .imperial)
        #expect(abs(result - 3.048) < 0.01)
    }

    @Test func distanceUnit_imperial() {
        #expect(UnitConverter.distanceUnit(.imperial) == "ft")
    }

    @Test func distanceUnit_metric() {
        #expect(UnitConverter.distanceUnit(.metric) == "m")
    }

    // MARK: - Calibration

    @Test func displayPixelsPerUnit_imperial_convertsToPixelsPerFoot() {
        // 32.8084 px/m -> 10 px/ft
        let result = UnitConverter.displayPixelsPerUnit(32.8084, system: .imperial)
        #expect(abs(result - 10.0) < 0.01)
    }

    @Test func displayPixelsPerUnit_metric_passesThrough() {
        let result = UnitConverter.displayPixelsPerUnit(32.8084, system: .metric)
        #expect(result == 32.8084)
    }

    @Test func calibrationUnit_imperial() {
        #expect(UnitConverter.calibrationUnit(.imperial) == "px/ft")
    }

    @Test func calibrationUnit_metric() {
        #expect(UnitConverter.calibrationUnit(.metric) == "px/m")
    }
}
