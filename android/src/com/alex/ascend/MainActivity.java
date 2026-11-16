package com.alex.ascend;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
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
        getWindow().setStatusBarColor(Color.parseColor("#0d0f17"));
        getWindow().setNavigationBarColor(Color.parseColor("#0d0f17"));
        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#0d0f17"));
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);   // localStorage = where your progress is saved
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        web.setWebViewClient(new WebViewClient());
        web.addJavascriptInterface(new Bridge(this), "AscendNative");
        setContentView(web);
        loadPage();
        Reminders.scheduleAll(this);
        Updater.start(this, false);
    }

    /** Newest content wins: the downloaded update if there is one, otherwise the copy inside the APK. */
    void loadPage() {
        java.io.File f = Updater.downloadedIfNewer(this);
        web.loadUrl(f != null ? "file://" + f.getAbsolutePath() : "file:///android_asset/index.html");
    }

    /** Called from the updater thread. */
    void onUpdateResult(final boolean webChanged, final int newApk, final boolean manual) {
        runOnUiThread(new UiTask(this, webChanged, newApk, manual));
    }

    void applyUpdateResult(boolean webChanged, int newApk, boolean manual) {
        if (webChanged) loadPage(); // the page shows "Ascend updated" itself
        if (newApk > 0) {
            String js = "window.ascendApkUpdate&&ascendApkUpdate(" + newApk + ")";
            if (webChanged) web.postDelayed(new UiTask(this, js), 1500); else web.evaluateJavascript(js, null);
        }
        if (!webChanged && newApk == 0 && manual) toastInPage("You're on the latest version ✓");
    }

    void toastInPage(final String msg) {
        String js = "window.ascendToast&&ascendToast(" + org.json.JSONObject.quote(msg) + ")";
        runOnUiThread(new UiTask(this, js));
    }

    void runJs(String js) { if (web != null) web.evaluateJavascript(js, null); }

    /** Called from the web page (via Bridge) when a reminder is switched on. */
    void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    @Override
    public void onBackPressed() {
        // Close an open dialog in the app first; otherwise leave the app
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
        // re-render so "Today" rolls over if the app was in the background overnight
        if (web != null) web.evaluateJavascript("window.dispatchEvent(new Event('focus'))", null);
        if (web != null) Updater.start(this, false);
    }
}
