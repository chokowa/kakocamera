# AGENTS.md

Instructions for coding agents working in this repository.

This project is in an early MVP stage. Keep implementation conservative and keep product proposals explicit.

## Priority

1. Follow the user's latest chat instruction first.
2. If a chat instruction changes or temporarily overrides the written spec, update `docs/CHANGELOG.md` or `SPEC.md` before or alongside implementation.
3. Follow this repository file.
4. Follow the global Codex rules for this machine.

## Project Summary

Kako Mirror / 過去ミラー is a local-first Android mirror app that shows the front camera preview with a user-configurable delay.

The app does not save video, photos, or camera frames to storage. It keeps only a short in-memory buffer for preview and review.

## Product Rules

- Android first. iPhone support is planned later, but do not add cross-platform architecture without approval.
- Japanese UI first. Keep localization-friendly strings so English can be added later.
- MVP is local-first and single-device.
- Do not add login, cloud sync, server persistence, analytics, ads, or account features without approval.
- Do not write camera frames to disk unless explicitly requested.
- Preserve the core product goal: delayed mirror preview with short private review.

## MVP Defaults

- Camera: front camera.
- Orientation: portrait fixed.
- Delay range: 0.0 to 10.0 seconds.
- Initial delay: 5.0 seconds.
- Buffer length: up to 10 seconds.
- Preview sampling target: 15 fps.
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
- Report final verification as `Verified`, `Unverified`, and `Failed`.

## Repository Commands

Fill these when the Android project is scaffolded.

- Install: TODO
- Dev: TODO
- Build: TODO
- Lint: TODO
- Typecheck: TODO
- Test: TODO
