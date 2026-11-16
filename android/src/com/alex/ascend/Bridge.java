package com.alex.ascend;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.JavascriptInterface;

/** Methods the web page can call as window.AscendNative.* */
public class Bridge implements Runnable {
    private final MainActivity act;

    Bridge(MainActivity act) { this.act = act; }

    private SharedPreferences prefs() {
        return act.getSharedPreferences(Reminders.PREFS, Context.MODE_PRIVATE);
    }

    /** Today's progress, so the evening reminder knows whether quests are still open. */
    @JavascriptInterface
    public void report(String date, int done, int total, int streak, String name) {
        prefs().edit().putString("date", date).putInt("done", done).putInt("total", total)
                .putInt("streak", streak).putString("name", name).apply();
    }

    /** JSON like {"m":{"on":true,"t":"08:00"},"e":{"on":false,"t":"20:00"}} */
    @JavascriptInterface
    public void setReminders(String json) {
        String old = prefs().getString("rem", "");
        if (json.equals(old)) return;
        prefs().edit().putString("rem", json).apply();
        Reminders.scheduleAll(act);
    }

    @JavascriptInterface
    public void askPermission() { act.runOnUiThread(this); }

    @Override
    public void run() { act.askNotificationPermission(); }

    /** Health Connect: ask permission + read (called from "Connect" in Settings). */
    @JavascriptInterface
    public void healthConnect() { act.runOnUiThread(new HealthTask(act, true)); }

    /** Health Connect: re-read today's data (on open, on resume, every 15 min). */
    @JavascriptInterface
    public void healthRefresh() { act.runOnUiThread(new HealthTask(act, false)); }

    @JavascriptInterface
    public void checkUpdates() { Updater.start(act, true); }

    @JavascriptInterface
    public void installApk() { ApkInstaller.start(act); }

    @JavascriptInterface
    public String apkVersion() {
        try { return act.getPackageManager().getPackageInfo(act.getPackageName(), 0).versionName; }
        catch (Exception e) { return "?"; }
    }

    @JavascriptInterface
    public void test() {
        Reminders.notify(act, "🔥 Ascend reminders work!", "This is what your daily nudge will look like.", 99);
    }
}
