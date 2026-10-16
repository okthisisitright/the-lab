# Widget Lab 0.3.0

This update responds to blank widgets on the default Vivo launcher, cramped editorial app styling, slash-separated names and the previous music designs.

## Widget delivery

Configuration now waits for `AppWidgetManager.updateAppWidget` to complete before returning success and closing. Previously it launched work in a Compose coroutine and immediately finished the activity; disposal could cancel the first update. Pin callbacks update only the new instance rather than redrawing every installed widget.

Widget layouts no longer inflate irrelevant clock components. Non-clock widgets have a lightweight image/control surface; digital clocks add only the TextClock children they display; the analog widget has a separate native surface. This removes a dependency on hidden clock inflation from every widget. A regression test verifies the actual inflated component types.

All Android versions now receive the established portrait/landscape `RemoteViews` pair. Actual option dimensions and resize broadcasts still calculate fresh compositions. This removes dependence on launcher selection of Android 12+ size maps while investigating the Vivo symptom. It is a compatibility change, not proof that Vivo's implementation caused the failure.

Render failures now log the exception and attempt a native tap-to-reload view. Settings shows local delivery timestamps and the last update exception. No diagnostics, media metadata, photos or calendar data are uploaded.

## App appearance and names

The app uses licensed Google Sans, a warm off-white surface, charcoal text and a restrained red accent. Spacing, body type, controls and search are larger. The previous editorial slogans, compressed headings, monogram and slash-separated catalog names are removed. All 50 names are plain and also appear in Android's widget picker. Configuration keys and provider component names remain stable.

## Replaced music collection

The eight previous layouts are replaced, using the attached glass/artwork/circular/now-playing references as visual direction:

1. Glass mini
2. Glass album
3. Soft glass
4. Circular player
5. Panorama player
6. Portrait player
7. Ambient disc
8. Now playing

The circles have transparent corners. Glass surfaces use alpha and a cached diffusion of the active album artwork; they do not capture or refract the wallpaper. The now-playing card emphasizes the cover, actual state, song and artist and opens the player when tapped. Other designs have actual transport buttons where the session supports them. Artwork comes from the active media session. Gallery artwork is explicitly labeled sample content; installed empty/permission states do not pretend music is playing. No simulated progress or continuous animation is added.

All transport geometry is shared between Canvas previews and native interactive views. Existing music provider IDs select the replacement designs automatically. The library remains 50 widgets.

## Verification

The old signed 0.2.0 APK passed an independent Android 15 host test ([run 38916982511](https://github.com/okthisisitright/the-lab/actions/runs/38916982511)), including its configuration screen and broadcasts to all 50 providers. Therefore the reported Vivo issue has not been reproduced here. The configuration timing flaw and compatibility concerns were addressed without claiming a confirmed physical-device root cause.

The [Android 15 integration run 38919748312](https://github.com/okthisisitright/the-lab/actions/runs/38919748312) passed all three tests: every provider binds and accepts native views, actual music control PendingIntents deliver next/pause to the playing session, and the new UI opens clock settings, changes transparency and navigates to the music gallery. A preceding run failed only because the UI assertion still expected an uppercase label; the selector was corrected to sentence case. The final component cleanup also passed [Android 15 run 38921438485](https://github.com/okthisisitright/the-lab/actions/runs/38921438485). Local tests now total 20, all passing, including a check that non-clock designs inflate no hidden clock classes. Lint reports no errors.

Clean [app](https://github.com/okthisisitright/the-lab/releases/download/widget-lab-0.3.0-qa/widgetlab-01-studio-home.png), [configuration](https://github.com/okthisisitright/the-lab/releases/download/widget-lab-0.3.0-qa/widgetlab-02-clock-detail.png) and [music gallery](https://github.com/okthisisitright/the-lab/releases/download/widget-lab-0.3.0-qa/widgetlab-03-music-gallery.png) screenshots were inspected. The cold-boot emulator launcher is restarted before UI tests; the earlier boot log is preserved separately, and the post-reset runtime has no Widget Lab update error or fatal exception.

Final build, signing, installed-APK results and actual release links are recorded after verification. Physical Vivo installation/placement acceptance remains pending. Do not uninstall the previous app before trying the signed update: preserving the installation keeps private photos and profiles.
