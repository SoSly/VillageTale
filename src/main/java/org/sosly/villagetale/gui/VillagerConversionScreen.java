package org.sosly.villagetale.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.sosly.villagetale.event.VillagerInteractionHandler;
import org.sosly.villagetale.helper.I18nHelper;
import org.sosly.villagetale.network.packets.serverbound.ConvertVillager;

@OnlyIn(Dist.CLIENT)
public class VillagerConversionScreen extends Screen {
    private static final int EMERALD_COST = 10;
    private static final int GUI_WIDTH = 200;
    private static final int GUI_HEIGHT = 120;
    private static final int CONTENT_PADDING = 20;

    private final int villagerEntityId;
    private int leftPos;
    private int topPos;

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

        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - GUI_HEIGHT) / 2;

        Button commit = Button.builder(
                        Component.translatable("villagetale.gui.conversion.commit"),
                        button -> this.onPurchase()
                )
                .bounds(leftPos + CONTENT_PADDING, topPos + GUI_HEIGHT - 34, 40, 14)
                .build();
        this.addRenderableWidget(commit);

        Button cancel = Button.builder(
                        Component.translatable("villagetale.gui.conversion.cancel"),
                        button -> this.onClose()
                )
                .bounds(leftPos + GUI_WIDTH - CONTENT_PADDING - 40, topPos + GUI_HEIGHT - 34, 40, 14)
                .build();
        this.addRenderableWidget(cancel);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        Component itemName = Items.EMERALD.getDescription();
        Component message = I18nHelper.translate("villagetale.gui.conversion.message", EMERALD_COST, itemName.getString());

        int maxWidth = GUI_WIDTH - (CONTENT_PADDING * 2);
        var wrappedLines = this.font.split(message, maxWidth);

        int y = topPos + CONTENT_PADDING;
        for (var line : wrappedLines) {
            guiGraphics.drawString(this.font, line, leftPos + CONTENT_PADDING, y, 0xFFFFFF, false);
            y += this.font.lineHeight + 2;
        }
    }

    private void onPurchase() {
        ConvertVillager.send(villagerEntityId);
        this.onClose();
    }
}
