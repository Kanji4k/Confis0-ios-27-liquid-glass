package client.confis;

import net.minecraft.client.gui.DrawContext;

/**
 * iOS-style "liquid glass" panels for the HUD modules.
 * Frosted translucent body, bright specular sheen on top, glowing rim that fades towards the bottom,
 * soft drop shadow. Built only from fill() calls so it works anywhere on the HUD.
 */
public final class Glass {
    private Glass() {}

    private static int lerp(int a, int b, float t) { return Math.round(a + (b - a) * t); }
    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
    private static int white(int a) { return (a & 255) << 24 | 0xFFFFFF; }
    private static int argb(int a, int r, int g, int b) {
        return (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | (b & 255);
    }

    /** Horizontal inset of corner row i (0 = outermost row) for corner radius r. */
    private static int inset(int i, int r) {
        if (i >= r) return 0;
        double dy = r - i - 0.5;
        return r - (int) Math.round(Math.sqrt(Math.max(0, r * r - dy * dy)));
    }

    /** Inset of row i of a panel that is h rows tall (same shape top and bottom). */
    private static int d(int i, int h, int r) {
        i = Math.max(0, Math.min(h - 1, i));
        return inset(Math.min(i, h - 1 - i), r);
    }

    /** Draws a liquid-glass panel. bg is the module's background color (its alpha sets how frosted it is). */
    public static void draw(DrawContext g, int x, int y, int w, int h, int bg) {
        if (w < 6 || h < 6) {
            g.fill(x, y, x + w, y + h, bg);
            return;
        }
        int r = Math.min(7, h / 2);
        int a = bg >>> 24;
        int bodyA = clamp((int) (a * 0.85f), 0x40, 0xC8);
        // tint the body slightly towards white so it reads as glass, not as a flat dark box
        int cr = lerp((bg >> 16) & 255, 255, 0.14f);
        int cg = lerp((bg >> 8) & 255, 255, 0.14f);
        int cb = lerp(bg & 255, 255, 0.14f);

        // soft drop shadow under the panel
        g.fill(x + 3, y + h, x + w - 3, y + h + 1, 0x26000000);
        g.fill(x + 5, y + h + 1, x + w - 5, y + h + 2, 0x12000000);

        int sheenRows = Math.max(2, h * 45 / 100);
        for (int i = 0; i < h; i++) {
            int ins = d(i, h, r);
            int x0 = x + ins, x1 = x + w - ins, ry = y + i;
            float t = h <= 1 ? 0f : (float) i / (h - 1);

            // body: a touch more opaque at the top, lighter at the bottom
            g.fill(x0, ry, x1, ry + 1, argb(clamp(lerp(bodyA + 0x10, bodyA - 0x10, t), 0x20, 0xFF), cr, cg, cb));

            // specular sheen across the top half
            if (i < sheenRows) {
                int sa = Math.round(0x34 * (1f - (float) i / sheenRows));
                if (sa > 0) g.fill(x0, ry, x1, ry + 1, white(sa));
            }

            // glowing rim: bright at the top, fading towards the bottom
            int rimA = lerp(0xB4, 0x34, t);
            if (i == 0 || i == h - 1) {
                g.fill(x0, ry, x1, ry + 1, white(rimA));
            } else {
                int rw = i < h / 2
                        ? Math.max(1, d(i - 1, h, r) - ins)
                        : Math.max(1, d(i + 1, h, r) - ins);
                g.fill(x0, ry, x0 + rw, ry + 1, white(rimA));
                g.fill(x1 - rw, ry, x1, ry + 1, white(rimA));
            }
        }

        // thin inner highlight just under the top edge, and a faint glow above the bottom edge
        if (h >= 9) {
            int tx = d(1, h, r) + 3;
            g.fill(x + tx, y + 1, x + w - tx, y + 2, white(0x3C));
            int bx = d(h - 2, h, r) + 3;
            g.fill(x + bx, y + h - 2, x + w - bx, y + h - 1, white(0x16));
        }
    }

    /** Small rounded "well" used for inventory slots. */
    public static void slot(DrawContext g, int x, int y, int w, int h) {
        int c = 0x26FFFFFF;
        g.fill(x + 1, y, x + w - 1, y + 1, c);
        g.fill(x, y + 1, x + w, y + h - 1, c);
        g.fill(x + 1, y + h - 1, x + w - 1, y + h, c);
    }
}
