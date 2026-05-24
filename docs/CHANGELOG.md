# Change Log

This file records user-approved product and implementation direction changes, including temporary overrides.

## 2026-05-25

- Reworked the bottom controls as a custom-drawn glass panel with a semicircular review dial and tick marks, because the previous stacked Material surfaces were too far from the reference quality.
- Changed camera analysis resolution selection from fixed 16:9 to a 4:3-priority `ResolutionSelector` to reduce unnecessary front-camera crop while keeping a high target size.
- Changed frame display to show the real camera frame with `ContentScale.Fit` over a cropped background layer, so the user sees more of the front-camera field of view instead of a forced full-screen crop.
- Stopped forcing CameraX zoom range to 1.0 or higher so devices that expose zoom-out values below 1.0 can use their real minimum zoom.
- Further minimized the mirror UI toward the latest reference: preview-first layout, a faint bottom control band, compact light/stop/flip buttons, and a subtle review dial instead of a large bottom review button.
- Confirmed zoom and light brightness sliders should remain on the preview but be visually quieter.
- Confirmed pseudo-flash brightness should change the whiteness/glow of the fixed-width side light strips, not significantly change their width.
- Moved delay rotation out of the bottom controls; the top delay label now acts as the minimal delay preset control.

## 2026-05-24

- Moved zoom and light adjustments onto the preview as vertical overlay sliders, removed zoom from the bottom control area, and changed delay control to a tap-to-rotate preset row.
- Redesigned the live mirror UI so the camera preview remains dominant: compact translucent bottom controls, top title overlay, quick light/stop/flip actions, compact zoom/delay steppers, and tap-to-expand fine sliders.
- Scaffolded Android Compose project.
- Implemented the first CameraX-based delayed preview MVP pass with in-memory frame buffering, stop-and-review mode, mirror flip, zoom controls, side-strip pseudo flash, Japanese UI strings, and local settings persistence.
- Added a minimal unit test for frame ring-buffer behavior.
- Added competitor review insights and user monetization judgment to `docs/REVIEW_INSIGHTS.md`.
- Confirmed no-ads direction, with possible in-app purchases for higher quality and flexible delay controls.
- Clarified that the free version should still work as a useful mirror and demonstrate delayed-preview value.
- Confirmed minimum supported Android version: Android 10.
- Confirmed available physical test devices include Android 10, Android 12, and Android 16.
- Confirmed review labels and camera permission explanation should stay minimal, soft, and not overly rigid.
- Confirmed zoom controls should include both pinch-in/pinch-out on the preview and a zoom slider.
- Confirmed settings should persist between launches.
- Confirmed persisted settings may include delay seconds, mirror flip state, pseudo flash brightness, and zoom value where reliable.
- Clarified that settings persistence does not allow saving camera frames, photos, videos, or review data.
- Created initial repository documentation before implementation.
- Confirmed app concept: front-camera mirror preview delayed by a user-configurable amount.
- Confirmed Android-first development, with iPhone planned later.
- Confirmed target use cases: hair, side-profile makeup, back-of-head, and back muscle checks.
- Confirmed MVP is not for long-form dance or exercise form analysis.
- Confirmed no persistent saving. Frames stay only in a short in-memory buffer.
- Confirmed delay range: 0.0 to 10.0 seconds.
- Confirmed initial delay: 5.0 seconds.
- Confirmed maximum buffer length: 10 seconds.
- Confirmed target sampling rate: 15 fps.
- Confirmed quality direction: device-managed high quality; avoid excessive downscaling.
- Confirmed mirror flip ON by default and toggleable during live preview and review.
- Confirmed zoom is included in MVP.
- Confirmed portrait fixed for MVP.
- Confirmed pseudo flash is side vertical light strips, not full-screen flash.
- Confirmed Stop freezes on the frame currently visible in delayed preview.
- Confirmed review mode can scrub around the stopped preview reference point and includes playback/pause.
- Confirmed buffer preview button is always visible, greyed out until at least 1 second of buffer is available, and stops capture when tapped.
- Confirmed Japanese UI first, with localization room for English later.
- Confirmed written specs should be updated whenever chat instructions change direction.
