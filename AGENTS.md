# AGENTS.md

Guidelines for AI agents working on SlowDown.

## Role

You are working on a cross-platform app that estimates vehicle speeds from video. The app runs on iOS (SwiftUI) and Android (Jetpack Compose) with shared business logic via Kotlin Multiplatform. It is used by residents and neighborhood groups to collect traffic speed data on residential streets.

## Priorities

1. **Correctness** — Speed calculations must be accurate. The speed formula and V85 computation are the core of the app's value. Changes to shared logic must be validated on both platforms.
2. **Parity** — Both platforms should produce identical results for the same inputs. The KMP shared module and Swift originals must stay in sync.
3. **Simplicity** — No unnecessary dependencies. Use Apple frameworks on iOS, standard Jetpack libraries on Android. Prefer straightforward implementations over clever abstractions.
4. **Safety** — This is a civic tool, not a law enforcement tool. Results are estimates, not measurements. Never present speeds as legally precise.

## Repository Layout

```
ios/           # iOS app — Swift, SwiftUI, SwiftData
android/       # Android app — Kotlin, Jetpack Compose, Room
shared/        # KMP shared module — pure Kotlin, no platform deps
```

## Constraints

### iOS
- iOS 17.0 minimum deployment target
- Swift 5.9+, SwiftUI only (no UIKit views except UIViewControllerRepresentable wrappers)
- No SPM packages, CocoaPods, or Carthage dependencies
- XcodeGen manages the project — never hand-edit `.pbxproj`

### Android
- Min SDK 26 (Android 8.0), Target SDK 35
- Kotlin 2.1+, Jetpack Compose for all UI
- Hilt for dependency injection
- Room for persistence, DataStore for preferences

### KMP Shared Module
- Pure Kotlin only — no platform-specific imports
- No Android or iOS framework dependencies
- Used by Android via Gradle project dependency; iOS keeps Swift originals

## Before Making Changes

1. Read CLAUDE.md for build instructions and architecture overview
2. **iOS**: Run `cd ios && xcodegen generate` after adding or removing files
3. **iOS**: Verify builds pass: `xcodebuild build -project ios/SlowDown.xcodeproj -scheme SlowDown -destination 'platform=iOS Simulator,name=iPhone 17 Pro' -quiet`
4. **Android**: Verify builds pass: `./gradlew :android:app:assembleDebug`
5. **Shared**: Run `./gradlew :shared:allTests` after modifying shared logic

## Cross-Platform Changes

When modifying shared business logic:
- Update `shared/src/commonMain/.../SpeedCalculator.kt` AND `ios/SlowDown/Models/SpeedCalculator.swift`
- Update `shared/src/commonMain/.../CoordinateMapper.kt` AND `ios/SlowDown/Services/CoordinateMapper.swift`
- Update `shared/src/commonMain/.../model/Enums.kt` AND `ios/SlowDown/Models/Calibration.swift`
- Run both test suites to verify parity

## iOS Guidelines

- Use `@Observable` (not `@ObservableObject`) for all view models
- Use `@Bindable` when passing `@Observable` objects to views that need bindings
- Break complex view bodies into extracted subviews — the Swift type checker will time out on deeply nested view hierarchies
- Use `GeometryReader` to get displayed image dimensions for coordinate mapping

## Android Guidelines

- Use `ViewModel` with `StateFlow` for state management
- Use `@HiltViewModel` with `@Inject constructor` for all view models
- Use `collectAsState()` in Compose to observe `StateFlow`
- Use `suspend` functions with `viewModelScope.launch` for async operations
- Use `Room` entity/DAO pattern for persistence
- Enums are stored as `String` rawValues in Room for compatibility with iOS data model

## Data Layer

### iOS
- `SpeedEntry` is the sole SwiftData `@Model`
- Enums stored as `String` rawValues with computed typed accessors
- `Calibration` uses `UserDefaults` via `Codable`

### Android
- `SpeedEntryEntity` is the Room `@Entity`, mirroring iOS `SpeedEntry` fields
- Enums stored as `String` rawValues with computed typed accessors
- `CalibrationStore` uses DataStore Preferences

### Adding new persistent properties
- Update `SpeedEntry.swift` (iOS) and `SpeedEntryEntity.kt` (Android)
- Consider SwiftData lightweight migration on iOS
- Consider Room migration on Android
- Update seed data if applicable

## Testing Expectations

- `SpeedCalculator` functions are pure — always verify formula changes with known inputs
- `CoordinateMapper` is pure — test with known image/view size ratios
- KMP shared tests in `shared/src/commonTest/` must produce identical results to Swift tests in `ios/SlowDownTests/`
- ViewModel state transitions should be predictable and testable
