package org.sosly.villagetale.client.gui.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.sosly.villagetale.client.gui.AbstractLedgerScreen;

@OnlyIn(Dist.CLIENT)
public class LedgerIconButton extends Button {
    private final int iconU;
    private final int iconV;
    private final int iconSize;
    private final int hoverShift;

    public LedgerIconButton(int x, int y, int iconU, int iconV, int iconSize, int hoverShift, OnPress onPress, Component message) {
        super(x, y, iconSize, iconSize, message, onPress, DEFAULT_NARRATION);
        this.iconU = iconU;
        this.iconV = iconV;
        this.iconSize = iconSize;
        this.hoverShift = hoverShift;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
//        guiGraphics.fill(this.getX(), this.getY(), this.getX() + iconSize, this.getY() + iconSize, 0xFFFF0000);
        guiGraphics.blit(AbstractLedgerScreen.LEDGER_TEXTURE, this.getX(), this.getY(),
                this.isHoveredOrFocused() ? iconU + hoverShift : iconU,
                iconV, iconSize, iconSize);
    }
}
