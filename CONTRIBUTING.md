# Contributing to SlowDown

Thanks for your interest in contributing to SlowDown! This project aims to help communities collect traffic speed data on residential streets.

## Getting Started

1. Fork the repository
2. Clone your fork and set up the project:
   ```bash
   brew install xcodegen
   git clone https://github.com/YOUR_USERNAME/SlowDown.git
   cd SlowDown
   xcodegen generate
   open SlowDown.xcodeproj
   ```
3. Create a feature branch: `git checkout -b my-feature`
4. Make your changes
5. Run `xcodegen generate` if you added or removed files
6. Verify the build: `xcodebuild build -project SlowDown.xcodeproj -scheme SlowDown -destination 'platform=iOS Simulator,name=iPhone 17 Pro' -quiet`
7. Commit and push your branch
8. Open a pull request

## Guidelines

### Code

- Use Swift 5.9+ and SwiftUI with `@Observable`
- No external dependencies — Apple frameworks only
- Follow existing patterns in the codebase (MVVM, extracted subviews, enum rawValues in SwiftData)
- Keep SwiftUI view bodies small to avoid type-checker timeouts

### Commits

- Write clear, concise commit messages
- One logical change per commit

### Pull Requests

- Describe what the change does and why
- Include steps to test the change
- Reference any related issues

## Reporting Issues

Open an issue on GitHub with:
- What you expected to happen
- What actually happened
- Steps to reproduce
- iOS version and device/simulator used

## Areas for Contribution

- Improving speed estimation accuracy
- Adding new calibration methods
- Accessibility improvements
- Localization
- Unit and UI tests
- Documentation

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
