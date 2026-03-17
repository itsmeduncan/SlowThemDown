# Contributing to SlowThemDown

Thanks for your interest in contributing to SlowThemDown! This project aims to help communities collect traffic speed data on residential streets. It runs on both iOS and Android with shared business logic via Kotlin Multiplatform.

## Getting Started

1. Fork the repository
2. Clone your fork:
   ```bash
   git clone https://github.com/YOUR_USERNAME/SlowThemDown.git
   cd SlowThemDown
   ```
3. Set up the platform you're working on (or both):

### iOS Setup

```bash
brew install xcodegen
cd ios && xcodegen generate
open SlowThemDown.xcodeproj
```

### Android Setup

Open the root `SlowThemDown/` directory in Android Studio, or build from the command line:

```bash
./gradlew :android:app:assembleDebug
```

Requires JDK 17+.

## Development Workflow

1. Create a feature branch: `git checkout -b my-feature`
2. Make your changes
3. Verify your changes build and pass tests (see below)
4. Commit and push your branch
5. Open a pull request

### Verifying iOS Changes

```bash
cd ios && xcodegen generate    # if you added or removed files
xcodebuild build -project SlowThemDown.xcodeproj -scheme SlowThemDown \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' -quiet
xcodebuild test -project SlowThemDown.xcodeproj -scheme SlowThemDownTests \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' -quiet
```

### Verifying Android Changes

```bash
./gradlew :android:app:assembleDebug
./gradlew :android:app:testDebugUnitTest
./gradlew :android:app:lintDebug
```

### Verifying Shared Module Changes

```bash
./gradlew :shared:allTests
```

If you modify shared business logic (speed formula, coordinate mapping, enums), you must update **both** the KMP shared module and the iOS Swift originals, then run tests on both.

## Guidelines

### Code

#### iOS
- Swift 5.9+ with `@Observable` (not `@ObservableObject`)
- No external dependencies — Apple frameworks only
- Keep SwiftUI view bodies small to avoid type-checker timeouts
- Follow existing MVVM patterns

#### Android
- Kotlin 2.3+ with Jetpack Compose
- Use `ViewModel` + `StateFlow` for state management
- Use `@HiltViewModel` with `@Inject constructor` for dependency injection
- Use Room for persistence, DataStore for preferences

#### KMP Shared Module
- Pure Kotlin only — no platform-specific imports
- Must produce identical results to Swift originals for the same inputs

### Cross-Platform Changes

Changes to shared business logic require updates in multiple places:

| What Changed | Update These Files |
|---|---|
| Speed formula | `shared/.../calculator/SpeedCalculator.kt` + `ios/SlowThemDown/Models/SpeedCalculator.swift` |
| Coordinate mapping | `shared/.../calculator/CoordinateMapper.kt` + `ios/SlowThemDown/Services/CoordinateMapper.swift` |
| Enums | `shared/.../model/Enums.kt` + `ios/SlowThemDown/Models/Calibration.swift` |
| Vehicle references | `shared/.../model/VehicleReferences.kt` + `ios/SlowThemDown/Models/VehicleReferences.swift` |
| Road standards | `shared/.../model/RoadStandards.kt` + `ios/SlowThemDown/Models/RoadStandards.swift` |
| Data model fields | `android/.../data/db/SpeedEntryEntity.kt` + `ios/SlowThemDown/Models/SpeedEntry.swift` |

### Commits

- Write clear, concise commit messages
- One logical change per commit

### Pull Requests

- Describe what the change does and why
- Specify which platform(s) the change affects
- Include steps to test the change
- Reference any related issues

## CI/CD

GitHub Actions runs automatically on every push and pull request to `main`:

- **iOS CI** — Triggers on `ios/` changes: XcodeGen generate, build, test
- **Android CI** — Triggers on `android/`, `shared/`, or Gradle file changes: shared tests, build, unit tests, lint

When both CI workflows pass on `main`, a **Beta Release** workflow automatically:
1. Computes the next version from the `VERSION` file and existing git tags
2. Builds a signed IPA and uploads to TestFlight
3. Builds a signed AAB and uploads to Google Play internal track
4. Creates a git tag (e.g., `v1.0.0-beta.3`) and a GitHub pre-release

Production releases are triggered by pushing a `vX.Y.Z` tag (without a pre-release suffix).

### Versioning

The `VERSION` file at the repo root controls the marketing version. Build numbers are derived from git tag counts — no manual bumping needed. To start a new version series, update `VERSION` in your PR.

## Reporting Issues

Open an issue on GitHub with:
- What you expected to happen
- What actually happened
- Steps to reproduce
- Platform and version (iOS version + device, or Android version + device)

## Areas for Contribution

- Improving speed estimation accuracy
- Adding new calibration methods
- Accessibility improvements
- Localization
- Unit and UI tests (both platforms)
- Documentation

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
