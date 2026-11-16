package com.alex.orbitcrush;

import android.content.Intent;
import android.net.Uri;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/** Keeps the app's own pages inside, opens web links (http/https) in the browser or Claude app. */
public class LinkClient extends WebViewClient {
    private final MainActivity act;
    LinkClient(MainActivity act) { this.act = act; }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
        Uri u = req.getUrl();
        String scheme = u.getScheme();
        if ("http".equals(scheme) || "https".equals(scheme)) {
            try { act.startActivity(new Intent(Intent.ACTION_VIEW, u).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); } catch (Exception ignored) { }
            return true;
        }
        return false;
    }
}
