# STATE.md

Current project state for cross-session continuity. Updated each session.

**Last updated:** 2026-03-21

## Version

- Marketing version: `1.0.0` (from `VERSION` file)
- Build numbers derived from `v*` tag count

## Platform Status

| Platform | Build Status | Notes |
|----------|-------------|-------|
| Android  | Passing     | Full feature parity with iOS |
| iOS      | Passing     | Xcode 26 / iOS 26 SDK, Swift 5 language mode |
| Shared (KMP) | Passing | `SpeedCalculator`, `CoordinateMapper`, enums, models |

## Recent Work

- **Agency directory & email reporting (2026-03-21)**: Added `data/agencies.json` (crowd-sourced agency directory), agency matching by location, and "Report to Agency" email flow on both platforms. Includes AgencyPickerView (iOS) / AgencyPickerSheet (Android), email composition with PDF attachment and inline stats, JSON schema validation CI, and matching logic tests. Issues #6, #7.
- **CI/CD fixes (2026-03-21)**: Fixed IPA artifact upload path in beta-release and release workflows (explicit find step). Bumped `gradle/actions/setup-gradle` v4 → v5 (Node.js 24) and `r0adkll/upload-google-play` v1 → v1.1 across all workflows.
- **Xcode 26 / iOS 26 SDK (2026-03-21)**: Upgraded xcodeVersion to 26.0, CI runners to macos-26, Swift 6.1 toolchain with Swift 5 language mode. Added `ITSAppUsesNonExemptEncryption = NO` to skip App Store encryption prompt. Closes issue #22.
- **Report street filtering (2026-03-21)**: Added street filter chip bar and per-street breakdown section to reports on both platforms. All stats, charts, and exports respect the selected street filter. PR #29 (merged).
- **License plate blurring (2026-03-21)**: Extended PIIBlurService to detect and blur license plates alongside faces using platform OCR (Apple Vision, ML Kit text-recognition). Renamed `blurFaces` → `blurPII`. PR #28 (merged).
- **Face blurring (2026-03-20)**: Added PIIBlurService on both platforms using Apple Vision VNDetectFaceRectanglesRequest (iOS) and ML Kit FaceDetector (Android). PR #26 (merged).
- **Dependency update (2026-03-16)**: Updated all dependencies to latest versions. Gradle 9.4.0, AGP 9.1.0, Kotlin 2.3.10, Compose BOM 2026.02.01, Room 2.8.4, Firebase iOS SDK 12.10.0.

## Known Issues

None currently tracked.

## Open PRs

None currently open.

## Key Decisions

- Dark theme enforced on both platforms
- No external charting libraries — Canvas-based custom charts (Android), Swift Charts (iOS)
- Firebase Crashlytics is the only external dependency on iOS (via SPM)
- Enums stored as `String` rawValues in both Room and SwiftData
- Cross-platform parity is mandatory for all features and fixes
- AGP 9 KMP plugin (`com.android.kotlin.multiplatform.library`) replaces `com.android.library` for the shared module
- PII blurring errs toward over-blurring — a false positive (blurred sign) is preferable to a false negative (visible plate/face)
- Swift 5 language mode used with Swift 6.1 toolchain; full Swift 6 concurrency adoption deferred
