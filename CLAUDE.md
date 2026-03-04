# CLAUDE.md

Instructions for Claude Code when working on the SlowDown project.

## Project Overview

SlowDown is a native iOS app for estimating vehicle speeds on residential streets using video analysis. It targets iOS 17+ and uses only Apple frameworks (no third-party dependencies).

## Build System

- **XcodeGen** generates `SlowDown.xcodeproj` from `project.yml`
- After modifying project structure (adding/removing files, changing targets), run: `xcodegen generate`
- Build with: `xcodebuild build -project SlowDown.xcodeproj -scheme SlowDown -destination 'platform=iOS Simulator,name=iPhone 17 Pro'`
- The `.xcodeproj` is a generated artifact — edit `project.yml` for project-level changes, not the pbxproj directly

## Architecture

- **MVVM** with `@Observable` macro (not ObservableObject)
- **SwiftData** for persistence (`SpeedEntry` is the sole `@Model`)
- **UserDefaults** for calibration data (`Calibration` struct with Codable)
- Enums stored as `String` rawValues in SwiftData for compatibility; computed properties provide typed access

## Key Patterns

- All coordinate mapping between view space and image space goes through `CoordinateMapper`
- Speed formula: `(pixelDisplacement / pixelsPerFoot) / timeDelta * 0.681818` → MPH
- V85 uses interpolated 85th percentile over sorted speed array
- `CaptureViewModel` is a state machine: `selectSource → recording → selectFrames → markFrame1 → markFrame2 → result`
- Dark theme enforced via `.preferredColorScheme(.dark)`
- `#if DEBUG` seed data auto-populates on first launch in debug builds

## Code Style

- Swift 5.9+ with strict concurrency where applicable
- Prefer `@Observable` over `@ObservableObject`/`@Published`
- Use `@Bindable` for bindings to `@Observable` objects in views
- Keep SwiftUI views small — extract complex subviews into separate structs to avoid type-checker timeouts
- No external dependencies — use Apple frameworks only

## File Organization

```
SlowDown/
├── Models/        # Data types, enums, business logic
├── Services/      # Framework wrappers (AV, Location, Haptics)
├── ViewModels/    # @Observable view models
├── Views/         # SwiftUI views organized by feature
│   ├── Calibrate/
│   ├── Capture/
│   ├── Components/
│   ├── Log/
│   ├── Reports/
│   └── Shared/
├── Debug/         # Debug-only utilities
└── SlowDownApp.swift
```

## Testing

No test target exists yet. When adding tests:
- Use Swift Testing (`@Test`) over XCTest for new tests
- `SpeedCalculator` and `CoordinateMapper` are pure functions — ideal unit test targets
- ViewModels are `@Observable` classes — test state transitions directly

## Common Tasks

- **Add a new model property**: Update `SpeedEntry.swift`, add the stored property (use String rawValue for enums), add computed accessor if needed. Seed data in `SeedData.swift` may need updating.
- **Add a new view**: Create the file under the appropriate `Views/` subdirectory. XcodeGen auto-discovers Swift files, so just run `xcodegen generate` afterward.
- **Modify the speed formula**: Edit `SpeedCalculator.calculateSpeedMPH()`. The same formula is used for both manual-distance and vehicle-reference calibration methods.
