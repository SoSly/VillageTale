package org.sosly.villagetale.client.gui;

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

    public VillageInfoScreen(UUID villageId) {
        super(Component.translatable("villagetale.gui.village_info.title"));
        this.villageId = villageId;
    }

    @Override
    protected void renderLedgerContent(GuiGraphics guiGraphics, int leftPos, int topPos, int mouseX, int mouseY, float partialTick) {
        IVillageCapability village = VillageDataManager.getInstance().getVillageData(villageId);
        if (village == null) {
            guiGraphics.drawString(this.font, Component.literal("No village data available"), leftPos + 20, topPos + 28, 0x3F3F3F, false);
            return;
        }

        int currentY = topPos + 16;
        int lineHeight = 12;

        guiGraphics.drawString(this.font, Component.literal(village.getName()), leftPos + 20, currentY, 0x3F3F3F, false);
        currentY += lineHeight;

        String villagersText = String.format("Villagers: %d", village.getVillagerUUIDs().size());
        guiGraphics.drawString(this.font, Component.literal(villagersText), leftPos + 20, currentY, 0x3F3F3F, false);
        currentY += lineHeight;

        String zonesText = String.format("Zones: %d", village.getZones().size());
        guiGraphics.drawString(this.font, Component.literal(zonesText), leftPos + 20, currentY, 0x3F3F3F, false);
        currentY += lineHeight;

        guiGraphics.drawString(this.font, Component.literal("Owners:"), leftPos + 20, currentY, 0x3F3F3F, false);
        currentY += lineHeight;

        long ownerCount = village.getPlayerPermissions().entrySet().stream()
                .filter(entry -> entry.getValue() == IVillageCapability.Permission.OWNER)
                .count();

        if (ownerCount == 0) {
            guiGraphics.drawString(this.font, Component.literal("- None"), leftPos + 20, currentY, 0x3F3F3F, false);
        } else {
            String ownerText = String.format("- %d owner(s)", ownerCount);
            guiGraphics.drawString(this.font, Component.literal(ownerText), leftPos + 20, currentY, 0x3F3F3F, false);
        }
    }
}
