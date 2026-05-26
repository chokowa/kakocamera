# Coach Mark Visual Audit

Date: 2026-05-27

Scope: current debug build installed to the connected Android device. Legacy saved screenshots in the repository were not used as visual evidence.

Evidence captured for this audit:

- Live tour: `docs/coachmark-visual-audit-evidence/current-live-contact-sheet.jpg`
- Review tour: `docs/coachmark-visual-audit-evidence/current-review-contact-sheet.jpg`
- Device: 1080 x 2340, density 440 dpi

## External Criteria Used

- Material Design feature discovery: show feature prompts at contextually relevant moments, limit volume, use a specific tap target, place mobile prompts according to target position, and use motion to show where the prompt originates. Source: <https://m1.material.io/growth-communications/feature-discovery.html>
- Android accessibility guidance: interactive elements should have at least a 48dp x 48dp focusable/touch target. Source: <https://developer.android.com/guide/topics/ui/accessibility/views/apps-views>
- W3C WCAG contrast guidance: normal text needs 4.5:1 contrast and large text needs 3:1. Source: <https://www.w3.org/WAI/WCAG21/Understanding/contrast-minimum.html>
- SEB Design Library coach mark guidance: place the coach mark next to the referenced feature, point directly to it, use one tip per coach mark, and avoid chains of coach marks. Source: <https://designlibrary.sebgroup.com/components/component-coachmark>

## Current Implementation Risks

The current implementation draws the overlay from fixed formula coordinates in `MainScreen.kt`, not from the actual measured bounds of the UI controls.

- Card placement is limited to `Center` or a shared bottom-center position with `bottom = 252.dp`.
- Target rings use constants such as `buttonY = size.height - 112.dp`, `BottomInnerControlOffset`, and `BottomOuterControlOffset`.
- Arrow start points are also fixed, so the arrow does not originate from a real card edge or caret.
- The current visual style uses `CoachPink = #FF78B7` on a pale card `#F0E8EF`, which calculates to roughly 2.03:1 contrast before alpha/compositing effects.

## Severe Visual Findings

### 1. The first live coach mark blocks the product instead of explaining it

Current evidence: `current-live-step1.png`

The first step highlights almost the entire camera preview, then places a large card directly over the preview. This is a mirror app, so covering the preview is not a minor flaw. The user is trying to see the delayed mirror, but the tutorial puts a large decorative object on top of the thing being introduced.

Why this is bad:

- The target is too broad to be meaningful. A huge rounded rectangle says "this whole screen matters", not "look here".
- The card sits inside the target, so the highlight and the explanation compete for the same space.
- The arrow collapses into a short vertical line below the card, so it does not visually teach the relationship between card and target.
- The help button remains visible behind the overlay, creating a second help affordance while help is already active.

Improvement:

- Do not use a full-preview target ring for the initial step.
- Use a compact value badge or coach bubble in the lower third of the preview, without an arrow, or point to the delay/stop control that creates the "past mirror" behavior.
- Hide the top-right help button while the coach overlay is open.

### 2. Seven live steps is not a guided experience. It is a forced inspection of the whole UI

Current evidence: `current-live-contact-sheet.jpg`

The live tour walks through 7 steps before the user has naturally used the app. This fails the basic onboarding goal: the user should learn just enough to succeed, not memorize all controls before acting.

Why this is bad:

- Material recommends limiting feature discovery volume and showing prompts at contextually relevant moments.
- SEB's coach mark guidance explicitly discourages triggering multiple coach marks in a row.
- Visually, all 7 steps reuse the same panel style and similar arrows, so the tour becomes repetitive rather than informative.
- Secondary controls such as light, display mode, fine controls, and mirror flip get equal visual weight with the core delay/review loop.

Improvement:

- First-run live should have 1 to 2 prompts only.
- Keep the first prompt focused on the core behavior: delayed preview and stop-to-review.
- Show secondary tips just in time:
  - Delay tip when the user first opens delay.
  - Review slider tip when the user first enters review.
  - Light, zoom, display mode, and flip should be available through the `?` replay/help surface instead of blocking first use.

### 3. The coach card is visually heavier than the controls it is supposed to explain

Current evidence: `current-live-step2.png`, `current-live-step3.png`, `current-review-step1.png`

The card is a wide pale panel with a pink border, title, nested white pill, progress label, skip text, and a metal-styled next button. This creates too many competing visual anchors.

Why this is bad:

- The card dominates the screen, while the actual target becomes a small circled object near the bottom.
- The nested white body pill adds another "card inside card" layer and makes the guide look like a settings panel rather than a pointer.
- The next button uses the same metallic visual language as app controls, so the tutorial action competes with product actions.
- The visual system has no hierarchy: pink title, pink outline, pink arrow, pink glow, and pink progress all shout at once.

Improvement:

- Use a smaller coach bubble with a max width around 300 to 320dp.
- Remove the nested body pill unless it is carrying a distinct status or example.
- Make tutorial navigation visually quiet: text button for skip, simple filled or tonal button for next.
- Reserve the accent color for either target/arrow or title, not all of them at once.

### 4. The arrow is not a real pointer

Current evidence: all live and review steps.

