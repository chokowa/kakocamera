# Kako Mirror MVP Specification

Last updated: 2026-05-28

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

## Monetization Direction

Avoid ads, especially disruptive or audio ads.

The product may use in-app purchases to unlock higher quality modes and more flexible controls, such as delay customization.

The free version should still work as a useful mirror and should demonstrate the delayed-preview value. Privacy, no-saving behavior, and basic mirror use must not be paid-only.

See [docs/REVIEW_INSIGHTS.md](docs/REVIEW_INSIGHTS.md) for the review-analysis background and monetization cautions.

## Platform

MVP target: Android.

Later target: iPhone.

The first Android version should be a portrait-only native app.

Minimum supported Android version: Android 10.

Known available physical test devices:

- Android 10.
- Android 12.
- Android 16.

## Camera Behavior

- Use the front camera first.
- Show a delayed preview based on the selected delay.
- Delay range: 0.0 to 5.0 seconds.
- Initial delay: 2.0 seconds.
- Target preview sampling rate: 30 fps.
- Quality: let the device provide high quality where practical; do not over-compress or over-downscale.
- Prefer 4:3 camera analysis output with a high target size when available, because fixed 16:9 can crop front-camera field of view before the app renders the frame.
- Keep a maximum 5-second in-memory buffer to reduce heat and memory pressure while preserving useful short review.
- Do not save frames, photos, videos, or review data to persistent storage.
- Clear the buffer when the app exits or when a new camera start begins.

## Settings Persistence

Persist user settings locally on the device.

Settings that should persist between launches:

- Delay seconds.
- Mirror flip state.
- Display mode: full-size camera frame or fullscreen crop.
- Whether the live and review coach marks have already been shown.
- Pseudo flash brightness.
- Zoom value, if technically reliable for the selected camera.

Backup and device transfer of app settings are allowed. Camera frames, photos, videos, and review data must still remain out of persistent storage and out of backup payloads.

Persisting settings does not change the no-saving rule for camera frames, photos, videos, or review data.

## Live Preview

The main live view is a delayed mirror preview.

Controls available during live preview:

- Stop.
- Delay adjustment.
- Mirror flip toggle.
- Display mode toggle between full-size camera frame and fullscreen crop.
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
- Do not force the minimum zoom to 1.0 if CameraX reports a lower supported zoom-out value.
- Persist the zoom value only when it can be restored reliably for the selected camera.

Pseudo flash:

- The main camera image should prefer sensor coverage with `Fit` rendering.
- Leave any unused letterbox area black when pseudo flash is off.
- When pseudo flash is on, brighten only the unused letterbox/background area according to the light strength.
- Do not place a white overlay over the camera image itself.

## Start State

On app launch, the screen should be dominated by a clear start button or start affordance.

The UI should avoid feeling busy before camera permission and camera start.

## Coach Marks

Show a short first-run guide on the live screen after live mode starts, without waiting for the delayed preview buffer to finish loading. The automatic first-run guide and the top-right help replay must use the same live guide steps so new installs and replayed help never diverge.

Live coach mark order:

1. Delay: `何秒前の過去を見るか選べます` / `0秒、2秒、3秒、5秒`
2. Mirror flip: `見やすい向きに切り替え` / `鏡と同じか相手からの見た目か`
3. Fullscreen: `画面いっぱいに大きく映せます` / `もう一度押すとフルサイズの画面に戻ります`
4. Light: `押す度に上下が白く変化してライト代わりに` / `周りが暗い時に便利です`
5. Fine controls: `細かい調整はここ` / `○で明るさ、＋でズーム`
6. Stop: `過去のミラーを見返せます` / `最大5秒前まで戻って確認できます`

Show a separate first-run guide on the review screen the first time the user enters review. The automatic first-run guide and the top-right help replay must use the same review guide steps.

Review coach mark order:

1. Slider: `好きな場所まで戻せます` / `止めた前後を静止画でチェック`
2. Step buttons: `少しずつ前後に動かせます` / `長押しでも移動できます`
3. Live: `終わったらLIVEに戻る` / `長押しで再生もできます`

The user can replay the current mode guide from the top-right help button. Replayed help should not contain different pages or a different order than the first-run guide.

Coach marks should feel like compact rose-silver callouts with high-contrast rose titles, visually quieter supporting text, small skip affordance, clear card-edge pointers, and bright target highlights sized from the actual measured UI element. The guide card should avoid covering the target. When a step references multiple buttons, each button should be highlighted separately. The target should remain brighter than the dimmed background. The top-right help button should be hidden while a coach mark is open. Coach mark navigation and help touch targets should be at least 48dp.

## Stop And Review Behavior

When the user taps Stop, or taps the buffer preview button while recording:

1. Stop the camera capture session.
2. Enter buffer review mode.
3. Freeze on the frame that was currently visible in the delayed preview.

In review mode:

- The user can scrub through buffered frames.
- The scrubber is based on the stopped preview frame as the reference point.
- Scrubber labels should stay minimal and soft, not technical or rigid.
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

Keep visible wording minimal, calm, and soft. Avoid stiff technical copy.

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

Camera permission copy:

- Keep any pre-permission explanation short and gentle.
- Do not over-explain privacy unless the screen needs reassurance.
- The key message should be that the camera is used only for mirror preview.

## Visual Direction

Simple, calm, and practical.

Avoid:

- Flashy decorative UI.
- Heavy camera-editor feeling.
- Social, account, or sharing features.

Prefer:

- Large preview.
- Display the full camera frame where practical, using background fill rather than cropping the main mirror image.
- Minimal translucent bottom controls.
- A subtle review dial or equivalent lightweight affordance instead of a heavy editor panel.
- Keep the review dial's play marker visually inside the semicircle in both live and review states.
- Quiet preview-overlay sliders for zoom and pseudo-flash brightness.
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
- Frames are sampled around 30 fps into an in-memory timestamped ring buffer.
- The live renderer selects the frame closest to `now - delay`.
- Review mode reads from the same in-memory buffer.

This is a starting direction, not permission for a broad architecture rewrite.

## Open Questions

- Exact final review scrubber label wording.
- Exact final camera permission explanation wording.
- Exact free-tier limits and paid unlocks.
- Whether paid unlocks are one-time purchase or subscription.
