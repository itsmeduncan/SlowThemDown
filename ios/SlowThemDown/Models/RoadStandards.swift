import Foundation

enum RoadStandards {
    /// Standard residential lane widths in feet
    static let narrowLane: Double = 9.0
    static let standardLane: Double = 10.0
    static let wideLane: Double = 11.0
    static let arteryLane: Double = 12.0

    static let allWidths: [(label: String, feet: Double)] = [
        ("Narrow (9 ft)", narrowLane),
        ("Standard (10 ft)", standardLane),
        ("Wide (11 ft)", wideLane),
        ("Artery (12 ft)", arteryLane),
    ]

    /// Common residential speed limits in MPH
    static let speedLimits: [Int] = [15, 20, 25, 30, 35, 40, 45]

    /// Default speed limit for new entries
    static let defaultSpeedLimit: Int = 25
}
