# STATE.md

Current project state for cross-session continuity. Updated each session.

**Last updated:** 2026-03-14

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

- **Android feature parity** (`13ce129`, `d246580`): Implemented all missing Android features — log search/filter/sort, swipe-to-delete, CSV/PDF export, report charts, V85 card, vehicle ref picker, frame marker overlays, full result form, haptic feedback. Then fixed 7 behavioral gaps (vehicle ref toggle placement, V85 speed limit source, measurement details, haptic timing, location permission, calibration timestamp).
- **Distribution cert upgrade** (`959963d`): Updated iOS distribution certificate.
- **Build signing fixes** (`e7da01c`, `112b87e`): Moved signing settings off xcodebuild CLI to fix SPM target errors.

## Known Issues

1. **iOS beta archive fails**: `Provisioning profile doesn't include signing certificate "iPhone Distribution: Duncan Grazier"`. The distribution cert was upgraded (`959963d`) but the provisioning profile wasn't regenerated to include the new cert. **Fix**: Regenerate profile in Apple Developer portal, update `IOS_PROVISIONING_PROFILE_BASE64` GitHub secret.

## Open PRs

None currently open.

## Key Decisions

- Dark theme enforced on both platforms
- No external charting libraries — Canvas-based custom charts
- No external dependencies on iOS (Apple frameworks only)
- Enums stored as `String` rawValues in both Room and SwiftData
- Cross-platform parity is mandatory for all features and fixes
