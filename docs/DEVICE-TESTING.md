# Real-device acceptance: Android 15 / Vivo

This checklist is deliberately separate from automated test results. Do not mark an item complete without running it on that device. Use a stable private signing key before relying on updates across CI runs.

## Install and launcher

1. Install the APK, launch without a computer, and browse all four collections. Verify offline content is labeled sample before any successful sync.
2. Check the launcher’s widget picker lists all 19 named Widget Lab providers, with sensible previews and sizes.
3. Add a clock from the picker, cancel configuration once, and verify no half-configured widget is left. Add it again and Save.
4. Pin a Race weekend from inside the app with paper theme and a selected timezone. Verify the launcher’s placement confirmation and the saved style.
5. Cancel a pin request, then add a different design. Check that a canceled profile is not applied to an unrelated widget.
6. Add two driver instances (Hamilton and Leclerc) with different themes/artwork. Edit one; the other must remain unchanged. Check installed-instance count and entry editing.
7. Resize every design through supported launcher dimensions. Check text clipping, image crops, native button hit areas and large system font scale.
8. Reboot. Clock/date and cached widgets should return without opening Widget Lab. Reopen the app and confirm profiles remain independent. Remove a widget; its installed-manager entry must disappear.
9. Rotate the launcher if supported; check dimensions and tap targets. Home-screen safe margins and rounded corners depend on launcher padding.

## Racing

1. Refresh with a network connection. Check source timestamps, correct season and standings round. Compare at least one race’s official published UTC times to selected local times.
2. Test device timezone, UTC, Europe/London, Asia/Kolkata and America/New_York. Check daylight-saving transitions using recorded fixtures rather than changing live historical facts.
3. Check a sprint weekend includes its published sprint-specific sessions. Missing times must show TBA.
4. Disconnect networking and refresh after the one-minute cooldown. Cached data should remain and show OFFLINE, with unchanged successful timestamps. On first-run offline, only labeled samples should appear.
5. Countdown must identify the actual next future race, show days/hours only, and stop choosing it once the race starts. After the season, show a complete/unavailable state rather than invent an event.
6. Check latest results are a completed GP, separate from upcoming schedule. Driver/team relationships and season labels must remain coherent.
7. Import portrait/car images with landscape, portrait and EXIF-rotated orientation. Test center/top crop. Relaunch and reboot; the app-private copy must persist without reselecting the source file.

## Music: at least two real players

Use two installed applications that expose Android media sessions (for example Spotify and a local player such as VLC; exact behavior depends on their versions).

1. Before media access, every music design shows the permission state and opens the grant flow. Declining must not crash.
2. Grant notification-listener access, start a track in player A, and return home. Check artwork, title, artist and playing/paused state on all seven designs.
3. Tap play/pause, previous and next. Confirm actual audio behavior and metadata changes in the correct player. Unsupported controls must remain dim and open configuration.
4. Start player B while A is paused. Automatic selection should prefer B. Configure an instance to prefer A and check routing when both are available.
5. Test two sessions from one package: the playing session token must receive controls, not merely the first session with the same package name. An instrumented test covers this with controlled sessions.
6. Test long titles, missing artist, no artwork, buffering, session destruction and no active playback. No artificial progress or sustained animation should appear.
7. Tap the artwork/body to open the media app. Long-press → Edit widget must still allow customization through the launcher.
8. Revoke access in Android settings. Widgets must show the connection/permission state and stop dispatching controls. Regrant and verify recovery.
9. Test after screen-off, idle and reboot. Check Vivo’s autostart/background rules if callbacks stop; avoid promising identical behavior for every third-party player.

## Essentials and privacy

1. Compare clock minute changes against the system clock without reopening Widget Lab; test 12/24-hour formats and an alternate zone.
2. Verify date rollover with the native date provider. Check app previews and home-screen layouts at default and larger font scales.
3. Compare battery percentage to system status; connect/disconnect a charger. Foreground updates should occur quickly; background percentage is periodic and may be delayed by Doze.
4. Open Private agenda, decline read access, and confirm a meaningful permission state. Grant access only from that screen.
5. Add a timed event, recurring event, all-day event and a multi-day event to a visible local calendar. Check their dates/times and empty-state behavior. Calendar titles must not enter logs or network requests.
6. Revoke calendar permission through app settings. Refresh the widget; event content must be replaced by the permission state.
7. Use Android network diagnostics if desired: only public racing API requests should leave the app; no artwork, notification bodies or calendar data should be transmitted.

Record device model, launcher version, Funtouch OS build, app version, signing identity and results in VERIFICATION.md before calling a release device-accepted.


## Edition II / 50-design acceptance

- Confirm all 50 designs appear in the picker, in the requested 12/8/15/5/5/5 distribution.
- Place transparent Air, Maison, Pixels and Vertical clocks over actual wallpapers. Resize to their smallest supported size and then expand. Check native minute rollover, date visibility, type/accent/alignment settings and two independent zones. Change the device timezone with Widget Lab closed.
- Import both a JPEG and a transparent PNG driver portrait. Verify that cropping and transparency survive closing the app and restarting the phone. Select 3 collage photos and up to 4 filmstrip photos.
- Select apps in Strip, Editorial and Index launchers. Test each real launch target, icon style and label visibility. Uninstall a selected test app; tapping its old target should immediately open configuration, where the unavailable selection can be removed.
- Play music in the actual players you use. Test all supported controls and missing/disconnected/revoked-access states. Confirm the widget artwork follows the active session. Do not infer third-party compatibility from controlled Android session tests.
- Check actual charging state, Android DND filter and optional local calendar events. Confirm no calendar permission is requested until you choose to enable it.
- Verify 4×2 and taller racing layouts on the Vivo grid, both classic and sprint weekends, offline freshness labels and the 2021 archival portrait marker.
- Test large system fonts and Vivo battery/autostart policy. Record the physical launcher/device version separately from emulator results.
