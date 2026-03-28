# SlowThemDown

A cross-platform app for estimating vehicle speeds on residential streets using video analysis. Available for **iOS** and **Android**.

[![iOS CI](https://github.com/itsmeduncan/SlowThemDown/actions/workflows/ci.yml/badge.svg)](https://github.com/itsmeduncan/SlowThemDown/actions/workflows/ci.yml)
[![Android CI](https://github.com/itsmeduncan/SlowThemDown/actions/workflows/android-ci.yml/badge.svg)](https://github.com/itsmeduncan/SlowThemDown/actions/workflows/android-ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

SlowThemDown helps residents, neighborhood groups, and traffic safety advocates collect speed data on their streets. Record or import a video clip, mark a vehicle across two frames, and SlowThemDown calculates the estimated speed using pixel displacement and a calibrated reference distance.

**[Download on the App Store](https://apps.apple.com/app/slow-them-down/id6760197118)** | **[Join the Android Beta](https://play.google.com/store/apps/details?id=com.slowdown.android)**

## Features

- **Capture** — Record video or import from your library, select two frames, mark the same point on a vehicle in each frame, and get an instant speed estimate. The home screen doubles as the speed log — browse all recorded entries with search, filtering by vehicle type, and over-limit highlighting
- **Calibrate** — Establish a pixels-per-meter ratio using a known distance in your scene (e.g., lane width) or use a vehicle-as-reference method with a built-in lookup table of common vehicle lengths. Supports both imperial and metric units
- **Reports** — V85 speed analysis, speed distribution histogram, hourly averages, scatter plot over time, street-level filtering and breakdown, and PDF/CSV export for sharing with local officials
- **Report to Agency** — Send speed data directly to your local traffic department. The app matches your location to a built-in directory of city, county, and state agencies, then composes an email with V85 stats and an attached PDF report
- **Privacy** — Automatic face and license plate blurring on all captured frames using on-device detection (no data leaves the device)

## Requirements

### iOS
- iOS 17.0+
- Xcode 26+
- Swift 6.1 toolchain (Swift 5 language mode)
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
| **iOS CI** | Push/PR to `main` (ios/ changes) | Build, test, coverage report (xccov) |
| **Android CI** | Push/PR to `main` (android/, shared/, gradle changes) | Shared tests, unit tests, lint, coverage report (Kover) |
| **Beta Release** | Both CIs pass on `main` | Builds signed IPA + AAB, uploads to TestFlight and Google Play internal track, tags `vX.Y.Z-beta.N` |
| **Release** | Push `vX.Y.Z` tag (no pre-release suffix) | Builds both platforms, uploads to App Store Connect + Google Play (draft), creates GitHub Release |
| **Changelog** | Push `vX.Y.Z` tag | Regenerates `CHANGELOG.md` from git history via `git-cliff` |
| **Validate Agencies** | Push/PR with `data/` changes | Validates `agencies.json` against JSON schema |
| **Sync Version** | Push to `main` with `VERSION` change | Updates version in `gradle.properties`, `project.yml`, `build.gradle.kts` |

### Versioning

- The `VERSION` file at repo root is the single source of truth for the marketing version
- **Build numbers** are derived from total commit count (`git rev-list --count HEAD`), monotonically increasing
- **Beta numbers** are derived from the count of `v{version}-beta.*` tags for the current version
- To bump the version: update the `VERSION` file in a PR — the next merge to `main` auto-generates `vX.Y.Z-beta.1`

## How It Works

### Speed Calculation

SlowThemDown estimates speed by measuring how far a vehicle moves (in pixels) between two video frames with a known time delta:

```
distance_meters = pixel_displacement / pixels_per_meter
speed_mps = distance_meters / time_delta_seconds
```

All internal values are stored in SI units (meters, m/s). Display conversion to MPH or km/h happens at the view layer based on the user's measurement system preference.

The `pixels_per_meter` ratio comes from calibration — either by marking a known distance in the scene or by using a vehicle's known length as a reference.

### V85

The V85 (85th percentile speed) is a standard traffic engineering metric. It represents the speed at or below which 85% of vehicles travel. SlowThemDown computes V85 using linear interpolation over sorted speed measurements.

### Calibration Methods

| Method | How It Works |
|--------|-------------|
| **Manual Distance** | Mark two points in a reference image with a known real-world distance (e.g., a 10-ft lane width or 3m parking space). SlowThemDown computes pixels-per-meter. |
| **Vehicle Reference** | Select a vehicle make/model from the built-in lookup table. Mark the vehicle's front and rear in a frame. SlowThemDown uses the known vehicle length to derive pixels-per-meter per-capture. |

## Tech Stack

### iOS
- **SwiftUI** with `@Observable` (MVVM)
- **SwiftData** for persistent storage
- **AVFoundation** for video frame extraction
- **Swift Charts** for visualizations
- **CoreLocation** for street name geocoding
- **Vision** framework for face and license plate detection (PII blurring)
- **Firebase Crashlytics** via SPM for crash reporting

### Android
- **Jetpack Compose** with `ViewModel` + `StateFlow` (MVVM)
- **Room** for persistent storage
- **MediaCodec** + **MediaMetadataRetriever** for precise video frame extraction with rotation correction
- **Hilt** for dependency injection
- **DataStore** for calibration settings
- **ML Kit** face detection + text recognition for PII blurring
- **FusedLocationProviderClient** for location

### Shared (KMP)
- **Kotlin Multiplatform** shared module with pure business logic
- `SpeedCalculator` — speed formula, V85, traffic statistics
- `CoordinateMapper` — view-to-image coordinate transforms
- All enums and reference data (vehicle lengths, road standards)

## Agency Directory

SlowThemDown includes a built-in directory of local agencies (`data/agencies.json`) that accept speeding concern reports. The directory is crowd-sourced — anyone can add their city, county, or state agency by opening a PR.

See [CONTRIBUTING.md](CONTRIBUTING.md#adding-an-agency) for instructions on adding an agency.

## Contributing

Contributions are welcome! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

Key documents for contributors:

- [CONTRIBUTING.md](CONTRIBUTING.md) — Setup, workflow, code guidelines, and how to add agencies
- [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) — Community standards
- [AI_USAGE.md](AI_USAGE.md) — Policy on AI-assisted contributions
- [AGENTS.md](AGENTS.md) — Guidelines for AI agents working on the codebase

## AI Usage

AI coding assistants are allowed and encouraged for contributions to this project. Every AI-assisted contribution must be reviewed by a human before merging. See [AI_USAGE.md](AI_USAGE.md) for the full policy, including what AI should and should not be used for.

## Security

SlowThemDown processes all data on-device. There are no user accounts, no cloud storage, and no telemetry beyond crash reporting (Firebase Crashlytics). The PII blurring pipeline detects and blurs faces and license plates before any sharing or export.

If you discover a security vulnerability, please report it privately to itsmeduncan+security@gmail.com rather than opening a public issue.

## Disclaimer

SlowThemDown produces **speed estimates, not certified measurements**. Results should not be presented as legally admissible evidence. Users are responsible for their own safety while recording. See the [Terms of Service](https://itsmeduncan.com/slowdown/) for full details.

## License

This project is licensed under the MIT License — see [LICENSE](LICENSE) for details.
