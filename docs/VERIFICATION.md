# Verification record

Development date: 15 October 2026. This record separates implementation checks from physical-device acceptance.

## Passed locally

- Android debug APK compilation, using Gradle 8.11.1, Android Gradle Plugin 8.9.2, Kotlin/Compose compiler 2.1.20, compile/target SDK 35, minimum SDK 29 and JDK 21.
- Optimized unsigned release compilation with R8. An unsigned APK is not installable; private release signing is configured through repository secrets.
- Debug Android instrumentation APK compilation. Compilation is not a passing device test.
- Twelve local tests: six racing parser/data-semantic tests and six Robolectric runtime/render/persistence tests, with zero failures.
- Native RemoteViews clock layout inflation and timezone/format setters on simulated API 29.
- All 19 provider registrations, metadata, configuration entry points, sizes and periodic-update declarations inspected through the merged Android manifest/resources.
- Per-instance profile isolation, pin-token consumption, safe malformed-profile defaults and private artwork path validation.
- Failed feed refresh retains the last successful payload/timestamp, marks its error and keeps unavailable feeds labeled sample.
- Recorded real provider fixtures: nonempty 2026 standings, matching team IDs, structured sprint sessions, missing-time handling, completed-result parsing and next-race exclusion.
- Timezone conversion across London DST and a half-hour-offset zone (Asia/Kolkata).
- Native graphics renders of all 19 designs, including opposite themes, ten-row state, empty media/permission states, battery and agenda fallbacks. Raw bitmap allocation limited to 420,000 pixels / 1.68 MB per surface.
- Visual contact sheet inspected. Fixed music title overflows; compact playback layout reserves space for native 44dp transport targets.
- Lint has no errors. Warnings include newer optional dependency versions, English-only literal labels, Android 12+ sizing attributes intentionally ignored on API 29, synchronous small profile commits for durability, and application-context references intentionally retained for process-lifetime media/update dispatch.
- Development APK signature verified with Android apksigner.
- Four public Jolpica HTTPS endpoints checked with successful responses. Usage terms and rate limits reviewed, with attribution and data license bundled in APK notices.

[Shared-renderer design contact sheet](previews/widget-collection.png). Picker preview resources are generated from the same renderer; they are static previews, not replacements for providers.

## Device checks and limitations

A Google APIs x86_64 API 35 emulator was installed and started locally. This executor does not expose `/dev/kvm`; it uses CPU emulation. ADB became available, but Android package/window services were still unavailable during initial install attempts. After more than 20 minutes, the emulator still lacked the package service and was stopped. Those install attempts failed before the app was installed; they are not app crashes. Do not interpret emulator startup or instrumentation compilation as successful widget placement or playback verification.

The instrumented test suite and manual **Android 15 device checks** GitHub workflow cover:

- Binding all 19 actual providers to an Android AppWidgetHost and applying native layouts.
- Selecting a playing media-session token when another paused session belongs to the same package.
- Real transport callback delivery, unsupported-action suppression, metadata callbacks and listener-access revocation.

Run that workflow or `./gradlew :app:connectedDebugAndroidTest` on a ready Android device/emulator. No physical Vivo was connected. No third-party music player, restart persistence, large font scale, launcher resizing/pinning UI or Funtouch OS power policy has been physically verified. The detailed [device checklist](DEVICE-TESTING.md) remains an acceptance requirement.

On 16 October 2026, source/workflows were pushed to [okthisisitright/the-lab](https://github.com/okthisisitright/the-lab). The [publishing run](https://github.com/okthisisitright/the-lab/actions/runs/38839324832) succeeded: it verified the existing optimized APK checksum and uploaded both an Actions artifact and release assets without rebuilding the app. The release APK was independently downloaded and passed checksum/signature verification. The corrected [Android APK run](https://github.com/okthisisitright/the-lab/actions/runs/38839317610) also compiled its debug APK and uploaded [Widget-Lab-development-APK](https://github.com/okthisisitright/the-lab/actions/runs/38839317610/artifacts/12020019952). Tests/lint/release compilation follow the debug upload. The Android 15 device-check workflow has not been run. See [delivery details](APK-DELIVERY.md).

These APKs use development signing identities. Configure and back up your own private release key for stable update signatures. Physical acceptance and durable private signing remain prerequisites for calling this a finished production release.
