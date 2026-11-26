#!/usr/bin/env bash
set -euo pipefail

jarvis_core_apk="app/build/outputs/apk/core/debug/app-core-debug.apk"
jarvis_full_apk="app/build/outputs/apk/full/debug/app-full-debug.apk"
jarvis_report_dir="app/build/reports/core-permissions"
mkdir -p "$jarvis_report_dir"

apkanalyzer manifest print "$jarvis_core_apk" > "$jarvis_report_dir/core-manifest.xml"
apkanalyzer manifest print "$jarvis_full_apk" > "$jarvis_report_dir/full-manifest.xml"

python3 .github/scripts/verify-core-apk.py "$jarvis_core_apk" "$jarvis_full_apk" "$jarvis_report_dir" | tee "$jarvis_report_dir/result.txt"
