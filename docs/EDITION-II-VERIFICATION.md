# Edition II verification / 0.2.0

16 October 2026. The catalog contains exactly 50 designs/providers with the requested 12 / 8 / 15 / 5 / 5 / 5 distribution.

## Passed locally

- Debug compilation, optimized release compilation, and instrumentation APK compilation with Kotlin/Compose 2.1.20, AGP 8.9.2, Gradle 8.11.1, SDK 35, minimum API 29.
- 13 local tests: six provider/data semantics tests and seven runtime/render/configuration tests.
- All 50 provider registrations, configuration entry points, sizing metadata and refresh declarations.
- Actual RemoteViews inflation of all 50 designs, including native clock children and vector controls.
- Rendering at default, compact, tall and alternate-theme proportions, including empty media/calendar states.
- Native clock region bounds, independent dual timezones, per-instance persistence, new photo/app profile fields, bad-profile fallbacks, safe artwork filenames, cache failure retention, DST and fractional-offset zones.
- Uniform raster scaling and 420,000-pixel / 1.68 MB per-surface bounds. Responsive updates share this pixel budget across variants.
- All 50 Android picker previews generated from tested renderer output; six collection sheets visually inspected. This inspection found and corrected overlapping constructor stats, cramped podium labels, early session truncation and unused vinyl space.
- Lint with zero errors after correcting a suspiciously indented painting branch. Warnings cover optional dependency upgrades, English-only labels, API 31 sizing attributes on min API 29, dynamically selected resource names and intentional process-lifetime caches.
- Workflows validated by actionlint. Public Jolpica 2026 schedule rechecked: 23 races returned. Prior parser/provider coverage is retained.

## Executed on Android 15

[Android 15 integration run 38842888179](https://github.com/okthisisitright/the-lab/actions/runs/38842888179) passed all three tests:

- Bind all 50 providers to Android AppWidgetHost, update them and inflate the configured native views.
- Two controlled media sessions in the same package: choose the playing token, deliver supported next/pause callbacks, suppress unsupported previous, observe metadata updates, and revoke listener access.
- Navigate from the studio gallery to clock settings and switch solid/transparent surfaces.

The preceding run exposed an unstable UI-test scroll selector; it was corrected with a stable settings-list identifier. A later run adds durable screenshot capture and further host-content assertions. Its result is recorded separately when finished.

## Limits of this verification

No physical Vivo is connected. Third-party music apps, actual phone launcher pin/resize behavior, device restart persistence, large system font settings and Vivo power policies still require the [device checklist](DEVICE-TESTING.md). The media test uses actual Android sessions controlled by test code; it is not a claim of Spotify/YouTube Music/etc. compatibility.

The original attachment filesystem paths are unavailable during this rebuild. Art direction uses the references visible in the conversation, their written specification and the credited archival portrait already in the project. No unlicensed driver/car image scraping is used.

Gallery-only NASA cover/photo studies are explicitly labeled. Native installed widgets use actual selected images/media or honest empty states. No fabricated live championship data, playback progress, proprietary focus status or continuous animation is claimed.

Signing and actual uploaded APK links/checksums are recorded after final artifact publication.
