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

## Supported Versions

| Platform | Supported |
|----------|-----------|
| iOS (latest release) | Yes |
| Android (latest release) | Yes |
| Older releases | Best effort |
