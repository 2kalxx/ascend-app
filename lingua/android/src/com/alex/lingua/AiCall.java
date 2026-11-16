package com.alex.lingua;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

/** POSTs a request to Google's Gemini API (user's own free key) and hands the answer to window.linguaAI(id, status, body). */
public class AiCall implements Runnable {
    static final String ALLOWED = "https://generativelanguage.googleapis.com/";
    private final MainActivity act; private final String id, url, body;
    AiCall(MainActivity act, String id, String url, String body) { this.act = act; this.id = id; this.url = url; this.body = body; }

    @Override
    public void run() {
        int status = 0; String text;
        try {
            if (!url.startsWith(ALLOWED)) throw new Exception("blocked url");
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setRequestMethod("POST"); c.setDoOutput(true);
            c.setConnectTimeout(15000); c.setReadTimeout(60000);
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            try (OutputStream o = c.getOutputStream()) { o.write(body.getBytes("UTF-8")); }
            status = c.getResponseCode();
            InputStream in = status >= 400 ? c.getErrorStream() : c.getInputStream();
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            if (in != null) { byte[] buf = new byte[8192]; int n; while ((n = in.read(buf)) > 0) b.write(buf, 0, n); in.close(); }
            text = b.toString("UTF-8");
        } catch (Exception e) { text = String.valueOf(e.getMessage()); }
        act.runJs("window.linguaAI&&linguaAI(" + JSONObject.quote(id) + "," + status + "," + JSONObject.quote(text) + ")");
    }
}
