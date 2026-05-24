# Kako Mirror MVP Specification

Last updated: 2026-05-24

## Product

Name: Kako Mirror / 過去ミラー

Tagline: 5-Second Delayed Preview

Japanese subtitle: 過去ミラー 〜5秒遅れてプレビュー

## Goal

Provide a lightweight mirror app that lets users see themselves a few seconds in the past without saving video or photos.

Primary uses:

- Hair checking.
- Side-profile makeup checking.
- Back-of-head checking.
- Back muscle checking.

Non-goals for the MVP:

- Long-form dance or exercise form analysis.
- Persistent recording.
- Cloud sync.
- Account-based features.

## Platform

MVP target: Android.

Later target: iPhone.

The first Android version should be a portrait-only native app.

## Camera Behavior

- Use the front camera first.
- Show a delayed preview based on the selected delay.
- Delay range: 0.0 to 10.0 seconds.
- Initial delay: 5.0 seconds.
- Target preview sampling rate: 15 fps.
- Quality: let the device provide high quality where practical; do not over-compress or over-downscale.
- Keep a maximum 10-second in-memory buffer.
- Do not save frames, photos, videos, or review data to persistent storage.
- Clear the buffer when the app exits or when a new camera start begins.

## Settings Persistence

Persist user settings locally on the device.

Settings that should persist between launches:

- Delay seconds.
- Mirror flip state.
- Pseudo flash brightness.
- Zoom value, if technically reliable for the selected camera.

Persisting settings does not change the no-saving rule for camera frames, photos, videos, or review data.

## Live Preview

The main live view is a delayed mirror preview.

Controls available during live preview:

- Stop.
- Delay adjustment.
- Mirror flip toggle.
- Zoom.
- Pseudo flash brightness.
- Buffer preview button.

Mirror flip:

- ON by default.
- Toggleable while live.
- Toggleable after stopping.

Zoom:

- Included in MVP.
- Support pinch-in and pinch-out on the preview.
- Support a zoom slider.
- Use the camera's available zoom range when possible, and clamp the UI to the selected device's supported range.
- Persist the zoom value only when it can be restored reliably for the selected camera.

Pseudo flash:

- Implement as vertical light strips on the left and right edges of the screen.
- Do not use a full-screen white overlay for the pseudo flash.
- Visual concept: `| preview |`, where the side bars act as light.

## Start State

On app launch, the screen should be dominated by a clear start button or start affordance.

The UI should avoid feeling busy before camera permission and camera start.

## Stop And Review Behavior

When the user taps Stop, or taps the buffer preview button while recording:

1. Stop the camera capture session.
2. Enter buffer review mode.
3. Freeze on the frame that was currently visible in the delayed preview.

In review mode:

- The user can scrub through buffered frames.
- The scrubber is based on the stopped preview frame as the reference point.
- The user can inspect both past frames and delayed "future" frames that were already captured but not yet shown due to the live delay.
- Playback and pause are available.
- Mirror flip remains toggleable.
- The time display may change later, but the MVP should start with stopped-preview-relative time.

## Buffer Preview Button

- Always visible.
- Disabled/greyed out while less than 1 second of buffer is available.
- When enabled and tapped during recording, it stops capture and enters buffer review mode.

## UI Language

Primary UI language: Japanese.

Keep strings localization-friendly because English localization is planned.

Example labels:

- 開始
- 停止
- 遅延
- 反転
- ライト
- ズーム
- 見返し
- 再生
- 一時停止

## Visual Direction

Simple, calm, and practical.

Avoid:

- Flashy decorative UI.
- Heavy camera-editor feeling.
- Social, account, or sharing features.

Prefer:

- Large preview.
- Clear bottom controls.
- One-handed operation.
- No accidental horizontal overflow.
- Mobile-first portrait layout.

## Architecture Notes

Preferred Android stack:

- Kotlin.
- Jetpack Compose.
- CameraX.

Expected camera pipeline:

- CameraX provides front-camera frames.
- Frames are sampled around 15 fps into an in-memory timestamped ring buffer.
- The live renderer selects the frame closest to `now - delay`.
- Review mode reads from the same in-memory buffer.

This is a starting direction, not permission for a broad architecture rewrite.

## Open Questions

- Exact review scrubber labels.
- Whether camera permission education copy is needed beyond the system prompt.
- Minimum supported Android version.
