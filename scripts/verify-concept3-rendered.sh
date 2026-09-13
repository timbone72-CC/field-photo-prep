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
adb exec-out run-as com.inandout.fieldphotoprep.internal tar -C files -cf - concept3-screens > verification-reports/light-screens.tar
for visual_variant in dark large-font; do
  if [ "$visual_variant" = dark ]; then adb shell cmd uimode night yes; else adb shell cmd uimode night no; fi
  if [ "$visual_variant" = large-font ]; then adb shell settings put system font_scale 1.3; else adb shell settings put system font_scale 1.0; fi
  # Direct runner arguments preserve the comma-separated classes and variant name.
  adb shell am instrument -w -e class com.inandout.fieldphotoprep.Concept3UiStructureInstrumentedTest,com.inandout.fieldphotoprep.Concept3RenderedScreensInstrumentedTest -e visualVariant "$visual_variant" com.inandout.fieldphotoprep.internal.test/androidx.test.runner.AndroidJUnitRunner | tee "verification-reports/$visual_variant-instrumentation.txt"
  grep -q '^OK (2 tests)' "verification-reports/$visual_variant-instrumentation.txt"
  adb shell run-as com.inandout.fieldphotoprep.internal test -f "files/concept3-screens/$visual_variant-home.png"
  adb shell run-as com.inandout.fieldphotoprep.internal test -f "files/concept3-screens/$visual_variant-work-orders.png"
  adb shell run-as com.inandout.fieldphotoprep.internal test -f "files/concept3-screens/$visual_variant-photos.png"
  adb exec-out run-as com.inandout.fieldphotoprep.internal tar -C files -cf - concept3-screens > "verification-reports/$visual_variant-screens.tar"
done
adb shell cmd uimode night no
adb shell settings put system font_scale 1.0
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am force-stop com.inandout.fieldphotoprep.internal
adb shell am start -W -n com.inandout.fieldphotoprep.internal/com.inandout.fieldphotoprep.MainActivity
sleep 3
adb shell pidof com.inandout.fieldphotoprep.internal | grep -q '[0-9]'
echo "Field Photo Prep Internal instrumentation and launch smoke tests passed"
