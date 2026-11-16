# LiftRank (Android)

Gym app that ranks every muscle (Untrained → Bronze → … → Legend, each III/II/I) from your logged lifts:
best set → estimated 1-rep max (Epley) ÷ bodyweight, compared to common strength standards (female factor applied).
UI: `web/index.html` (bump `LIFTRANK_WEB_VERSION` and `"web"` in `version.json` → auto-update on next app start).
Native wrapper sources in `android/` (WebView, updater, training reminder, vibration), signed with the same key as Ascend.

Install link: https://raw.githubusercontent.com/2kalxx/ascend-app/main/liftrank/apk/LiftRank.apk
