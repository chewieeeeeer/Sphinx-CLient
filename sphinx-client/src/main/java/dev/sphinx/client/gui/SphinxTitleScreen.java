package dev.sphinx.client.gui;

import dev.sphinx.client.SphinxClient;
import dev.sphinx.client.util.FolderOpener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerWarningScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.option.SkinOptionsScreen;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;

/** The Sphinx Client main menu. */
public final class SphinxTitleScreen extends SphinxScreen {
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