The arrows are lines from hard-coded screen coordinates to target anchors. They do not emerge from a card caret and often look like random pink lines drawn across the UI.

Why this is bad:

- A pointer should visually answer "this card refers to that object". Here, the arrow start point is not tied to the card's actual bounds.
- For left/right targets, long diagonal arrows cross the control area and add visual noise.
- For multi-target steps, two arrows from one card create a V shape that looks like decoration, not instruction.
- For the preview and review slider steps, the card almost touches or overlaps the highlight, leaving no breathing room.

Improvement:

- Measure target bounds with `onGloballyPositioned` and draw arrows from an actual card caret.
- Prefer a short caret when the card can be placed next to the target.
- Use arrows only when the target is not obvious. For a large preview area, do not draw an arrow.
- For two-button targets, either highlight the group with a label that names the group, or split into two contextual tips. Do not make two equal arrows fight for attention.

### 5. Target highlights are approximate decorations, not measured targets

Current evidence: `current-live-step2.png` through `current-live-step7.png`

The rings are drawn from fixed constants rather than target bounds. On the current device they land near the controls, but the visual logic is fragile and imprecise.

Why this is bad:

- The Stop highlight is so large it visually swallows neighboring controls.
- Small side controls get large magenta halos that cover adjacent controls.
- Fine-control highlights are drawn at fixed edge positions, while the actual rail and knobs can move with layout changes.
- Review slider highlighting frames a large area but does not distinguish the scrubber thumb from the track.
- The overlay dims the whole screen, including the highlighted control, then adds outlines. The target is not truly spotlighted.

Improvement:

- Store measured bounds per `CoachTarget`.
- Draw target highlight as actual control bounds plus 8 to 12dp padding.
- For circular controls, use the measured center and radius.
- For sliders, highlight either the whole slider group or the scrubber thumb based on the message, not a generic large rounded rectangle.
- Consider a cutout/spotlight scrim so the target remains visually clear instead of dimmed.

### 6. The title color fails visual contrast on the current pale card

Current code colors:

- Title: `#FF78B7`
- Card: `#F0E8EF` at 96 percent alpha
- Approximate contrast: 2.03:1

Why this is bad:

- W3C's 3:1 large-text threshold is not met, even before considering alpha and camera background variation.
- The title is meant to be the primary reading anchor, but it is visually weaker than the body text.
- Pale pink-on-pale pink makes the card feel washed out and amateur, especially over a dimmed live camera feed.

Improvement:

- Darken the title/accent substantially, for example a deeper rose around `#B83272` or `#A62463`, then recheck contrast.
- Keep the body text dark and remove competing low-contrast decorative pink labels where possible.
- If the sticky-note direction is required, make the note surface warm/light but the typography disciplined and high contrast.

### 7. Tutorial controls are under the recommended touch target height

Current code:

- Skip: 58dp x 34dp
- Next/OK: 82dp x 40dp
- Help button: 42dp x 42dp

Why this is bad visually:

- Even if taps technically work, the buttons look cramped and low-confidence.
- The next button uses a visually large metal asset, but the actual pointer-input box is only 40dp tall.
- The help button appears as a small floating circle at the top right and remains under the 48dp recommendation.

Improvement:

- Give coach navigation and help controls at least 48dp x 48dp touch bounds.
- Add proper Compose semantics/clickable behavior so accessibility and tooling understand these as buttons.
- Keep the visual size compact if needed, but expand the touch/focus area.

## Recommended Redesign Direction

### Product-level flow

Replace the current first-run tour with progressive, contextual coaching:

1. First live screen: one compact prompt that teaches the core value, not every control.
2. First stop/review: one prompt for the review slider and return-to-live action.
3. Secondary controls: no automatic first-run interruption. Surface them through the replay help button or show a small contextual tip only after the user approaches that control.

This keeps the MVP's main loop intact: see delayed preview, stop, review, return to live.

### Visual component

Build one measured coach bubble component:

- `CoachTargetRegistry`: each target reports actual bounds with `onGloballyPositioned`.
- `CoachOverlay`: scrim, target cutout/highlight, bubble placement, arrow/caret.
- `CoachBubble`: compact surface, high-contrast title, quiet body, small progress only for real multi-step flows.
- `CoachPlacement`: chooses above/below/left/right based on target bounds, screen insets, and available space.

### Visual rules

- The card must not cover the target unless the target is the whole preview and no precise operation is expected.
- The pointer must originate from the card edge/caret and terminate at the measured target.
- The target must remain visually brighter than surrounding content.
- One card should explain one concept. If two controls are highlighted, the group relationship must be visually clear.
- Tutorial navigation must look like tutorial navigation, not like primary app controls.
- Hide unrelated controls that compete with the active guide, especially the top-right help button.

## Verification Needed After Redesign

- Capture live and review coach marks on the connected 1080 x 2340 device.
- Check a narrow emulator/device width around 360 to 390dp.
- Check a taller/wider Android device if available.
- Confirm no horizontal overflow.
- Confirm target rings align with measured controls after delay picker, fullscreen mode, review mode, and navigation bar insets.
- Recalculate title/body/button contrast ratios.
- Verify all coach navigation buttons have at least 48dp touch targets.
