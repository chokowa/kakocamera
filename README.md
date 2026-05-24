# Kako Mirror / 過去ミラー

5-second delayed preview mirror app.

Kako Mirror is a lightweight local-first mirror app for checking hair, side-profile makeup, the back of the head, and back muscles with a short delayed camera preview.

The Android MVP is planned first. iPhone support is planned later.

## Core Idea

The app shows the front camera preview after a user-configurable delay. Although the product name references 5 seconds, users can adjust the delay from 0.0 to 10.0 seconds.

The app does not save captured frames. It only keeps a short in-memory buffer so the user can review recent moments after stopping the camera.

## Current Status

Specification phase. No Android app has been scaffolded yet.

See:

- [SPEC.md](SPEC.md)
- [docs/CHANGELOG.md](docs/CHANGELOG.md)

## Design Direction

Simple, practical, and calm. The app should feel like a daily tool rather than a flashy camera product.

Common mirror-app feature references include brightness, zoom, horizontal flip, and short review behavior. Kako Mirror's differentiator is private delayed preview without saving.
