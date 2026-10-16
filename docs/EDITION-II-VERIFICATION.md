# Edition II verification / 0.2.0

16 October 2026. The catalog contains exactly 50 designs/providers with the requested 12 / 8 / 15 / 5 / 5 / 5 distribution.

## Passed locally

- Debug compilation, optimized release compilation, and instrumentation APK compilation with Kotlin/Compose 2.1.20, AGP 8.9.2, Gradle 8.11.1, SDK 35, minimum API 29.
- 16 local tests: six provider/data semantics tests and ten runtime/render/configuration/asset tests.
- All 50 provider registrations, configuration entry points, sizing metadata and refresh declarations.
- Actual RemoteViews inflation of all 50 designs, including native clock children and interactive transport controls. An additional API 29 test verifies new clock fonts, configured zones and actual native primary-text colors.
- Android 10 rejects reflective TextClock.setGravity; XML alignment variants now provide the same composition on API 29 and newer. Control tint is rendered into small native icon bitmaps rather than relying on newer reflection support.
- Rendering at default, compact, tall and alternate-theme proportions, including empty media/calendar states.
- Minimum-height clock region bounds, independent dual timezones, per-instance persistence, new photo/app profile fields, bad-profile fallbacks, safe artwork filenames, cache failure retention, DST and fractional-offset zones.
- 33 licensed offline circuit paths validated; all 23 circuit IDs in the checked 2026 schedule match. Unknown IDs return no path.
- Uniform raster scaling and 420,000-pixel / 1.68 MB per-surface bounds. Responsive updates share this pixel budget across variants.
- All 50 Android picker previews generated from tested renderer output; six collection sheets visually inspected. This inspection found and corrected overlapping constructor stats, cramped podium labels, early session truncation and unused vinyl space.
- Lint with zero errors after correcting a suspiciously indented painting branch. Warnings cover optional dependency upgrades, English-only labels, API 31 sizing attributes on min API 29, dynamically selected resource names and intentional process-lifetime caches.
- Workflows validated by actionlint. Public Jolpica 2026 schedule rechecked: 23 races returned. Prior parser/provider coverage is retained.

## Executed on Android 15

[Android 15 integration run 38851369239](https://github.com/okthisisitright/the-lab/actions/runs/38851369239) passed all three tests:

- Bind all 50 providers to Android AppWidgetHost, update them and inflate the configured native views.
- Two controlled media sessions in the same package: choose the playing token, deliver supported next/pause callbacks, suppress unsupported previous, observe metadata updates, and revoke listener access.
- Navigate from the studio gallery to clock settings and switch solid/transparent surfaces.

The preceding run exposed an unstable UI-test scroll selector; it was corrected with a stable settings-list identifier. Later successful runs assert that all 50 host views contain the actual widget root rather than Android's error view. Emulator screenshots are captured to shell-owned storage so they survive test-runner app cleanup.

The first real studio screenshots exposed a wrapping headline and incorrect dark status-bar icons. The headline now uses measured type width, including system font scale; status-bar contrast is explicitly configured. Preview wallpaper drawing is clipped to its viewport. The final screenshots and run links are recorded after the last verification run.

## Limits of this verification

No physical Vivo is connected. Third-party music apps, actual phone launcher pin/resize behavior, device restart persistence, large system font settings and Vivo power policies still require the [device checklist](DEVICE-TESTING.md). The media test uses actual Android sessions controlled by test code; it is not a claim of Spotify/YouTube Music/etc. compatibility.

The original attachment filesystem paths are unavailable during this rebuild. Art direction uses the references visible in the conversation, their written specification and the credited archival portrait already in the project. No unlicensed driver/car image scraping is used.

Gallery-only NASA cover/photo studies and examples using currently installed apps are explicitly labeled. Native installed widgets use actual selected images/media or honest empty states. No fabricated live championship data, playback progress, proprietary focus status or continuous animation is claimed.

## Final package

GitHub [build 38851369088](https://github.com/okthisisitright/the-lab/actions/runs/38851369088) passed compilation, local tests, lint and optimized release assembly. The signed sideload package is version 0.2.0 / versionCode 2, min API 29, target API 35: **8,650,612 bytes**.

- SHA-256: `71f3bd95df953f30f0bb0529a71dbf1e31497f7c034650d232540df2aded15ba`
- Signing certificate SHA-256: `e1eac9a80df5a56dba2f65e2cf23f1c7449dd33fbcd658172a31acdad5c30368`
- APK signature verification, ZIP integrity, required APK entries and 16 KB ZIP alignment passed. The signer matches the previous optimized 0.1.0 APK. This is an optimized build signed with the existing development key; the private key is excluded from git.

Publication status and links are recorded after the upload is verified.


## Launcher stress finding

A screenshot after binding all 50 providers caught a Pixel Launcher ANR dialog even though Widget Lab's three assertions passed. This is recorded separately from app/test success. Widget update handling was found to perform redundant work during a burst: every targeted update enumerated all 50 providers, and every provider re-enqueued scheduling initialization. Targeted updates now resolve only their own IDs; music events enumerate only the eight music providers; scheduling initialization is idempotent per process. Subsequent stress runs also capture Android's last-ANR diagnostic instead of inferring its cause from a screenshot.

The final run still recorded a Pixel Launcher no-focused-window ANR at **04:36:53 UTC** during emulator startup. The three instrumentation tests completed at approximately **04:39:04 UTC**, taking 13.545 seconds in the XML report. The ANR precedes those tests; its CPU snapshot shows system/Google startup processes, without Widget Lab among the listed consumers. This establishes timing, not a proven root cause. The dialog remains visible in the captured studio screenshots; those screenshots are diagnostic evidence, not clean visual acceptance. No Widget Lab crash is recorded in the captured AndroidRuntime log. Physical launcher stability remains unverified.

[Final Android evidence](https://github.com/okthisisitright/the-lab/releases/tag/widget-lab-0.2.0-qa) includes XML results, screenshots, runtime output and the last-ANR dump, mirrored by [run 38851821603](https://github.com/okthisisitright/the-lab/actions/runs/38851821603).
