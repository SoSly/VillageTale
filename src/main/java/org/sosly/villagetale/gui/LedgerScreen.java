package org.sosly.villagetale.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.gui.ILedgerPage;

@OnlyIn(Dist.CLIENT)
public class LedgerScreen extends Screen {
    public static final ResourceLocation LEDGER_TEXTURE = new ResourceLocation(VillageTale.MOD_ID, "textures/gui/ledger.png");
    public static final int TEXTURE_WIDTH = 384;
    public static final int TEXTURE_HEIGHT = 384;
    public static final int GUI_WIDTH = 293;
    public static final int GUI_HEIGHT = 180;
    protected static final int VERTICAL_OFFSET = 0;
    protected static final int HORIZONTAL_OFFSET = 0;
    public static final int CONTENT_LEFT_START = 20;
    public static final int CONTENT_RIGHT_MARGIN = 20;
    public static final int CONTENT_WIDTH = 116;
    public static final int CONTENT_HEIGHT = GUI_HEIGHT - 20;
    public static final int GUTTER = 21;
    public static final int CONTENT_RIGHT_START = CONTENT_LEFT_START + CONTENT_WIDTH + GUTTER;
    public static final int INDENT_WIDTH = 10;

    protected ILedgerPage leftPage;
    protected ILedgerPage rightPage;

    private int leftPos;
    private int topPos;

    public LedgerScreen(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - GUI_HEIGHT) / 2;

        if (leftPage != null) {
            leftPage.attach(leftPos + CONTENT_LEFT_START, topPos);
        }

        if (rightPage != null) {
            rightPage.attach(leftPos + CONTENT_RIGHT_START, topPos);
        }
    }

    public void setLeftPage(ILedgerPage page) {
        if (this.leftPage != null) {
            this.leftPage.detach();
        }
        this.leftPage = page;
        if (page != null && leftPos != 0) {
            page.attach(leftPos + CONTENT_LEFT_START, topPos);
        }
    }

    public void setRightPage(ILedgerPage page) {
        if (this.rightPage != null) {
            this.rightPage.detach();
        }
        this.rightPage = page;
        if (page != null && leftPos != 0) {
            page.attach(leftPos + CONTENT_RIGHT_START, topPos);
        }
    }

    public <T extends GuiEventListener & NarratableEntry> T pAddWidget(T widget) {
        return this.addWidget(widget);
    }

    public <T extends GuiEventListener & Renderable & NarratableEntry> T pAddRenderableWidget(T widget) {
        return this.addRenderableWidget(widget);
    }

    public <T extends GuiEventListener & Renderable & NarratableEntry> void pRemoveWidget(T widget) {
        this.removeWidget(widget);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(LEDGER_TEXTURE, leftPos, topPos, VERTICAL_OFFSET, HORIZONTAL_OFFSET, GUI_WIDTH, GUI_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        if (leftPage != null) {
            leftPage.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        if (rightPage != null) {
            rightPage.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    public void close() {
        Minecraft mc = getMinecraft();
        if (mc.player == null) {
            return;
        }

        mc.setScreen(null);
    }

    @Override
    public void tick() {
        super.tick();

        if (leftPage != null) {
            leftPage.tick();
        }

        if (rightPage != null) {
            rightPage.tick();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
