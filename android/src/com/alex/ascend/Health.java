package com.alex.ascend;

import android.content.pm.PackageManager;
import android.health.connect.AggregateRecordsRequest;
import android.health.connect.AggregateRecordsResponse;
import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsResponse;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.ExerciseSessionRecord;
import android.health.connect.datatypes.HydrationRecord;
import android.health.connect.datatypes.SleepSessionRecord;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.datatypes.units.Volume;
import android.os.Build;
import android.os.OutcomeReceiver;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.json.JSONObject;

/**
 * Reads today's steps, exercise minutes, water and last night's sleep from Health Connect
 * (Samsung Health syncs into it) and hands them to the page as window.ascendHealth(json).
 * Uses the Health Connect built into Android 14+ – no extra libraries.
 */
public class Health {
    static final String[] PERMS = {
        "android.permission.health.READ_STEPS",
        "android.permission.health.READ_EXERCISE",
        "android.permission.health.READ_SLEEP",
        "android.permission.health.READ_HYDRATION"
    };
    static final int REQ = 7;

    private final MainActivity act;
    private JSONObject out;
    private int pending;

    Health(MainActivity act) { this.act = act; }

    static boolean supported() { return Build.VERSION.SDK_INT >= 34; }

    boolean anyGranted() {
        for (String p : PERMS) if (act.checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED) return true;
        return false;
    }

    /** "Connect" button: ask for permission (Health Connect's own screen), then read. */
    void connect() {
        if (!supported()) { send("{\"status\":\"unsupported\"}"); return; }
        boolean all = true;
        for (String p : PERMS) if (act.checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) all = false;
        if (all) refresh(); else act.requestPermissions(PERMS, REQ);
    }

    void onPermissionResult() {
        if (anyGranted()) refresh(); else send("{\"status\":\"denied\"}");
    }

    /** Read everything for today; results arrive asynchronously and are sent once all are in. */
    void refresh() {
        if (!supported()) { send("{\"status\":\"unsupported\"}"); return; }
        if (!anyGranted()) { send("{\"status\":\"denied\"}"); return; }
        try {
            HealthConnectManager hc = act.getSystemService(HealthConnectManager.class);
            if (hc == null) { send("{\"status\":\"unsupported\"}"); return; }
            ZoneId z = ZoneId.systemDefault();
            LocalDate today = LocalDate.now(z);
            Instant start = today.atStartOfDay(z).toInstant(), now = Instant.now();
            TimeInstantRangeFilter day = new TimeInstantRangeFilter.Builder().setStartTime(start).setEndTime(now).build();
            // last night: yesterday 15:00 → now
            Instant nightStart = ZonedDateTime.of(today.minusDays(1), LocalTime.of(15, 0), z).toInstant();
            TimeInstantRangeFilter night = new TimeInstantRangeFilter.Builder().setStartTime(nightStart).setEndTime(now).build();

            out = new JSONObject();
            out.put("status", "ok");
            out.put("date", today.toString());
            pending = 4;

            if (granted(PERMS[0])) {
                hc.aggregate(new AggregateRecordsRequest.Builder<Long>(day).addAggregationType(StepsRecord.STEPS_COUNT_TOTAL).build(),
                    act.getMainExecutor(), new OutcomeReceiver<AggregateRecordsResponse<Long>, HealthConnectException>() {
                        @Override public void onResult(AggregateRecordsResponse<Long> r) {
                            Long v = r.get(StepsRecord.STEPS_COUNT_TOTAL); put("steps", v == null ? 0 : v); done();
                        }
                        @Override public void onError(HealthConnectException e) { done(); }
                    });
            } else done();

            if (granted(PERMS[1])) {
                hc.aggregate(new AggregateRecordsRequest.Builder<Long>(day).addAggregationType(ExerciseSessionRecord.EXERCISE_DURATION_TOTAL).build(),
                    act.getMainExecutor(), new OutcomeReceiver<AggregateRecordsResponse<Long>, HealthConnectException>() {
                        @Override public void onResult(AggregateRecordsResponse<Long> r) {
                            Long ms = r.get(ExerciseSessionRecord.EXERCISE_DURATION_TOTAL); put("exerciseMin", ms == null ? 0 : ms / 60000); done();
                        }
                        @Override public void onError(HealthConnectException e) { done(); }
                    });
            } else done();

            if (granted(PERMS[3])) {
                hc.aggregate(new AggregateRecordsRequest.Builder<Volume>(day).addAggregationType(HydrationRecord.VOLUME_TOTAL).build(),
                    act.getMainExecutor(), new OutcomeReceiver<AggregateRecordsResponse<Volume>, HealthConnectException>() {
                        @Override public void onResult(AggregateRecordsResponse<Volume> r) {
                            Volume v = r.get(HydrationRecord.VOLUME_TOTAL); put("waterL", v == null ? 0 : v.getInLiters()); done();
                        }
                        @Override public void onError(HealthConnectException e) { done(); }
                    });
            } else done();

            if (granted(PERMS[2])) {
                hc.readRecords(new ReadRecordsRequestUsingFilters.Builder<SleepSessionRecord>(SleepSessionRecord.class).setTimeRangeFilter(night).build(),
                    act.getMainExecutor(), new OutcomeReceiver<ReadRecordsResponse<SleepSessionRecord>, HealthConnectException>() {
                        @Override public void onResult(ReadRecordsResponse<SleepSessionRecord> r) {
                            List<SleepSessionRecord> list = r.getRecords();
                            SleepSessionRecord longest = null;
                            for (SleepSessionRecord s : list) {
                                if (longest == null || dur(s) > dur(longest)) longest = s;
                            }
                            if (longest != null) {
                                LocalTime t = longest.getStartTime().atZone(z).toLocalTime();
                                put("sleepStart", String.format(java.util.Locale.US, "%02d:%02d", t.getHour(), t.getMinute()));
                                put("sleepHours", Math.round(dur(longest) / 360000.0) / 10.0);
                            }
                            done();
                        }
                        @Override public void onError(HealthConnectException e) { done(); }
                    });
            } else done();
        } catch (Throwable t) {
            send("{\"status\":\"error\"}");
        }
    }

    private static long dur(SleepSessionRecord s) { return s.getEndTime().toEpochMilli() - s.getStartTime().toEpochMilli(); }
    private boolean granted(String p) { return act.checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED; }
    private void put(String k, Object v) { try { out.put(k, v); } catch (Exception ignored) { } }
    private void done() { if (--pending == 0) send(out.toString()); }

    private void send(String json) {
        act.runJs("window.ascendHealth&&ascendHealth(" + JSONObject.quote(json) + ")");
    }
}
