# Ascend

My personal, paywall-free 66-day habit app (daily quests, XP, levels, streaks, rotating side quests, reminders).

## How updates work
- `web/index.html` – the whole app UI. Bump `ASCEND_WEB_VERSION` in it **and** `"web"` in `version.json` → the Android app downloads it automatically on next open.
- `apk/Ascend.apk` – the Android wrapper (notifications, updater). Bump `versionCode` in `android/AndroidManifest.xml` and `"apk"` in `version.json` → the app shows an "Install" button.
- `minApk` – the oldest APK that can run the current `web/index.html`.

## Building the APK
Gradle project in `android/` (AGP 8.12, Kotlin 2.1, compileSdk 36, androidx Health Connect 1.1.0). Needs JDK 17+ and the Android SDK.

```
openssl enc -d -aes-256-cbc -pbkdf2 -iter 200000 -in android/keystore.enc -out ~/ascend.keystore   # passphrase kept privately
mkdir -p android/app/src/main/assets && cp web/index.html android/app/src/main/assets/
cd android && KS_PASS=… gradle assembleRelease
cp app/build/outputs/apk/release/app-release.apk ../apk/Ascend.apk
```
Bump `versionCode` in `android/app/build.gradle.kts` and `"apk"` in `version.json`. Every APK **must** be signed with this same key or it can't update the installed app.

Install link: https://raw.githubusercontent.com/2kalxx/ascend-app/main/apk/Ascend.apk
