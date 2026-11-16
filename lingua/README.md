# Lingua (Android)

Offline language app. The UI is `src/app.html`; every offline course is one file in `courses/<code>.js`.
`python3 lingua/build_web.py` inlines all courses into `web/index.html` and bumps `"web"` in `version.json`
→ the installed app downloads it automatically on next open (no reinstall).

## Course pack format (`courses/<code>.js`)
```js
COURSES["fr"] = {lang:"fr", tts:"fr-FR", nospace:false, units:[
  {w:[["bonjour","hello"], ... 12 words],          // [target, English(, romanization)]
   s:[["Bonjour, Marie","Hello, Marie"], ... 8 sentences]},
  ... 30 units, same topics/order as UNITS in src/app.html (A1 1–10, A2 11–20, B1 21–30)
]};
```
- `lang` must match the code in the LANGS list in `src/app.html`; `tts` is a BCP-47 voice tag.
- Non-Latin scripts: add romanization as 3rd element of every word and sentence.
- Languages without spaces (zh, ja, th, lo, km, my): set `nospace:true` and separate natural word chunks with single spaces in sentences (they're joined for display and used as build-a-sentence tiles).
- Words must be unique across the whole course. Check with `node lingua/validate_course.js lingua/courses/<code>.js`.

## Native app
Sources in `android/` (WebView + Android TextToSpeech + reminders + updater), signed with the same key as Ascend.
Install link: https://raw.githubusercontent.com/2kalxx/ascend-app/main/lingua/apk/Lingua.apk
