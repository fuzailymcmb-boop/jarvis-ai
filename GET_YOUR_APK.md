# Install your JARVIS test APK

1. Open https://github.com/fuzailymcmb-boop/jarvis-ai/actions/workflows/build-apk.yml and sign in to GitHub.
2. Open the latest successful run for the `main` branch.
3. Download **JARVIS-AI-Android-debug-APK** from the run's Artifacts section.
4. Extract the ZIP. It contains **JARVIS-AI-debug.apk** and **SHA256SUMS.txt**.
5. Transfer the APK to an Android 8.0-or-newer phone, open it, and approve installation for the app you used to open the file if Android asks.
6. Open JARVIS and grant the permissions for the features you want to test.

The GitHub build does not need an OpenAI API key. General AI conversations in the installed app require a running HTTPS backend and your personal server access token, configured under JARVIS Settings. Reminders and WhatsApp notification access are configured on the phone.

A successful build checks compilation, unit tests, lint, and the APK signature. Physical-phone testing is still required. This debug APK is for personal testing and is not a signed Play Store release. Debug signing keys can change between workflow runs; a later test build may need the old version uninstalled first, which clears its local data.

Read README.md for the backend setup and device test checklist.
