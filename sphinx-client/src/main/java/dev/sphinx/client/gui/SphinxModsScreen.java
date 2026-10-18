package dev.sphinx.client.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.Person;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/**
 * Lists installed Fabric mods (from fabric.mod.json metadata) with search + paging.
 * Configuration: if Mod Menu is installed, a button opens ITS screen (that's the only supported config hook);
 * Sphinx does not pretend arbitrary mods are configurable.
 */
public final class SphinxModsScreen extends SphinxScreen {
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
