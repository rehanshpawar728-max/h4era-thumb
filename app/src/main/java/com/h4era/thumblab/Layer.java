package com.h4era.thumblab;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;

import org.json.JSONObject;

public class Layer {
    public static final int TEXT = 0, IMAGE = 1, SHAPE = 2;

    public int type;
    public String name = "Layer";
    public float x = 640, y = 360, w = 300, h = 200, rot = 0, opacity = 1f;
    public boolean visible = true, locked = false, shadow = true, flip = false, bold = true, italic = false;
    public String text = "TEXT", font = "sans-serif-black", shape = "rect", file;
    public int color = 0xFFFFFFFF, strokeColor = 0xFF000000, outlineColor = 0xFF22D3EE;
    public float size = 120, strokeW = 8, spacing = 0f, lineSp = 1.1f, bright = 0f, sat = 1f, outlineW = 0f;
    public Bitmap bmp, alphaBmp;

    public static Layer newText(String s, int col, int stroke, float sw, float size) {
        Layer l = new Layer();
        l.type = TEXT;
        l.name = "Text";
        l.text = s;
        l.color = col;
        l.strokeColor = stroke;
        l.strokeW = sw;
        l.size = size;
        return l;
    }

    public static Layer newShape(String shape, int col, float w, float h) {
        Layer l = new Layer();
        l.type = SHAPE;
        l.shape = shape;
        l.name = shape.substring(0, 1).toUpperCase() + shape.substring(1);
        l.color = col;
        l.w = w;
        l.h = h;
        l.strokeW = 0;
        return l;
    }

    public static Layer newImage(String file, Bitmap b) {
        Layer l = new Layer();
        l.type = IMAGE;
        l.name = "Photo";
        l.file = file;
        l.bmp = b;
        float s = Math.min(1f, Math.min(900f / b.getWidth(), 600f / b.getHeight()));
        l.w = b.getWidth() * s;
        l.h = b.getHeight() * s;
        l.shadow = false;
        l.strokeW = 0;
        return l;
    }

    public Layer copy() {
        Layer c = fromJson(toJson());
        c.bmp = bmp;
        c.alphaBmp = alphaBmp;
        return c;
    }

    public Paint textPaint() {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        int st = bold && italic ? Typeface.BOLD_ITALIC : bold ? Typeface.BOLD : italic ? Typeface.ITALIC : Typeface.NORMAL;
        p.setTypeface(Typeface.create(font, st));
        p.setTextSize(size);
        p.setLetterSpacing(spacing);
        p.setTextAlign(Paint.Align.CENTER);
        return p;
    }

    public void measure() {
        if (type != TEXT) return;
        Paint p = textPaint();
        String[] ls = text.split("\n", -1);
        float mw = 0;
        for (String s : ls) mw = Math.max(mw, p.measureText(s));
        w = mw + strokeW * 2 + 20;
        h = ls.length * size * lineSp + strokeW * 2;
    }

    static int withOp(int col, float op) {
        return Color.argb((int) (Color.alpha(col) * op), Color.red(col), Color.green(col), Color.blue(col));
    }

    public boolean hit(float sx, float sy) {
        measure();
        float dx = sx - x, dy = sy - y;
        double r = Math.toRadians(-rot);
        float lx = (float) (dx * Math.cos(r) - dy * Math.sin(r));
        float ly = (float) (dx * Math.sin(r) + dy * Math.cos(r));
        return Math.abs(lx) <= w / 2 + 10 && Math.abs(ly) <= h / 2 + 10;
    }

    public void draw(Canvas c) {
        if (!visible) return;
        c.save();
        c.translate(x, y);
        c.rotate(rot);
        if (type == TEXT) drawText(c);
        else if (type == IMAGE) drawImage(c);
        else drawShape(c);
        c.restore();
    }

