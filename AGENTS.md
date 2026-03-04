# AGENTS.md

Guidelines for AI agents working on SlowDown.

## Role

You are working on a native iOS app that estimates vehicle speeds from video. The app is used by residents and neighborhood groups to collect traffic speed data on residential streets.

## Priorities

1. **Correctness** — Speed calculations must be accurate. The speed formula and V85 computation are the core of the app's value.
2. **Simplicity** — No external dependencies. Use Apple frameworks only. Prefer straightforward implementations over clever abstractions.
3. **Safety** — This is a civic tool, not a law enforcement tool. Results are estimates, not measurements. Never present speeds as legally precise.

## Constraints

- iOS 17.0 minimum deployment target
- Swift 5.9+, SwiftUI only (no UIKit views except UIViewControllerRepresentable wrappers)
- No SPM packages, CocoaPods, or Carthage dependencies
- XcodeGen manages the project — never hand-edit `.pbxproj`

## Before Making Changes

1. Read CLAUDE.md for build instructions and architecture overview
2. Run `xcodegen generate` after adding or removing files
3. Verify builds pass: `xcodebuild build -project SlowDown.xcodeproj -scheme SlowDown -destination 'platform=iOS Simulator,name=iPhone 17 Pro' -quiet`

## SwiftUI Guidelines

- Use `@Observable` (not `@ObservableObject`) for all view models
- Use `@Bindable` when passing `@Observable` objects to views that need bindings
- Break complex view bodies into extracted subviews — the Swift type checker will time out on deeply nested view hierarchies
- Use `GeometryReader` to get displayed image dimensions for coordinate mapping

## Data Layer

- `SpeedEntry` is the sole SwiftData `@Model`
- Enums are stored as `String` rawValues with computed typed accessors
- `Calibration` uses `UserDefaults` via `Codable` (not SwiftData)
- Adding new persistent properties to `SpeedEntry` requires considering SwiftData migration

## Testing Expectations

- `SpeedCalculator` functions are pure — always verify formula changes with known inputs
- `CoordinateMapper` is pure — test with known image/view size ratios
- ViewModel state transitions should be predictable and testable
