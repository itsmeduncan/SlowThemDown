# Changelog

All notable changes to Slow Them Down are documented here.

This changelog is automatically generated from the git history.

## [1.0.0](https://github.com/itsmeduncan/SlowThemDown/releases/tag/v1.0.0) — 2026-03-22

### Bug Fixes

- Fix changelog branch name collision with timestamp suffix
- Fix changelog workflow: create PR instead of pushing to protected main
- Fix changelog workflow: add pull-requests permission and pass token

### Changes

- Update CHANGELOG.md for v1.0.0 (#49)

### Other

- Make changelog auto-merge optional, fall back to notice
- Revert changelog to PR approach now that Actions can create PRs
- Changelog workflow: use Contents API instead of PR creation
## [1.0.0-beta.18](https://github.com/itsmeduncan/SlowThemDown/releases/tag/v1.0.0-beta.18) — 2026-03-22

### Bug Fixes

- Fix agency matching when geocoder returns county/city suffixes
- Fix beta tag push rejected by workflow file protection
- Fix release workflow: add Play Store upload and signing

### Features

- Add workflows permission to beta-release for tag push

### Other

- Sync documentation with current codebase state
- Replace fragile sed patterns in sync-version workflow

### Refactoring

- Refactor large files: split CaptureScreen.kt and ReportView.swift
## [1.0.0-beta.17](https://github.com/itsmeduncan/SlowThemDown/releases/tag/v1.0.0-beta.17) — 2026-03-22

### Other

- Extract hardcoded strings for i18n on both platforms
## [1.0.0-beta.16](https://github.com/itsmeduncan/SlowThemDown/releases/tag/v1.0.0-beta.16) — 2026-03-22

### Changes

- Pre-launch audit remediation: store blockers, crash prevention, polish

### Features

- Add auto-generated CHANGELOG.md from git history
## [1.0.0-beta.15](https://github.com/itsmeduncan/SlowThemDown/releases/tag/v1.0.0-beta.15) — 2026-03-22

### Bug Fixes

- Fix agency matching when geocoder returns full state names
- Fix inaccurate touch targeting on Android frame markers
## [1.0.0-beta.14](https://github.com/itsmeduncan/SlowThemDown/releases/tag/v1.0.0-beta.14) — 2026-03-22

### Changes

- Polish Android UI to match iOS production quality
- Calibration save confirmation, PII timeout, and migration crash fix
## [1.0.0-beta.13](https://github.com/itsmeduncan/SlowThemDown/releases/tag/v1.0.0-beta.13) — 2026-03-21

### Bug Fixes

- Fix iOS CI test host resolution failure
- Fix iOS home screen app name and Android debug migration hang
- Fix beta-release and release CI/CD failures
- Fix all build warnings across Android and shared module
- Fix Android splash screen to show updated logo
- Fix CI/CD warnings for IPA artifact path and Node.js 20 deprecation

### Features

- Support metric measurements (#30)
- Add San Clemente, Orange County, and Caltrans to agency directory
- Add agency directory and email reporting

### Other

- Navigate to Calibrate tab after onboarding completes
- Expand agency directory to 32 agencies across 17 states
- Prepare repository for open sourcing
- Sync documentation with current codebase state

### Removed

- Remove checked-in agencies.json copy from Android assets
## [1.0.0-beta.12](https://github.com/itsmeduncan/SlowThemDown/releases/tag/v1.0.0-beta.12) — 2026-03-21

### Changes

- Upgrade to Xcode 26 / iOS 26 SDK

### Features

- Add street filtering and grouping to reports
## [1.0.0-beta.11](https://github.com/itsmeduncan/SlowThemDown/releases/tag/v1.0.0-beta.11) — 2026-03-21

### Features

- Add license plate blurring to PII blur pipeline

### Other

- Declare no non-exempt encryption in Info.plist
## [1.0.0-beta.10](https://github.com/itsmeduncan/SlowThemDown/releases/tag/v1.0.0-beta.10) — 2026-03-21

### Other

- Blur faces in displayed frames to protect PII
## [1.0.0-beta.9](https://github.com/itsmeduncan/SlowThemDown/releases/tag/v1.0.0-beta.9) — 2026-03-20

### Bug Fixes

- Fix var-to-let warnings in ReportViewModelTests

### Features

- Add comprehensive unit tests across all platforms
## [1.0.0-beta.8](https://github.com/itsmeduncan/SlowThemDown/releases/tag/v1.0.0-beta.8) — 2026-03-20

### Other

- Auto-fill nearest intersection from GPS on result screen
## [1.0.0-beta.7](https://github.com/itsmeduncan/SlowThemDown/releases/tag/v1.0.0-beta.7) — 2026-03-20

### Bug Fixes

- Fix CI compilation errors from report export overlay
- Fix stale calibration warning on iOS Capture tab

### Features

- Add Firebase credentials to beta and release workflows
- Add Crashlytics dSYM upload build phase
- Add log entry detail view on both platforms
- Add RECORD_AUDIO permission to Android manifest

### Other

- Only run Crashlytics dSYM upload for Release builds
- Skip Crashlytics dSYM upload when run binary is missing
- Show loading overlay during report PDF/CSV generation
## [1.0.0-beta.6](https://github.com/itsmeduncan/SlowThemDown/releases/tag/v1.0.0-beta.6) — 2026-03-20

### Features

- Add NSMicrophoneUsageDescription to fix TCC crash
## [1.0.0-beta.5](https://github.com/itsmeduncan/SlowThemDown/releases/tag/v1.0.0-beta.5) — 2026-03-20

### Changes

- Update app icon on both platforms
## [1.0.0-beta.4](https://github.com/itsmeduncan/SlowThemDown/releases/tag/v1.0.0-beta.4) — 2026-03-20

### Bug Fixes

- Fix TestFlight validation: add orientations and remove icon alpha
- Fix TestFlight upload: add destination=upload to ExportOptions

### Changes

- Restrict app to iPhone only — exclude iPadOS
- Bump upload-artifact and download-artifact to v6 for Node.js 24

### Other

- Disable Gradle configuration cache in CI for KSP2 compatibility
## [1.0.0-beta.3](https://github.com/itsmeduncan/SlowThemDown/releases/tag/v1.0.0-beta.3) — 2026-03-20

### Bug Fixes

- Fix source hash: use shasum instead of sha256sum on macOS
- Fix stale xcodeproj cache missing new source files
- Fix iOS CI: remove #if DEBUG guards from DemoBanner and call sites
- Fix IPA artifact upload using runner.temp expression syntax

### Features

- Add demo data banner to Log and Reports screens

### Removed

- Remove XcodeGen binary cache — use brew install directly
- Remove xcodeproj cache — always regenerate from project.yml
## [1.0.0-beta.2](https://github.com/itsmeduncan/SlowThemDown/releases/tag/v1.0.0-beta.2) — 2026-03-20

### Performance

- Optimize CI/CD with reusable iOS setup action and Gradle parallelism
## [1.0.0-beta.1](https://github.com/itsmeduncan/SlowThemDown/releases/tag/v1.0.0-beta.1) — 2026-03-20

### Bug Fixes

- Fix iOS export: use app-store-connect method and API key auth
- Fix Android feature gaps to match iOS behavior
- Fix beta archive failing due to provisioning profile on SPM targets
- Fix XcodeGen install failing silently in CI workflows
- Fix Firebase SPM package not included in Xcode project
- Fix iOS archive failing to resolve Firebase SPM dependencies
- Fix CI pipeline failures and restore original package names
- Fix IPA path in beta release by finding it dynamically
- Fix YAML syntax in sync-version workflow
- Fix Google Play upload for draft apps
- Fix build number collision and show version in Spaceship prompt
- Fix beta-release workflow permissions for GitHub API access
- Fix MediaMetadataRetriever lint error for API < 29
- Fix duplicate fillColor attribute in launcher icon

### Changes

- Bump GitHub Actions to v5 for Node.js 24 compatibility
- Upgrade distribtion cert
- Update all dependencies to latest and refresh documentation
- Upgrade distribtion cert
- Seed example data on Android in debug builds
- Rename SlowDown to SlowThemDown across all files and packages (#12)
- Rename app to "Slow Them Down" across all user-facing strings
- Update docs to reflect beta release CI/CD pipeline (#8)
- Bump Hilt to 2.56.2 for KSP2 compatibility
- Bump Room to 2.7.1 for KSP 2.x compatibility
- Update Gradle wrapper to 8.13 and gitignore .kotlin/ cache
- Update app icon to dark background variant

### Features

- Add signing and App Store upload to release workflow
- Add STATE.md for cross-session project state tracking
- Implement missing Android features to reach iOS parity
- Implement full calibration UI on Android matching iOS 3-step wizard
- Add Firebase Crashlytics for automatic crash reporting on both platforms (#13)
- Add sync-version workflow to auto-update version across platforms
- Add onboarding flow for iOS (#11)
- Add onboarding flow and debug seed data for Android (#10)
- Add automatic beta release to TestFlight and Google Play (#3)
- Add gradle-wrapper.jar to version control
- Add Android launcher icon resources
- Add Compose Compiler Gradle plugin required for Kotlin 2.0+
- Add Android app and KMP shared module for cross-platform support
- Add GitHub Actions CI/CD and unit test target
- Add open source documentation and project files

### Other

- Move all signing settings off xcodebuild CLI to fix SPM target errors
- Wire up Record Video button on Android to launch system camera
- Improve CI workflow caching and performance (#14)
- Replace app icons with new speedometer/camera lens design
- Match Android app icon to iOS (V85 stop sign) (#4)
- Merge pull request #2 from itsmeduncan/android/kmp-cross-platform
- Enable Gradle configuration cache for faster builds
- Merge pull request #1 from itsmeduncan/ci/github-actions-tests
- Initial commit: SlowDown iOS app Phase 1 MVP

### Removed

- Remove xcpretty from beta archive step to expose build errors

