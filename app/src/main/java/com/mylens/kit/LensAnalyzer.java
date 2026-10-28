package com.mylens.kit;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.util.List;

/**
 * Offline analysis engine (Google ML Kit, bundled models). Internal — use
 * MyLens.analyze() or MyLens.showAnalysisDialog().
 *
 * The ENTIRE worker body runs inside catch(Throwable): engine errors
 * (UnsatisfiedLinkError, init NPEs…) are reported as notes, never thrown.
 */
final class LensAnalyzer {

    private LensAnalyzer() {}

    static void analyzeAndShow(final Activity act, final Uri uri) {
        final AlertDialog[] progress = new AlertDialog[1];
        act.runOnUiThread(() -> {
            try {
                progress[0] = new AlertDialog.Builder(act)
                        .setMessage("Analyzing on device…")
                        .setCancelable(false)
                        .show();
            } catch (Throwable ignored) {}
        });
        new Thread(() -> {
            final MyLens.Result r = new MyLens.Result();
            try {
                run(act, uri, r);
            } catch (Throwable t) {
                r.errors.add("Analysis: " + t);
            }
            act.runOnUiThread(() -> {
                try { if (progress[0] != null) progress[0].dismiss(); } catch (Throwable ignored) {}
                try { showResults(act, uri, r); } catch (Throwable ignored) {}
            });
        }).start();
    }

    static void run(android.content.Context ctx, Uri uri, MyLens.Result r) throws Exception {
        try {
            // Normally the MlKitInitProvider (manifest-merged) initializes ML Kit;
            // this explicit call covers apps where the provider did not run.
            com.google.mlkit.common.sdkinternal.MlKitContext.initializeIfNeeded(ctx);
        } catch (Throwable t) {
            r.errors.add("ML Kit init: " + t);
        }

        Bitmap bmp = Utils.decodeSampled(ctx, uri, 1280);
        if (bmp == null) {
            r.errors.add("Could not decode image");
            return;
        }
        InputImage image;
        try {
            image = InputImage.fromBitmap(bmp, 0);
        } catch (Throwable t) {
            // Init-order failure inside ML Kit — retry once after forcing init.
            try {
                com.google.mlkit.common.sdkinternal.MlKitContext.initializeIfNeeded(ctx);
                image = InputImage.fromBitmap(bmp, 0);
            } catch (Throwable t2) {
                r.errors.add("ML Kit vision: " + t2);
                return;
            }
        }

        // ---- Image labeling ----
        try {
            ImageLabelerOptions opts = new ImageLabelerOptions.Builder()
                    .setConfidenceThreshold(0.35f)
                    .build();
            ImageLabeler labeler = ImageLabeling.getClient(opts);
            List<ImageLabel> labels = Tasks.await(labeler.process(image));
            for (ImageLabel l : labels) {
                r.labels.add(l.getText() + " — " + Math.round(l.getConfidence() * 100) + "%");
            }
            if (!labels.isEmpty()) r.topLabel = labels.get(0).getText();
        } catch (Throwable t) {
            r.errors.add("Labeling: " + t);
        }

        // ---- Face detection ----
        try {
            FaceDetectorOptions fdo = new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                    .build();
            List<Face> faces = Tasks.await(FaceDetection.getClient(fdo).process(image));
            r.faces = faces.size();
        } catch (Throwable t) {
            r.errors.add("Face detection: " + t);
        }

        // ---- OCR (Latin, bundled) ----
        try {
            TextRecognizer recognizer = TextRecognition.getClient(
                    TextRecognizerOptions.DEFAULT_OPTIONS);
            Text text = Tasks.await(recognizer.process(image));
            r.ocrText = text.getText();
        } catch (Throwable t) {
            r.errors.add("Text recognition: " + t);
        }
    }

    private static void showResults(final Activity act, final Uri uri, final MyLens.Result r) {
        StringBuilder sb = new StringBuilder();
        if (!r.labels.isEmpty()) {
            sb.append("What's in the photo:\n");
            for (int i = 0; i < Math.min(r.labels.size(), 10); i++) sb.append("  • ").append(r.labels.get(i)).append("\n");
            sb.append("\n");
        }
        if (r.faces >= 0) {
            sb.append("Faces detected: ").append(r.faces).append("\n\n");
        }
        if (!TextUtils.isEmpty(r.ocrText)) {
            String ocr = r.ocrText.replace("\n\n", "\n").trim();
            if (ocr.length() > 600) ocr = ocr.substring(0, 600) + "…";
            sb.append("Detected text:\n  ").append(ocr.replace("\n", "\n  ")).append("\n\n");
        }
        if (r.labels.isEmpty() && r.faces < 0 && TextUtils.isEmpty(r.ocrText)) {
            sb.append("No on-device results for this photo.\n\n");
        }
        if (!r.errors.isEmpty()) {
            sb.append("Notes:\n");
            for (String e : r.errors) {
                sb.append("  ").append(e.length() > 160 ? e.substring(0, 160) + "…" : e).append("\n");
            }
        }

        AlertDialog.Builder b = new AlertDialog.Builder(act)
                .setTitle("On device analysis")
                .setMessage(sb.toString().trim())
                .setNegativeButton("Close", null);

        if (!TextUtils.isEmpty(r.topLabel)) {
            final String q = r.topLabel;
            b.setNeutralButton("Web search: " + q, (d, w) -> MyLens.webSearch(act, q));
        }
        b.setPositiveButton("Visual matches", (d, w) -> MyLens.openVisualMatches(act, uri));
        b.show();
    }
}
