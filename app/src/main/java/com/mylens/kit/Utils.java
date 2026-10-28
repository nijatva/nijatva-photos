package com.mylens.kit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/** Small shared helpers. Internal. */
final class Utils {

    private Utils() {}

    /** Decodes a bitmap from a Uri, sampled so the longest edge is <= maxEdge. */
    static Bitmap decodeSampled(Context ctx, Uri uri, int maxEdge) {
        InputStream is = null;
        try {
            is = ctx.getContentResolver().openInputStream(uri);
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, o);
            close(is);
            int w = o.outWidth, h = o.outHeight;
            if (w <= 0 || h <= 0) return null;
            int sample = 1;
            while (Math.max(w, h) / sample > maxEdge * 2) sample *= 2;
            o = new BitmapFactory.Options();
            o.inSampleSize = sample;
            is = ctx.getContentResolver().openInputStream(uri);
            Bitmap bmp = BitmapFactory.decodeStream(is, null, o);
            close(is);
            if (bmp != null && Math.max(bmp.getWidth(), bmp.getHeight()) > maxEdge) {
                float scale = (float) maxEdge / Math.max(bmp.getWidth(), bmp.getHeight());
                int nw = Math.max(1, Math.round(bmp.getWidth() * scale));
                int nh = Math.max(1, Math.round(bmp.getHeight() * scale));
                try {
                    Bitmap scaled = Bitmap.createScaledBitmap(bmp, nw, nh, true);
                    if (scaled != null) bmp = scaled;
                } catch (Throwable ignored) {}
            }
            return bmp;
        } catch (Throwable t) {
            close(is);
            return null;
        }
    }

    /** Copies the content of a Uri into a cache file. */
    static File copyToCache(Context ctx, Uri uri, String name) throws Exception {
        File out = new File(ctx.getCacheDir(), name);
        InputStream is = null;
        OutputStream os = null;
        try {
            is = ctx.getContentResolver().openInputStream(uri);
            if (is == null) throw new Exception("null stream");
            os = new FileOutputStream(out);
            byte[] buf = new byte[65536];
            int n;
            while ((n = is.read(buf)) > 0) os.write(buf, 0, n);
            return out;
        } finally {
            close(is);
            close(os);
        }
    }

    static void close(java.io.Closeable c) {
        if (c != null) {
            try { c.close(); } catch (Throwable ignored) {}
        }
    }
}
