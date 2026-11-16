package com.alex.ascend;

/** Runs a Health Connect call on the UI thread. */
public class HealthTask implements Runnable {
    private final MainActivity act; private final boolean connect;
    HealthTask(MainActivity act, boolean connect) { this.act = act; this.connect = connect; }
    @Override public void run() { if (connect) act.health.connect(); else act.health.refresh(); }
}
