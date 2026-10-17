package com.h4era.thumblab;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class MainActivity extends Activity {
    static final int NAVY = 0xFF0A1020, SURF = 0xFF141D33, SURF2 = 0xFF1D2945, BLUE = 0xFF2F6BFF,
            CYAN = 0xFF22D3EE, MUTED = 0xFF8FA0C0, WHITE = 0xFFFFFFFF;
    static final int DT = android.R.style.Theme_Material_Dialog_Alert;
    static final int REQ_IMG = 11, REQ_BG = 12;
    static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT, WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;
    static final String[] FONTS = {"sans-serif-black", "sans-serif-condensed", "sans-serif", "serif", "monospace", "casual", "cursive"};
    static final String[] BG_NAMES = {"Neon blue", "Cyan night", "Sunset", "Toxic green", "Purple storm", "Dark steel"};
    static final int[] BG_C1 = {0xFF0B1B3A, 0xFF03131F, 0xFF2A0B3A, 0xFF03140C, 0xFF1B0B3A, 0xFF111418};
    static final int[] BG_C2 = {0xFF1E6BFF, 0xFF22D3EE, 0xFFFF9500, 0xFFA3E635, 0xFFFF4FD8, 0xFF5B6575};

    FrameLayout root;
    EditorView editor;
    String curId, curName = "Untitled thumbnail";
    SharedPreferences prefs;
    final Handler h = new Handler(Looper.getMainLooper());
    final Runnable saver = this::saveNow;

    TextView titleView, valLabel;
    LinearLayout chipRow, actionRow;
    SeekBar seek;
    String mode = "Size";
    boolean updating;

    // ================= helpers =================
    int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    GradientDrawable rr(int c, float r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(c);
        g.setCornerRadius(dp(r));
        return g;
    }

    TextView tv(String s, float sp, int col, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(col);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    TextView chip(String s, int bg, View.OnClickListener l) {
        TextView t = tv(s, 13, WHITE, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(14), dp(10), dp(14), dp(10));
        t.setBackground(rr(bg, 12));
        t.setOnClickListener(l);
        return t;
    }

    LinearLayout vbox() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    LinearLayout hbox() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        return l;
    }

    LinearLayout.LayoutParams lpm(int w, int hh, float wt, int m) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, hh, wt);
        p.setMargins(dp(m), dp(m), dp(m), dp(m));
        return p;
    }

    TextView section(String s) {
        TextView t = tv(s, 16, WHITE, true);
        t.setPadding(0, dp(22), 0, dp(8));
        return t;
    }

    HorizontalScrollView hscroll(View child) {
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        hs.addView(child);
        return hs;
    }

    void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    void askText(String title, String cur, boolean multi, Consumer<String> cb) {
        EditText e = new EditText(this);
        e.setText(cur);
        e.setTextColor(WHITE);
        if (multi) {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
            e.setMinLines(2);
        }
        e.setSelectAllOnFocus(true);
        FrameLayout wrap = new FrameLayout(this);
        wrap.setPadding(dp(20), dp(8), dp(20), 0);
        wrap.addView(e);
        new AlertDialog.Builder(this, DT).setTitle(title).setView(wrap)
                .setPositiveButton("OK", (d, w) -> cb.accept(e.getText().toString()))
                .setNegativeButton("Cancel", null).show();
    }

    void pickColor(int cur, IntConsumer cb) {
        int[] pal = {0xFFFFFFFF, 0xFF000000, 0xFFFFE600, 0xFF22D3EE, 0xFF2F6BFF, 0xFF0A1020,
                0xFF22C55E, 0xFFA3E635, 0xFFFF9500, 0xFFFF4FD8, 0xFF8B5CF6, 0xFFFF3B30,
                0xFF94A3B8, 0xFF1E293B, 0xFF7C2D12, 0xFFFDE68A, 0xFF67E8F9, 0xFFC4B5FD};
        LinearLayout c = vbox();
        c.setPadding(dp(16), dp(8), dp(16), dp(8));
        final AlertDialog[] dlg = new AlertDialog[1];
        LinearLayout row = null;
        for (int i = 0; i < pal.length; i++) {
            if (i % 6 == 0) {
                row = hbox();
                c.addView(row);
            }
            final int col = pal[i];
            View sw = new View(this);
            GradientDrawable g = rr(col, 10);
            g.setStroke(dp(2), 0x55FFFFFF);
            sw.setBackground(g);
            sw.setOnClickListener(x -> {
                dlg[0].dismiss();
                cb.accept(col);
            });
            row.addView(sw, lpm(dp(44), dp(44), 0, 4));
        }
        EditText hex = new EditText(this);
        hex.setHint("#RRGGBB");
        hex.setTextColor(WHITE);
        hex.setSingleLine();
        c.addView(hex);
        c.addView(chip("Apply hex colour", BLUE, x -> {
            try {
                int col = Color.parseColor(hex.getText().toString().trim());
                dlg[0].dismiss();
                cb.accept(col | 0xFF000000);
            } catch (Exception ex) {
                toast("Use a format like #FFE600");
            }
        }));
        dlg[0] = new AlertDialog.Builder(this, DT).setTitle("Pick a colour").setView(c)
                .setNegativeButton("Cancel", null).show();
    }

    // ================= lifecycle =================
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("h4era", MODE_PRIVATE);
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);
        root = new FrameLayout(this);
        root.setBackgroundColor(NAVY);
        setContentView(root);
        showSplash();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveNow();
    }

    @Override
    public void onBackPressed() {
        if (editor != null) exitEditor();
        else super.onBackPressed();
    }

    void scheduleSave() {
        h.removeCallbacks(saver);
        h.postDelayed(saver, 1500);
    }

    void saveNow() {
        if (editor != null && curId != null) ProjectStore.save(this, curId, curName, editor);
    }

    // ================= splash / onboarding =================
    void showSplash() {
        LinearLayout c = vbox();
        c.setGravity(Gravity.CENTER);
        c.setBackgroundColor(NAVY);
        TextView a = tv("H4ERA", 44, CYAN, true);
        a.setGravity(Gravity.CENTER);
        TextView b = tv("THUMB LAB", 24, WHITE, true);
        b.setGravity(Gravity.CENTER);
        TextView t = tv("Your Game. Your Style. Your Thumbnail.", 14, MUTED, false);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, dp(10), 0, 0);
        c.addView(a);
        c.addView(b);
        c.addView(t);
        root.removeAllViews();
        root.addView(c, new FrameLayout.LayoutParams(MATCH, MATCH));
        h.postDelayed(() -> {
            showHome();
            if (!prefs.getBoolean("seen", false)) {
                prefs.edit().putBoolean("seen", true).apply();
                new AlertDialog.Builder(this, DT).setTitle("Welcome to H4ERA THUMB LAB")
                        .setMessage("1. Tap Create thumbnail, or start from a template.\n\n"
                                + "2. Add text, photos and elements. Drag to move, pull the blue corner handle to resize and rotate, pinch to zoom.\n\n"
                                + "3. Export saves a 1280 x 720 PNG or JPG to Pictures/H4ERA THUMB LAB.")
                        .setPositiveButton("Let's go", null).show();
            }
        }, 1000);
    }

    // ================= tab screens =================
    void setScreen(View content, int tab) {
        editor = null;
        chipRow = null;
        LinearLayout col = vbox();
        col.setBackgroundColor(NAVY);
        col.addView(content, lpm(MATCH, 0, 1f, 0));
        LinearLayout nav = hbox();
        nav.setBackgroundColor(SURF);
        String[] n = {"Home", "Projects", "Templates", "Settings"};
        for (int i = 0; i < n.length; i++) {
            final int idx = i;
            TextView t = tv(n[i], 13, i == tab ? CYAN : MUTED, true);
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, dp(14), 0, dp(14));
            t.setOnClickListener(x -> showTab(idx));
            nav.addView(t, lpm(0, WRAP, 1f, 0));
        }
        col.addView(nav);
        root.removeAllViews();
        root.addView(col, new FrameLayout.LayoutParams(MATCH, MATCH));
    }

    void showTab(int i) {
        if (i == 0) showHome();
        else if (i == 1) showProjects();
        else if (i == 2) showTemplates();
        else showSettings();
    }

    ScrollView scroll(View c) {
        ScrollView sv = new ScrollView(this);
        sv.addView(c);
        return sv;
    }

    void showHome() {
        LinearLayout c = vbox();
        c.setPadding(dp(16), dp(18), dp(16), dp(16));
        LinearLayout logo = hbox();
        logo.addView(tv("H4ERA", 28, CYAN, true));
        logo.addView(tv(" THUMB LAB", 28, WHITE, true));
        c.addView(logo);
        c.addView(tv("Your Game. Your Style. Your Thumbnail.", 14, MUTED, false));
        TextView create = chip("Create thumbnail", BLUE, x -> newProject(-1));
        create.setTextSize(18);
        create.setPadding(0, dp(18), 0, dp(18));
        LinearLayout.LayoutParams cp = lpm(MATCH, WRAP, 0, 0);
        cp.topMargin = dp(18);
        c.addView(create, cp);
        LinearLayout qa = hbox();
        qa.addView(chip("New", SURF2, x -> newProject(-1)), lpm(0, WRAP, 1f, 4));
        qa.addView(chip("My projects", SURF2, x -> showProjects()), lpm(0, WRAP, 1f, 4));
        qa.addView(chip("Templates", SURF2, x -> showTemplates()), lpm(0, WRAP, 1f, 4));
        c.addView(qa);

        c.addView(section("Recent projects"));
        List<ProjectStore.Info> ps = ProjectStore.list(this);
        if (ps.isEmpty()) {
            c.addView(tv("No projects yet. Tap Create thumbnail to start.", 13, MUTED, false));
        } else {
            LinearLayout r = hbox();
            for (int i = 0; i < Math.min(8, ps.size()); i++) r.addView(projectCard(ps.get(i)), lpm(WRAP, WRAP, 0, 5));
            c.addView(hscroll(r));
        }

        c.addView(section("Featured gaming templates"));
        LinearLayout tr = hbox();
        for (int i = 0; i < 6; i++) tr.addView(tplCard(i), lpm(dp(150), dp(86), 0, 5));
        c.addView(hscroll(tr));
        setScreen(scroll(c), 0);
    }

    View projectCard(ProjectStore.Info p) {
        LinearLayout card = vbox();
        ImageView iv = new ImageView(this);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setBackground(rr(SURF, 10));
        iv.setImageBitmap(BitmapFactory.decodeFile(ProjectStore.preview(this, p.id).getPath()));
        card.addView(iv, new LinearLayout.LayoutParams(dp(170), dp(96)));
        TextView n = tv(p.name, 13, WHITE, true);
        n.setSingleLine();
        n.setEllipsize(TextUtils.TruncateAt.END);
        n.setPadding(0, dp(4), 0, 0);
        card.addView(n, new LinearLayout.LayoutParams(dp(170), WRAP));
        card.setOnClickListener(x -> openProject(p));
        return card;
    }

    TextView tplCard(int i) {
        TextView t = tv(Templates.NAMES[i], 14, WHITE, true);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{Templates.C1[i], Templates.C2[i]});
        g.setCornerRadius(dp(14));
        t.setBackground(g);
        t.setGravity(Gravity.BOTTOM | Gravity.START);
        t.setPadding(dp(12), dp(12), dp(12), dp(12));
        t.setOnClickListener(x -> newProject(i));
        return t;
    }

    void showProjects() {
        LinearLayout c = vbox();
        c.setPadding(dp(16), dp(18), dp(16), dp(16));
        c.addView(tv("My projects", 24, WHITE, true));
        List<ProjectStore.Info> ps = ProjectStore.list(this);
        if (ps.isEmpty()) c.addView(tv("Nothing saved yet. Projects save automatically while you edit.", 14, MUTED, false));
        for (ProjectStore.Info p : ps) {
            LinearLayout row = hbox();
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(rr(SURF, 14));
            row.setPadding(dp(8), dp(8), dp(8), dp(8));
            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageBitmap(BitmapFactory.decodeFile(ProjectStore.preview(this, p.id).getPath()));
            row.addView(iv, new LinearLayout.LayoutParams(dp(128), dp(72)));
            LinearLayout info = vbox();
            info.setPadding(dp(12), 0, dp(8), 0);
            info.addView(tv(p.name, 16, WHITE, true));
            info.addView(tv("Edited " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(p.modified)), 12, MUTED, false));
            row.addView(info, lpm(0, WRAP, 1f, 0));
            row.addView(chip("...", SURF2, x -> projectMenu(p)));
            row.setOnClickListener(x -> openProject(p));
            LinearLayout.LayoutParams rp = lpm(MATCH, WRAP, 0, 0);
            rp.topMargin = dp(10);
            c.addView(row, rp);
        }
        setScreen(scroll(c), 1);
    }

    void projectMenu(ProjectStore.Info p) {
        new AlertDialog.Builder(this, DT).setTitle(p.name)
                .setItems(new String[]{"Open", "Rename", "Duplicate", "Delete"}, (d, w) -> {
                    if (w == 0) openProject(p);
                    else if (w == 1) askText("Rename project", p.name, false, s -> {
                        if (!s.trim().isEmpty()) ProjectStore.rename(this, p.id, s.trim());
                        showProjects();
                    });
                    else if (w == 2) {
                        ProjectStore.duplicate(this, p.id);
                        showProjects();
                    } else new AlertDialog.Builder(this, DT).setTitle("Delete project?")
                            .setMessage("\"" + p.name + "\" will be removed. This can't be undone.")
                            .setPositiveButton("Delete", (d2, w2) -> {
                                ProjectStore.delete(this, p.id);
                                showProjects();
                            }).setNegativeButton("Cancel", null).show();
                }).show();
    }

    void showTemplates() {
        LinearLayout c = vbox();
        c.setPadding(dp(16), dp(18), dp(16), dp(16));
        c.addView(tv("Templates", 24, WHITE, true));
        c.addView(tv("Every item is an editable layer. Replace the text and drop in your own cutouts.", 13, MUTED, false));
        int n = Templates.NAMES.length;
        for (int i = 0; i < n; i += 2) {
            LinearLayout r = hbox();
            for (int j = i; j < i + 2 && j < n; j++) r.addView(tplCard(j), lpm(0, dp(96), 1f, 5));
            c.addView(r);
        }
        setScreen(scroll(c), 2);
    }

    void showSettings() {
        LinearLayout c = vbox();
        c.setPadding(dp(16), dp(18), dp(16), dp(16));
        c.addView(tv("Settings", 24, WHITE, true));
        Switch sw = new Switch(this);
        sw.setText("Show grid and safe-area guides in new projects");
        sw.setTextColor(WHITE);
        sw.setChecked(prefs.getBoolean("grid", false));
        sw.setOnCheckedChangeListener((b, on) -> prefs.edit().putBoolean("grid", on).apply());
        sw.setPadding(0, dp(18), 0, dp(18));
        c.addView(sw);
        c.addView(section("About H4ERA"));
        c.addView(tv("H4ERA THUMB LAB 1.0\nCreate, save and export YouTube thumbnails (1280 x 720) on your phone.\n\n"
                + "Projects and imported images stay on this device. Exports go to Pictures/H4ERA THUMB LAB.\n\n"
                + "Automatic background removal is not included. Import transparent PNG cutouts for clean edges.", 14, MUTED, false));
        setScreen(scroll(c), 3);
    }

    // ================= projects =================
    void newProject(int tpl) {
        openEditor(ProjectStore.newId(), tpl >= 0 ? Templates.NAMES[tpl] : "Untitled thumbnail", null, tpl);
    }

    void openProject(ProjectStore.Info p) {
        openEditor(p.id, p.name, ProjectStore.loadScene(this, p.id), -1);
    }

    void openEditor(String id, String name, String scene, int tpl) {
        curId = id;
        curName = name;
        editor = new EditorView(this);
        editor.grid = prefs.getBoolean("grid", false);
        if (scene != null) editor.restore(scene);
        else if (tpl >= 0) Templates.apply(tpl, editor);
        editor.clearHistory();
        buildEditorUI();
        saveNow();
    }

    void exitEditor() {
        h.removeCallbacks(saver);
        saveNow();
        showHome();
    }

    // ================= editor UI =================
    void buildEditorUI() {
        LinearLayout col = vbox();
        col.setBackgroundColor(NAVY);

        LinearLayout top = hbox();
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(6), dp(6), dp(6), dp(6));
        top.addView(chip("<", SURF2, x -> exitEditor()), lpm(WRAP, WRAP, 0, 3));
        titleView = tv(curName, 15, WHITE, true);
        titleView.setSingleLine();
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        titleView.setPadding(dp(8), 0, dp(8), 0);
        titleView.setOnClickListener(x -> askText("Project name", curName, false, s -> {
            if (!s.trim().isEmpty()) curName = s.trim();
            titleView.setText(curName);
            scheduleSave();
        }));
        top.addView(titleView, lpm(0, WRAP, 1f, 0));
        top.addView(chip("Undo", SURF2, x -> editor.undo()), lpm(WRAP, WRAP, 0, 3));
        top.addView(chip("Redo", SURF2, x -> editor.redo()), lpm(WRAP, WRAP, 0, 3));
        top.addView(chip("Grid", SURF2, x -> {
            editor.grid = !editor.grid;
            editor.invalidate();
        }), lpm(WRAP, WRAP, 0, 3));
        top.addView(chip("Export", BLUE, x -> showExport()), lpm(WRAP, WRAP, 0, 3));
        col.addView(top);

        col.addView(editor, lpm(MATCH, 0, 1f, 0));

        LinearLayout panel = vbox();
        panel.setBackgroundColor(SURF);
        panel.setPadding(dp(6), dp(6), dp(6), dp(6));
        chipRow = hbox();
        panel.addView(hscroll(chipRow));
        LinearLayout seekRow = hbox();
        seekRow.setGravity(Gravity.CENTER_VERTICAL);
        valLabel = tv("", 13, CYAN, true);
        valLabel.setGravity(Gravity.CENTER);
                seekRow.addView(valLabel, new LinearLayout.LayoutParams(dp(60), WRAP));
        seek = new SeekBar(this);
        seek.setMax(1000);
        seekRow.addView(seek, lpm(0, WRAP, 1f, 0));
        panel.addView(seekRow);
        actionRow = hbox();
        actionRow.setGravity(Gravity.CENTER_VERTICAL);
        panel.addView(hscroll(actionRow));
        col.addView(panel);

        LinearLayout tools = hbox();
        tools.setBackgroundColor(NAVY);
        tools.setPadding(dp(4), dp(6), dp(4), dp(6));
        tools.addView(chip("Text", BLUE, x -> askText("Add text", "EPIC WIN", true,
                s -> editor.addLayer(Layer.newText(s.isEmpty() ? "TEXT" : s, 0xFFFFE600, 0xFF000000, 10, 160)))), lpm(WRAP, WRAP, 0, 3));
        tools.addView(chip("Photo", BLUE, x -> pick(REQ_IMG)), lpm(WRAP, WRAP, 0, 3));
        tools.addView(chip("Background", BLUE, x -> showBgMenu()), lpm(WRAP, WRAP, 0, 3));
        tools.addView(chip("Elements", BLUE, x -> showElements()), lpm(WRAP, WRAP, 0, 3));
        tools.addView(chip("Text styles", BLUE, x -> showStyles()), lpm(WRAP, WRAP, 0, 3));
        tools.addView(chip("Layers", BLUE, x -> showLayers()), lpm(WRAP, WRAP, 0, 3));
        tools.addView(chip("Fit view", SURF2, x -> editor.resetView()), lpm(WRAP, WRAP, 0, 3));
        col.addView(hscroll(tools));

        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean user) {
                if (!user || updating || editor == null) return;
                Layer l = editor.selected();
                if (l == null && !mode.equals("BG dim")) return;
                float[] r = range(mode, l);
                float v = r[0] + (r[1] - r[0]) * p / 1000f;
                setVal(mode, l, v);
                valLabel.setText(fmt(v));
                editor.invalidate();
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {
                if (editor != null) editor.checkpoint();
            }

            @Override
            public void onStopTrackingTouch(SeekBar s) {
                scheduleSave();
            }
        });

        editor.listener = () -> {
            refreshPanel();
            scheduleSave();
        };
        root.removeAllViews();
        root.addView(col, new FrameLayout.LayoutParams(MATCH, MATCH));
        refreshPanel();
    }

    // ---- adjust panel ----
    String[] modesFor(Layer l) {
        if (l == null) return new String[]{"BG dim"};
        if (l.type == Layer.TEXT) return new String[]{"Size", "Rotate", "Opacity", "Outline", "Spacing", "Line"};
        if (l.type == Layer.IMAGE) return new String[]{"Size", "Rotate", "Opacity", "Outline", "Bright", "Sat"};
        return new String[]{"Size", "Rotate", "Opacity", "Outline"};
    }

    float[] range(String m, Layer l) {
        switch (m) {
            case "Size":
                return l != null && l.type == Layer.TEXT ? new float[]{20, 500} : new float[]{40, 2400};
            case "Rotate":
                return new float[]{-180, 180};
            case "Opacity":
                return new float[]{0, 1};
            case "Outline":
                return new float[]{0, 30};
            case "Spacing":
                return new float[]{0, 0.4f};
            case "Line":
                return new float[]{0.7f, 2f};
            case "Bright":
                return new float[]{-0.5f, 0.5f};
            case "Sat":
                return new float[]{0, 2};
            default:
                return new float[]{0, 0.85f};
        }
    }

    float getVal(String m, Layer l) {
        if (l == null) return editor.bgDim;
        switch (m) {
            case "Size":
                return l.type == Layer.TEXT ? l.size : l.w;
            case "Rotate":
                return l.rot;
            case "Opacity":
                return l.opacity;
            case "Outline":
                return l.type == Layer.IMAGE ? l.outlineW : l.strokeW;
            case "Spacing":
                return l.spacing;
            case "Line":
                return l.lineSp;
            case "Bright":
                return l.bright;
            case "Sat":
                return l.sat;
            default:
                return editor.bgDim;
        }
    }

    void setVal(String m, Layer l, float v) {
        if (l == null) {
            editor.bgDim = v;
            return;
        }
        switch (m) {
            case "Size":
                if (l.type == Layer.TEXT) l.size = v;
                else {
                    float r = v / Math.max(1f, l.w);
                    l.w = v;
                    l.h *= r;
                }
                break;
            case "Rotate":
                l.rot = v;
                break;
            case "Opacity":
                l.opacity = v;
                break;
            case "Outline":
                if (l.type == Layer.IMAGE) l.outlineW = v;
                else l.strokeW = v;
                break;
            case "Spacing":
                l.spacing = v;
                break;
            case "Line":
                l.lineSp = v;
                break;
            case "Bright":
                l.bright = v;
                break;
            case "Sat":
                l.sat = v;
                break;
            default:
                editor.bgDim = v;
        }
    }

    String fmt(float v) {
        return Math.abs(v) >= 10 ? String.valueOf(Math.round(v)) : String.format(Locale.US, "%.2f", v);
    }

    void refreshPanel() {
        if (editor == null || chipRow == null) return;
        Layer l = editor.selected();
        String[] ms = modesFor(l);
        boolean ok = false;
        for (String m : ms) if (m.equals(mode)) ok = true;
        if (!ok) mode = ms[0];
        chipRow.removeAllViews();
        for (String m : ms) {
            chipRow.addView(chip(m, m.equals(mode) ? BLUE : SURF2, x -> {
                mode = m;
                refreshPanel();
            }), lpm(WRAP, WRAP, 0, 3));
        }
        float[] r = range(mode, l);
        float v = getVal(mode, l);
        updating = true;
        seek.setProgress(Math.round((v - r[0]) / (r[1] - r[0]) * 1000f));
        updating = false;
        valLabel.setText(fmt(v));

        actionRow.removeAllViews();
        if (l == null) {
            actionRow.addView(tv("Tap an item to edit it, or use the tools below.", 13, MUTED, false));
            return;
        }
        if (l.type == Layer.TEXT) {
            act("Edit", () -> askText("Edit text", l.text, true, s -> mut(() -> l.text = s)));
            act("Colour", () -> pickColor(l.color, c -> mut(() -> l.color = c)));
            act("Outline colour", () -> pickColor(l.strokeColor, c -> mut(() -> l.strokeColor = c)));
            act("Font", () -> mut(() -> {
                int i = 0;
                for (int k = 0; k < FONTS.length; k++) if (FONTS[k].equals(l.font)) i = k;
                l.font = FONTS[(i + 1) % FONTS.length];
                toast("Font: " + l.font);
            }));
            act("Bold", () -> mut(() -> l.bold = !l.bold));
            act("Italic", () -> mut(() -> l.italic = !l.italic));
            act("Shadow", () -> mut(() -> l.shadow = !l.shadow));
        } else if (l.type == Layer.IMAGE) {
            act("Flip", () -> mut(() -> l.flip = !l.flip));
            act("Outline colour", () -> pickColor(l.outlineColor, c -> mut(() -> l.outlineColor = c)));
            act("Shadow", () -> mut(() -> l.shadow = !l.shadow));
        } else {
            act("Colour", () -> pickColor(l.color, c -> mut(() -> l.color = c)));
            act("Outline colour", () -> pickColor(l.strokeColor, c -> mut(() -> l.strokeColor = c)));
            act("Shadow", () -> mut(() -> l.shadow = !l.shadow));
        }
        act("Duplicate", () -> editor.duplicateSel());
        act("Forward", () -> editor.moveLayer(editor.sel, 1));
        act("Back", () -> editor.moveLayer(editor.sel, -1));
        act(l.locked ? "Unlock" : "Lock", () -> mut(() -> l.locked = !l.locked));
        act(l.visible ? "Hide" : "Show", () -> mut(() -> l.visible = !l.visible));
        act("Delete", () -> editor.deleteSel());
    }

    void act(String label, Runnable r) {
        actionRow.addView(chip(label, SURF2, x -> r.run()), lpm(WRAP, WRAP, 0, 3));
    }

    void mut(Runnable r) {
        editor.checkpoint();
        r.run();
        editor.invalidate();
        refreshPanel();
        scheduleSave();
    }

    // ---- dialogs ----
    void showBgMenu() {
        String[] items = {"Gradient: " + BG_NAMES[0], "Gradient: " + BG_NAMES[1], "Gradient: " + BG_NAMES[2],
                "Gradient: " + BG_NAMES[3], "Gradient: " + BG_NAMES[4], "Gradient: " + BG_NAMES[5],
                "Custom colour 1", "Custom colour 2", "Import background image", "Remove background image"};
        new AlertDialog.Builder(this, DT).setTitle("Background").setItems(items, (d, w) -> {
            if (w < 6) {
                editor.checkpoint();
                editor.bgC1 = BG_C1[w];
                editor.bgC2 = BG_C2[w];
                editor.bgFile = null;
                editor.bgBmp = null;
                editor.invalidate();
                scheduleSave();
            } else if (w == 6) {
                pickColor(editor.bgC1, c -> {
                    editor.checkpoint();
                    editor.bgC1 = c;
                    editor.bgFile = null;
                    editor.bgBmp = null;
                    editor.invalidate();
                    scheduleSave();
                });
            } else if (w == 7) {
                pickColor(editor.bgC2, c -> {
                    editor.checkpoint();
                    editor.bgC2 = c;
                    editor.bgFile = null;
                    editor.bgBmp = null;
                    editor.invalidate();
                    scheduleSave();
                });
            } else if (w == 8) {
                pick(REQ_BG);
            } else {
                editor.checkpoint();
                editor.bgFile = null;
                editor.bgBmp = null;
                editor.invalidate();
                scheduleSave();
            }
        }).show();
    }

    void showElements() {
        String[] items = {"Arrow", "Circle", "Ring", "Star", "Lightning", "Rectangle", "Question mark", "Exclamation mark"};
        new AlertDialog.Builder(this, DT).setTitle("Elements").setItems(items, (d, w) -> {
            Layer l;
            switch (w) {
                case 0:
                    l = Layer.newShape("arrow", 0xFFFFE600, 320, 180);
                    break;
                case 1:
                    l = Layer.newShape("circle", 0xFF22D3EE, 260, 260);
                    break;
                case 2:
                    l = Layer.newShape("ring", 0xFFFFE600, 320, 320);
                    break;
                case 3:
                    l = Layer.newShape("star", 0xFFFFE600, 240, 240);
                    break;
                case 4:
                    l = Layer.newShape("bolt", 0xFF22D3EE, 200, 300);
                    break;
                case 5:
                    l = Layer.newShape("rect", 0xFF2F6BFF, 360, 200);
                    break;
                case 6:
                    l = Layer.newText("?", 0xFFFFE600, 0xFF000000, 12, 360);
                    break;
                default:
                    l = Layer.newText("!", 0xFFFFE600, 0xFF000000, 12, 360);
            }
            editor.addLayer(l);
        }).show();
    }

    void showStyles() {
        String[] names = {"EPIC WIN", "NEW UPDATE", "1 VS 5", "GOD MODE", "INSANE\nCLUTCH"};
        int[] fill = {0xFFFFE600, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF22D3EE, 0xFFFFFFFF};
        int[] stroke = {0xFF000000, 0xFF2F6BFF, 0xFF0A1020, 0xFF0A1020, 0xFF000000};
        float[] size = {170, 150, 200, 180, 140};
        String[] labels = new String[names.length];
        for (int i = 0; i < names.length; i++) labels[i] = names[i].replace("\n", " ");
        new AlertDialog.Builder(this, DT).setTitle("Text styles").setItems(labels, (d, w) ->
                editor.addLayer(Layer.newText(names[w], fill[w], stroke[w], 10, size[w]))).show();
    }

    void showLayers() {
        LinearLayout c = vbox();
        c.setPadding(dp(12), dp(8), dp(12), dp(8));
        final AlertDialog[] dlg = new AlertDialog[1];
        if (editor.layers.isEmpty()) c.addView(tv("No layers yet. Add text, a photo or an element.", 14, MUTED, false));
        for (int i = editor.layers.size() - 1; i >= 0; i--) {
            final int idx = i;
            final Layer l = editor.layers.get(i);
            LinearLayout row = hbox();
            row.setGravity(Gravity.CENTER_VERTICAL);
            String label = l.type == Layer.TEXT ? l.text.replace("\n", " ") : l.name;
            TextView t = tv(label, 15, i == editor.sel ? CYAN : WHITE, true);
            t.setSingleLine();
            t.setEllipsize(TextUtils.TruncateAt.END);
            t.setPadding(0, dp(10), dp(6), dp(10));
            t.setOnClickListener(x -> {
                dlg[0].dismiss();
                editor.select(idx);
            });
            row.addView(t, lpm(0, WRAP, 1f, 0));
            row.addView(chip(l.visible ? "Hide" : "Show", SURF2, x -> {
                dlg[0].dismiss();
                mutLayer(() -> l.visible = !l.visible);
            }), lpm(WRAP, WRAP, 0, 2));
            row.addView(chip(l.locked ? "Unlock" : "Lock", SURF2, x -> {
                dlg[0].dismiss();
                mutLayer(() -> l.locked = !l.locked);
            }), lpm(WRAP, WRAP, 0, 2));
            row.addView(chip("Up", SURF2, x -> {
                dlg[0].dismiss();
                editor.moveLayer(idx, 1);
                showLayers();
            }), lpm(WRAP, WRAP, 0, 2));
            row.addView(chip("Down", SURF2, x -> {
                dlg[0].dismiss();
                editor.moveLayer(idx, -1);
                showLayers();
            }), lpm(WRAP, WRAP, 0, 2));
            c.addView(row);
        }
        dlg[0] = new AlertDialog.Builder(this, DT).setTitle("Layers (top first)").setView(scroll(c))
                .setPositiveButton("Close", null).show();
    }

    void mutLayer(Runnable r) {
        mut(r);
        showLayers();
    }

    // ---- images ----
    void pick(int req) {
        Intent i;
        if (Build.VERSION.SDK_INT >= 33) {
            i = new Intent(MediaStore.ACTION_PICK_IMAGES);
        } else {
            i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("image/*");
            i.addCategory(Intent.CATEGORY_OPENABLE);
        }
        try {
            startActivityForResult(i, req);
        } catch (ActivityNotFoundException e) {
            Intent f = new Intent(Intent.ACTION_GET_CONTENT);
            f.setType("image/*");
            try {
                startActivityForResult(f, req);
            } catch (Exception ex) {
                toast("No photo picker found on this device.");
            }
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null || data.getData() == null || editor == null) return;
        final Uri u = data.getData();
        final EditorView ev = editor;
        new Thread(() -> {
            String f = ImageStore.importUri(this, u);
            runOnUiThread(() -> {
                Bitmap b = f == null ? null : ImageStore.get(this, f);
                if (b == null) {
                    toast("That file couldn't be read as an image. Try a PNG, JPG or WebP.");
                    return;
                }
                if (req == REQ_BG) {
                    ev.checkpoint();
                    ev.bgFile = f;
                    ev.bgBmp = b;
                    ev.invalidate();
                    scheduleSave();
                } else {
                    ev.addLayer(Layer.newImage(f, b));
                }
            });
        }).start();
    }

    // ---- export ----
    void showExport() {
        String[] items = {"Save PNG - 1280x720", "Save JPG - quality 95", "Save JPG - quality 80", "Share as PNG"};
        new AlertDialog.Builder(this, DT).setTitle("Export thumbnail").setItems(items, (d, w) ->
                doExport(w == 0 || w == 3, w == 1 ? 95 : 80, w == 3)).show();
    }

    void doExport(boolean png, int quality, boolean share) {
        LinearLayout box = hbox();
        box.setPadding(dp(24), dp(16), dp(24), dp(16));
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.addView(new ProgressBar(this));
        TextView msg = tv("  Rendering 1280 x 720...", 15, WHITE, false);
        box.addView(msg);
        final AlertDialog pd = new AlertDialog.Builder(this, DT).setTitle("Exporting").setView(box).setCancelable(false).show();
        final EditorView ev = editor;
        new Thread(() -> {
            try {
                Bitmap b = ev.render(1280, 720);
                Uri u = Exporter.save(this, b, png, quality);
                b.recycle();
                runOnUiThread(() -> {
                    pd.dismiss();
                    toast("Saved to Pictures/H4ERA THUMB LAB");
                    if (share) {
                        Intent s = new Intent(Intent.ACTION_SEND);
                        s.setType(png ? "image/png" : "image/jpeg");
                        s.putExtra(Intent.EXTRA_STREAM, u);
                        s.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(Intent.createChooser(s, "Share thumbnail"));
                    }
                });
            } catch (Throwable ex) {
                final String m = ex.getMessage();
                runOnUiThread(() -> {
                    pd.dismiss();
                    toast("Export failed: " + m);
                });
            }
        }).start();
    }
 }
