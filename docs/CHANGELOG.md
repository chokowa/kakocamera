# Change Log

This file records user-approved product and implementation direction changes, including temporary overrides.

## 2026-05-24

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
