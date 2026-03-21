import Testing
@testable import SlowThemDown

@Suite("SpeedCalculator")
struct SpeedCalculatorTests {

    // MARK: - calculateSpeed

    @Test func calculateSpeed_knownInputs() {
        // 100 pixels at 10 px/m over 1 second = 10 m/s
        let result = SpeedCalculator.calculateSpeed(
            pixelDisplacement: 100,
            pixelsPerMeter: 10,
            timeDeltaSeconds: 1
        )
        #expect(abs(result - 10.0) < 0.001)
    }

    @Test func calculateSpeed_zeroPixelsPerMeter_returnsZero() {
        let result = SpeedCalculator.calculateSpeed(
            pixelDisplacement: 100,
            pixelsPerMeter: 0,
            timeDeltaSeconds: 1
        )
        #expect(result == 0)
    }

    @Test func calculateSpeed_zeroTimeDelta_returnsZero() {
        let result = SpeedCalculator.calculateSpeed(
            pixelDisplacement: 100,
            pixelsPerMeter: 10,
            timeDeltaSeconds: 0
        )
        #expect(result == 0)
    }

    // MARK: - pixelsPerMeter

    @Test func pixelsPerMeter_knownConversion() {
        let result = SpeedCalculator.pixelsPerMeter(pixelDistance: 200, referenceMeters: 10)
        #expect(result == 20)
    }

    @Test func pixelsPerMeter_zeroReference_returnsZero() {
        let result = SpeedCalculator.pixelsPerMeter(pixelDistance: 200, referenceMeters: 0)
        #expect(result == 0)
    }

    // MARK: - v85

    @Test func v85_emptyArray_returnsNil() {
        #expect(SpeedCalculator.v85(speeds: []) == nil)
    }

    @Test func v85_singleElement() {
        let result = SpeedCalculator.v85(speeds: [13.41])
        #expect(result == 13.41)
    }

    @Test func v85_knownArray() {
        // Sorted: [8.94, 9.83, 11.18, 12.52, 13.41, 14.31, 15.65, 16.99, 17.88, 20.12]
        // rank = 0.85 * 9 = 7.65
        // lower = 7 (value 16.99), upper = 8 (value 17.88), fraction = 0.65
        // result = 16.99 + 0.65 * (17.88 - 16.99) = 17.5685
        let speeds = [13.41, 11.18, 15.65, 8.94, 17.88, 12.52, 20.12, 9.83, 14.31, 16.99]
        let result = SpeedCalculator.v85(speeds: speeds)!
        #expect(abs(result - 17.5685) < 0.001)
    }

    // MARK: - trafficStats

    @Test func trafficStats_emptyEntries_returnsNil() {
        #expect(SpeedCalculator.trafficStats(entries: []) == nil)
    }

    @Test func trafficStats_aggregation() {
        // Speeds in m/s, limit in m/s (11.176 = 25 MPH)
        let limit = 11.176
        let entries = [
            SpeedEntry(speed: 8.94, speedLimit: limit),   // ~20 MPH, under
            SpeedEntry(speed: 13.41, speedLimit: limit),   // ~30 MPH, over
            SpeedEntry(speed: 17.88, speedLimit: limit),   // ~40 MPH, over
        ]
        let stats = SpeedCalculator.trafficStats(entries: entries)!

        #expect(stats.count == 3)
        #expect(abs(stats.mean - 13.41) < 0.01)
        #expect(stats.median == 13.41)
        #expect(stats.min == 8.94)
        #expect(stats.max == 17.88)
        #expect(stats.overLimitCount == 2)  // 13.41 and 17.88 are over 11.176
        #expect(abs(stats.overLimitPercent - 66.666) < 0.1)
    }
}
