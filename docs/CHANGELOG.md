# Change Log

This file records user-approved product and implementation direction changes, including temporary overrides.

## 2026-05-27

- Refined coach marks from dark glass panels into brighter sticky-note-style callouts with paler pink titles, clearer title/body contrast, smaller skip text, arrows, and more precise target highlights.
- Changed multi-control coach marks so separate buttons are highlighted individually instead of being enclosed by one large shared frame.
- Added first-run coach marks for the live and review screens, using black glass cards, vivid magenta titles, subtle silver target rings, skip/next controls, and a small top-right help button for replay.
- Added a live bottom display-mode button between light and stop, toggling between full-size sensor-fit display and fullscreen crop display.
- Removed the text label from the display-mode toggle so it reads as an icon-only control.
- Persisted the display-mode setting locally so the user's preferred mirror size survives relaunch.
- Lowered the live light and zoom labels to the review scrubber axis and added a subtle fine-control rail so the live letterbox control area no longer feels empty.
- Tuned the sensor-fit camera placement to keep a native-camera-like top gap while collecting enough unused letterbox space at the bottom for controls and review scrubbing.
- Changed the main camera rendering from crop-fill to sensor-range-priority fit rendering, accepting black letterbox space so more of the front-camera frame remains visible.
- Changed pseudo flash behavior so the unused letterbox/background area brightens with the light strength instead of placing light bars over the camera image.
- Reduced the maximum user delay and in-memory review buffer from 10 seconds to 5 seconds to lower heat and memory pressure while keeping the 30 fps sampling target.
- Clamped persisted delay settings to the new 5 second maximum so older 10 second local settings are normalized.

## 2026-05-25

- Changed the live and review bottom controls to use shared fixed slots: light/rewind on the left inner slot, delay/fast-forward on the right inner slot, and mirror flip on the far-right slot.
- Made the light and zoom fine sliders appear only after tapping their small preview labels, keeping the camera preview cleaner by default.
- Restyled the review position slider toward the same metallic/glass control language and removed the remaining blue accent from the timeline.
- Clamped the floating light and zoom slider thumb travel to the visible rail height so the controls stop at their maximum values without the thumb disappearing above the screen.
- Allowed switching from delayed-preview loading into review mode as soon as any in-memory frame is available, without stopping the camera when no frame exists.
- Prevented the camera from stopping when the primary stop button is pressed while the delayed preview is still loading and no review frame is available yet.
- Fixed delay loading getting stuck at the 10 second preset by allowing a small readiness tolerance near the in-memory buffer limit.
- Smoothed pinch zoom by keeping transform gesture detection stable while zoom state updates.
- Added a centered loading spinner with delay countdown while the live delay buffer is filling after launch or returning from review.
- Changed review timeline time labels to high-contrast white text with a soft shadow so they remain readable over bright or dark camera backgrounds.
- Aligned review rewind with the live light button position and review fast-forward with the live delay button position so mode switching keeps the side controls stable.
- Balanced the review rewind and fast-forward buttons at equal distance from the central live-resume button while keeping mirror flip fixed.
- Removed the delay button from review/player mode, kept mirror flip fixed at the same right-side position in both live and review, and raised the live controls to match the player clearance from Android navigation.
- Rebalanced review transport spacing so fast-forward no longer overlaps the fixed mirror button.
- Kept mirror flip visible in both live and review/player modes, placing it horizontally to the right of the delay preset button instead of stacking it vertically.
- Restored the mirror flip button after moving delay control; delay now uses a separate bottom-right slot instead of replacing mirror flip.
- Raised the review player controls away from the Android navigation buttons.
- Removed the top title and delay labels from the preview, and moved delay preset rotation to a compact bottom-right seconds button.
- Fixed stale pointer callbacks in icon buttons so repeated light taps cycle through the current brightness state instead of reusing the first captured value.
- Removed the remaining white bottom guide line from the controls.
- Removed the white rectangular bottom control background so the controls sit directly on the preview.
- Tightened the lower control panel height and review transport spacing so the live button no longer floats under a large empty band and the player timeline sits closer to the controls.
- Replaced the primary live/stop buttons with transparent generated PNG assets: blue for live resume and amber for stop-to-review, so the two states are visually distinct.
- Changed the light control to cycle through stepped brightness levels on tap and turn off immediately on long press.
- Tightened the review player controls: time readouts now sit under the timeline, transport buttons are closer together, and rewind/fast-forward buttons show their 0.5 second step.
- Redrew the live-resume icon as a cleaner signal/play mark to avoid the previous broken nested-wave look.
- Removed the separate live "confirm" action from the bottom controls; the single primary action now switches between stopping into review and returning to live.
- Replaced the semicircular review dial with a music-player-style horizontal review timeline that shows the relative buffer range, current position, and total buffered seconds.
- Changed review navigation to rewind/fast-forward icon buttons with tap-to-step and long-press continuous seek, and changed the primary review button to an icon-only live-resume control with hold-to-play review playback.
- Rebuilt the pseudo-light behavior: removed white full-screen overlays and layered glow zones, leaving only the strongest edge light bars plus CameraX exposure compensation where supported.
- Changed the rendered camera image to fill the whole screen with `ContentScale.Crop`, prioritizing all-screen preview coverage over letterboxed full-frame display.
- Raised the frame sampling target from 15 fps to 30 fps by reducing the analysis frame interval from 66 ms to 33 ms.
- Reworked the review dial copy and state feedback from "見返し" to action-oriented states such as "確認", "再生", and "一時停止".
- Replaced the shallow boomerang-like review curve with a true semicircular dial.
- Polished the bottom mirror controls with custom-drawn control glyphs, a lighter layered glass base, and a quieter review dial to reduce the cheap gray-panel feeling.
- Removed the startup explanation panel and changed launch behavior to start the camera preview immediately when permission is available.
- Refined the mirror controls toward a richer minimal glass style, keeping the action buttons inside the bottom base and widening the review semicircle without adding height.
- Removed the separate review preview slider; review scrubbing now uses the semicircular review dial rotation instead.
- Adjusted the review dial content so the play marker sits inside the semicircle consistently in live and review layouts.
- Changed pseudo-flash rendering from inset vertical bars to edge-mounted frame lights that start exactly at the left and right screen edges.
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
