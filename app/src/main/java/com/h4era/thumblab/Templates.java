package com.h4era.thumblab;

public class Templates {
    public static final String[] NAMES = {
            "Minecraft PvP", "Minecraft Survival", "Minecraft Hardcore", "Minecraft Mods",
            "Minecraft SMP", "Roblox Gameplay", "Free Fire Highlights", "BGMI Highlights",
            "Gaming Updates", "YouTube Challenges", "Gaming Tutorials", "Funny Moments"};

    static final int[] C1 = {0xFF0B1B3A, 0xFF062B2B, 0xFF16181D, 0xFF1B0B3A, 0xFF0B2A3A, 0xFF0A1020,
            0xFF2A1A05, 0xFF0A1A12, 0xFF0A1020, 0xFF2A0B3A, 0xFF0B1B3A, 0xFF1A0B2A};
    static final int[] C2 = {0xFF1E6BFF, 0xFF14B87A, 0xFF4B5563, 0xFF8B5CF6, 0xFF22D3EE, 0xFF2F6BFF,
            0xFFFF9500, 0xFF22C55E, 0xFF22D3EE, 0xFFFF4FD8, 0xFF2F6BFF, 0xFF8B5CF6};
    static final String[] MAIN = {"1 VS 1", "100 DAYS", "HARDCORE", "NEW MODS", "SMP S2", "I WON!",
            "CLUTCH", "1 VS 4", "NEW UPDATE", "24 HOURS", "HOW TO", "WAIT WHAT?!"};
    static final String[] SUB = {"PVP BATTLE", "SURVIVAL", "I SURVIVED", "INSANE!", "JOIN NOW", "ROBLOX",
            "HIGHLIGHTS", "BGMI", "EVERYTHING CHANGED", "CHALLENGE", "EASY GUIDE", "FUNNY MOMENTS"};
    static final int[] COL = {0xFFFFE600, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF22D3EE, 0xFFFFE600, 0xFFFFE600,
            0xFFFFFFFF, 0xFFFFE600, 0xFFFFFFFF, 0xFFFFE600, 0xFF22D3EE, 0xFFFFE600};
    static final int[] LAY = {1, 0, 2, 0, 1, 0, 0, 2, 0, 2, 0, 1};

    public static void apply(int i, EditorView v) {
        v.bgC1 = C1[i];
        v.bgC2 = C2[i];
        v.bgFile = null;
        v.bgBmp = null;
        v.bgDim = 0;
        v.layers.clear();
        v.sel = -1;
        int lay = LAY[i];
        if (lay == 0) {
            slot(v, 990, 380, 460, 620);
            add(v, Layer.newText(MAIN[i], COL[i], 0xFF000000, 10, 200), 380, 290);
            add(v, Layer.newText(SUB[i], 0xFFFFFFFF, 0xFF0A1020, 8, 84), 380, 470);
            add(v, Layer.newShape("arrow", 0xFF22D3EE, 200, 110), 540, 600);
        } else if (lay == 1) {
            slot(v, 230, 400, 380, 560);
            slot(v, 1050, 400, 380, 560);
            add(v, Layer.newText(MAIN[i], COL[i], 0xFF000000, 10, 230), 640, 220);
            add(v, Layer.newText(SUB[i], 0xFFFFFFFF, 0xFF0A1020, 8, 80), 640, 610);
            add(v, Layer.newShape("bolt", 0xFF22D3EE, 150, 230), 640, 420);
        } else {
            slot(v, 640, 230, 700, 380);
            add(v, Layer.newText(MAIN[i], COL[i], 0xFF000000, 10, 190), 640, 540);
            add(v, Layer.newText(SUB[i], 0xFFFFFFFF, 0xFF0A1020, 8, 80), 640, 670);
            add(v, Layer.newShape("star", 0xFFFFE600, 120, 120), 1130, 120);
        }
    }

    private static void add(EditorView v, Layer l, float x, float y) {
        l.x = x;
        l.y = y;
        v.layers.add(l);
    }

    private static void slot(EditorView v, float x, float y, float w, float h) {
        Layer r = Layer.newShape("rect", 0x33FFFFFF, w, h);
        r.name = "Cutout slot";
        r.shadow = false;
        add(v, r, x, y);
        Layer t = Layer.newText("ADD\nCUTOUT", 0xFFFFFFFF, 0xFF000000, 0, 60);
        t.name = "Slot label";
        t.shadow = false;
        t.opacity = 0.6f;
        add(v, t, x, y);
    }
}
