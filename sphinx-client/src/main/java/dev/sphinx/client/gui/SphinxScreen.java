package dev.sphinx.client.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Base class: themed background, themed button rendering + hover animation, Esc returns to parent. */
public abstract class SphinxScreen extends Screen {
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
