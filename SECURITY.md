# Security Policy

## Reporting a Vulnerability

If you discover a security vulnerability in SlowThemDown, please report it privately:

**Email:** itsmeduncan+security@gmail.com

Do **not** open a public GitHub issue for security vulnerabilities.

Please include:
- Description of the vulnerability
- Steps to reproduce
- Affected platform(s) (iOS, Android, or both)
- Potential impact

You can expect an initial response within 72 hours.

## Scope

SlowThemDown processes all data on-device. There is no backend server, no user authentication, and no cloud storage. The security surface is limited to:

- **On-device data storage** — SwiftData (iOS), Room (Android)
- **PII blurring pipeline** — face and license plate detection and blurring
- **Export/sharing** — PDF and CSV generation, email composition
- **Agency directory** — static JSON bundled with the app
- **Crash reporting** — Firebase Crashlytics (the only network dependency)

## Firebase Configuration Files

The `GoogleService-Info.plist` (iOS) and `google-services.json` (Android) files are **intentionally committed** to this repository. These contain public Firebase project configuration (API keys, project IDs) that are restricted to Firebase services. They are not secrets — this is standard practice per [Firebase documentation](https://firebase.google.com/docs/projects/api-keys).

## Data at Rest

SlowThemDown stores speed measurements (including GPS coordinates) on-device only:

- **iOS**: SwiftData with default file protection (`NSFileProtectionComplete` when device is locked)
- **Android**: Room with standard SQLite (unencrypted)

Speed and location data collected by this app is the user's own measurements of public traffic — it is not considered highly sensitive personal data. GPS coordinates identify the street being measured, not the user's private location. No encryption beyond platform defaults is applied. If stronger protection is needed in the future, SQLCipher can be added for Android.

## Supported Versions

| Platform | Supported |
|----------|-----------|
| iOS (latest release) | Yes |
| Android (latest release) | Yes |
| Older releases | Best effort |