    private void drawText(Canvas c) {
        measure();
        Paint p = textPaint();
        String[] ls = text.split("\n", -1);
        float lh = size * lineSp;
        float top = -(ls.length * lh) / 2f;
        Paint.FontMetrics fm = p.getFontMetrics();
        float off = -(fm.ascent + fm.descent) / 2f;
        Paint sp = null;
        if (strokeW > 0) {
            sp = textPaint();
            sp.setStyle(Paint.Style.STROKE);
            sp.setStrokeWidth(strokeW * 2);
            sp.setStrokeJoin(Paint.Join.ROUND);
            sp.setColor(withOp(strokeColor, opacity));
            if (shadow) sp.setShadowLayer(strokeW + 4, 4, 6, 0xAA000000);
        }
        p.setColor(withOp(color, opacity));
        if (sp == null && shadow) p.setShadowLayer(8, 4, 6, 0xAA000000);
        for (int i = 0; i < ls.length; i++) {
            float by = top + i * lh + lh / 2f + off;
            if (sp != null) c.drawText(ls[i], 0, by, sp);
            c.drawText(ls[i], 0, by, p);
        }
    }

    private Path poly(float[] q) {
        Path pa = new Path();
        for (int i = 0; i < q.length; i += 2) {
            float px = q[i] * w / 2, py = q[i + 1] * h / 2;
            if (i == 0) pa.moveTo(px, py);
            else pa.lineTo(px, py);
        }
        pa.close();
        return pa;
    }

