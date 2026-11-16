package com.alex.lingua;

/** Starts (lang != null) or stops listening on the UI thread – SpeechRecognizer must run there. */
public class SpeechTask implements Runnable {
    private final MainActivity act; private final String lang;
    SpeechTask(MainActivity act, String lang) { this.act = act; this.lang = lang; }
    @Override public void run() { if (lang != null) act.listen(lang); else act.speech.stop(); }
}
