Widget Lab 0.3.0

- A quieter, spacious interface with Google Sans, off-white surfaces and plain widget names.
- Replaced all eight old music designs with glass, circular, tall artwork, panoramic and now-playing compositions inspired by the supplied references.
- Configuration waits for the first widget update before closing. Pin callbacks update only the new instance.
- Launcher compatibility: portrait/landscape RemoteViews on every Android version, recalculated on resize.
- Widgets inflate only their needed native components; non-clock designs no longer depend on hidden clocks.
- Local update diagnostics and a visible retry state instead of silent render failures.
- All 50 provider IDs remain stable, preserving existing profiles and installations.

Download WidgetLab-0.3.0.apk on the phone, open it, permit installation from your browser/file manager if asked, and install as an update. It uses the same development signing certificate as the earlier optimized release APKs.

After updating, open Widget Lab once. Use Settings → Refresh installed widgets for existing blank instances. If one remains blank, remove and re-add that instance; see Settings for the last local update/error. Do not uninstall the whole app unless you intend to erase its private photos and settings.

This is a development-signed testing release. The old APK's Vivo problem was not reproduced on the Android 15 test launcher. Configuration timing and compatibility changes address plausible causes; physical Vivo verification is still needed. Media integration checks use controlled real Android sessions, not a claim of tested compatibility with every player. Glass uses supported transparency/artwork diffusion, not live wallpaper blur.

Verification: 20 local tests and three Android 15 integration tests passed. Independent installed-APK checks also passed for all 50 providers, native clock pixels, requested resize geometry, configuration completion and framework-driven updates after process death. Full evidence: docs/REVISION-0.3.0.md.
