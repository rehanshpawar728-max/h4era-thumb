package com.h4era.thumblab;

import android.content.Context;
import android.graphics.Bitmap;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ProjectStore {
    public static class Info {
        public String id, name;
        public long modified;
    }

    static File dir(Context c) {
        File d = new File(c.getFilesDir(), "projects");
        d.mkdirs();
        return d;
    }

    public static File preview(Context c, String id) {
        return new File(dir(c), id + ".png");
    }

    static File json(Context c, String id) {
        return new File(dir(c), id + ".json");
    }

    public static String newId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static JSONObject read(Context c, String id) {
        try {
            return new JSONObject(new String(Files.readAllBytes(json(c, id).toPath()), StandardCharsets.UTF_8));
        } catch (Exception e) {
            return null;
        }
    }

    private static void write(Context c, String id, JSONObject o) {
        try {
            Files.write(json(c, id).toPath(), o.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    public static void save(Context c, String id, String name, EditorView v) {
        try {
            JSONObject o = new JSONObject();
            o.put("name", name);
            o.put("modified", System.currentTimeMillis());
            o.put("scene", new JSONObject(v.snapshot()));
            write(c, id, o);
            Bitmap b = v.render(480, 270);
            try (FileOutputStream fo = new FileOutputStream(preview(c, id))) {
                b.compress(Bitmap.CompressFormat.PNG, 100, fo);
            }
            b.recycle();
        } catch (Exception ignored) {
        }
    }

    public static String loadScene(Context c, String id) {
        JSONObject o = read(c, id);
        if (o == null) return null;
        JSONObject s = o.optJSONObject("scene");
        return s == null ? null : s.toString();
    }

    public static List<Info> list(Context c) {
        List<Info> out = new ArrayList<>();
        File[] fs = dir(c).listFiles((d, n) -> n.endsWith(".json"));
        if (fs == null) return out;
        for (File f : fs) {
            String id = f.getName().replace(".json", "");
            JSONObject o = read(c, id);
            if (o == null) continue;
            Info i = new Info();
            i.id = id;
            i.name = o.optString("name", "Untitled");
            i.modified = o.optLong("modified", 0);
            out.add(i);
        }
        Collections.sort(out, (a, b) -> Long.compare(b.modified, a.modified));
        return out;
    }

    public static void delete(Context c, String id) {
        json(c, id).delete();
        preview(c, id).delete();
    }

    public static void rename(Context c, String id, String name) {
        JSONObject o = read(c, id);
        if (o == null) return;
        try {
            o.put("name", name);
            o.put("modified", System.currentTimeMillis());
        } catch (Exception ignored) {
        }
        write(c, id, o);
    }

    public static void duplicate(Context c, String id) {
        JSONObject o = read(c, id);
        if (o == null) return;
        String nid = newId();
        try {
            o.put("name", o.optString("name", "Untitled") + " copy");
            o.put("modified", System.currentTimeMillis());
            write(c, nid, o);
            if (preview(c, id).exists()) {
                Files.copy(preview(c, id).toPath(), preview(c, nid).toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception ignored) {
        }
    }
}
