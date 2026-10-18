package dev.sphinx.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/** Colours, texture ids and small drawing helpers shared by all Sphinx screens. */
public final class SphinxTheme {
    private SphinxTheme() {}

    // ---- Colours (ARGB) ----
    public static final int BG_IDLE      = 0xCC0A0A0C;
    public static final int BG_HOVER     = 0xE61A1A1E;
    public static final int BORDER_IDLE  = 0xFF2C2C31;
    public static final int GOLD         = 0xFFD4AF37;
    public static final int GOLD_DIM     = 0xFF8C7530;
    public static final int TEXT         = 0xFFFFFFFF;
    public static final int TEXT_DIM     = 0xFFB4B4BA;
    public static final int TEXT_MUTED   = 0xFF7C7C84;
    public static final int ERROR        = 0xFFFF6B6B;

    // ---- Textures (sizes must match the PNGs in assets/sphinxclient/textures/gui) ----
    public static final Identifier BACKGROUND = tex("background.png");
    public static final int BG_W = 1280, BG_H = 720;
    public static final Identifier LOGO = tex("logo.png");
    public static final int LOGO_SIZE = 256;
    public static final Identifier WORDMARK = tex("wordmark.png");
    public static final int WM_W = 1024, WM_H = 256;
    public static final int ICON_SIZE = 48;

    public static final Identifier ICON_PLAY     = icon("play");
    public static final Identifier ICON_GLOBE    = icon("globe");
    public static final Identifier ICON_SHIRT    = icon("shirt");
    public static final Identifier ICON_GEAR     = icon("gear");
    public static final Identifier ICON_FOLDER   = icon("folder");
    public static final Identifier ICON_USER     = icon("user");
    public static final Identifier ICON_DOCUMENT = icon("document");
    public static final Identifier ICON_EXIT     = icon("exit");

    private static boolean hasBackground, hasLogo, hasWordmark;

    private static Identifier tex(String name) { return Identifier.of("sphinxclient", "textures/gui/" + name); }
    private static Identifier icon(String name) { return Identifier.of("sphinxclient", "textures/gui/icons/" + name + ".png"); }

    /** Re-check which assets exist (called from each screen's init, i.e. after resource reloads/resizes). */
    public static void refreshAssets(MinecraftClient mc) {
        hasBackground = mc.getResourceManager().getResource(BACKGROUND).isPresent();
        hasLogo       = mc.getResourceManager().getResource(LOGO).isPresent();
        hasWordmark   = mc.getResourceManager().getResource(WORDMARK).isPresent();
    }
    public static boolean hasLogo() { return hasLogo; }
    public static boolean hasWordmark() { return hasWordmark; }

    // ---- Drawing helpers ----

    /** Full-screen background: "cover" scaling (crop, never stretch); gradient + square moon if the image is missing. */
    public static void drawBackground(DrawContext c, int w, int h) {
        if (hasBackground) {
            float screenAspect = (float) w / h, texAspect = (float) BG_W / BG_H;
            int rw, rh, u, v;
            if (screenAspect > texAspect) { rw = BG_W; rh = Math.round(BG_W / screenAspect); u = 0; v = (BG_H - rh) / 2; }
            else                          { rh = BG_H; rw = Math.round(BG_H * screenAspect); v = 0; u = (BG_W - rw) / 2; }
            c.drawTexture(RenderPipelines.GUI_TEXTURED, BACKGROUND, 0, 0, (float) u, (float) v, w, h, rw, rh, BG_W, BG_H);
        } else {
            c.fillGradient(0, 0, w, h, 0xFF0B1224, 0xFF020306);
            int mx = (int) (w * 0.8f), my = (int) (h * 0.16f);
            c.fill(mx - 14, my - 14, mx + 14, my + 14, 0x22AFC4E6);
            c.fill(mx - 8, my - 8, mx + 8, my + 8, 0xFFE8EEF8);
        }
        // Darken top/bottom a little so UI text stays readable.
        c.fillGradient(0, 0, w, h, 0x30000000, 0xB0000000);
    }

    public static void drawLogo(DrawContext c, int x, int y, int size) {
        if (hasLogo) c.drawTexture(RenderPipelines.GUI_TEXTURED, LOGO, x, y, 0f, 0f, size, size, LOGO_SIZE, LOGO_SIZE, LOGO_SIZE, LOGO_SIZE);
    }

    public static void drawWordmark(DrawContext c, int x, int y, int w, int h) {
        c.drawTexture(RenderPipelines.GUI_TEXTURED, WORDMARK, x, y, 0f, 0f, w, h, WM_W, WM_H, WM_W, WM_H);
    }

    public static void drawIcon(DrawContext c, Identifier icon, int x, int y, int size) {
        c.drawTexture(RenderPipelines.GUI_TEXTURED, icon, x, y, 0f, 0f, size, size, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
    }

    public static void drawBorder(DrawContext c, int x, int y, int w, int h, int color) {
        c.fill(x, y, x + w, y + 1, color);
        c.fill(x, y + h - 1, x + w, y + h, color);
        c.fill(x, y + 1, x + 1, y + h - 1, color);
        c.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    public static int lerpColor(int a, int b, float t) {
        int aa = a >>> 24, ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255;
        int ba = b >>> 24, br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
             | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    /** Letter-spaced text. Glyph strings and widths are computed once, so rendering allocates nothing. */
    public static final class Spaced {
        private final String[] glyphs;
        private final int[] widths;
        private final int tracking;
        public final int width;

        public Spaced(TextRenderer tr, String text, int tracking) {
            this.tracking = tracking;
            glyphs = new String[text.length()];
            widths = new int[text.length()];
            int total = 0;
            for (int i = 0; i < glyphs.length; i++) {
                glyphs[i] = String.valueOf(text.charAt(i));
                widths[i] = tr.getWidth(glyphs[i]);
                total += widths[i] + (i < glyphs.length - 1 ? tracking : 0);
            }
            width = total;
        }

        public void draw(DrawContext c, TextRenderer tr, int x, int y, int color) {
            for (int i = 0; i < glyphs.length; i++) {
                c.drawText(tr, glyphs[i], x, y, color, false);
                x += widths[i] + tracking;
            }
        }
    }
}
