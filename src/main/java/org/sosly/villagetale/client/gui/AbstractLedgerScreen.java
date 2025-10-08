package org.sosly.villagetale.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.sosly.villagetale.VillageTale;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractLedgerScreen extends Screen {
    public static final ResourceLocation LEDGER_TEXTURE = new ResourceLocation(VillageTale.MOD_ID, "textures/gui/ledger.png");
    protected static final int TEXTURE_WIDTH = 256;
    protected static final int TEXTURE_HEIGHT = 256;
    protected static final int GUI_WIDTH = 146;
    protected static final int GUI_HEIGHT = 180;
    protected static final int VERTICAL_OFFSET = 20;
    protected static final int HORIZONTAL_OFFSET = 1;
    protected static final int CONTENT_LEFT_MARGIN = 15;
    protected static final int CONTENT_WIDTH = 116;
    protected static final int INDENT_WIDTH = 10;

    protected AbstractLedgerScreen(Component title) {
        super(title);
    }

    protected int getLeftPos() {
        return (this.width - GUI_WIDTH) / 2;
    }

    protected int getTopPos() {
        return (this.height - GUI_HEIGHT) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        int leftPos = getLeftPos();
        int topPos = getTopPos();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(LEDGER_TEXTURE, leftPos, topPos, VERTICAL_OFFSET, HORIZONTAL_OFFSET, GUI_WIDTH, GUI_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        renderLedgerContent(guiGraphics, leftPos, topPos, mouseX, mouseY, partialTick);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    protected abstract void renderLedgerContent(GuiGraphics guiGraphics, int leftPos, int topPos, int mouseX, int mouseY, float partialTick);

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
