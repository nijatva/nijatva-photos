package com.mylens.kit;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * MyLensKit — drop-in visual search for any Android app.
 *
 * Features:
 *  - Fully offline on-device photo analysis (Google ML Kit, bundled models):
 *    image labels with confidence %, Latin OCR, face count.
 *  - One-shot "Lens-like" results dialog with Web search + Visual matches.
 *  - Camera capture flow that returns the taken photo.
 *  - Web visual search (photo attached into the search page automatically).
 *  - Optional: send photos to your own OpenLens server.
 *
 * Setup: see README-INTEGRATION.md. Minimum requirements on the host app:
 *  - ML Kit dependencies (text-recognition, image-labeling, face-detection)
 *  - androidx.core (FileProvider) + a FileProvider authority, or set
 *    {@link #fileProviderAuthority} to your existing one.
 *  - INTERNET permission (for web search / visual matches / server mode).
 *
 * Every method is safe: analysis errors are reported in the result/errors
 * list (or shown inside the dialog) and never crash the host app.
 */
public final class MyLens {

    /** Request code used by {@link #startCamera}. Forward it to {@link #getCameraResult}. */
    public static final int REQ_CAMERA = 24040;

    /**
     * FileProvider authority used for camera output and the visual-search
     * file chooser. If your app already declares a FileProvider, set this to
     * YOUR authority before the first MyLens call (must also cover cache-path).
     */
    public static String fileProviderAuthority = "com.mylens.kit.fileprovider";

    private MyLens() {}

    /** Result of an on-device analysis. */
    public static final class Result {
        /** Formatted label lines, e.g. "Dog — 99%". Highest confidence first. */
        public final List<String> labels = new ArrayList<>();
        /** Single best label ("Dog"), or null. */
        public String topLabel;
        /** Number of faces found, or -1 if detection failed. */
        public int faces = -1;
        /** All recognized (Latin) text, or null. */
        public String ocrText;
        /** Non-fatal engine notes (useful when a feature is unavailable). */
        public final List<String> errors = new ArrayList<>();
    }

    /** Callback for {@link #analyze}. Always invoked on the main thread. */
    public interface Callback {
        void onResult(Result result);
    }

    // ------------------------------------------------------------------ api

    /**
     * Fully offline analysis of a photo: labels + OCR + face count.
     * Never throws; per-feature problems land in {@code result.errors}.
     *
     * @param photo any image Uri the ContentResolver can open (content://, file://…)
     */
    public static void analyze(final Context ctx, final Uri photo, final Callback cb) {
        final android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
        new Thread(() -> {
            final Result r = new Result();
            try {
                LensAnalyzer.run(ctx, photo, r);
            } catch (Throwable t) {
                r.errors.add("Analysis: " + t);
            }
            main.post(() -> {
                try { cb.onResult(r); } catch (Throwable ignored) {}
            });
        }).start();
    }

    /**
     * One-shot: analyzes the photo offline and shows a "Lens-like" dialog with
     * the results plus [Web search: top label] and [Visual matches] buttons.
     */
    public static void showAnalysisDialog(final Activity act, final Uri photo) {
        LensAnalyzer.analyzeAndShow(act, photo);
    }

    /**
     * Opens the camera with a file target so the captured photo comes back to
     * the app. Then in your {@code onActivityResult} forward the result to
     * {@link #getCameraResult}.
     */
    public static void startCamera(Activity act) {
        try {
            File f = new File(act.getCacheDir(), "mylens_camera.jpg");
            f.delete();
            Uri out = androidx.core.content.FileProvider.getUriForFile(
                    act.getApplicationContext(), fileProviderAuthority, f);
            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            i.putExtra(MediaStore.EXTRA_OUTPUT, out);
            i.setClipData(ClipData.newRawUri(MediaStore.EXTRA_OUTPUT, out));
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            act.startActivityForResult(i, REQ_CAMERA);
        } catch (Throwable t) {
            // Camera apps that can't handle a content-URI target: plain capture.
            try {
                act.startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE), REQ_CAMERA);
            } catch (Throwable ignored) {}
        }
    }

    /**
     * Consumes the result of {@link #startCamera} from your
     * {@code onActivityResult}. Returns the captured photo's Uri, or null
     * (not a camera result / cancelled).
     */
    public static Uri getCameraResult(Context ctx, int requestCode, int resultCode, Intent data) {
        try {
            if (requestCode != REQ_CAMERA || resultCode != Activity.RESULT_OK) return null;
            File f = new File(ctx.getCacheDir(), "mylens_camera.jpg");
            if (f.exists() && f.length() > 0) {
                return androidx.core.content.FileProvider.getUriForFile(ctx, fileProviderAuthority, f);
            }
            // Some cameras ignore EXTRA_OUTPUT and return only a thumbnail extra.
            if (data != null && data.getExtras() != null) {
                Object o = data.getExtras().get("data");
                if (o instanceof Bitmap) {
                    FileOutputStream fos = new FileOutputStream(f);
                    ((Bitmap) o).compress(Bitmap.CompressFormat.JPEG, 92, fos);
                    fos.close();
                    return androidx.core.content.FileProvider.getUriForFile(ctx, fileProviderAuthority, f);
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    /**
     * Visual matches on the web: opens the visual-search camera page in a
     * WebView and attaches the photo into its file input automatically
     * (no button click needed). Clearly a web engine — Google Lens web
     * requires sign-in, so a page that accepts photos without sign-in is used.
     */
    public static void openVisualMatches(Activity act, Uri photo) {
        try {
            File f = Utils.copyToCache(act, photo, "mylens_visual.jpg");
            Intent i = new Intent(act, BingVisionActivity.class);
            i.putExtra("photo", f.getAbsolutePath());
            act.startActivity(i);
        } catch (Throwable ignored) {}
    }

    /**
     * Optional: send the photo to your own OpenLens server (classification,
     * OCR, translation, visual matches, web search). On any failure this
     * silently falls back to the offline analysis dialog.
     *
     * @param serverUrl e.g. "http://192.168.1.10:8000"
     */
    public static void analyzeOnServer(final Activity act, final Uri photo, String serverUrl) {
        ServerClient.analyze(act, photo, serverUrl);
    }

    /** Simple web search for a label (Google). */
    public static void webSearch(Activity act, String query) {
        try {
            act.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
                    "https://www.google.com/search?q=" + Uri.encode(query))));
        } catch (Throwable ignored) {}
    }
}
