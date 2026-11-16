package com.alex.ascend;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONObject;

/**
 * Checks GitHub for a newer version of the app content (index.html) or of the APK itself.
 * Content updates are downloaded and applied automatically; APK updates are offered to the user.
 */
public class Updater implements Runnable {
    static final String BASE = "https://raw.githubusercontent.com/2kalxx/ascend-app/main/";
    static final String WEB_FILE = "web.html";
    static final long CHECK_EVERY_MS = 3L * 60 * 60 * 1000; // at most every 3 hours (unless "Check now")
    static volatile boolean running = false;

    private final MainActivity act;
    private final boolean manual;

    Updater(MainActivity act, boolean manual) { this.act = act; this.manual = manual; }

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences(Reminders.PREFS, Context.MODE_PRIVATE); }

    /** Content version contained in an index.html (const ASCEND_WEB_VERSION = N). */
    static int webVersionOf(String html) {
        Matcher m = Pattern.compile("ASCEND_WEB_VERSION\\s*=\\s*(\\d+)").matcher(html);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    static String bundledHtml(Context c) {
        try { return new String(readAll(c.getAssets().open("index.html")), "UTF-8"); } catch (Exception e) { return ""; }
    }

    /** The downloaded content if it is newer than what's bundled in the APK, else null. */
    static File downloadedIfNewer(Context c) {
        File f = new File(c.getFilesDir(), WEB_FILE);
        if (!f.exists()) return null;
        int dl = prefs(c).getInt("webVer", 0);
        return dl > webVersionOf(bundledHtml(c)) ? f : null;
    }

    static int currentWebVersion(Context c) {
        return Math.max(prefs(c).getInt("webVer", 0), webVersionOf(bundledHtml(c)));
    }

    static void start(MainActivity act, boolean manual) {
        if (running) return;
        long last = prefs(act).getLong("lastCheck", 0);
        if (!manual && System.currentTimeMillis() - last < CHECK_EVERY_MS) return;
        running = true;
        new Thread(new Updater(act, manual)).start();
    }

    @Override
    public void run() {
        try {
            JSONObject v = new JSONObject(new String(get(BASE + "version.json?t=" + System.currentTimeMillis()), "UTF-8"));
            prefs(act).edit().putLong("lastCheck", System.currentTimeMillis()).apply();
            int myApk = act.getPackageManager().getPackageInfo(act.getPackageName(), 0).versionCode;
            int remoteWeb = v.optInt("web", 0), minApk = v.optInt("minApk", 0), remoteApk = v.optInt("apk", 0);
            boolean changed = false;

            if (remoteWeb > currentWebVersion(act) && myApk >= minApk) {
                byte[] html = get(BASE + "web/index.html?t=" + System.currentTimeMillis());
                String s = new String(html, "UTF-8");
                // only accept a complete, valid page with the expected version
                if (s.startsWith("<!DOCTYPE html") && s.contains("</html>") && webVersionOf(s) == remoteWeb) {
                    File tmp = new File(act.getFilesDir(), WEB_FILE + ".tmp");
                    FileOutputStream o = new FileOutputStream(tmp); o.write(html); o.close();
                    if (tmp.renameTo(new File(act.getFilesDir(), WEB_FILE))) {
                        prefs(act).edit().putInt("webVer", remoteWeb).apply();
                        changed = true;
                    }
                }
            }
            if (remoteApk > myApk) {
                prefs(act).edit().putString("apkUrl", BASE + v.optString("apkPath", "apk/Ascend.apk")).apply();
                act.onUpdateResult(changed, remoteApk, manual);
            } else {
                act.onUpdateResult(changed, 0, manual);
            }
        } catch (Exception e) {
            if (manual) act.toastInPage("Couldn't check for updates – are you online?");
        } finally {
            running = false;
        }
    }

    static byte[] get(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000); c.setReadTimeout(60000);
        c.setRequestProperty("Cache-Control", "no-cache");
        if (c.getResponseCode() != 200) throw new Exception("HTTP " + c.getResponseCode());
        try (InputStream in = c.getInputStream()) { return readAll(in); }
    }

    static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] buf = new byte[16384]; int n;
        while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
        in.close();
        return b.toByteArray();
    }
}
