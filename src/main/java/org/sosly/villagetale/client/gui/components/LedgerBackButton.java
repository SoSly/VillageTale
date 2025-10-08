package org.sosly.villagetale.client.gui.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.sosly.villagetale.client.gui.AbstractLedgerScreen;

@OnlyIn(Dist.CLIENT)
public class LedgerBackButton extends Button {
    final static int BUTTON_WIDTH = 14;
    final static int BUTTON_HEIGHT = 14;

    public LedgerBackButton(int x, int y, OnPress onPress) {
        super(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int u = 49;
        int v = 190;

        if (this.isHoveredOrFocused()) {
            u = 68;
        }

        guiGraphics.blit(AbstractLedgerScreen.LEDGER_TEXTURE, this.getX(), this.getY(), u, v, BUTTON_WIDTH, BUTTON_HEIGHT);
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        soundManager.play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
    }
}
