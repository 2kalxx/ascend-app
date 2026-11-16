package com.alex.lingua;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.JavascriptInterface;

/** Methods the page can call as window.LinguaNative.* */
public class Bridge implements Runnable {
    private final MainActivity act;
    Bridge(MainActivity act) { this.act = act; }

    private SharedPreferences prefs() { return act.getSharedPreferences(Reminders.PREFS, Context.MODE_PRIVATE); }

    @JavascriptInterface public boolean hasVoice(String tag) { return act.hasVoice(tag); }

    @JavascriptInterface public void speak(String text, String tag, double rate) { act.say(text, tag, (float) rate); }

    /** Today's XP vs goal, so the reminder only fires when the goal isn't reached. */
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

    @JavascriptInterface public void checkUpdates() { Updater.start(act, true); }
    @JavascriptInterface public void installApk() { ApkInstaller.start(act); }

    @JavascriptInterface
    public String apkVersion() {
        try { return act.getPackageManager().getPackageInfo(act.getPackageName(), 0).versionName; }
        catch (Exception e) { return "?"; }
    }
}
