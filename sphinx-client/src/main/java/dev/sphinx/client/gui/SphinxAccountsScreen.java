package dev.sphinx.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.session.Session;
import net.minecraft.text.Text;

/**
 * Read-only account page. Shows the profile name / UUID of the account this game session was launched with.
 * It never reads, shows or stores access tokens, and it does NOT fake account switching - Minecraft can only
 * change accounts by relaunching with a different session (done by your launcher).
 */
public final class SphinxAccountsScreen extends SphinxScreen {
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
