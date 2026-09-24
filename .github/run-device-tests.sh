#!/usr/bin/env bash
set -uo pipefail
gradle connectedDebugAndroidTest --no-daemon
result=$?
mkdir -p screenshots
for name in studyflow-home studyflow-editor; do
  if adb shell run-as dev.asimonyan.studyflow test -f "files/$name.png"; then
    adb exec-out run-as dev.asimonyan.studyflow cat "files/$name.png" > "screenshots/$name.png"
  fi
done
exit "$result"
