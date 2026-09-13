# OneState Staging Automation Tester v0.2.0

**Scope:** Authorized staging/test-account testing only. This build does not include anti-cheat bypass,
stealth, root tricks, packet modification, or production-economy features.

## Full toggle controls
Main screen:
- Master Automation
- Vision detector
- Auto movement
- Auto Continue popup
- Stop after one completed round
- Dry Run
- Arrival wait
- Control interval
- Steering dead-zone
- Arrival marker Y threshold
- Marker-lost timeout
- Joystick center/radius
- Continue-button fallback coordinate

Floating overlay:
- AUTO ON
- PAUSE
- STOP
- DRY switch
- CONT switch
- live state text
- draggable header

## Vision logic calibrated from supplied mining screenshots
- Searches the world view for the compact red destination marker.
- Excludes top-left minimap and most top HUD to reduce false detections.
- Uses nearby purple target-zone pixels as an arrival confidence signal.
- Searches the left/mid screen for the yellow "continue" button.
- Does not use OCR; Thai language text is not required.

## State flow
SEARCHING -> MOVING -> ARRIVAL_WAIT -> WAIT_NEXT -> MOVING ...
At end-of-round popup:
- Auto Continue ON: tap detected yellow button, start next round.
- Auto Continue OFF: pause and wait for user.
- Stop after one round ON: stop after popup event.

## Safe first test
1. Install only on the authorized test device/account.
2. Grant Overlay.
3. Enable Accessibility service.
4. Keep Dry Run ON.
5. Tap "Start Screen Capture + Overlay" and approve Android screen-capture prompt.
6. Open staging game.
7. Tap AUTO ON.
8. Confirm live overlay reports sensible target coordinates before disabling Dry Run.
9. Use STOP immediately if steering is wrong.

## Build without a PC
The project contains `.github/workflows/build-apk.yml`.
Push the project to a GitHub repository and run the workflow; download `app-debug.apk` from the workflow artifact.

## Calibration note
The defaults were derived from 1480x1024 screenshots and are percentage-based, so they should scale,
but real Android HUD/aspect ratio can still require adjustment.
