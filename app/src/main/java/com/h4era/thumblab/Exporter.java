package com.h4era.thumblab;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.IOException;
import java.io.OutputStream;

public class Exporter {
    public static Uri save(Context c, Bitmap b, boolean png, int quality) throws IOException {
        ContentResolver cr = c.getContentResolver();
        ContentValues v = new ContentValues();
        v.put(MediaStore.MediaColumns.DISPLAY_NAME, "H4ERA_" + System.currentTimeMillis() + (png ? ".png" : ".jpg"));
        v.put(MediaStore.MediaColumns.MIME_TYPE, png ? "image/png" : "image/jpeg");
        v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/H4ERA THUMB LAB");
        v.put(MediaStore.MediaColumns.IS_PENDING, 1);
        Uri u = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
        if (u == null) throw new IOException("Could not create file in gallery");
        try (OutputStream os = cr.openOutputStream(u)) {
            if (os == null) throw new IOException("Could not open file");
            boolean ok = b.compress(png ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG, png ? 100 : quality, os);
            if (!ok) throw new IOException("Compression failed");
        } catch (IOException e) {
            cr.delete(u, null, null);
            throw e;
        }
        ContentValues done = new ContentValues();
        done.put(MediaStore.MediaColumns.IS_PENDING, 0);
        cr.update(u, done, null, null);
        return u;
    }
}
