## Summary

<!-- What does this PR do and why? -->

## Platforms affected

- [ ] iOS
- [ ] Android
- [ ] Shared (KMP)
- [ ] Data / CI / Docs only

## Verification

- [ ] iOS builds: `xcodebuild build -project ios/SlowThemDown.xcodeproj -scheme SlowThemDown -destination 'platform=iOS Simulator,name=iPhone 17 Pro' -skipPackagePluginValidation CODE_SIGNING_ALLOWED=NO`
- [ ] iOS tests pass: `xcodebuild test -project ios/SlowThemDown.xcodeproj -scheme SlowThemDownTests -destination 'platform=iOS Simulator,name=iPhone 17 Pro' -skipPackagePluginValidation CODE_SIGNING_ALLOWED=NO`
- [ ] Android builds: `./gradlew :android:app:assembleDebug`
- [ ] Android tests pass: `./gradlew :android:app:testDebugUnitTest`
- [ ] Shared tests pass: `./gradlew :shared:allTests` (if shared logic changed)

## AI disclosure

<!-- If AI tools were used to write code in this PR, briefly note which tool and what it generated. See AI_USAGE.md for guidelines. -->
