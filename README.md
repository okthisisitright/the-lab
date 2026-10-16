# Widget Lab

A native Android studio for a personal collection of **50 real home-screen widgets**. Kotlin, Compose, AppWidgetProvider and RemoteViews. Android 10+; Android 15 is the primary integration-test target. No development server, accounts, ads or analytics.

The app has a searchable collection browser, artwork-backed design studies, live-rendered previews, proportion/wallpaper inspection, per-instance customization, pinning, installed-widget management and local permission diagnostics.

| Collection | Designs |
|---|---:|
| Motorsport | 12 |
| Music | 8 |
| Clocks | 15 |
| Photography | 5 |
| System / essentials | 5 |
| Launchers / labels | 5 |

See [all 50 designs, dimensions and controls](docs/CATALOG.md), [Edition II architecture and behavior](docs/EDITION-II.md), [verification](docs/EDITION-II-VERIFICATION.md) and [physical-device acceptance](docs/DEVICE-TESTING.md).

## Widget behavior

Layouts are calculated for actual widget dimensions. Compact layouts simplify information; portrait layouts move images; tables limit visible rows; launcher grids reveal as many targets as fit. All supported Android versions use independent portrait/landscape RemoteViews for launcher compatibility. Resize broadcasts generate fresh layouts. Raster scaling is uniform, and bitmap allocations are bounded across size variants.

Most clocks default to a fully transparent background. Six surface treatments and six licensed typographic languages support different home-screen compositions. Native TextClock/AnalogClock views keep time without background bitmap polling. Previews share the actual geometry and renderer. Wallpaper backdrops and clearly labeled NASA art studies are preview content, not fake live data.

Music widgets use real active sessions, published metadata, album artwork, capability-aware controls and callbacks. They do not simulate progress or continuous animation. Photography uses the Android photo picker and privately stored, orientation-corrected image copies. App launchers use selected installed activity components and native tap regions; they do not create launcher folders. DND and battery use actual Android state. Calendar events remain on the device.

Racing uses independent cached Jolpica feeds, source timestamps, IANA timezone conversion and explicit historical-sample labels. Season-specific relationships are preserved. Personal driver/car imagery is supported; a bundled Hamilton photo is credited as a 2021 archive. No unrelated Racecraft app is included.

## Install 0.3.0

[**Download WidgetLab-0.3.0.apk (9.32 MB)**](https://github.com/okthisisitright/the-lab/releases/download/widget-lab-0.3.0-test/WidgetLab-0.3.0.apk) · [Release notes and checksum](https://github.com/okthisisitright/the-lab/releases/tag/widget-lab-0.3.0-test)

Install over the previous optimized release. Open Widget Lab once, then **Settings → Refresh installed widgets**. Existing profiles and private photos are preserved by the matching signing certificate. If one old instance stays blank, remove and re-add that widget. Do not uninstall the whole app to troubleshoot.

This update adds a spacious Google Sans interface, plain widget names, eight replacement music designs, configuration completion ordering and launcher compatibility fixes. Non-clock designs no longer inflate hidden clocks. See [0.3.0 changes and verification](docs/REVISION-0.3.0.md), the [new app UI](https://github.com/okthisisitright/the-lab/releases/download/widget-lab-0.3.0-qa/widgetlab-01-studio-home.png) and a [real transparent clock in the external host](https://github.com/okthisisitright/the-lab/releases/download/widget-lab-0.3.0-qa/widgetlab-release-clock.png).

The published APK was downloaded and compared byte-for-byte with the verified signed file. [Exact Actions artifact ZIP](https://github.com/okthisisitright/the-lab/actions/runs/38925000635/artifacts/12055943506), retained for 30 days, contains the same APK/checksum. The release asset is the simpler phone download.

20 local tests, three Android 15 integration tests and two installed-APK tests pass. The latter host all 50 providers across processes, check native clock pixels, requested resize geometry and framework updates after process death. Physical Vivo placement acceptance and real third-party music-app compatibility still require phone testing.

## Build and install

Use JDK 17 or 21, Android SDK platform 35/build-tools 35.0.0, and Gradle 8.11.1 (wrapper included). Set ANDROID_HOME or an ignored local.properties sdk.dir.

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Open the APK on your phone, allow **Install unknown apps** for the browser/file manager when Android asks, and install. A computer is only required for building. Add through a design's **Add to Home Screen** action or the launcher's Widgets picker. Long-press to resize; supported grid sizes depend on your launcher.

## GitHub distribution and checks

[Android APK](https://github.com/okthisisitright/the-lab/actions/workflows/android.yml) runs on main pushes, pull requests and manual dispatch. It uploads **Widget-Lab-development-APK** immediately after debug compilation, before tests/lint/release checks. Download its artifact ZIP and extract app-debug.apk. Artifacts expire after 30 days. The SDK action explicitly avoids its obsolete default tools package.

[Android 15 device checks](https://github.com/okthisisitright/the-lab/actions/workflows/device-tests.yml) runs manually on a hardware-accelerated Android 15 emulator. It checks all providers in an AppWidgetHost, real controlled media sessions, and studio configuration navigation; screenshots/reports are retained as artifacts. This does not replace physical Vivo or real third-party-player acceptance.

For stable private release updates, back up a private signing key and configure WIDGETLAB_KEYSTORE_BASE64, WIDGETLAB_STORE_PASSWORD, WIDGETLAB_KEY_ALIAS and WIDGETLAB_KEY_PASSWORD as repository Actions secrets. CI development signing uses a best-effort cached identity and can differ from the optimized device-test release. Android rejects updates signed with a different key; uninstalling removes settings. Signing keys never belong in git.

The owner authorized proceeding with the repository's current visibility and will manage it separately. APKs contain no credentials. [Licenses, imagery and data attribution](docs/THIRD-PARTY-NOTICES.md) are also available inside Studio → Credits.
