package org.sosly.villagetale.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.client.VillageDataManager;

import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class VillageInfoScreen extends AbstractLedgerScreen {
    private final UUID villageId;
    private int zonesTextX;
    private int zonesTextY;
    private int zonesTextWidth;

    public VillageInfoScreen(UUID villageId) {
        super(Component.translatable("villagetale.gui.village_info.title"));
        this.villageId = villageId;
    }

    @Override
    protected void renderLedgerContent(GuiGraphics guiGraphics, int leftPos, int topPos, int mouseX, int mouseY, float partialTick) {
        IVillageCapability village = VillageDataManager.getInstance().getVillageData(villageId);
        if (village == null) {
            guiGraphics.drawString(this.font, Component.translatable("villagetale.gui.village_info.no_data"), leftPos + CONTENT_LEFT_MARGIN, topPos + 28, 0x3F3F3F, false);
            return;
        }

        int currentY = topPos + 16;
        int lineHeight = 12;

        guiGraphics.drawString(this.font, Component.literal(village.getName()), leftPos + CONTENT_LEFT_MARGIN, currentY, 0x3F3F3F, false);
        currentY += lineHeight;

        Component villagersText = Component.translatable("villagetale.gui.village_info.villagers", village.getVillagerUUIDs().size());
        guiGraphics.drawString(this.font, villagersText, leftPos + CONTENT_LEFT_MARGIN, currentY, 0x3F3F3F, false);
        currentY += lineHeight;

        Component zonesText = Component.translatable("villagetale.gui.village_info.zones", village.getZones().size())
                .withStyle(ChatFormatting.BLUE)
                .withStyle(ChatFormatting.UNDERLINE);
        zonesTextX = leftPos + CONTENT_LEFT_MARGIN;
        zonesTextY = currentY;
        zonesTextWidth = this.font.width(zonesText);
        guiGraphics.drawString(this.font, zonesText, zonesTextX, zonesTextY, 0x3F3F3F, false);
        currentY += lineHeight;

        guiGraphics.drawString(this.font, Component.translatable("villagetale.gui.village_info.owners"), leftPos + CONTENT_LEFT_MARGIN, currentY, 0x3F3F3F, false);
        currentY += lineHeight;

        long ownerCount = village.getPlayerPermissions().entrySet().stream()
                .filter(entry -> entry.getValue() == IVillageCapability.Permission.OWNER)
                .count();

        if (ownerCount == 0) {
            guiGraphics.drawString(this.font, Component.translatable("villagetale.gui.village_info.no_owners"), leftPos + CONTENT_LEFT_MARGIN, currentY, 0x3F3F3F, false);
        } else {
            guiGraphics.drawString(this.font, Component.translatable("villagetale.gui.village_info.owner_count", ownerCount), leftPos + CONTENT_LEFT_MARGIN, currentY, 0x3F3F3F, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (isMouseOverZonesText(mouseX, mouseY)) {
            openZoneDetailScreen();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isMouseOverZonesText(double mouseX, double mouseY) {
        if (zonesTextWidth == 0) {
            return false;
        }

        int lineHeight = 12;
        return mouseX >= zonesTextX && mouseX <= zonesTextX + zonesTextWidth &&
               mouseY >= zonesTextY && mouseY <= zonesTextY + lineHeight;
    }

    private void openZoneDetailScreen() {
        IVillageCapability village = VillageDataManager.getInstance().getVillageData(villageId);
        if (village == null || village.getZones().isEmpty()) {
            return;
        }

        if (this.minecraft != null) {
            this.minecraft.setScreen(new ZoneDetailScreen(villageId, 0));
        }
    }
}
