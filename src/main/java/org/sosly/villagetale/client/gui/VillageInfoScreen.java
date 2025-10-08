package org.sosly.villagetale.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.sosly.villagetale.client.VillageDataManager;
import org.sosly.villagetale.client.data.ClientVillageData;

import java.util.List;
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
        ClientVillageData village = VillageDataManager.getInstance().getVillageData(villageId);
        if (village == null) {
            guiGraphics.drawString(this.font, Component.literal("No village data available"), leftPos + 20, topPos + 28, 0x3F3F3F, false);
            return;
        }

        int currentY = topPos + 16;
        int lineHeight = 12;

        guiGraphics.drawString(this.font, Component.literal(village.getVillageName()), leftPos + 20, currentY, 0x3F3F3F, false);
        currentY += lineHeight;

        String positionText = String.format("At: %d, %d [%d]",
                village.getStartingChunk().x,
                village.getStartingChunk().z,
                village.getSquadius());
        guiGraphics.drawString(this.font, Component.literal(positionText), leftPos + 20, currentY, 0x3F3F3F, false);
        currentY += lineHeight;

        String villagersText = String.format("Villagers: %d", village.getVillagerUUIDs().size());
        guiGraphics.drawString(this.font, Component.literal(villagersText), leftPos + 20, currentY, 0x3F3F3F, false);
        currentY += lineHeight;

        String zonesText = String.format("Zones: %d", village.getZones().size());
        guiGraphics.drawString(this.font, Component.literal(zonesText), leftPos + 20, currentY, 0x3F3F3F, false);
        currentY += lineHeight;

        guiGraphics.drawString(this.font, Component.literal("Owners:"), leftPos + 20, currentY, 0x3F3F3F, false);
        currentY += lineHeight;

        List<String> owners = village.getOwnerNames();
        if (owners.isEmpty()) {
            guiGraphics.drawString(this.font, Component.literal("- None"), leftPos + 20, currentY, 0x3F3F3F, false);
        } else {
            for (String ownerName : owners) {
                String ownerText = "- " + ownerName;
                guiGraphics.drawString(this.font, Component.literal(ownerText), leftPos + 20, currentY, 0x3F3F3F, false);
                currentY += lineHeight;
            }
        }
    }
}
