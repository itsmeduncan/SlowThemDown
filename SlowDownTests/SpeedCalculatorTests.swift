import Testing
@testable import SlowDown

@Suite("SpeedCalculator")
struct SpeedCalculatorTests {

    // MARK: - calculateSpeedMPH

    @Test func calculateSpeedMPH_knownInputs() {
        // 100 pixels at 10 px/ft over 1 second = 10 ft/s ≈ 6.818 MPH
        let result = SpeedCalculator.calculateSpeedMPH(
            pixelDisplacement: 100,
            pixelsPerFoot: 10,
            timeDeltaSeconds: 1
        )
        #expect(abs(result - 6.81818) < 0.001)
    }

    @Test func calculateSpeedMPH_zeroPixelsPerFoot_returnsZero() {
        let result = SpeedCalculator.calculateSpeedMPH(
            pixelDisplacement: 100,
            pixelsPerFoot: 0,
            timeDeltaSeconds: 1
        )
        #expect(result == 0)
    }

    @Test func calculateSpeedMPH_zeroTimeDelta_returnsZero() {
        let result = SpeedCalculator.calculateSpeedMPH(
            pixelDisplacement: 100,
            pixelsPerFoot: 10,
            timeDeltaSeconds: 0
        )
        #expect(result == 0)
    }

    // MARK: - pixelsPerFoot

    @Test func pixelsPerFoot_knownConversion() {
        let result = SpeedCalculator.pixelsPerFoot(pixelDistance: 200, referenceFeet: 10)
        #expect(result == 20)
    }

    @Test func pixelsPerFoot_zeroReference_returnsZero() {
        let result = SpeedCalculator.pixelsPerFoot(pixelDistance: 200, referenceFeet: 0)
        #expect(result == 0)
    }

    // MARK: - v85

    @Test func v85_emptyArray_returnsNil() {
        #expect(SpeedCalculator.v85(speeds: []) == nil)
    }

    @Test func v85_singleElement() {
        let result = SpeedCalculator.v85(speeds: [30])
        #expect(result == 30)
    }

    @Test func v85_knownArray() {
        // Sorted: [20, 22, 25, 28, 30, 32, 35, 38, 40, 45]
        // rank = 0.85 * 9 = 7.65
        // lower = 7 (value 38), upper = 8 (value 40), fraction = 0.65
        // result = 38 + 0.65 * (40 - 38) = 39.3
        let speeds = [30.0, 25, 35, 20, 40, 28, 45, 22, 32, 38]
        let result = SpeedCalculator.v85(speeds: speeds)!
        #expect(abs(result - 39.3) < 0.001)
    }

    // MARK: - trafficStats

    @Test func trafficStats_emptyEntries_returnsNil() {
        #expect(SpeedCalculator.trafficStats(entries: []) == nil)
    }

    @Test func trafficStats_aggregation() {
        let entries = [
            SpeedEntry(speedMPH: 20, speedLimit: 25),
            SpeedEntry(speedMPH: 30, speedLimit: 25),
            SpeedEntry(speedMPH: 40, speedLimit: 25),
        ]
        let stats = SpeedCalculator.trafficStats(entries: entries)!

        #expect(stats.count == 3)
        #expect(abs(stats.mean - 30) < 0.001)
        #expect(stats.median == 30)
        #expect(stats.min == 20)
        #expect(stats.max == 40)
        #expect(stats.overLimitCount == 2)  // 30 and 40 are over 25
        #expect(abs(stats.overLimitPercent - 66.666) < 0.1)
    }
}
