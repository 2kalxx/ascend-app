package com.alex.orbitcrush;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.JavascriptInterface;

/** Methods the page can call as window.AppNative.* */
public class Bridge implements Runnable {
    private final MainActivity act;
    Bridge(MainActivity act) { this.act = act; }

    private SharedPreferences prefs() { return act.getSharedPreferences(Reminders.PREFS, Context.MODE_PRIVATE); }

    /** Whether you trained today, so the reminder only fires on rest days you didn't plan. */
    @JavascriptInterface
    public void report(String date, int xp, int goal, int streak, String name) {
        prefs().edit().putString("date", date).putInt("done", xp).putInt("total", goal)
                .putInt("streak", streak).putString("name", name).apply();
    }

    @JavascriptInterface
    public void setReminders(String json) {
        if (json.equals(prefs().getString("rem", ""))) return;
        prefs().edit().putString("rem", json).apply();
        Reminders.scheduleAll(act);
    }

    @JavascriptInterface public void askPermission() { act.runOnUiThread(this); }
    @Override public void run() { act.askNotificationPermission(); }

    /** Opens a web link outside the app (e.g. the AI version of Lingua in Claude). */
    @JavascriptInterface
    public void openUrl(String url) {
        if (url == null || !(url.startsWith("https://") || url.startsWith("http://"))) return;
        act.startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    @JavascriptInterface public void buzz() { act.buzz(); }

    @JavascriptInterface public void checkUpdates() { Updater.start(act, true); }
    @JavascriptInterface public void installApk() { ApkInstaller.start(act); }

    @JavascriptInterface
    public String apkVersion() {
        try { return act.getPackageManager().getPackageInfo(act.getPackageName(), 0).versionName; }
        catch (Exception e) { return "?"; }
    }
}
