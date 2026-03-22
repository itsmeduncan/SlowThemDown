# CLAUDE.md

Instructions for Claude Code when working on the SlowThemDown project.

## Project Overview

SlowThemDown is a cross-platform app for estimating vehicle speeds on residential streets using video analysis. It targets iOS 17+ (SwiftUI) and Android 8.0+ (Jetpack Compose) with shared business logic via Kotlin Multiplatform (KMP).

## Repository Structure

```
SlowThemDown/
├── ios/                    # iOS app (SwiftUI + SwiftData)
│   ├── project.yml         # XcodeGen spec
│   ├── SlowThemDown/           # iOS source files
│   └── SlowThemDownTests/      # iOS tests (Swift Testing)
├── android/                # Android app (Jetpack Compose + Room)
│   └── app/src/main/java/com/slowthemdown/android/
├── shared/                 # KMP shared module (pure Kotlin)
│   └── src/commonMain/kotlin/com/slowthemdown/shared/
│       ├── model/          # Enums, data classes, constants
│       └── calculator/     # SpeedCalculator, CoordinateMapper
├── build.gradle.kts        # Root Gradle build
├── settings.gradle.kts     # Gradle settings
└── .github/workflows/      # CI/CD
```

## Build System

### iOS
- **XcodeGen** generates `SlowThemDown.xcodeproj` from `ios/project.yml`
- Generate: `cd ios && xcodegen generate`
- Build: `xcodebuild build -project ios/SlowThemDown.xcodeproj -scheme SlowThemDown -destination 'platform=iOS Simulator,name=iPhone 17 Pro'`
- Firebase Crashlytics via SPM; otherwise Apple frameworks only

### Android
- **Gradle** with Kotlin DSL
- Build: `./gradlew :android:app:assembleDebug`
- Test shared module: `./gradlew :shared:allTests`
- Test Android: `./gradlew :android:app:testDebugUnitTest`
- Lint: `./gradlew :android:app:lintDebug`
- Coverage: `./gradlew :android:app:koverXmlReport` (requires tests to have run first)
- Dependencies: Jetpack Compose, Room, Hilt, CameraX, DataStore, ML Kit (face-detection, text-recognition), Kover (coverage)

### KMP Shared Module
- Pure Kotlin, no platform dependencies
- Consumed by Android via Gradle project dependency
- iOS keeps its own Swift implementations (shared surface is small ~300 lines)

## Architecture

### iOS
- **MVVM** with `@Observable` macro (not ObservableObject), `@MainActor` on all ViewModels
- **SwiftData** for persistence (`SpeedEntry` is the sole `@Model`)
- **UserDefaults** for calibration data (`Calibration` struct with Codable)
- **String Catalog** (`Localizable.xcstrings`) for i18n — SwiftUI views use `LocalizedStringKey` automatically

### Android
- **MVVM** with `ViewModel` + `StateFlow`
- **Room** for persistence (`SpeedEntryEntity`)
- **DataStore** for calibration settings
- **Hilt** for dependency injection
- **Coroutines** for async operations
- **String resources** (`strings.xml`) for i18n — all composables use `stringResource()`

### Shared (KMP)
- `SpeedCalculator` — speed formula, V85, traffic stats
- `CoordinateMapper` — view-to-image coordinate transforms (uses `Point`/`Size` instead of CGPoint/CGSize)
- All enums: `VehicleType`, `TravelDirection`, `CalibrationMethod`, `SpeedCategory`, `VehicleCategory`
- Data: `VehicleReferences`, `RoadStandards`, `TrafficStats`

## Key Patterns

- All coordinate mapping goes through `CoordinateMapper` (shared KMP on Android, Swift original on iOS)
- Speed formula: `(pixelDisplacement / pixelsPerMeter) / timeDeltaSeconds` → m/s (display conversion to MPH/km/h at the view layer via `UnitConverter`)
- V85 uses interpolated 85th percentile over sorted speed array
- `CaptureViewModel` is a state machine: `selectSource -> recording -> selectFrames -> markFrame1 -> markFrame2 -> result`
- Dark theme enforced on both platforms
- Enums stored as `String` rawValues in persistence layers

## Code Style

### iOS
- Swift 6.1 toolchain with Swift 5 language mode (Xcode 26+)
- Prefer `@Observable` over `@ObservableObject`/`@Published`
- Firebase Crashlytics via SPM (only external dependency)
- ML Kit–free PII blur pipeline (faces via Vision, plates via VNRecognizeTextRequest)

### Android/Shared
- Kotlin 2.3+
- Jetpack Compose for UI
- `StateFlow` for observable state
- Hilt `@Inject` for DI
- ML Kit face detection + text recognition for PII blurring

## Testing

### iOS
- Swift Testing (`@Test`) in `ios/SlowThemDownTests/`
- Tests for `SpeedCalculator`, `CoordinateMapper`, `Calibration`, `SpeedEntry`, `PIIBlurService`, `LogViewModel`, `ReportViewModel`, `CaptureViewModel`, `CalibrationViewModel`, `UnitConverter`, `AgencyDirectory`

### KMP Shared
- `kotlin.test` in `shared/src/commonTest/`
- Parity tests validating identical results to Swift originals

## Common Tasks

- **Modify the speed formula**: Update both `shared/.../SpeedCalculator.kt` AND `ios/SlowThemDown/Models/SpeedCalculator.swift`. Run both test suites.
- **Add a new enum value**: Update `shared/.../model/Enums.kt` and `ios/SlowThemDown/Models/Calibration.swift`
- **Add an iOS view**: Create under `ios/SlowThemDown/Views/`, run `cd ios && xcodegen generate`
- **Add an Android screen**: Create under `android/app/.../ui/`, add to navigation in `SlowThemDownApp.kt`
- **Add a new model property**: Update `SpeedEntryEntity.kt` (Android) and `SpeedEntry.swift` (iOS), plus Room migration if needed

## CI/CD

- **iOS CI** (`.github/workflows/ci.yml`) — Runs on `ios/` changes: XcodeGen generate, build, test, coverage report (xccov JSON)
- **Android CI** (`.github/workflows/android-ci.yml`) — Runs on `android/`, `shared/`, or Gradle file changes: shared tests, unit tests, lint, Kover coverage report (XML + HTML)
- **Beta Release** (`.github/workflows/beta-release.yml`) — Triggers when both CIs pass on `main`: builds signed IPA + AAB, uploads to TestFlight and Google Play internal track, creates `vX.Y.Z-beta.N` tag and GitHub pre-release
- **Release** (`.github/workflows/release.yml`) — Triggered by `vX.Y.Z` tags (no pre-release suffix): builds both platforms in parallel, uploads to App Store Connect (iOS) and Google Play production track as draft (Android), creates GitHub Release with artifacts
- **Changelog** (`.github/workflows/changelog.yml`) — Triggered by `vX.Y.Z` tags: regenerates `CHANGELOG.md` from git history using `git-cliff` and commits to `main`
- **Validate Agencies** (`.github/workflows/validate-agencies.yml`) — Runs on `data/` changes: validates `agencies.json` against the JSON schema

### Versioning

- `VERSION` file at repo root is the marketing version (e.g., `1.0.0`)
- Build numbers derived from total commit count (`git rev-list --count HEAD`) — monotonically increasing, no manual bumping needed
- Beta numbers derived from `v{version}-beta.*` tag count for current version
- To bump version: edit the `VERSION` file only. The `sync-version` workflow auto-updates `gradle.properties`, `ios/project.yml`, and `android/app/build.gradle.kts` on push to main
