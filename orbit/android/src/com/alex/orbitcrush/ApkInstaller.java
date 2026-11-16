package com.alex.orbitcrush;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Downloads the new APK from GitHub and hands it to Android's installer (the user confirms once). */
public class ApkInstaller implements Runnable {
    private final MainActivity act;

    ApkInstaller(MainActivity act) { this.act = act; }

    static void start(MainActivity act) {
        if (!act.getPackageManager().canRequestPackageInstalls()) {
            act.toastInPage("Allow Orbit Crush to install updates, then tap Install again");
            act.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + act.getPackageName())));
            return;
        }
        new Thread(new ApkInstaller(act)).start();
    }

    @Override
    public void run() {
        String url = Updater.prefs(act).getString("apkUrl", Updater.BASE + "apk/OrbitCrush.apk");
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(url + "?t=" + System.currentTimeMillis()).openConnection();
            c.setConnectTimeout(15000); c.setReadTimeout(120000);
            if (c.getResponseCode() != 200) throw new Exception("HTTP " + c.getResponseCode());
            PackageInstaller pi = act.getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            params.setAppPackageName(act.getPackageName());
            int id = pi.createSession(params);
            PackageInstaller.Session s = pi.openSession(id);
            try (InputStream in = c.getInputStream(); OutputStream out = s.openWrite("orbitcrush.apk", 0, -1)) {
                byte[] buf = new byte[65536]; int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                s.fsync(out);
            }
            Intent cb = new Intent(act, InstallReceiver.class);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0);
            s.commit(PendingIntent.getBroadcast(act, id, cb, flags).getIntentSender());
            s.close();
        } catch (Exception e) {
            // fall back to downloading in the browser
            act.toastInPage("Opening download in your browser…");
            act.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }
}
