#!/usr/bin/env bash
set -uo pipefail
status=0
bash gradlew connectedGithubDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.deniscerri.ytdl.SolidUiSmokeTest --stacktrace || status=$?
adb pull /sdcard/Download/solid-previews/ solid-previews/ || true
exit "$status"
