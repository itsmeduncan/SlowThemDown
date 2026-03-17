# SlowThemDown

A cross-platform app for estimating vehicle speeds on residential streets using video analysis. Available for **iOS** and **Android**.

SlowThemDown helps residents, neighborhood groups, and traffic safety advocates collect speed data on their streets. Record or import a video clip, mark a vehicle across two frames, and SlowThemDown calculates the estimated speed using pixel displacement and a calibrated reference distance.

## Features

- **Capture** — Record video or import from your library, select two frames, mark the same point on a vehicle in each frame, and get an instant speed estimate
- **Calibrate** — Establish a pixels-per-foot ratio using a known distance in your scene (e.g., lane width) or use a vehicle-as-reference method with a built-in lookup table of common vehicle lengths
- **Log** — Browse all recorded speed entries with search, filtering by vehicle type, and over-limit highlighting
- **Reports** — V85 speed analysis, speed distribution histogram, hourly averages, scatter plot over time, and PDF/CSV export for sharing with local officials

## Requirements

### iOS
- iOS 17.0+
- Xcode 15.0+
- Swift 5.9+
- [XcodeGen](https://github.com/yonaskolb/XcodeGen)

### Android
- Android 8.0+ (API 26)
- JDK 17+
- Android Studio Meerkat or later

## Getting Started

### iOS

```bash
git clone https://github.com/itsmeduncan/SlowThemDown.git
cd SlowThemDown
brew install xcodegen
cd ios && xcodegen generate
open SlowThemDown.xcodeproj
```

Select an iOS Simulator target and press **Cmd+R** to build and run. Debug builds auto-seed 50 realistic speed entries so the Log and Reports tabs are populated immediately.

### Android

```bash
git clone https://github.com/itsmeduncan/SlowThemDown.git
cd SlowThemDown
./gradlew :android:app:assembleDebug
```

Or open the root directory in Android Studio — it will detect the Gradle project and configure automatically.

## Project Structure

```
SlowThemDown/
├── ios/                        # iOS app (SwiftUI)
│   ├── project.yml             # XcodeGen project spec
│   ├── SlowThemDown/
│   │   ├── Models/             # Data models, enums, speed math
│   │   ├── Services/           # AVFoundation, CoreLocation, Haptics
│   │   ├── ViewModels/         # @Observable view models
│   │   ├── Views/              # SwiftUI views by feature
│   │   ├── Debug/              # Seed data for debug builds
│   │   └── SlowThemDownApp.swift
│   └── SlowThemDownTests/          # Swift Testing unit tests
├── android/                    # Android app (Jetpack Compose)
│   └── app/src/main/java/com/slowthemdown/android/
│       ├── data/               # Room database, DataStore
│       ├── di/                 # Hilt dependency injection
│       ├── service/            # Video extraction, location
│       ├── viewmodel/          # ViewModel + StateFlow
│       └── ui/                 # Compose screens by feature
├── shared/                     # KMP shared module (pure Kotlin)
│   └── src/
│       ├── commonMain/         # SpeedCalculator, CoordinateMapper, enums
│       └── commonTest/         # Parity tests with Swift originals
├── build.gradle.kts            # Root Gradle build
├── settings.gradle.kts         # Gradle settings
├── VERSION                     # Central marketing version (e.g., 1.0.0)
└── .github/workflows/          # CI/CD for both platforms
```

## CI/CD

SlowThemDown uses GitHub Actions for continuous integration and automated beta releases.

| Workflow | Trigger | What it does |
|---|---|---|
| **iOS CI** | Push/PR to `main` (ios/ changes) | Build + test on iOS Simulator |
| **Android CI** | Push/PR to `main` (android/, shared/, gradle changes) | Shared tests, build, unit tests, lint |
| **Beta Release** | Both CIs pass on `main` | Builds signed IPA + AAB, uploads to TestFlight and Google Play internal track, tags `vX.Y.Z-beta.N` |
| **Release** | Push `vX.Y.Z` tag (no pre-release suffix) | Builds both platforms, creates GitHub Release with artifacts |

### Versioning

- The `VERSION` file at repo root is the single source of truth for the marketing version
- **Build numbers** are derived from the total count of `v*` git tags (monotonically increasing)
- **Beta numbers** are derived from the count of `v{version}-beta.*` tags for the current version
- To bump the version: update the `VERSION` file in a PR — the next merge to `main` auto-generates `vX.Y.Z-beta.1`

## How It Works

### Speed Calculation

SlowThemDown estimates speed by measuring how far a vehicle moves (in pixels) between two video frames with a known time delta:

```
distance_feet = pixel_displacement / pixels_per_foot
speed_mph = (distance_feet / time_delta_seconds) * 0.681818
```

The `pixels_per_foot` ratio comes from calibration — either by marking a known distance in the scene or by using a vehicle's known length as a reference.

### V85

The V85 (85th percentile speed) is a standard traffic engineering metric. It represents the speed at or below which 85% of vehicles travel. SlowThemDown computes V85 using linear interpolation over sorted speed measurements.

### Calibration Methods

| Method | How It Works |
|--------|-------------|
| **Manual Distance** | Mark two points in a reference image with a known real-world distance (e.g., a 10-ft lane width). SlowThemDown computes pixels-per-foot. |
| **Vehicle Reference** | Select a vehicle make/model from the built-in lookup table. Mark the vehicle's front and rear in a frame. SlowThemDown uses the known vehicle length to derive pixels-per-foot per-capture. |

## Tech Stack

### iOS
- **SwiftUI** with `@Observable` (MVVM)
- **SwiftData** for persistent storage
- **AVFoundation** for video frame extraction
- **Swift Charts** for visualizations
- **CoreLocation** for street name geocoding
- **Firebase Crashlytics** via SPM for crash reporting

### Android
- **Jetpack Compose** with `ViewModel` + `StateFlow` (MVVM)
- **Room** for persistent storage
- **MediaMetadataRetriever** for video frame extraction
- **Hilt** for dependency injection
- **DataStore** for calibration settings
- **FusedLocationProviderClient** for location

### Shared (KMP)
- **Kotlin Multiplatform** shared module with pure business logic
- `SpeedCalculator` — speed formula, V85, traffic statistics
- `CoordinateMapper` — view-to-image coordinate transforms
- All enums and reference data (vehicle lengths, road standards)

## Contributing

Contributions are welcome! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## License

This project is licensed under the MIT License — see [LICENSE](LICENSE) for details.
