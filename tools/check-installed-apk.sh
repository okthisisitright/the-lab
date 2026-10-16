#!/usr/bin/env bash
set -uo pipefail
./gradlew --no-daemon --console=plain :releasecheck:assembleDebug :releasecheck:assembleDebugAndroidTest || exit $?
adb shell am force-stop com.google.android.apps.nexuslauncher
adb shell input keyevent HOME
adb install -r "$WIDGETLAB_CHECK_APK" || exit $?
adb logcat -c
./gradlew --no-daemon --console=plain :releasecheck:connectedDebugAndroidTest
widgetlab_result=$?
mkdir -p releasecheck/build/device-evidence
adb logcat -d > releasecheck/build/device-evidence/runtime.txt
adb shell dumpsys appwidget > releasecheck/build/device-evidence/widgets.txt
for widgetlab_capture in clock player race; do
  adb pull "/data/local/tmp/widgetlab-release-$widgetlab_capture.png" releasecheck/build/device-evidence/ 2>/dev/null
done
exit "$widgetlab_result"
