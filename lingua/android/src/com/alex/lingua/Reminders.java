package com.alex.lingua;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/** Schedules and shows the daily reminders. Also re-schedules them after a reboot or app update. */
public class Reminders extends BroadcastReceiver {
    static final String PREFS = "lingua";
    static final String ACTION = "com.alex.lingua.REMIND";
    static final String CHANNEL = "daily";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (ACTION.equals(intent.getAction())) {
            String type = intent.getStringExtra("type");
            if ("m".equals(type)) morning(ctx); else if ("e".equals(type)) evening(ctx);
        }
        scheduleAll(ctx); // queue the next day (and restore after boot)
    }

    private static void morning(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int streak = p.getInt("streak", 0);
        String name = p.getString("name", "");
        String hi = name.isEmpty() ? "Good morning" : "Good morning, " + name;
        String body = streak > 0
                ? "Your quests are ready. Keep the " + streak + "-day streak going 🔥"
                : "Your quests are ready. Today is a good day to start a streak 🔥";
        notify(ctx, "☀️ " + hi, body, 1);
    }

    private static void evening(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().getTime());
        int xp = p.getInt("done", 0), goal = p.getInt("total", 30), streak = p.getInt("streak", 0);
        boolean doneToday = today.equals(p.getString("date", "")) && xp >= goal;
        if (doneToday) return; // daily goal reached – no reminder
        String title = streak > 0 ? "\uD83D\uDD25 Keep your " + streak + "-day streak!" : "\uD83C\uDF0D Time for a quick lesson";
        String body = today.equals(p.getString("date", "")) && xp > 0
                ? (goal - xp) + " XP left to reach today's goal. One lesson is enough."
                : "A 5-minute lesson keeps your Spanish growing. \u00A1Vamos!";
        notify(ctx, title, body, 2);
    }

    static void notify(Context ctx, String title, String text, int id) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Practice reminder", NotificationManager.IMPORTANCE_DEFAULT));
        }
        Intent open = new Intent(ctx, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(ctx, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();
        try { nm.notify(id, n); } catch (SecurityException ignored) { }
    }

    static void scheduleAll(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = p.getString("rem", "");
        boolean mOn = false, eOn = false; String mT = "08:00", eT = "20:00";
        try {
            JSONObject o = new JSONObject(json);
            JSONObject m = o.getJSONObject("m"), e = o.getJSONObject("e");
            mOn = m.optBoolean("on", false); mT = m.optString("t", mT);
            eOn = e.optBoolean("on", false); eT = e.optString("t", eT);
        } catch (Exception ignored) { }
        schedule(ctx, "m", mOn, mT, 101);
        schedule(ctx, "e", eOn, eT, 102);
    }

    private static void schedule(Context ctx, String type, boolean on, String time, int code) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(ctx, Reminders.class).setAction(ACTION).putExtra("type", type);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, code, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.cancel(pi);
        if (!on) return;
        int h = 8, min = 0;
        try { String[] parts = time.split(":"); h = Integer.parseInt(parts[0]); min = Integer.parseInt(parts[1]); }
        catch (Exception ignored) { }
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, h); c.set(Calendar.MINUTE, min);
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        if (c.getTimeInMillis() <= System.currentTimeMillis() + 1000) c.add(Calendar.DAY_OF_YEAR, 1);
        boolean exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
        if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pi);
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pi);
    }
}
