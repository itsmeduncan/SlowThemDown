# Contributing to SlowDown

Thanks for your interest in contributing to SlowDown! This project aims to help communities collect traffic speed data on residential streets. It runs on both iOS and Android with shared business logic via Kotlin Multiplatform.

## Getting Started

1. Fork the repository
2. Clone your fork:
   ```bash
   git clone https://github.com/YOUR_USERNAME/SlowDown.git
   cd SlowDown
   ```
3. Set up the platform you're working on (or both):

### iOS Setup

```bash
brew install xcodegen
cd ios && xcodegen generate
open SlowDown.xcodeproj
```

### Android Setup

Open the root `SlowDown/` directory in Android Studio, or build from the command line:

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
xcodebuild build -project SlowDown.xcodeproj -scheme SlowDown \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' -quiet
xcodebuild test -project SlowDown.xcodeproj -scheme SlowDownTests \
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
- Kotlin 2.1+ with Jetpack Compose
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
| Speed formula | `shared/.../calculator/SpeedCalculator.kt` + `ios/SlowDown/Models/SpeedCalculator.swift` |
| Coordinate mapping | `shared/.../calculator/CoordinateMapper.kt` + `ios/SlowDown/Services/CoordinateMapper.swift` |
| Enums | `shared/.../model/Enums.kt` + `ios/SlowDown/Models/Calibration.swift` |
| Vehicle references | `shared/.../model/VehicleReferences.kt` + `ios/SlowDown/Models/VehicleReferences.swift` |
| Road standards | `shared/.../model/RoadStandards.kt` + `ios/SlowDown/Models/RoadStandards.swift` |
| Data model fields | `android/.../data/db/SpeedEntryEntity.kt` + `ios/SlowDown/Models/SpeedEntry.swift` |

### Commits

- Write clear, concise commit messages
- One logical change per commit

### Pull Requests

- Describe what the change does and why
- Specify which platform(s) the change affects
- Include steps to test the change
- Reference any related issues

## Reporting Issues

Open an issue on GitHub with:
- What you expected to happen
- What actually happened
- Steps to reproduce
- Platform and version (iOS version + device, or Android version + device)

## Areas for Contribution

- Improving speed estimation accuracy
- Adding new calibration methods
- Android frame marker overlay (Canvas + pointerInput tap-to-mark)
- Android CameraX video recording integration
- Android PDF report export
- Accessibility improvements
- Localization
- Unit and UI tests (both platforms)
- Documentation

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
