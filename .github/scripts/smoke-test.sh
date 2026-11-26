#!/usr/bin/env bash
set -euo pipefail

jarvis_report_dir="app/build/reports/device-smoke"
mkdir -p "$jarvis_report_dir"
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -c
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
adb shell am start -W -n com.jarvis.assistant/.MainActivity > "$jarvis_report_dir/launch.txt"
sleep 5
adb shell pidof com.jarvis.assistant > "$jarvis_report_dir/process.txt"
adb shell dumpsys activity activities > "$jarvis_report_dir/activities.txt"
adb logcat -d -b crash > "$jarvis_report_dir/crashes.txt"
adb shell uiautomator dump /sdcard/jarvis-window.xml
adb pull /sdcard/jarvis-window.xml "$jarvis_report_dir/window.xml"
adb exec-out screencap -p > "$jarvis_report_dir/home.png"

python3 - <<'PY'
from pathlib import Path
import re
import xml.etree.ElementTree as ET

reports = Path('app/build/reports/device-smoke')
assert 'Status: ok' in (reports / 'launch.txt').read_text(), 'Android did not launch JARVIS'
assert (reports / 'process.txt').read_text().strip(), 'JARVIS process is not running'
assert re.search(r'mResumedActivity:.*com\.jarvis\.assistant', (reports / 'activities.txt').read_text()), 'JARVIS is not the resumed activity'
assert 'com.jarvis.assistant' not in (reports / 'crashes.txt').read_text(), 'JARVIS crashed after launch'
window = ET.parse(reports / 'window.xml')
assert any('JARVIS' in node.attrib.get('text', '') for node in window.iter()), 'JARVIS interface is not visible'
print('PASS: JARVIS installed, launched, stayed running, and displayed its interface on Android 10.')
PY
