#!/usr/bin/env bash
set -euo pipefail

mkdir -p verification-reports
collect_evidence() {
  adb exec-out run-as com.inandout.fieldphotoprep.internal tar -C files -cf - concept3-screens > verification-reports/concept3-screens.tar 2>/dev/null || true
  python3 - <<'PY'
import pathlib
import xml.etree.ElementTree as ET
for path in pathlib.Path('app/build/outputs/androidTest-results').rglob('*.xml'):
    try:
        root = ET.parse(path).getroot()
    except ET.ParseError:
        continue
    for failure in root.iter('failure'):
        print(path, failure.attrib, failure.text)
PY
}
trap collect_evidence EXIT

adb shell wm size 1080x2400
adb shell wm density 440
adb shell cmd uimode night no
adb shell settings put system font_scale 1.0
gradle connectedDebugAndroidTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true
cp -R app/build/reports/androidTests/connected verification-reports/full-instrumentation
for visual_variant in dark large-font; do
  if [ "$visual_variant" = dark ]; then adb shell cmd uimode night yes; else adb shell cmd uimode night no; fi
  if [ "$visual_variant" = large-font ]; then adb shell settings put system font_scale 1.3; else adb shell settings put system font_scale 1.0; fi
  gradle connectedDebugAndroidTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true -Pandroid.testInstrumentationRunnerArguments.class=com.inandout.fieldphotoprep.Concept3UiStructureInstrumentedTest,com.inandout.fieldphotoprep.Concept3RenderedScreensInstrumentedTest -Pandroid.testInstrumentationRunnerArguments.visualVariant="$visual_variant"
  cp -R app/build/reports/androidTests/connected "verification-reports/$visual_variant-instrumentation"
done
adb shell cmd uimode night no
adb shell settings put system font_scale 1.0
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am force-stop com.inandout.fieldphotoprep.internal
adb shell am start -W -n com.inandout.fieldphotoprep.internal/com.inandout.fieldphotoprep.MainActivity
sleep 3
adb shell pidof com.inandout.fieldphotoprep.internal | grep -q '[0-9]'
echo "Field Photo Prep Internal instrumentation and launch smoke tests passed"
