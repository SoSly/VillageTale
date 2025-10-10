package org.sosly.villagetale.gui.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import org.sosly.villagetale.api.gui.ILedgerPage;
import org.sosly.villagetale.gui.LedgerScreen;

public abstract class AbstractLedgerPage implements ILedgerPage {
    protected int uStart;
    protected int vStart;
    protected final LedgerScreen screen;
    protected final Font font;
    protected final UUID villageId;
    protected static final int LINE_HEIGHT = 12;


    private final List<Renderable> elements;

    public AbstractLedgerPage(LedgerScreen screen, UUID villageId) {
        this.elements = new ArrayList<>();
        this.font = Minecraft.getInstance().font;
        this.screen = screen;
        this.villageId = villageId;
    }

    public <T extends GuiEventListener & Renderable & NarratableEntry> void addWidget(T widget) {
        elements.add(widget);
        screen.pAddWidget(widget);
    }

    public <T extends GuiEventListener & Renderable & NarratableEntry> void addRenderableWidget(T widget) {
        elements.add(widget);
        screen.pAddRenderableWidget(widget);
    }

    @Override
    public void attach(int uStart, int vStart) {
        this.uStart = uStart;
        this.vStart = vStart;
    }

    @Override
    public void detach() {
        elements.forEach((widget) -> {
            screen.pRemoveWidget((GuiEventListener & Renderable & NarratableEntry) widget);
        });
    }
}
