package com.alex.lingua;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.util.Locale;

public class MainActivity extends Activity implements ValueCallback<String>, TextToSpeech.OnInitListener {
    private WebView web;
    TextToSpeech tts;
    volatile boolean ttsReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.parseColor("#0c1016"));
        getWindow().setNavigationBarColor(Color.parseColor("#0c1016"));
        tts = new TextToSpeech(this, this);
        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#0c1016"));
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);   // progress is saved in localStorage
        s.setAllowFileAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        web.setWebViewClient(new LinkClient(this));
        web.addJavascriptInterface(new Bridge(this), "LinguaNative");
        setContentView(web);
        loadPage();
        Reminders.scheduleAll(this);
        Updater.start(this, false);
    }

    @Override
    public void onInit(int status) {
        ttsReady = status == TextToSpeech.SUCCESS;
        if (ttsReady) runJs("window.__lingua&&__lingua.render&&!__lingua.lesson&&__lingua.render()");
    }

    boolean hasVoice(String tag) {
        if (!ttsReady) return false;
        try { return tts.isLanguageAvailable(Locale.forLanguageTag(tag)) >= TextToSpeech.LANG_AVAILABLE; }
        catch (Exception e) { return false; }
    }

    void say(String text, String tag, float rate) {
        if (!ttsReady) return;
        tts.setLanguage(Locale.forLanguageTag(tag));
        tts.setSpeechRate(rate);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lingua");
    }

    void loadPage() {
        java.io.File f = Updater.downloadedIfNewer(this);
        web.loadUrl(f != null ? "file://" + f.getAbsolutePath() : "file:///android_asset/index.html");
    }

    void onUpdateResult(final boolean webChanged, final int newApk, final boolean manual) {
        runOnUiThread(new UiTask(this, webChanged, newApk, manual));
    }

    void applyUpdateResult(boolean webChanged, int newApk, boolean manual) {
        if (webChanged) loadPage();
        if (newApk > 0) {
            String js = "window.ascendApkUpdate&&ascendApkUpdate(" + newApk + ")";
            if (webChanged) web.postDelayed(new UiTask(this, js), 1500); else web.evaluateJavascript(js, null);
        }
        if (!webChanged && newApk == 0 && manual) toastInPage("You're on the latest version ✓");
    }

    void toastInPage(final String msg) {
        runOnUiThread(new UiTask(this, "window.ascendToast&&ascendToast(" + org.json.JSONObject.quote(msg) + ")"));
    }

    void runJs(String js) { if (web != null) web.post(new UiTask(this, js, true)); }
    void runJsNow(String js) { if (web != null) web.evaluateJavascript(js, null); }

    void askNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33
                && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        }
    }

    @Override
    public void onBackPressed() {
        web.evaluateJavascript(
            "(function(){var l=document.getElementById('layer');if(l&&l.innerHTML){l.innerHTML='';return 'closed';}return 'none';})()",
            this);
    }

    @Override
    public void onReceiveValue(String r) {
        if (!"\"closed\"".equals(r)) finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) { web.evaluateJavascript("window.dispatchEvent(new Event('focus'))", null); Updater.start(this, false); }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) tts.shutdown();
        super.onDestroy();
    }
}
