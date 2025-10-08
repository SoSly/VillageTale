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
public class LedgerPageButton extends Button {
    private final boolean isForward;

    public LedgerPageButton(int x, int y, boolean isForward, OnPress onPress) {
        super(x, y, 23, 13, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
        this.isForward = isForward;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int u = 0;
        int v = 192;

        if (this.isHoveredOrFocused()) {
            u += 23;
        }

        if (!this.isForward) {
            v += 13;
        }

        guiGraphics.blit(AbstractLedgerScreen.LEDGER_TEXTURE, this.getX(), this.getY(), u, v, 23, 13);
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        soundManager.play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
    }
}
