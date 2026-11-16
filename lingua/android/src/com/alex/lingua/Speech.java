package com.alex.lingua;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import java.util.ArrayList;
import org.json.JSONObject;

/** Microphone → text with Android's built-in speech recognition. Results go to window.linguaSTT(json). */
public class Speech implements RecognitionListener {
    private final MainActivity act;
    private SpeechRecognizer rec;
    String pendingLang = null;

    Speech(MainActivity act) { this.act = act; }

    boolean available() { return SpeechRecognizer.isRecognitionAvailable(act); }

    void start(String lang) {
        if (rec == null) { rec = SpeechRecognizer.createSpeechRecognizer(act); rec.setRecognitionListener(this); }
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang);
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        rec.startListening(i);
        send("start", "");
    }

    void stop() { if (rec != null) rec.stopListening(); }

    void destroy() { if (rec != null) { rec.destroy(); rec = null; } }

    private void send(String type, String text) {
        try {
            JSONObject o = new JSONObject().put("type", type).put("text", text);
            act.runJs("window.linguaSTT&&linguaSTT(" + JSONObject.quote(o.toString()) + ")");
        } catch (Exception ignored) { }
    }

    private static String first(Bundle b) {
        ArrayList<String> r = b == null ? null : b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        return r == null || r.isEmpty() ? "" : r.get(0);
    }

    @Override public void onReadyForSpeech(Bundle params) { send("ready", ""); }
    @Override public void onBeginningOfSpeech() { }
    @Override public void onRmsChanged(float rmsdB) { }
    @Override public void onBufferReceived(byte[] buffer) { }
    @Override public void onEndOfSpeech() { send("end", ""); }
    @Override public void onError(int error) { send("error", String.valueOf(error)); }
    @Override public void onResults(Bundle results) { send("final", first(results)); }
    @Override public void onPartialResults(Bundle partial) { String t = first(partial); if (!t.isEmpty()) send("partial", t); }
    @Override public void onEvent(int eventType, Bundle params) { }
}
