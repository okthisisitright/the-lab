Widget Lab / Edition II — Android 10+ device-test release

A complete new editorial studio and exactly 50 real Android widget designs: 12 motorsport, 8 music, 15 clocks, 5 photography, 5 essentials and 5 launcher/label designs.

- Transparent clocks, native timekeeping, six licensed typographic languages and independent widget profiles.
- Size-aware compositions, compact/tall arrangements, responsive Android 12+ size maps and separate older-Android orientations.
- Real media-session artwork and controls; no invented playback progress or continuous animation.
- Private photo-picker imports with focal cropping and preserved PNG transparency.
- Real app shortcuts with original, monochrome or initial-based icons and immediate missing-app fallback.
- Actual battery, interruption-filter and optional local calendar information.
- Cached, timestamped Jolpica racing data; clearly identified samples and archival imagery.

Install by opening WidgetLab-0.2.0.apk on your Android phone. Allow installation from your browser/file manager when prompted. It uses the same development certificate as the earlier optimized 0.1.0 APK, allowing an update that preserves settings. CI debug builds can have a different certificate. The previous separate Agenda provider is replaced by Date / agenda and Today / briefing; re-add an old agenda instance if the launcher removes it.

This is a development-signed testing release. Automated Android 15 checks are separate from physical Vivo, restart, real music-app and launcher acceptance; see the repository verification record. Analog timezone selection requires Android 12+; Android 10–11 uses the device timezone. Glass is an illustration, not live wallpaper refraction. Labels/launchers are widgets, not native launcher folders. Gallery art studies are explicitly examples; installed widgets use your own photos/media or honest empty states.

Known test-environment finding: the Android 15 emulator recorded a Pixel Launcher startup ANR before instrumentation began; all three app assertions passed, but clean launcher/physical-device acceptance remains pending. The full diagnostic evidence and timing are in docs/EDITION-II-VERIFICATION.md.
