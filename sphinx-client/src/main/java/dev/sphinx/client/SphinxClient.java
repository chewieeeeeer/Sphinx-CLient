package dev.sphinx.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.Person;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerWarningScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.option.SkinOptionsScreen;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.session.Session;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Entrypoint. The menu itself is installed by {@link dev.sphinx.client.mixin.MinecraftClientMixin}. */
public final class SphinxClient implements ClientModInitializer {
    public static final String MOD_ID = "sphinxclient";
    public static final Logger LOGGER = LoggerFactory.getLogger("SphinxClient");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Sphinx Client {} initialised", version());
    }

    /** Real version from fabric.mod.json (set by mod_version in gradle.properties). */
    public static String version() {
        return FabricLoader.getInstance().getModContainer(MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("dev");
    }

    // ==================== Menu classes (nested so the whole client menu lives in one file) ====================

    /** Colours, texture ids and small drawing helpers shared by all Sphinx screens. */
    public static final class SphinxTheme {
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

    /**
     * Themed button. A real vanilla {@link ButtonWidget} handles clicks, sounds, Tab/Enter keyboard focus and narration;
     * it is registered as a selectable child only (never drawn), and this class draws the Sphinx look on top of it.
     */
    public static final class SphinxButton {
        private final ButtonWidget widget;
        private final Identifier icon;          // may be null
        private final SphinxTheme.Spaced label; // may be null (icon-only button)
        private final boolean primary;          // keeps a dim gold border when idle (the "selected" look)
        private final boolean wide;
        private float hover;

        public SphinxButton(TextRenderer tr, String label, Identifier icon, int x, int y, int w, int h,
                            boolean primary, int tracking, Runnable action) {
            this.widget = ButtonWidget.builder(Text.literal(label == null ? "Settings" : label), b -> action.run())
                    .dimensions(x, y, w, h).build();
            this.icon = icon;
            this.label = label == null ? null : new SphinxTheme.Spaced(tr, label.toUpperCase(), tracking);
            this.primary = primary;
            this.wide = w >= 150;
        }

        public ButtonWidget widget() { return widget; }

        public void render(DrawContext c, TextRenderer tr, int mouseX, int mouseY, float step) {
            boolean active = widget.active;
            boolean over = active && widget.isMouseOver(mouseX, mouseY);
            boolean focused = active && widget.isFocused();
            hover += ((over || focused ? 1f : 0f) - hover) * step;

            int x = widget.getX(), y = widget.getY(), w = widget.getWidth(), h = widget.getHeight();
            boolean iconOnly = label == null;

            if (hover > 0.02f) {                         // soft gold glow
                int a = (int) (hover * 46);
                c.fill(x - 2, y - 2, x + w + 2, y + h + 2, (a << 24) | 0x00D4AF37);
            }
            if (!iconOnly) {
                c.fill(x, y, x + w, y + h, SphinxTheme.lerpColor(SphinxTheme.BG_IDLE, SphinxTheme.BG_HOVER, hover));
                int idle = primary ? SphinxTheme.GOLD_DIM : SphinxTheme.BORDER_IDLE;
                SphinxTheme.drawBorder(c, x, y, w, h, SphinxTheme.lerpColor(idle, SphinxTheme.GOLD, hover));
            } else if (hover > 0.02f) {
                c.fill(x, y, x + w, y + h, SphinxTheme.lerpColor(0x00000000, SphinxTheme.BG_HOVER, hover));
                SphinxTheme.drawBorder(c, x, y, w, h, SphinxTheme.lerpColor(0x00D4AF37, SphinxTheme.GOLD, hover));
            }

            int textColor = active ? SphinxTheme.lerpColor(SphinxTheme.TEXT_DIM, SphinxTheme.TEXT, hover) : SphinxTheme.TEXT_MUTED;
            if (icon != null) {
                int is = iconOnly ? h - 4 : Math.max(8, Math.min(14, h - 8));
                int ix = iconOnly ? x + (w - is) / 2 : x + Math.max(5, h / 3);
                int iy = y + (h - is) / 2;
                SphinxTheme.drawIcon(c, icon, ix, iy, is);
                if (!active) c.fill(ix, iy, ix + is, iy + is, 0x88000000);
            }
            if (!iconOnly) {
                int tx = wide ? x + (int) (w * 0.24f) : x + Math.max(5, h / 3) + Math.max(8, Math.min(14, h - 8)) + 6;
                label.draw(c, tr, tx, y + (h - 8) / 2, textColor);
            }
        }
    }

    /** Base class: themed background, themed button rendering + hover animation, Esc returns to parent. */
    public static abstract class SphinxScreen extends Screen {
        protected final Screen parent;
        private final List<SphinxButton> buttons = new ArrayList<>();
        private long lastNanos = System.nanoTime();
        private final long openedNanos = lastNanos;

        protected SphinxScreen(Text title, Screen parent) {
            super(title);
            this.parent = parent;
        }

        /** Screen.init(client,w,h) clears children first, then calls this. */
        @Override
        protected final void init() {
            buttons.clear();
            SphinxTheme.refreshAssets(client);
            build();
        }

        /** Create widgets/buttons here (replaces init). */
        protected abstract void build();

        /** Draw everything except background and buttons. */
        protected abstract void renderContent(DrawContext c, int mouseX, int mouseY);

        protected float fadeInSeconds() { return 0f; }

        protected final SphinxButton add(SphinxButton b) {
            addSelectableChild(b.widget());   // clickable + keyboard-focusable, but NOT drawn by vanilla
            buttons.add(b);
            return b;
        }

        @Override
        public void renderBackground(DrawContext c, int mouseX, int mouseY, float delta) {
            SphinxTheme.drawBackground(c, width, height);   // no vanilla blur/panorama
        }

        @Override
        public void render(DrawContext c, int mouseX, int mouseY, float delta) {
            super.render(c, mouseX, mouseY, delta);          // background + any vanilla drawables (e.g. text field)
            long now = System.nanoTime();
            float step = Math.min(1f, (now - lastNanos) / 1.0e9f * 14f);
            lastNanos = now;
            for (SphinxButton b : buttons) b.render(c, textRenderer, mouseX, mouseY, step);
            renderContent(c, mouseX, mouseY);
            float fadeSec = fadeInSeconds();
            if (fadeSec > 0f) {
                float f = 1f - Math.min(1f, (now - openedNanos) / (fadeSec * 1.0e9f));
                if (f > 0f) c.fill(0, 0, width, height, ((int) (f * 255) << 24));
            }
        }

        @Override
        public void close() {
            client.setScreen(parent);
        }
    }

    /** The Sphinx Client main menu. */
    public static final class SphinxTitleScreen extends SphinxScreen {
        private static final String SLOGAN = "PLAY DIFFERENT.";

        private SphinxTheme.Spaced sloganText, brandText, pvpText;
        private String versionText = "";
        private String accountName = "Not Logged In";
        private boolean loggedIn;
        private SphinxButton gear, first;

        // layout (computed in build)
        private int logoX, logoY, logoSize, wmX, wmY, wmW, wmH;

        // transient status line (e.g. folder open failed)
        private String status;
        private long statusUntil;

        public SphinxTitleScreen() {
            super(Text.literal("Sphinx Client"), null);
        }

        @Override protected float fadeInSeconds() { return 0.5f; }
        @Override public boolean shouldCloseOnEsc() { return false; }   // like vanilla: Esc does nothing on the title screen
        @Override public void close() { }

        private int s(float k, int v) { return Math.max(1, Math.round(v * k)); }

        @Override
        protected void build() {
            // ---- text caches ----
            sloganText = new SphinxTheme.Spaced(textRenderer, SLOGAN, 1);
            brandText  = new SphinxTheme.Spaced(textRenderer, "SPHINX CLIENT", 1);
            pvpText    = new SphinxTheme.Spaced(textRenderer, "BUILT FOR PVP", 1);
            versionText = "v" + SphinxClient.version();
            String name = client.getSession().getUsername();   // real account name from the game session
            loggedIn = name != null && !name.isBlank();
            accountName = loggedIn ? name : "Not Logged In";

            // ---- layout: scales down on short GUI heights so everything always fits ----
            float k = Math.max(0.8f, Math.min(1f, height / 250f));
            int mainH = Math.max(16, s(k, 22)), smallH = Math.max(15, s(k, 18)), quitH = Math.max(15, s(k, 20));
            int gap = s(k, 3);
            logoSize = s(k, 52);
            wmH = s(k, 30);
            wmW = wmH * 4;
            int total = logoSize + s(k, 2) + wmH + s(k, 8) + 3 * mainH + 2 * gap + gap + 2 * smallH + gap + gap + quitH;
            int top = Math.min((height - total) / 2 - s(k, 4), height - 20 - total);
            int y = Math.max(2, top);

            int mainW = Math.min(230, width - 24);
            int smallW = (mainW - 4) / 2;
            int left = (width - mainW) / 2;
            int right = left + smallW + 4;

            logoX = (width - logoSize) / 2; logoY = y; y += logoSize + s(k, 2);
            wmX = (width - wmW) / 2;        wmY = y;   y += wmH + s(k, 8);

            first = add(new SphinxButton(textRenderer, "Singleplayer", SphinxTheme.ICON_PLAY, left, y, mainW, mainH, true, 2, this::openSingleplayer));
            y += mainH + gap;
            add(new SphinxButton(textRenderer, "Multiplayer", SphinxTheme.ICON_GLOBE, left, y, mainW, mainH, false, 2, this::openMultiplayer));
            y += mainH + gap;
            add(new SphinxButton(textRenderer, "Skin Customization", SphinxTheme.ICON_SHIRT, left, y, mainW, mainH, false, 2, this::openSkins));
            y += mainH + gap + gap;

            add(new SphinxButton(textRenderer, "Mods", SphinxTheme.ICON_GEAR, left, y, smallW, smallH, false, 1,
                    () -> client.setScreen(new SphinxModsScreen(this))));
            add(new SphinxButton(textRenderer, "Open Mod Folder", SphinxTheme.ICON_FOLDER, right, y, smallW, smallH, false, 0, this::openModFolder));
            y += smallH + gap;
            add(new SphinxButton(textRenderer, "Accounts", SphinxTheme.ICON_USER, left, y, smallW, smallH, false, 1,
                    () -> client.setScreen(new SphinxAccountsScreen(this))));
            add(new SphinxButton(textRenderer, "Resource Packs", SphinxTheme.ICON_DOCUMENT, right, y, smallW, smallH, false, 1, this::openResourcePacks));
            y += smallH + gap + gap;

            add(new SphinxButton(textRenderer, "Quit Game", SphinxTheme.ICON_EXIT, left, y, mainW, quitH, false, 2, client::scheduleStop));

            // top-right settings gear
            gear = add(new SphinxButton(textRenderer, null, SphinxTheme.ICON_GEAR, width - 22, 8, 14, 14, false, 0, this::openSettings));

            setInitialFocus(first.widget());
        }

        // ---------- navigation (all vanilla screens, so their full functionality is preserved) ----------

        private void openSingleplayer() { client.setScreen(new SelectWorldScreen(this)); }

        private void openMultiplayer() {
            // Same logic as vanilla: show the one-time warning unless the player has disabled it.
            Screen next = client.options.skipMultiplayerWarning ? new MultiplayerScreen(this) : new MultiplayerWarningScreen(this);
            client.setScreen(next);
        }

        private void openSkins() { client.setScreen(new SkinOptionsScreen(this, client.options)); }

        private void openSettings() { client.setScreen(new OptionsScreen(this, client.options)); }

        private void openResourcePacks() {
            client.setScreen(new PackScreen(
                    client.getResourcePackManager(),
                    manager -> {                                   // same "done" handling as vanilla's options screen
                        client.options.refreshResourcePacks(manager);
                        client.setScreen(this);
                    },
                    client.getResourcePackDir(),
                    Text.translatable("resourcePack.title")));
        }

        private void openModFolder() {
            // Active instance's game dir (not a hard-coded path) -> <gameDir>/mods
            String error = FolderOpener.open(FabricLoader.getInstance().getGameDir().resolve("mods"));
            if (error != null) {
                SphinxClient.LOGGER.warn(error);
                status = "Could not open the mods folder (see log).";
                statusUntil = System.currentTimeMillis() + 4000;
            }
        }

        // ---------- drawing ----------

        @Override
        protected void renderContent(DrawContext c, int mouseX, int mouseY) {
            // Logo + wordmark above the buttons (text fallback if the PNGs are missing)
            if (SphinxTheme.hasLogo()) SphinxTheme.drawLogo(c, logoX, logoY, logoSize);
            if (SphinxTheme.hasWordmark()) {
                SphinxTheme.drawWordmark(c, wmX, wmY, wmW, wmH);
            } else {
                c.drawCenteredTextWithShadow(textRenderer, "S P H I N X", width / 2, wmY + 6, SphinxTheme.TEXT);
                c.drawCenteredTextWithShadow(textRenderer, "C L I E N T", width / 2, wmY + 18, SphinxTheme.TEXT_DIM);
            }

            // Top-right: account indicator (real session name) next to the gear
            int nameW = textRenderer.getWidth(accountName);
            int gx = gear.widget().getX();
            int nx = gx - 8 - nameW;
            SphinxTheme.drawIcon(c, SphinxTheme.ICON_USER, nx - 14, 9, 11);
            c.drawText(textRenderer, accountName, nx, 11, loggedIn ? SphinxTheme.TEXT : SphinxTheme.TEXT_DIM, false);
            c.fill(gx - 4, 9, gx - 3, 21, SphinxTheme.BORDER_IDLE);

            // Bottom-left: logo, brand, version, slogan
            int by = height - 22;
            if (SphinxTheme.hasLogo()) SphinxTheme.drawLogo(c, 8, by - 1, 18);
            c.fill(32, by, 33, by + 16, SphinxTheme.GOLD);
            brandText.draw(c, textRenderer, 38, by, SphinxTheme.TEXT_DIM);
            int vx = 38;
            c.drawText(textRenderer, versionText, vx, by + 9, SphinxTheme.TEXT_MUTED, false);
            sloganText.draw(c, textRenderer, vx + textRenderer.getWidth(versionText) + 8, by + 9, SphinxTheme.GOLD_DIM);

            // Bottom-right
            c.fill(width - pvpText.width - 14, by + 4, width - pvpText.width - 13, by + 14, SphinxTheme.GOLD);
            pvpText.draw(c, textRenderer, width - pvpText.width - 8, by + 5, SphinxTheme.TEXT_DIM);

            // Status line
            if (status != null) {
                if (System.currentTimeMillis() > statusUntil) status = null;
                else c.drawCenteredTextWithShadow(textRenderer, status, width / 2, height - 36, SphinxTheme.ERROR);
            }
        }
    }

    /**
     * Read-only account page. Shows the profile name / UUID of the account this game session was launched with.
     * It never reads, shows or stores access tokens, and it does NOT fake account switching - Minecraft can only
     * change accounts by relaunching with a different session (done by your launcher).
     */
    public static final class SphinxAccountsScreen extends SphinxScreen {
        private String name = "";
        private String uuid = "";

        public SphinxAccountsScreen(Screen parent) {
            super(Text.literal("Accounts"), parent);
        }

        @Override
        protected void build() {
            Session session = client.getSession();
            name = session.getUsername() == null ? "" : session.getUsername();
            uuid = session.getUuidOrNull() == null ? "" : session.getUuidOrNull().toString();
            add(new SphinxButton(textRenderer, "Back", null, (width - 120) / 2, height - 34, 120, 20, true, 2, this::close));
        }

        @Override
        protected void renderContent(DrawContext c, int mouseX, int mouseY) {
            int cx = width / 2, panelW = Math.min(300, width - 24), px = cx - panelW / 2, py = 40, ph = 112;
            c.drawCenteredTextWithShadow(textRenderer, "A C C O U N T S", cx, 18, SphinxTheme.TEXT);
            c.fill(px, py, px + panelW, py + ph, SphinxTheme.BG_IDLE);
            SphinxTheme.drawBorder(c, px, py, panelW, ph, SphinxTheme.BORDER_IDLE);
            SphinxTheme.drawIcon(c, SphinxTheme.ICON_USER, px + 12, py + 12, 24);

            boolean has = !name.isBlank();
            c.drawText(textRenderer, "CURRENT ACCOUNT", px + 46, py + 12, SphinxTheme.TEXT_MUTED, false);
            c.drawText(textRenderer, has ? name : "Not Logged In", px + 46, py + 24, has ? SphinxTheme.TEXT : SphinxTheme.TEXT_DIM, false);
            if (!uuid.isEmpty()) {
                c.drawText(textRenderer, "UUID", px + 12, py + 50, SphinxTheme.TEXT_MUTED, false);
                c.drawText(textRenderer, textRenderer.trimToWidth(uuid, panelW - 24), px + 12, py + 61, SphinxTheme.TEXT_DIM, false);
            }
            c.drawText(textRenderer, "Tokens and passwords are never read or shown.", px + 12, py + 82, SphinxTheme.TEXT_MUTED, false);
            c.drawText(textRenderer, "To switch accounts, relaunch with another account in your launcher.",
                    px + 12, py + 94, SphinxTheme.TEXT_MUTED, false);
        }
    }

    /**
     * Lists installed Fabric mods (from fabric.mod.json metadata) with search + paging.
     * Configuration: if Mod Menu is installed, a button opens ITS screen (that's the only supported config hook);
     * Sphinx does not pretend arbitrary mods are configurable.
     */
    public static final class SphinxModsScreen extends SphinxScreen {
        private record Entry(String id, String name, String version, String desc, String authors, String searchKey) {}

        private static final int ROW_H = 30;
        private final List<Entry> all = new ArrayList<>();
        private final List<Entry> filtered = new ArrayList<>();
        private TextFieldWidget search;
        private SphinxButton prev, next;
        private int page, perPage = 1, listTop, listBottom;
        private String query = "";
        private String status;
        private long statusUntil;

        public SphinxModsScreen(Screen parent) {
            super(Text.literal("Mods"), parent);
            for (ModContainer mc : FabricLoader.getInstance().getAllMods()) {
                if (mc.getContainingMod().isPresent()) continue;            // skip bundled sub-modules (fabric-api internals)
                ModMetadata m = mc.getMetadata();
                if (m.getId().equals("java")) continue;
                StringBuilder authors = new StringBuilder();
                for (Person p : m.getAuthors()) {
                    if (authors.length() > 0) authors.append(", ");
                    authors.append(p.getName());
                }
                String desc = m.getDescription() == null ? "" : m.getDescription().replace('\n', ' ').trim();
                String key = (m.getName() + " " + m.getId() + " " + desc + " " + authors).toLowerCase(Locale.ROOT);
                all.add(new Entry(m.getId(), m.getName(), m.getVersion().getFriendlyString(), desc, authors.toString(), key));
            }
            all.sort(Comparator.comparing(e -> e.name().toLowerCase(Locale.ROOT)));
            filtered.addAll(all);
        }

        @Override
        protected void build() {
            int pw = Math.min(360, width - 24), px = (width - pw) / 2;
            search = new TextFieldWidget(textRenderer, px, 30, pw, 16, Text.literal("Search mods"));
            search.setPlaceholder(Text.literal("Search mods..."));
            search.setText(query);
            search.setChangedListener(this::applyFilter);
            addDrawableChild(search);
            setInitialFocus(search);

            listTop = 52;
            listBottom = height - 40;
            perPage = Math.max(1, (listBottom - listTop) / ROW_H);
            page = Math.min(page, maxPage());

            int by = height - 30;
            prev = add(new SphinxButton(textRenderer, "<", null, px, by, 30, 20, false, 0, () -> { page = Math.max(0, page - 1); }));
            next = add(new SphinxButton(textRenderer, ">", null, px + 34, by, 30, 20, false, 0, () -> { page = Math.min(maxPage(), page + 1); }));
            add(new SphinxButton(textRenderer, "Done", null, px + pw - 80, by, 80, 20, true, 2, this::close));

            boolean modMenu = FabricLoader.getInstance().isModLoaded("modmenu");
            if (modMenu) {
                add(new SphinxButton(textRenderer, "Configure (Mod Menu)", null, px + 70, by, pw - 160 - 6, 20, false, 0, this::openModMenu));
            }
        }

        private int maxPage() { return Math.max(0, (filtered.size() - 1) / perPage); }

        private void applyFilter(String q) {
            query = q;
            String needle = q.toLowerCase(Locale.ROOT).trim();
            filtered.clear();
            for (Entry e : all) if (needle.isEmpty() || e.searchKey().contains(needle)) filtered.add(e);
            page = 0;
        }

        private void openModMenu() {
            try {   // reflection: no compile-time dependency on Mod Menu
                Class<?> cls = Class.forName("com.terraformersmc.modmenu.gui.ModsScreen");
                client.setScreen((Screen) cls.getConstructor(Screen.class).newInstance(this));
            } catch (Throwable t) {
                status = "Could not open Mod Menu.";
                statusUntil = System.currentTimeMillis() + 4000;
            }
        }

        @Override
        protected void renderContent(DrawContext c, int mouseX, int mouseY) {
            int pw = Math.min(360, width - 24), px = (width - pw) / 2;
            c.drawCenteredTextWithShadow(textRenderer, "M O D S", width / 2, 14, SphinxTheme.TEXT);

            int start = page * perPage;
            for (int i = 0; i < perPage && start + i < filtered.size(); i++) {
                Entry e = filtered.get(start + i);
                int y = listTop + i * ROW_H, h = ROW_H - 3;
                boolean over = mouseX >= px && mouseX < px + pw && mouseY >= y && mouseY < y + h;
                c.fill(px, y, px + pw, y + h, over ? SphinxTheme.BG_HOVER : SphinxTheme.BG_IDLE);
                SphinxTheme.drawBorder(c, px, y, pw, h, over ? SphinxTheme.GOLD_DIM : SphinxTheme.BORDER_IDLE);
                String right = e.id() + "  v" + e.version();
                int rw = textRenderer.getWidth(right);
                c.drawText(textRenderer, right, px + pw - rw - 6, y + 4, SphinxTheme.TEXT_MUTED, false);
                String title = e.authors().isEmpty() ? e.name() : e.name() + "  by " + e.authors();
                c.drawText(textRenderer, textRenderer.trimToWidth(title, pw - rw - 24), px + 6, y + 4, SphinxTheme.TEXT, false);
                c.drawText(textRenderer, textRenderer.trimToWidth(e.desc().isEmpty() ? "No description provided." : e.desc(), pw - 12),
                        px + 6, y + 15, SphinxTheme.TEXT_DIM, false);
            }
            if (filtered.isEmpty()) c.drawCenteredTextWithShadow(textRenderer, "No mods match your search.", width / 2, listTop + 12, SphinxTheme.TEXT_DIM);

            prev.widget().active = page > 0;
            next.widget().active = page < maxPage();
            String info = filtered.size() + " mods  -  page " + (page + 1) + "/" + (maxPage() + 1);
            c.drawCenteredTextWithShadow(textRenderer, info, width / 2, listBottom + 2, SphinxTheme.TEXT_MUTED);
            if (status != null) {
                if (System.currentTimeMillis() > statusUntil) status = null;
                else c.drawCenteredTextWithShadow(textRenderer, status, width / 2, listBottom - 10, SphinxTheme.ERROR);
            }
        }
    }

    /** Opens a directory in the OS file explorer. Never throws; returns an error message or null on success. */
    public static final class FolderOpener {
        private FolderOpener() {}

        public static String open(Path dir) {
            try {
                Files.createDirectories(dir);          // handles a missing mods folder
                String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
                String path = dir.toAbsolutePath().toString();
                ProcessBuilder pb;
                if (os.contains("win")) {
                    pb = new ProcessBuilder("explorer.exe", path);
                } else if (os.contains("mac")) {
                    pb = new ProcessBuilder("open", path);
                } else {
                    pb = new ProcessBuilder("xdg-open", path);
                }
                pb.redirectErrorStream(true);
                pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                Process p = pb.start();
                // Reap the helper process so it doesn't linger as a zombie.
                Thread t = new Thread(() -> {
                    try { p.waitFor(); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                }, "sphinx-folder-open");
                t.setDaemon(true);
                t.start();
                return null;
            } catch (IOException | RuntimeException e) {
                return "Could not open folder: " + e.getMessage();
            }
        }
    }
}
