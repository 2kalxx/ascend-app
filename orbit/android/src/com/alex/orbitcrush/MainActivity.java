package com.alex.orbitcrush;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity implements ValueCallback<String> {
    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.parseColor("#000000"));
        getWindow().setNavigationBarColor(Color.parseColor("#000000"));
        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#000000"));
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);   // progress is saved in localStorage
        s.setAllowFileAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        web.setWebViewClient(new LinkClient(this));
        web.addJavascriptInterface(new Bridge(this), "AppNative");
        setContentView(web);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        loadPage();
        Reminders.scheduleAll(this);
        Updater.start(this, false);
    }

    void buzz() {
        try {
            android.os.Vibrator v = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v != null) v.vibrate(android.os.VibrationEffect.createWaveform(new long[]{0, 180, 90, 180}, -1));
        } catch (Exception ignored) { }
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
        super.onDestroy();
    }
}
