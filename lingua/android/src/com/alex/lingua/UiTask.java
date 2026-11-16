package com.alex.lingua;

/** Runs things on the UI thread without lambdas. */
public class UiTask implements Runnable {
    private final MainActivity act; private final String js;
    private final boolean webChanged, manual; private final int newApk;

    UiTask(MainActivity act, String js) { this(act, js, true); }
    UiTask(MainActivity act, String js, boolean isJs) { this.act = act; this.js = js; this.webChanged = false; this.newApk = 0; this.manual = false; }
    UiTask(MainActivity act, boolean webChanged, int newApk, boolean manual) {
        this.act = act; this.js = null; this.webChanged = webChanged; this.newApk = newApk; this.manual = manual;
    }

    @Override
    public void run() {
        if (js != null) act.runJsNow(js); else act.applyUpdateResult(webChanged, newApk, manual);
    }
}
