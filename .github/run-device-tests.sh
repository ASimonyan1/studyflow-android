#!/usr/bin/env bash
set -uo pipefail
gradle connectedDebugAndroidTest --no-daemon
result=$?
mkdir -p screenshots
for name in studyflow-home studyflow-editor; do
  if adb shell test -f "/sdcard/Download/StudyFlow/$name.png"; then
    adb pull "/sdcard/Download/StudyFlow/$name.png" "screenshots/$name.png"
  fi
done
exit "$result"