    private void drawShape(Canvas c) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(withOp(color, opacity));
        if (shadow) p.setShadowLayer(12, 0, 6, 0x88000000);
        RectF r = new RectF(-w / 2, -h / 2, w / 2, h / 2);
        Path path = null;
        switch (shape) {
            case "circle":
                c.drawOval(r, p);
                break;
            case "ring": {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(Math.max(strokeW, 14));
                c.drawOval(r, p);
                return;
            }
            case "star": {
                float[] q = new float[20];
                for (int i = 0; i < 10; i++) {
                    double a = Math.toRadians(-90 + i * 36);
                    float rad = i % 2 == 0 ? 1f : 0.45f;
                    q[i * 2] = (float) Math.cos(a) * rad;
                    q[i * 2 + 1] = (float) Math.sin(a) * rad;
                }
                path = poly(q);
                break;
            }
            case "arrow":
                path = poly(new float[]{-1, -0.35f, 0.2f, -0.35f, 0.2f, -0.8f, 1, 0, 0.2f, 0.8f, 0.2f, 0.35f, -1, 0.35f});
                break;
            case "bolt":
                path = poly(new float[]{0.2f, -1, -0.55f, 0.1f, -0.05f, 0.1f, -0.25f, 1, 0.55f, -0.2f, 0.05f, -0.2f, 0.45f, -1});
                break;
            default:
                c.drawRoundRect(r, 24, 24, p);
        }
        if (path != null) c.drawPath(path, p);
        if (strokeW > 0) {
            Paint s = new Paint(Paint.ANTI_ALIAS_FLAG);
            s.setStyle(Paint.Style.STROKE);
            s.setStrokeJoin(Paint.Join.ROUND);
            s.setStrokeWidth(strokeW);
            s.setColor(withOp(strokeColor, opacity));
            if (path != null) c.drawPath(path, s);
            else if (shape.equals("circle")) c.drawOval(r, s);
            else c.drawRoundRect(r, 24, 24, s);
        }
    }

    private void drawImage(Canvas c) {
        RectF dst = new RectF(-w / 2, -h / 2, w / 2, h / 2);
        if (bmp == null) {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(0x55FFFFFF);
            c.drawRect(dst, p);
            p.setColor(0xFFFFFFFF);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(36);
            c.drawText("Missing image", 0, 0, p);
            return;
        }
        c.save();
        if (flip) c.scale(-1, 1);
        if ((shadow || outlineW > 0) && alphaBmp == null) alphaBmp = bmp.extractAlpha();
        if (shadow) {
            Paint sp = new Paint(Paint.FILTER_BITMAP_FLAG);
            sp.setColor(0xFF000000);
            sp.setAlpha((int) (110 * opacity));
            RectF s = new RectF(dst);
            s.offset(10, 14);
            c.drawBitmap(alphaBmp, null, s, sp);
        }
        if (outlineW > 0) {
            Paint op = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            op.setColor(withOp(outlineColor, opacity));
            for (int i = 0; i < 16; i++) {
                double an = i * Math.PI / 8;
                RectF o = new RectF(dst);
                o.offset((float) (Math.cos(an) * outlineW), (float) (Math.sin(an) * outlineW));
                c.drawBitmap(alphaBmp, null, o, op);
            }
        }
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        p.setAlpha((int) (255 * opacity));
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(sat);
        if (bright != 0) {
            float b = bright * 255f;
            cm.postConcat(new ColorMatrix(new float[]{1, 0, 0, 0, b, 0, 1, 0, 0, b, 0, 0, 1, 0, b, 0, 0, 0, 1, 0}));
        }
        p.setColorFilter(new ColorMatrixColorFilter(cm));
        c.drawBitmap(bmp, null, dst, p);
        c.restore();
    }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("type", type);
            o.put("name", name);
            o.put("x", x);
            o.put("y", y);
            o.put("w", w);
            o.put("h", h);
            o.put("rot", rot);
            o.put("opacity", opacity);
            o.put("visible", visible);
            o.put("locked", locked);
            o.put("shadow", shadow);
            o.put("flip", flip);
            o.put("bold", bold);
            o.put("italic", italic);
            o.put("text", text);
            o.put("font", font);
            o.put("shape", shape);
            o.put("file", file == null ? "" : file);
            o.put("color", color);
            o.put("strokeColor", strokeColor);
            o.put("outlineColor", outlineColor);
            o.put("size", size);
            o.put("strokeW", strokeW);
            o.put("spacing", spacing);
            o.put("lineSp", lineSp);
            o.put("bright", bright);
            o.put("sat", sat);
            o.put("outlineW", outlineW);
        } catch (Exception ignored) {
        }
        return o;
    }

    public static Layer fromJson(JSONObject o) {
        Layer l = new Layer();
        l.type = o.optInt("type", TEXT);
        l.name = o.optString("name", "Layer");
        l.x = (float) o.optDouble("x", 640);
        l.y = (float) o.optDouble("y", 360);
        l.w = (float) o.optDouble("w", 300);
        l.h = (float) o.optDouble("h", 200);
        l.rot = (float) o.optDouble("rot", 0);
        l.opacity = (float) o.optDouble("opacity", 1);
        l.visible = o.optBoolean("visible", true);
        l.locked = o.optBoolean("locked", false);
        l.shadow = o.optBoolean("shadow", true);
        l.flip = o.optBoolean("flip", false);
        l.bold = o.optBoolean("bold", true);
        l.italic = o.optBoolean("italic", false);
        l.text = o.optString("text", "TEXT");
        l.font = o.optString("font", "sans-serif-black");
        l.shape = o.optString("shape", "rect");
        String f = o.optString("file", "");
        l.file = f.isEmpty() ? null : f;
        l.color = o.optInt("color", 0xFFFFFFFF);
        l.strokeColor = o.optInt("strokeColor", 0xFF000000);
        l.outlineColor = o.optInt("outlineColor", 0xFF22D3EE);
        l.size = (float) o.optDouble("size", 120);
        l.strokeW = (float) o.optDouble("strokeW", 8);
        l.spacing = (float) o.optDouble("spacing", 0);
        l.lineSp = (float) o.optDouble("lineSp", 1.1);
        l.bright = (float) o.optDouble("bright", 0);
        l.sat = (float) o.optDouble("sat", 1);
        l.outlineW = (float) o.optDouble("outlineW", 0);
        return l;
    }
             }
