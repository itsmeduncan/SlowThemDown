# Responsible AI Usage

SlowThemDown welcomes contributions that use AI coding assistants. This document describes the project's expectations for responsible AI usage in development, review, and community participation.

## AI-Assisted Contributions

AI tools (Claude, Copilot, ChatGPT, etc.) are **allowed and encouraged** for:

- Writing code, tests, and documentation
- Exploring approaches and prototyping
- Generating boilerplate and scaffolding
- Debugging and code review
- Translating code between platforms (e.g., Swift to Kotlin)

## Human Review Required

Every AI-assisted contribution must be reviewed by a human before merging. This is not optional. Specifically:

- **Read what it wrote.** Don't submit AI output you haven't read and understood. If you can't explain why a line of code is there, don't ship it.
- **Run the tests.** AI-generated code must pass all existing tests and include new tests where appropriate. "It compiled" is not sufficient.
- **Verify cross-platform parity.** This project runs on both iOS and Android. AI tools may generate code that works on one platform but not the other. Build and test both.
- **Check for hallucinated APIs.** AI tools sometimes reference APIs, methods, or parameters that don't exist. Verify that every API call is real and available on the minimum supported OS versions (iOS 17.0, Android API 26).
- **Review security implications.** AI-generated code may introduce vulnerabilities (e.g., SQL injection in Room queries, unsafe URL construction, missing input validation). Review with the same rigor you'd apply to any PR.

## Attribution

If a commit was substantially written by an AI tool, include a `Co-Authored-By` trailer in the commit message:

```
Co-Authored-By: Claude <noreply@anthropic.com>
```

This is for transparency, not blame. It helps maintainers understand the provenance of code and calibrate review effort accordingly.

## What AI Should Not Be Used For

- **Generating fake speed data or reports** — SlowThemDown is a civic tool. Fabricating data undermines the trust that makes it useful.
- **Automating agency submissions** — The "Report to Agency" feature composes an email for human review before sending. Never automate the actual submission of reports to government agencies.
- **Circumventing privacy protections** — The PII blurring pipeline exists for a reason. Don't use AI tools to reverse, bypass, or weaken face/plate detection.
- **Generating legal claims** — Speed estimates are not certified measurements. AI should not be used to generate language that presents SlowThemDown data as legally admissible evidence.

## AI in the Speed Estimation Pipeline

The app's speed estimation is deterministic math, not AI inference. The formula is:

```
speed_mps = (pixel_displacement / pixels_per_meter) / time_delta_seconds
```

All internal values use SI units (meters, m/s). Display conversion to MPH or km/h happens at the view layer. There is no machine learning in the speed calculation. The only ML components in the app are:

- **Face detection** (Apple Vision / ML Kit) — used to blur faces for privacy
- **Text recognition** (Apple Vision / ML Kit) — used to detect and blur license plates

These run on-device and do not transmit data.

## For Maintainers

When reviewing AI-assisted PRs:

- Apply the same quality bar as any other PR. AI authorship is not a reason to relax or tighten standards.
- Watch for subtle issues AI tools commonly introduce: unnecessary abstractions, over-engineered error handling, hallucinated APIs, stale patterns from training data.
- If a PR feels like bulk AI output with no human curation, request that the contributor review and refine before re-submitting.

## Questions

If you're unsure whether a particular use of AI is appropriate for this project, open a discussion or ask in your PR description. We'd rather talk about it than discover a problem after merge.
