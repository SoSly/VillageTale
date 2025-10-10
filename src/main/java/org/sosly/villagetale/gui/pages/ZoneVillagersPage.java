package org.sosly.villagetale.gui.pages;

import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.client.VillageDataManager;
import org.sosly.villagetale.gui.LedgerScreen;

public class ZoneVillagersPage extends AbstractLedgerPage {
    private static final int TEXT_Y_OFFSET = 16;

    private final int zoneIndex;

    public ZoneVillagersPage(LedgerScreen screen, UUID villageId, int zoneIndex) {
        super(screen, villageId);
        this.zoneIndex = zoneIndex;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        IVillageCapability village = VillageDataManager.getInstance().getVillageData(villageId);
        if (village == null) {
            return;
        }

        List<IVillageZone> zones = village.getZones();
        if (zoneIndex < 0 || zoneIndex >= zones.size()) {
            return;
        }

        IVillageZone zone = zones.get(zoneIndex);
        int currentY = vStart + TEXT_Y_OFFSET;

        List<UUID> assignedVillagers = zone.getAssignedVillagers();
        int count = assignedVillagers.size();
        String villagerText = "Villagers: " + count;
        guiGraphics.drawString(font, Component.literal(villagerText), uStart, currentY, 0, false);
    }
}
