# Lingua (Android)

Offline language app: course packs built into `web/index.html` (Spanish: `course_es.js`, 30 units A1–B1).
Same update system as Ascend, under `lingua/`: bump `LINGUA_WEB_VERSION` in `web/index.html` and `"web"` in `version.json`
→ the app downloads it on next open. Native wrapper sources in `android/` (WebView + Android TextToSpeech + reminders + updater),
signed with the same key as Ascend (`../android/keystore.enc`).

Install link: https://raw.githubusercontent.com/2kalxx/ascend-app/main/lingua/apk/Lingua.apk
