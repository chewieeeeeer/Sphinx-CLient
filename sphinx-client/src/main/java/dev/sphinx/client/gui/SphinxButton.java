package dev.sphinx.client.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Themed button. A real vanilla {@link ButtonWidget} handles clicks, sounds, Tab/Enter keyboard focus and narration;
 * it is registered as a selectable child only (never drawn), and this class draws the Sphinx look on top of it.
 */
public final class SphinxButton {
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
