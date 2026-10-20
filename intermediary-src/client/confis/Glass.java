package client.confis;

import net.minecraft.client.gui.DrawContext;

/**
 * iOS-style "liquid glass" drawing for the HUD modules and the menu.
 * Frosted translucent body, a bright specular sheen on top, a glowing rim that fades towards the
 * bottom and an optional soft shadow. Built only from fill() calls so it works anywhere.
 */
public final class Glass {
    private Glass() {}

    private static int lerp(int a, int b, float t) { return Math.round(a + (b - a) * t); }
    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
    private static int white(int a) { return (a & 255) << 24 | 0xFFFFFF; }
    private static int rgba(int a, int rgb) { return (a & 255) << 24 | (rgb & 0xFFFFFF); }
    private static int argb(int a, int r, int g, int b) {
        return (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | (b & 255);
    }
    private static int lum(int c) {
        return (((c >> 16) & 255) * 299 + ((c >> 8) & 255) * 587 + (c & 255) * 114) / 1000;
    }

    /** Horizontal inset of corner row i (0 = outermost row) for corner radius r. */
    private static int inset(int i, int r) {
        if (i >= r) return 0;
        double dy = r - i - 0.5;
        return r - (int) Math.round(Math.sqrt(Math.max(0, r * r - dy * dy)));
    }

    /** Inset of row i of a shape that is h rows tall (same shape top and bottom). */
    private static int d(int i, int h, int r) {
        i = Math.max(0, Math.min(h - 1, i));
        return inset(Math.min(i, h - 1 - i), r);
    }

    private static void span(DrawContext g, int x0, int x1, int y, int color) {
        if (x1 > x0) g.fill(x0, y, x1, y + 1, color);
    }

    // ------------------------------------------------------------------ core
    private static void render(DrawContext g, int x, int y, int w, int h, int r,
                               int bodyA, int cr, int cg, int cb,
                               boolean shadow, int sheenMax, int rimTop, int rimBottom, int rimRgb) {
        r = Math.max(1, Math.min(r, Math.min(h / 2, (w + 1) / 2)));

        if (shadow) {
            g.fill(x + 3, y + h, x + w - 3, y + h + 1, 0x26000000);
            g.fill(x + 5, y + h + 1, x + w - 5, y + h + 2, 0x12000000);
        }

        int sheenRows = Math.max(2, h * 45 / 100);
        for (int i = 0; i < h; i++) {
            int ins = d(i, h, r);
            int x0 = x + ins, x1 = x + w - ins, ry = y + i;
            float t = h <= 1 ? 0f : (float) i / (h - 1);

            // body: a touch more opaque at the top, lighter at the bottom
            span(g, x0, x1, ry, argb(clamp(lerp(bodyA + 0x10, bodyA - 0x10, t), 0x18, 0xFF), cr, cg, cb));

            // specular sheen across the top half
            if (sheenMax > 0 && i < sheenRows) {
                int sa = Math.round(sheenMax * (1f - (float) i / sheenRows));
                if (sa > 0) span(g, x0, x1, ry, white(sa));
            }

            // glowing rim: bright at the top, fading towards the bottom
            int rimCol = rgba(lerp(rimTop, rimBottom, t), rimRgb);
            if (i == 0 || i == h - 1) {
                span(g, x0, x1, ry, rimCol);
            } else {
                int rw = i < h / 2
                        ? Math.max(1, d(i - 1, h, r) - ins)
                        : Math.max(1, d(i + 1, h, r) - ins);
                span(g, x0, Math.min(x0 + rw, x1), ry, rimCol);
                span(g, Math.max(x1 - rw, x0), x1, ry, rimCol);
            }
        }

        // thin inner highlight just under the top edge, and a faint glow above the bottom edge
        if (h >= 9 && sheenMax > 0) {
            int tx = d(1, h, r) + 3;
            span(g, x + tx, x + w - tx, y + 1, white(0x3C));
            int bx = d(h - 2, h, r) + 3;
            span(g, x + bx, x + w - bx, y + h - 2, white(0x16));
        }
    }

    // ------------------------------------------------------------ HUD modules
    /** Liquid-glass panel for a HUD module. bg is the module's background color (its alpha sets how frosted it is). */
    public static void draw(DrawContext g, int x, int y, int w, int h, int bg) {
        if (w < 6 || h < 6) {
            g.fill(x, y, x + w, y + h, bg);
            return;
        }
        int bodyA = clamp((int) ((bg >>> 24) * 0.85f), 0x40, 0xC8);
        render(g, x, y, w, h, 7, bodyA,
                lerp((bg >> 16) & 255, 255, 0.14f), lerp((bg >> 8) & 255, 255, 0.14f), lerp(bg & 255, 255, 0.14f),
                true, 0x34, 0xB4, 0x34, 0xFFFFFF);
    }

    /** Small rounded "well" used for inventory slots. */
    public static void slot(DrawContext g, int x, int y, int w, int h) {
        fillRound(g, x, y, w, h, 3, 0x26FFFFFF);
    }

    // -------------------------------------------------------------------- menu
    /** The big menu window. argb = panel color (alpha = how see-through the glass is). */
    public static void panel(DrawContext g, int x, int y, int w, int h, int argb) {
        render(g, x, y, w, h, 12, clamp(argb >>> 24, 0x30, 0xE0),
                lerp((argb >> 16) & 255, 255, 0.10f), lerp((argb >> 8) & 255, 255, 0.10f), lerp(argb & 255, 255, 0.10f),
                true, 0x1C, 0xB4, 0x38, 0xFFFFFF);
    }

    /** A module card in the menu grid. */
    public static void card(DrawContext g, int x, int y, int w, int h, boolean hover, boolean on) {
        int a = on ? 0x6A : 0x44;
        int base = on ? 0x3C : 0x24;
        if (hover) { a += 0x1C; base += 0x10; }
        render(g, x, y, w, h, 8, a, base, base, base, false, 0x22,
                hover ? 0xD0 : (on ? 0xA0 : 0x5A), hover ? 0x78 : (on ? 0x40 : 0x22), 0xFFFFFF);
    }

    /** A pill button. fill = base color: bright fills (active tab, Enabled) become light glass, dark ones dark glass. */
    public static void button(DrawContext g, int x, int y, int w, int h, int fill, boolean hover) {
        boolean light = lum(fill) > 140;
        int a = light ? 0xD6 : 0x66;
        float tint = light ? 0.0f : 0.16f;
        if (hover) { a = Math.min(0xF0, a + 0x28); tint += 0.10f; }
        int cr = lerp((fill >> 16) & 255, 255, tint);
        int cg = lerp((fill >> 8) & 255, 255, tint);
        int cb = lerp(fill & 255, 255, tint);
        render(g, x, y, w, h, 7, a, cr, cg, cb, false, light ? 0x30 : 0x26,
                hover ? 0xE0 : 0x96, hover ? 0x60 : 0x30, 0xFFFFFF);
    }

    /** Inset text field (search box). Focused = rim in the accent color. */
    public static void field(DrawContext g, int x, int y, int w, int h, boolean focus, int accent) {
        render(g, x, y, w, h, 7, 0x78, 0, 0, 0, false, 0,
                focus ? 0xE8 : 0x40, focus ? 0xB0 : 0x24, focus ? (accent & 0xFFFFFF) : 0xFFFFFF);
    }

    /** Plain rounded fill in one color (sliders, scrollbars, slots). */
    public static void fillRound(DrawContext g, int x, int y, int w, int h, int r, int argb) {
        r = Math.max(1, Math.min(r, Math.min(h / 2, (w + 1) / 2)));
        for (int i = 0; i < h; i++) {
            int ins = d(i, h, r);
            span(g, x + ins, x + w - ins, y + i, argb);
        }
    }
}
