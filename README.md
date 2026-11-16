# Ascend

My personal, paywall-free 66-day habit app (daily quests, XP, levels, streaks, rotating side quests, reminders).

## How updates work
- `web/index.html` – the whole app UI. Bump `ASCEND_WEB_VERSION` in it **and** `"web"` in `version.json` → the Android app downloads it automatically on next open.
- `apk/Ascend.apk` – the Android wrapper (notifications, updater). Bump `versionCode` in `android/AndroidManifest.xml` and `"apk"` in `version.json` → the app shows an "Install" button.
- `minApk` – the oldest APK that can run the current `web/index.html`.

## Building the APK
Needs Android SDK (platform 34, build-tools 35). The signing key is `android/keystore.enc`
(decrypt: `openssl enc -d -aes-256-cbc -pbkdf2 -iter 200000 -in android/keystore.enc -out ~/ascend.keystore`; passphrase kept privately).
Then `KS_PASS=… android/build.sh`. Every APK **must** be signed with this same key or it can't update the installed app.

Install link: https://raw.githubusercontent.com/2kalxx/ascend-app/main/apk/Ascend.apk
