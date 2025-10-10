package org.sosly.villagetale.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.sosly.villagetale.event.VillagerInteractionHandler;
import org.sosly.villagetale.network.packets.serverbound.ConvertVillager;

@OnlyIn(Dist.CLIENT)
public class VillagerConversionScreen extends AbstractLedgerScreen {
    private static final int EMERALD_COST = 10;

    private final int villagerEntityId;

    public VillagerConversionScreen(int villagerEntityId) {
        super(Component.translatable("villagetale.gui.conversion.title"));
        this.villagerEntityId = villagerEntityId;
    }

    @Override
    public void onClose() {
        VillagerInteractionHandler.releaseVillager(villagerEntityId);
        super.onClose();
    }

    @Override
    protected void init() {
        super.init();

        int leftPos = getLeftPos();
        int topPos = getTopPos();

        Button commit = Button.builder(
                        Component.translatable("villagetale.gui.conversion.commit"),
                        button -> this.onPurchase()
                )
                .bounds(leftPos + CONTENT_LEFT_MARGIN, topPos + GUI_HEIGHT - 40, 40, 14)
                .build();
        this.addRenderableWidget(commit);

        Button cancel = Button.builder(
                        Component.translatable("villagetale.gui.conversion.cancel"),
                        button -> this.onClose()
                )
                .bounds(leftPos + CONTENT_LEFT_MARGIN + CONTENT_WIDTH - 40, topPos + GUI_HEIGHT - 40, 40, 14)
                .build();
        this.addRenderableWidget(cancel);
    }

    @Override
    protected void renderLedgerContent(GuiGraphics guiGraphics, int leftPos, int topPos, int mouseX, int mouseY, float partialTick) {
        Component itemName = Items.EMERALD.getDescription();
        Component message = Component.translatable("villagetale.gui.conversion.message", EMERALD_COST, itemName);

        int maxWidth = CONTENT_WIDTH;
        var wrappedLines = this.font.split(message, maxWidth);

        int y = topPos + 28;
        for (var line : wrappedLines) {
            guiGraphics.drawString(this.font, line, leftPos + CONTENT_LEFT_MARGIN, y, 0x3F3F3F, false);
            y += this.font.lineHeight + 2;
        }
    }

    private void onPurchase() {
        ConvertVillager.send(villagerEntityId);
        this.onClose();
    }
}
