package com.h4era.thumblab;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EditorView extends View {
    public interface Listener {
        void onChanged();
    }

    public static final float W = 1280f, H = 720f;

    public final List<Layer> layers = new ArrayList<>();
    public int sel = -1, bgC1 = 0xFF0A1020, bgC2 = 0xFF2F6BFF;
    public float bgDim = 0f;
    public String bgFile;
    public Bitmap bgBmp;
    public boolean grid;
    public Listener listener;

    private final ArrayList<String> undo = new ArrayList<>(), redo = new ArrayList<>();
    private float scale = 1f, ox, oy, lastX, lastY, startDist, startAng, startSize, startW, startH, startRot;
    private int mode;
    private boolean armed;
    private final ScaleGestureDetector sgd;

    public EditorView(Context c) {
        super(c);
        sgd = new ScaleGestureDetector(c, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            float lfx, lfy;

            @Override
            public boolean onScaleBegin(ScaleGestureDetector d) {
                lfx = d.getFocusX();
                lfy = d.getFocusY();
                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector d) {
                float ns = Math.max(0.2f, Math.min(8f, scale * d.getScaleFactor()));
                float f = ns / scale;
                float fx = d.getFocusX(), fy = d.getFocusY();
                ox = fx - (fx - ox) * f + (fx - lfx);
                oy = fy - (fy - oy) * f + (fy - lfy);
                lfx = fx;
                lfy = fy;
                scale = ns;
                invalidate();
                return true;
            }
        });
    }

    public Layer selected() {
        return sel >= 0 && sel < layers.size() ? layers.get(sel) : null;
    }

    public void select(int i) {
        sel = i;
        invalidate();
        changed();
    }

    private void changed() {
        if (listener != null) listener.onChanged();
    }

    public void addLayer(Layer l) {
        checkpoint();
        layers.add(l);
        sel = layers.size() - 1;
        invalidate();
        changed();
    }

    public void duplicateSel() {
        Layer s = selected();
        if (s == null) return;
        checkpoint();
        Layer d = s.copy();
        d.x += 40;
        d.y += 40;
        layers.add(sel + 1, d);
        sel = sel + 1;
        invalidate();
        changed();
    }

    public void deleteSel() {
        if (selected() == null) return;
        checkpoint();
        layers.remove(sel);
        sel = -1;
        invalidate();
        changed();
    }

    public void moveLayer(int i, int dir) {
        int j = i + dir;
        if (i < 0 || j < 0 || i >= layers.size() || j >= layers.size()) return;
        checkpoint();
        Collections.swap(layers, i, j);
        if (sel == i) sel = j;
        else if (sel == j) sel = i;
        invalidate();
        changed();
    }

    public void checkpoint() {
        undo.add(snapshot());
        if (undo.size() > 60) undo.remove(0);
        redo.clear();
    }

    public void clearHistory() {
        undo.clear();
        redo.clear();
    }

    public void undo() {
        if (undo.isEmpty()) return;
        redo.add(snapshot());
        restore(undo.remove(undo.size() - 1));
    }

    public void redo() {
        if (redo.isEmpty()) return;
        undo.add(snapshot());
        restore(redo.remove(redo.size() - 1));
    }

    public String snapshot() {
        try {
            JSONObject o = new JSONObject();
            o.put("c1", bgC1);
            o.put("c2", bgC2);
            o.put("dim", bgDim);
            o.put("bgFile", bgFile == null ? "" : bgFile);
            JSONArray a = new JSONArray();
            for (Layer l : layers) a.put(l.toJson());
            o.put("layers", a);
            return o.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    public void restore(String s) {
        try {
            JSONObject o = new JSONObject(s);
            bgC1 = o.optInt("c1", 0xFF0A1020);
            bgC2 = o.optInt("c2", 0xFF2F6BFF);
            bgDim = (float) o.optDouble("dim", 0);
            String bf = o.optString("bgFile", "");
            bgFile = bf.isEmpty() ? null : bf;
            bgBmp = bgFile == null ? null : ImageStore.get(getContext(), bgFile);
            layers.clear();
            JSONArray a = o.optJSONArray("layers");
            if (a != null) {
                for (int i = 0; i < a.length(); i++) {
                    Layer l = Layer.fromJson(a.getJSONObject(i));
                    if (l.type == Layer.IMAGE) l.bmp = ImageStore.get(getContext(), l.file);
                    layers.add(l);
                }
            }
            sel = -1;
            invalidate();
            changed();
        } catch (Exception ignored) {
        }
    }

    public void drawScene(Canvas c) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        if (bgBmp != null) {
            float bw = bgBmp.getWidth(), bh = bgBmp.getHeight();
            float s = Math.max(W / bw, H / bh);
            float dw = bw * s, dh = bh * s;
            c.drawBitmap(bgBmp, null, new RectF((W - dw) / 2, (H - dh) / 2, (W + dw) / 2, (H + dh) / 2), p);
        } else {
            p.setShader(new LinearGradient(0, 0, W, H, bgC1, bgC2, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, W, H, p);
            p.setShader(null);
        }
        if (bgDim > 0) {
            p.setColor(0xFF000000);
            p.setAlpha((int) (255 * bgDim));
            c.drawRect(0, 0, W, H, p);
        }
        for (Layer l : layers) l.draw(c);
    }

    public Bitmap render(int outW, int outH) {
        Bitmap b = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        c.scale(outW / W, outH / H);
        drawScene(c);
        return b;
    }

    public void resetView() {
        if (getWidth() == 0) return;
        scale = Math.min(getWidth() / W, getHeight() / H) * 0.94f;
        ox = (getWidth() - W * scale) / 2f;
        oy = (getHeight() - H * scale) / 2f;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        resetView();
    }

    private void drawGrid(Canvas c) {
        Paint p = new Paint();
        p.setColor(0x66FFFFFF);
        p.setStrokeWidth(2f / scale);
        for (int i = 1; i < 3; i++) {
            c.drawLine(W * i / 3, 0, W * i / 3, H, p);
            c.drawLine(0, H * i / 3, W, H * i / 3, p);
        }
        p.setStyle(Paint.Style.STROKE);
        p.setColor(0xAA22D3EE);
        c.drawRect(W * 0.05f, H * 0.05f, W * 0.95f, H * 0.95f, p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0x55FF5555);
        c.drawRect(W - 210, H - 70, W, H, p);
    }

    @Override
    protected void onDraw(Canvas c) {
        c.drawColor(0xFF060A14);
        c.save();
        c.translate(ox, oy);
        c.scale(scale, scale);
        c.save();
        c.clipRect(0, 0, W, H);
        drawScene(c);
        if (grid) drawGrid(c);
        c.restore();
        Paint f = new Paint(Paint.ANTI_ALIAS_FLAG);
        f.setStyle(Paint.Style.STROKE);
        f.setColor(0x6622D3EE);
        f.setStrokeWidth(2f / scale);
        c.drawRect(0, 0, W, H, f);
        Layer l = selected();
        if (l != null && l.visible) {
            l.measure();
            c.save();
            c.translate(l.x, l.y);
            c.rotate(l.rot);
            f.setColor(0xFF22D3EE);
            f.setStrokeWidth(3f / scale);
            c.drawRect(-l.w / 2, -l.h / 2, l.w / 2, l.h / 2, f);
            c.restore();
            if (!l.locked) {
                float[] hp = handlePos(l);
                Paint hpnt = new Paint(Paint.ANTI_ALIAS_FLAG);
                hpnt.setColor(0xFFFFFFFF);
                c.drawCircle(hp[0], hp[1], 18f / scale, hpnt);
                hpnt.setColor(0xFF2F6BFF);
                c.drawCircle(hp[0], hp[1], 11f / scale, hpnt);
            }
        }
        c.restore();
    }

    private float[] handlePos(Layer l) {
        double r = Math.toRadians(l.rot);
        float hx = l.x + (float) (l.w / 2 * Math.cos(r) - l.h / 2 * Math.sin(r));
        float hy = l.y + (float) (l.w / 2 * Math.sin(r) + l.h / 2 * Math.cos(r));
        return new float[]{hx, hy};
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        sgd.onTouchEvent(e);
        if (sgd.isInProgress() || e.getPointerCount() > 1) {
            mode = 0;
            return true;
        }
        float sx = (e.getX() - ox) / scale, sy = (e.getY() - oy) / scale;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                lastX = e.getX();
                lastY = e.getY();
                armed = true;
                Layer s = selected();
                if (s != null && !s.locked && s.visible) {
                    s.measure();
                    float[] hp = handlePos(s);
                    if (Math.hypot(sx - hp[0], sy - hp[1]) < 44f / scale) {
                        mode = 2;
                        startDist = Math.max(10f, (float) Math.hypot(sx - s.x, sy - s.y));
                        startAng = (float) Math.toDegrees(Math.atan2(sy - s.y, sx - s.x));
                        startSize = s.size;
                        startW = s.w;
                        startH = s.h;
                        startRot = s.rot;
                        return true;
                    }
                }
                int hit = -1;
                for (int i = layers.size() - 1; i >= 0; i--) {
                    Layer l = layers.get(i);
                    if (l.visible && l.hit(sx, sy)) {
                        hit = i;
                        break;
                    }
                }
                select(hit);
                mode = hit >= 0 ? 1 : 3;
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                float dx = e.getX() - lastX, dy = e.getY() - lastY;
                lastX = e.getX();
                lastY = e.getY();
                Layer s = selected();
                if (mode == 3) {
                    ox += dx;
                    oy += dy;
                    invalidate();
                } else if (mode == 1 && s != null && !s.locked) {
                    if (armed) {
                        checkpoint();
                        armed = false;
                    }
                    s.x += dx / scale;
                    s.y += dy / scale;
                    invalidate();
                } else if (mode == 2 && s != null) {
                    if (armed) {
                        checkpoint();
                        armed = false;
                    }
                    float d = (float) Math.hypot(sx - s.x, sy - s.y);
                    float ratio = Math.max(0.05f, d / startDist);
                    float ang = (float) Math.toDegrees(Math.atan2(sy - s.y, sx - s.x));
                    if (s.type == Layer.TEXT) {
                        s.size = Math.max(20f, Math.min(600f, startSize * ratio));
                    } else {
                        s.w = Math.max(30f, startW * ratio);
                        s.h = Math.max(30f, startH * ratio);
                    }
                    s.rot = startRot + (ang - startAng);
                    invalidate();
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mode = 0;
                changed();
                return true;
            default:
                return true;
        }
    }
}
