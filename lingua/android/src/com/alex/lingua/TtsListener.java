package com.alex.lingua;

import android.speech.tts.UtteranceProgressListener;

/** Tells the page when the voice starts/stops/reaches a word, so the tutor's mouth can move. */
public class TtsListener extends UtteranceProgressListener {
    private final MainActivity act;
    TtsListener(MainActivity act) { this.act = act; }
    private void js(String s) { act.runJs("window.linguaTTS&&linguaTTS('" + s + "')"); }
    @Override public void onStart(String id) { js("start"); }
    @Override public void onDone(String id) { js("done"); }
    @Override public void onError(String id) { js("done"); }
    @Override public void onStop(String id, boolean interrupted) { js("done"); }
    @Override public void onRangeStart(String id, int start, int end, int frame) { js("word"); }
}
