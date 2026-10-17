package com.h4era.thumblab;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.LruCache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

public class ImageStore {
    private static final LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(64 * 1024) {
        @Override
        protected int sizeOf(String key, Bitmap b) {
            return b.getByteCount() / 1024;
        }
    };

    public static File dir(Context c) {
        File d = new File(c.getFilesDir(), "images");
        d.mkdirs();
        return d;
    }

    public static Bitmap get(Context c, String file) {
        if (file == null) return null;
        Bitmap b = cache.get(file);
        if (b != null) return b;
        File f = new File(dir(c), file);
        if (!f.exists()) return null;
        b = BitmapFactory.decodeFile(f.getPath());
        if (b != null) cache.put(file, b);
        return b;
    }

    public static String importUri(Context c, Uri u) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            try (InputStream in = c.getContentResolver().openInputStream(u)) {
                BitmapFactory.decodeStream(in, null, o);
            }
            if (o.outWidth <= 0 || o.outHeight <= 0) return null;
            int ss = 1;
            while (Math.max(o.outWidth, o.outHeight) / ss > 2048) ss *= 2;
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = ss;
            Bitmap b;
            try (InputStream in = c.getContentResolver().openInputStream(u)) {
                b = BitmapFactory.decodeStream(in, null, o2);
            }
            if (b == null) return null;
            int rot = 0;
            try (InputStream in = c.getContentResolver().openInputStream(u)) {
                int ori = new ExifInterface(in).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
                if (ori == 6) rot = 90;
                else if (ori == 3) rot = 180;
                else if (ori == 8) rot = 270;
            } catch (Exception ignored) {
            }
            if (rot != 0) {
                Matrix m = new Matrix();
                m.postRotate(rot);
                b = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
            }
            boolean png = b.hasAlpha();
            String name = UUID.randomUUID().toString() + (png ? ".png" : ".jpg");
            try (FileOutputStream fo = new FileOutputStream(new File(dir(c), name))) {
                b.compress(png ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG, png ? 100 : 95, fo);
            }
            cache.put(name, b);
            return name;
        } catch (Throwable t) {
            return null;
        }
    }
}
