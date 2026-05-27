# AGENTS.md

Instructions for coding agents working in this repository.

This project is in an early MVP stage. Keep implementation conservative and keep product proposals explicit.

## Priority

1. Follow the user's latest chat instruction first.
2. If a chat instruction changes or temporarily overrides the written spec, update `docs/CHANGELOG.md` or `SPEC.md` before or alongside implementation.
3. If the app implementation and the documents drift, treat the current app implementation as the source of truth, use the newest relevant entry in `docs/CHANGELOG.md` as supporting evidence, and update the stale documents.
4. Follow this repository file.
5. Follow the global Codex rules for this machine.

## Project Summary

Kako Mirror / 過去ミラー is a local-first Android mirror app that shows the front camera preview with a user-configurable delay.

The app does not save video, photos, or camera frames to storage. It keeps only a short in-memory buffer for preview and review.

## Product Rules

- Android first. iPhone support is planned later, but do not add cross-platform architecture without approval.
- Japanese UI first. Keep localization-friendly strings so English can be added later.
- MVP is local-first and single-device.
- Do not add login, cloud sync, server persistence, analytics, ads, or account features without approval.
- Do not write camera frames to disk unless explicitly requested.
- Persisting user settings is allowed and expected. This does not allow persisting camera frames, photos, videos, or review data.
- Backup or device transfer of app settings is allowed. This does not allow backing up camera frames, photos, videos, or review data.
- Preserve the core product goal: delayed mirror preview with short private review.

## MVP Defaults

- Camera: front camera.
- Orientation: portrait fixed.
- Delay range: 0.0 to 5.0 seconds.
- Initial delay: 2.0 seconds.
- Buffer length: up to 5 seconds.
- Preview sampling target: 30 fps.
- Minimum Android version: Android 10.
- Quality target: device-managed high quality, avoiding excessive downscaling.
- Mirror mode: horizontal flip ON by default, toggleable during live preview and review.
- Zoom: included in MVP.
- Pseudo flash: vertical light strips on the left and right edges of the screen, not a full-screen white overlay.

## Implementation Rules

- Prefer Kotlin, Jetpack Compose, and CameraX for the Android MVP unless the user approves another stack.
- Keep edits small and scoped.
- Prefer modifying existing code over introducing new abstractions.
- Do not install dependencies without checking existing project scripts and explaining why they are needed.
- Use repository scripts as the source of truth once they exist.

## Verification

- For docs-only changes, review the changed documents.
- For Android code changes, run the relevant Gradle task when available.
- For UI/camera changes, verify on a rendered screen, emulator, or device when available.
- Physical test devices may be available on Android 10, Android 12, and Android 16.
- Report final verification as `Verified`, `Unverified`, and `Failed`.

## Repository Commands

- Install: no separate install command; use the checked-in Gradle Wrapper.
- Dev: `.\gradlew.bat assembleDebug`
- Build: `.\gradlew.bat assembleDebug`
- Lint: `.\gradlew.bat lintDebug`
- Typecheck: included in Gradle Kotlin compile tasks, for example `.\gradlew.bat assembleDebug`
- Test: `.\gradlew.bat testDebugUnitTest`
