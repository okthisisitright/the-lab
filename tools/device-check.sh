#!/usr/bin/env bash
# Preserve diagnostic evidence even if an instrumentation assertion fails.
set +e
./gradlew --no-daemon --console=plain :app:assembleDebug :app:assembleDebugAndroidTest
widgetlab_build_status=$?
if [ "$widgetlab_build_status" -ne 0 ]; then exit "$widgetlab_build_status"; fi
# Let cold-boot setup finish during compilation, then restart the test launcher before
# UI assertions. Preserve any earlier boot ANR separately rather than hiding its log.
mkdir -p app/build/device-visuals
adb logcat -d > app/build/device-visuals/boot-runtime.txt
adb shell am force-stop com.google.android.apps.nexuslauncher
adb shell am start -W -a android.intent.action.MAIN -c android.intent.category.HOME
adb logcat -c
./gradlew --no-daemon --console=plain :app:connectedDebugAndroidTest
widgetlab_test_status=$?
mkdir -p app/build/device-visuals
adb exec-out run-as dev.widgetlab tar -cf - files/visuals 2>/dev/null | tar -xf - -C app/build/device-visuals 2>/dev/null
adb pull /data/local/tmp/widgetlab-01-studio-home.png app/build/device-visuals/ 2>/dev/null
adb pull /data/local/tmp/widgetlab-02-clock-detail.png app/build/device-visuals/ 2>/dev/null
adb pull /data/local/tmp/widgetlab-03-music-gallery.png app/build/device-visuals/ 2>/dev/null
adb logcat -d -s AndroidRuntime:E WidgetLab:W ActivityManager:W > app/build/device-visuals/runtime.txt
adb shell dumpsys activity lastanr > app/build/device-visuals/last-anr.txt
exit "$widgetlab_test_status"
