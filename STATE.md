# STATE.md

Current project state for cross-session continuity. Updated each session.

**Last updated:** 2026-03-16

## Version

- Marketing version: `1.0.0` (from `VERSION` file)
- Build numbers derived from `v*` tag count

## Platform Status

| Platform | Build Status | Notes |
|----------|-------------|-------|
| Android  | Passing     | Full feature parity with iOS as of `d246580` |
| iOS      | CI passing, beta archive failing | Provisioning profile needs regeneration (see Known Issues) |
| Shared (KMP) | Passing | `SpeedCalculator`, `CoordinateMapper`, enums, models |

## Recent Work

- **Dependency update (2026-03-16)**: Updated all dependencies to latest versions. Major changes:
  - Gradle 8.13 → 9.4.0, AGP 8.13.2 → 9.1.0, Kotlin 2.2.20 → 2.3.10
  - Migrated shared module from `com.android.library` to `com.android.kotlin.multiplatform.library` (AGP 9 KMP plugin)
  - KSP 2.2.20-2.0.4 → 2.3.5, Hilt 2.56.2 → 2.57.1
  - Compose BOM 2024.12.01 → 2026.02.01, Lifecycle 2.8.7 → 2.10.0, Navigation 2.8.5 → 2.9.7
  - Room 2.7.1 → 2.8.4, CameraX 1.4.1 → 1.5.1, Coil 2.7.0 → 3.4.0
  - Firebase Android BOM 33.7.0 → 34.10.0, Firebase iOS SDK 11.7.0 → 12.10.0
  - Refreshed README, AGENTS.md, CONTRIBUTING.md, CLAUDE.md, STATE.md
- **Android feature parity** (`13ce129`, `d246580`): Implemented all missing Android features — log search/filter/sort, swipe-to-delete, CSV/PDF export, report charts, V85 card, vehicle ref picker, frame marker overlays, full result form, haptic feedback. Then fixed 7 behavioral gaps (vehicle ref toggle placement, V85 speed limit source, measurement details, haptic timing, location permission, calibration timestamp).
- **Distribution cert upgrade** (`959963d`): Updated iOS distribution certificate.
- **Build signing fixes** (`e7da01c`, `112b87e`): Moved signing settings off xcodebuild CLI to fix SPM target errors.

## Known Issues

1. **iOS beta archive fails**: `Provisioning profile doesn't include signing certificate "iPhone Distribution: Duncan Grazier"`. The distribution cert was upgraded (`959963d`) but the provisioning profile wasn't regenerated to include the new cert. **Fix**: Regenerate profile in Apple Developer portal, update `IOS_PROVISIONING_PROFILE_BASE64` GitHub secret.
2. **Dependency update not build-verified**: The AGP 9 migration (shared module → `com.android.kotlin.multiplatform.library`) and other dependency bumps need a build verification pass on both platforms. CI will validate on next push.

## Open PRs

None currently open.

## Key Decisions

- Dark theme enforced on both platforms
- No external charting libraries — Canvas-based custom charts
- Firebase Crashlytics is the only external dependency on iOS (via SPM)
- Enums stored as `String` rawValues in both Room and SwiftData
- Cross-platform parity is mandatory for all features and fixes
- AGP 9 KMP plugin (`com.android.kotlin.multiplatform.library`) replaces `com.android.library` for the shared module
