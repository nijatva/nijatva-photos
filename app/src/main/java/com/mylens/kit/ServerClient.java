package com.mylens.kit;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebView;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Optional server mode: sends the photo to your own OpenLens server and
 * renders the returned HTML. On any failure it silently falls back to the
 * offline analysis dialog. Internal — use MyLens.analyzeOnServer().
 */
final class ServerClient {

    private ServerClient() {}

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    static void analyze(final Activity act, final Uri uri, final String serverUrl) {
        new Thread(() -> {
            String base = serverUrl == null ? "" : serverUrl.trim().replaceAll("/+$", "");
            if (base.isEmpty()) {
                fallback(act, uri);
                return;
            }
            File photo = null;
            HttpURLConnection conn = null;
            try {
                photo = Utils.copyToCache(act, uri, "mylens_server.jpg");
                String boundary = "mylens" + System.currentTimeMillis();
                conn = (HttpURLConnection) new URL(base + "/analyze").openConnection();
                conn.setDoOutput(true);
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(25000);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                OutputStream os = conn.getOutputStream();
                writeMultipart(os, boundary, photo);
                os.close();

                int code = conn.getResponseCode();
                if (code == 404) {
                    try { conn.disconnect(); } catch (Throwable ignored) {}
                    conn = (HttpURLConnection) new URL(base + "/").openConnection();
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(25000);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                    os = conn.getOutputStream();
                    writeMultipart(os, boundary, photo);
                    os.close();
                    code = conn.getResponseCode();
                }
                if (code / 100 == 2) {
                    StringBuilder sb = new StringBuilder();
                    BufferedReader r = new BufferedReader(new InputStreamReader(
                            new BufferedInputStream(conn.getInputStream()), "UTF-8"));
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line).append("\n");
                    r.close();
                    final String body = sb.toString().trim();
                    MAIN.post(() -> showServerResult(act, base, body));
                    return;
                }
                throw new Exception("HTTP " + code);
            } catch (Throwable ignored) {
                fallback(act, uri);
            } finally {
                if (conn != null) { try { conn.disconnect(); } catch (Throwable ignored) {} }
                if (photo != null) photo.delete();
            }
        }).start();
    }

    private static void writeMultipart(OutputStream os, String boundary, File photo) throws Exception {
        String head = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"photo.jpg\"\r\n"
                + "Content-Type: image/jpeg\r\n\r\n";
        os.write(head.getBytes("UTF-8"));
        FileInputStream in = new FileInputStream(photo);
        byte[] buf = new byte[65536];
        int n;
        while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
        in.close();
        os.write(("\r\n--" + boundary + "--\r\n").getBytes("UTF-8"));
    }

    private static void showServerResult(Activity act, String base, String body) {
        try {
            if (body.startsWith("<")) {
                WebView wv = new WebView(act);
                wv.getSettings().setJavaScriptEnabled(true);
                wv.loadDataWithBaseURL(base + "/", body, "text/html", "utf-8", null);
                new AlertDialog.Builder(act)
                        .setTitle("MyLens server results")
                        .setView(wv)
                        .setPositiveButton("Close", null)
                        .show();
            } else {
                new AlertDialog.Builder(act)
                        .setTitle("MyLens server results")
                        .setMessage(body.length() > 4000 ? body.substring(0, 4000) : body)
                        .setPositiveButton("Close", null)
                        .show();
            }
        } catch (Throwable ignored) {}
    }

    private static void fallback(final Activity act, final Uri uri) {
        MAIN.post(() -> LensAnalyzer.analyzeAndShow(act, uri));
    }
}
