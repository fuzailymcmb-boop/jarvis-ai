"""Audit the packaged Core APK; Full provides a positive control for listener detection."""
from pathlib import Path
import sys
import xml.etree.ElementTree as ET
import zipfile


ANDROID = "{http://schemas.android.com/apk/res/android}"
CORE_PERMISSIONS = {
    "android.permission.INTERNET",
    "android.permission.RECORD_AUDIO",
    "android.permission.POST_NOTIFICATIONS",
    "android.permission.SCHEDULE_EXACT_ALARM",
    "android.permission.RECEIVE_BOOT_COMPLETED",
    "com.jarvis.assistant.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
}
BLOCKED_MANIFEST_VALUES = {
    "android.permission.READ_SMS",
    "android.permission.RECEIVE_SMS",
    "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",
    "android.permission.BIND_ACCESSIBILITY_SERVICE",
    "android.service.notification.NotificationListenerService",
    "android.accessibilityservice.AccessibilityService",
}
CORE_EXCLUDED_DEX_STRINGS = (
    b"Lcom/jarvis/assistant/whatsapp/WhatsAppListener;",
    b"Lcom/jarvis/assistant/whatsapp/WhatsAppStore;",
    b"Landroid/service/notification/NotificationListenerService;",
    b"Landroid/service/notification/StatusBarNotification;",
    b"android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS",
    b"android.settings.NOTIFICATION_LISTENER_SETTINGS",
    b"android.settings.NOTIFICATION_LISTENER_DETAIL_SETTINGS",
    b"Lcom/jarvis/assistant/whatsapp/WhatsAppListener$",
    b"Lcom/jarvis/assistant/whatsapp/WhatsAppStore$",
)


def dex_bytes(apk):
    with zipfile.ZipFile(apk) as archive:
        entries = [name for name in archive.namelist() if name.startswith("classes") and name.endswith(".dex")]
        assert entries, f"No DEX code found in {apk}"
        return b"\n".join(archive.read(name) for name in entries)


core_apk, full_apk, report_dir = map(Path, sys.argv[1:])
core = ET.parse(report_dir / "core-manifest.xml").getroot()
full = ET.parse(report_dir / "full-manifest.xml").getroot()

assert core.attrib["package"] == "com.jarvis.assistant", "Unexpected Core application ID"
assert full.attrib["package"] == core.attrib["package"], "Editions must retain the same application ID"
assert core.attrib.get(ANDROID + "versionCode") == "2", "Unexpected Core version"
assert core.attrib.get(ANDROID + "versionName") == "0.1.1-core", "Core edition must be clearly versioned"

permissions = {
    node.attrib[ANDROID + "name"]
    for node in core
    if node.tag.startswith("uses-permission")
}
assert permissions <= CORE_PERMISSIONS, f"Unexpected Core permissions: {permissions - CORE_PERMISSIONS}"
assert "android.permission.RECORD_AUDIO" in permissions, "Voice permission missing"
assert "android.permission.POST_NOTIFICATIONS" in permissions, "Reminder notification permission missing"
for node in core.iter():
    assert not (set(node.attrib.values()) & BLOCKED_MANIFEST_VALUES), f"Sensitive declaration remains: {node.attrib}"
    assert "WhatsAppListener" not in " ".join(node.attrib.values()), "Listener remains in Core manifest"
assert not core.findall("./application/service"), "Core should not expose any background services"
assert core.find("./application").attrib.get(ANDROID + "testOnly") != "true", "Core must install without adb test flags"
assert any(
    service.attrib.get(ANDROID + "permission") == "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
    for service in full.findall("./application/service")
), "Positive control failed: Full notification listener is missing"

core_dex = dex_bytes(core_apk)
full_dex = dex_bytes(full_apk)
for excluded in CORE_EXCLUDED_DEX_STRINGS:
    assert excluded not in core_dex, f"Notification-reading code/settings remain in Core: {excluded.decode()}"
for expected in CORE_EXCLUDED_DEX_STRINGS[:4]:
    assert expected in full_dex, f"Positive control failed: Full missing {expected.decode()}"

print("PASS: Core retains com.jarvis.assistant and is versioned 0.1.1-core.")
print("PASS: Core declares only expected voice, network, reminder, and AndroidX internal permissions.")
print("PASS: Core has no notification-listener, accessibility, or SMS manifest declarations.")
print("PASS: Core DEX excludes the notification reader/store, platform listener classes, and notification-access Settings links.")
print("PASS: Full still contains its declared notification listener and implementation.")
print("This audit verifies the packaged capability separation; it is not Google Play Protect approval.")
