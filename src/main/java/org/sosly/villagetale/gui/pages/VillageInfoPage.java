package org.sosly.villagetale.gui.pages;

import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.client.VillageDataManager;
import org.sosly.villagetale.gui.LedgerScreen;
import org.sosly.villagetale.gui.components.LedgerIconButton;
import org.sosly.villagetale.gui.components.NoShadowEditBox;
import org.sosly.villagetale.helper.I18nHelper;
import org.sosly.villagetale.network.packets.serverbound.UpdateVillageInfo;

public class VillageInfoPage extends AbstractLedgerPage {
    private static final int TEXT_Y_OFFSET = 16;
    private static final int EDIT_ICON_SIZE = 10;

    private boolean isEditingName;
    private IVillageCapability village;
    private NoShadowEditBox nameEditBox;

    public VillageInfoPage(LedgerScreen screen, UUID villageId) {
        super(screen, villageId);
        this.isEditingName = false;
    }

    @Override
    public void attach(int uStart, int vStart) {
        super.attach(uStart, vStart);

        this.village = VillageDataManager.getInstance().getVillageData(villageId);

        addRenderableWidget(LedgerIconButton.edit(
            uStart + LedgerScreen.CONTENT_WIDTH - EDIT_ICON_SIZE,
            vStart + TEXT_Y_OFFSET - 1,
            button -> toggleNameEditing(),
            Component.literal("Edit Name")
        ));

        this.nameEditBox = new NoShadowEditBox(
            font,
            uStart,
            vStart + TEXT_Y_OFFSET,
            95,
            10,
            Component.literal("Village Name")
        );
        this.nameEditBox.setMaxLength(64);
        this.nameEditBox.setBordered(false);
        this.nameEditBox.setTextColor(0x000000);
        this.nameEditBox.setVisible(false);
        addRenderableWidget(this.nameEditBox);
    }

    @Override
    public void detach() {
        super.detach();
        this.village = null;
        this.nameEditBox = null;
        this.isEditingName = false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        IVillageCapability currentVillage = VillageDataManager.getInstance().getVillageData(villageId);
        if (currentVillage == null) {
            guiGraphics.drawString(font, Component.translatable("villagetale.gui.village_info.no_data"), uStart, vStart + 28, 0x3F3F3F, false);
            return;
        }

        int currentY = vStart + TEXT_Y_OFFSET;

        if (!isEditingName) {
            guiGraphics.drawString(font, Component.literal(currentVillage.getName()), uStart, currentY, 0, false);
        }
        currentY += LINE_HEIGHT + 1;

        Component villagersText = Component.translatable("villagetale.gui.village_info.villagers", currentVillage.getVillagerUUIDs().size());
        guiGraphics.drawString(font, villagersText, uStart, currentY, 0x3F3F3F, false);
        currentY += LINE_HEIGHT;

        Component zonesText = Component.translatable("villagetale.gui.village_info.zones", currentVillage.getZones().size());
        guiGraphics.drawString(font, zonesText, uStart, currentY, 0x3F3F3F, false);
        currentY += LINE_HEIGHT;

        guiGraphics.drawString(font, Component.translatable("villagetale.gui.village_info.owners"), uStart, currentY, 0x3F3F3F, false);
        currentY += LINE_HEIGHT;

        long ownerCount = currentVillage.getPlayerPermissions().entrySet().stream()
            .filter(entry -> entry.getValue() == IVillageCapability.Permission.OWNER)
            .count();

        Component owners = ownerCount > 0 ? I18nHelper.translate("villagetale.gui.village_info.owner_count", ownerCount) : Component.translatable("villagetale.gui.village_info.no_owners");
        guiGraphics.drawString(font, owners, uStart, currentY, 0x3F3F3F, false);
    }

    private void toggleNameEditing() {
        if (village == null) {
            return;
        }

        if (!isEditingName) {
            this.nameEditBox.setValue(village.getName());
            this.nameEditBox.setVisible(true);
            this.nameEditBox.setFocused(true);
            this.nameEditBox.moveCursorToEnd();
            this.isEditingName = true;
        } else {
            String newName = this.nameEditBox.getValue().trim();
            if (!newName.isEmpty() && !newName.equals(village.getName())) {
                UpdateVillageInfo.send(villageId, newName);
            }
            this.nameEditBox.setVisible(false);
            this.nameEditBox.setFocused(false);
            this.isEditingName = false;
        }
    }
}
