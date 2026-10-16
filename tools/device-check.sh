#!/usr/bin/env bash
# Preserve diagnostic evidence even if an instrumentation assertion fails.
set +e
./gradlew --no-daemon --console=plain :app:connectedDebugAndroidTest
widgetlab_test_status=$?
mkdir -p app/build/device-visuals
adb exec-out run-as dev.widgetlab tar -cf - files/visuals 2>/dev/null | tar -xf - -C app/build/device-visuals 2>/dev/null
adb pull /data/local/tmp/widgetlab-01-studio-home.png app/build/device-visuals/ 2>/dev/null
adb pull /data/local/tmp/widgetlab-02-clock-detail.png app/build/device-visuals/ 2>/dev/null
adb logcat -d -s AndroidRuntime:E WidgetLab:W > app/build/device-visuals/runtime.txt
exit "$widgetlab_test_status"
