# Kako Mirror / 過去ミラー

5-second delayed preview mirror app.

Kako Mirror is a lightweight local-first mirror app for checking hair, side-profile makeup, the back of the head, and back muscles with a short delayed camera preview.

The Android MVP is planned first. iPhone support is planned later.

## Core Idea

The app shows the front camera preview after a user-configurable delay. The current Android app supports 0.0 to 5.0 seconds, with 2.0 seconds as the initial delay.

The app does not save captured frames. It only keeps a short in-memory buffer, currently up to 5 seconds, so the user can review recent moments after stopping the camera.

## Current Status

Android MVP implementation is in progress. The project includes a native Compose app scaffold with CameraX-based delayed preview behavior.

Useful commands:

- Build debug APK: `.\gradlew.bat assembleDebug`
- Run local tests: `.\gradlew.bat testDebugUnitTest`

See:

- [SPEC.md](SPEC.md)
- [docs/CHANGELOG.md](docs/CHANGELOG.md)

## Design Direction

Simple, practical, and calm. The app should feel like a daily tool rather than a flashy camera product.

Common mirror-app feature references include brightness, zoom, horizontal flip, and short review behavior. Kako Mirror's differentiator is private delayed preview without saving.
