# CLAUDE.md

Instructions for Claude Code when working on the SlowDown project.

## Project Overview

SlowDown is a cross-platform app for estimating vehicle speeds on residential streets using video analysis. It targets iOS 17+ (SwiftUI) and Android 8.0+ (Jetpack Compose) with shared business logic via Kotlin Multiplatform (KMP).

## Repository Structure

```
SlowDown/
├── ios/                    # iOS app (SwiftUI + SwiftData)
│   ├── project.yml         # XcodeGen spec
│   ├── SlowDown/           # iOS source files
│   └── SlowDownTests/      # iOS tests (Swift Testing)
├── android/                # Android app (Jetpack Compose + Room)
│   └── app/src/main/java/com/slowdown/android/
├── shared/                 # KMP shared module (pure Kotlin)
│   └── src/commonMain/kotlin/com/slowdown/shared/
│       ├── model/          # Enums, data classes, constants
│       └── calculator/     # SpeedCalculator, CoordinateMapper
├── build.gradle.kts        # Root Gradle build
├── settings.gradle.kts     # Gradle settings
└── .github/workflows/      # CI/CD
```

## Build System

### iOS
- **XcodeGen** generates `SlowDown.xcodeproj` from `ios/project.yml`
- Generate: `cd ios && xcodegen generate`
- Build: `xcodebuild build -project ios/SlowDown.xcodeproj -scheme SlowDown -destination 'platform=iOS Simulator,name=iPhone 17 Pro'`
- No external dependencies — Apple frameworks only

### Android
- **Gradle** with Kotlin DSL
- Build: `./gradlew :android:app:assembleDebug`
- Test shared module: `./gradlew :shared:allTests`
- Test Android: `./gradlew :android:app:testDebugUnitTest`
- Lint: `./gradlew :android:app:lintDebug`
- Dependencies: Jetpack Compose, Room, Hilt, CameraX, DataStore

### KMP Shared Module
- Pure Kotlin, no platform dependencies
- Consumed by Android via Gradle project dependency
- iOS keeps its own Swift implementations (shared surface is small ~300 lines)

## Architecture

### iOS
- **MVVM** with `@Observable` macro (not ObservableObject)
- **SwiftData** for persistence (`SpeedEntry` is the sole `@Model`)
- **UserDefaults** for calibration data (`Calibration` struct with Codable)

### Android
- **MVVM** with `ViewModel` + `StateFlow`
- **Room** for persistence (`SpeedEntryEntity`)
- **DataStore** for calibration settings
- **Hilt** for dependency injection
- **Coroutines** for async operations

### Shared (KMP)
- `SpeedCalculator` — speed formula, V85, traffic stats
- `CoordinateMapper` — view-to-image coordinate transforms (uses `Point`/`Size` instead of CGPoint/CGSize)
- All enums: `VehicleType`, `TravelDirection`, `CalibrationMethod`, `SpeedCategory`, `VehicleCategory`
- Data: `VehicleReferences`, `RoadStandards`, `TrafficStats`

## Key Patterns

- All coordinate mapping goes through `CoordinateMapper` (shared KMP on Android, Swift original on iOS)
- Speed formula: `(pixelDisplacement / pixelsPerFoot) / timeDelta * 0.681818` -> MPH
- V85 uses interpolated 85th percentile over sorted speed array
- `CaptureViewModel` is a state machine: `selectSource -> recording -> selectFrames -> markFrame1 -> markFrame2 -> result`
- Dark theme enforced on both platforms
- Enums stored as `String` rawValues in persistence layers

## Code Style

### iOS
- Swift 5.9+ with strict concurrency
- Prefer `@Observable` over `@ObservableObject`/`@Published`
- No external dependencies — Apple frameworks only

### Android/Shared
- Kotlin 2.1+
- Jetpack Compose for UI
- `StateFlow` for observable state
- Hilt `@Inject` for DI

## Testing

### iOS
- Swift Testing (`@Test`) in `ios/SlowDownTests/`
- Tests for `SpeedCalculator` and `CoordinateMapper`

### KMP Shared
- `kotlin.test` in `shared/src/commonTest/`
- Parity tests validating identical results to Swift originals

## Common Tasks

- **Modify the speed formula**: Update both `shared/.../SpeedCalculator.kt` AND `ios/SlowDown/Models/SpeedCalculator.swift`. Run both test suites.
- **Add a new enum value**: Update `shared/.../model/Enums.kt` and `ios/SlowDown/Models/Calibration.swift`
- **Add an iOS view**: Create under `ios/SlowDown/Views/`, run `cd ios && xcodegen generate`
- **Add an Android screen**: Create under `android/app/.../ui/`, add to navigation in `SlowDownApp.kt`
- **Add a new model property**: Update `SpeedEntryEntity.kt` (Android) and `SpeedEntry.swift` (iOS), plus Room migration if needed

## CI/CD

- **iOS CI** (`.github/workflows/ci.yml`) — Runs on `ios/` changes: XcodeGen generate, build, test
- **Android CI** (`.github/workflows/android-ci.yml`) — Runs on `android/`, `shared/`, or Gradle file changes: shared tests, build, unit tests, lint
- **Beta Release** (`.github/workflows/beta-release.yml`) — Triggers when both CIs pass on `main`: builds signed IPA + AAB, uploads to TestFlight and Google Play internal track, creates `vX.Y.Z-beta.N` tag and GitHub pre-release
- **Release** (`.github/workflows/release.yml`) — Triggered by `vX.Y.Z` tags (no pre-release suffix): builds both platforms in parallel, creates GitHub Release with iOS archive + Android APK/AAB

### Versioning

- `VERSION` file at repo root is the marketing version (e.g., `1.0.0`)
- Build numbers derived from total `v*` tag count — no commits needed to bump
- Beta numbers derived from `v{version}-beta.*` tag count for current version
- To bump version: edit the `VERSION` file only. The `sync-version` workflow auto-updates `gradle.properties`, `ios/project.yml`, and `android/app/build.gradle.kts` on push to main
