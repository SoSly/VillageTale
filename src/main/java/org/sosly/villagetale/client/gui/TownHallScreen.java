package org.sosly.villagetale.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.sosly.villagetale.network.packets.serverbound.UpdateVillageInfo;

import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class TownHallScreen extends AbstractLedgerScreen {
    private final UUID villageId;
    private final String currentName;
    private EditBox nameField;
    private Button saveButton;
    private Button cancelButton;

    public TownHallScreen(UUID villageId, String villageName) {
        super(Component.translatable("villagetale.gui.townhall.title"));
        this.villageId = villageId;
        this.currentName = villageName;
    }

    @Override
    protected void init() {
        super.init();

        int leftPos = getLeftPos();
        int topPos = getTopPos();

        Component nameMessage = Component.translatable("villagetale.gui.townhall.village_name");
        this.nameField = new NoShadowEditBox(this.font, leftPos + 20, topPos + 40,  GUI_WIDTH - 40, 10, nameMessage);
        this.nameField.setMaxLength(64);
        this.nameField.setValue(currentName);
        this.nameField.setBordered(false);
        this.nameField.setTextColor(0x000000);
        this.addRenderableWidget(this.nameField);

        this.saveButton = Button.builder(
            Component.translatable("villagetale.gui.townhall.save"),
            button -> this.onSave()
        )
        .bounds(leftPos + 25, topPos + GUI_HEIGHT - 40, 40, 14)
        .build();
        this.addRenderableWidget(this.saveButton);

        this.cancelButton = Button.builder(
            Component.translatable("villagetale.gui.townhall.cancel"),
            button -> this.onClose()
        )
        .bounds(leftPos + GUI_WIDTH - 65, topPos + GUI_HEIGHT - 40, 40, 14)
        .build();
        this.addRenderableWidget(this.cancelButton);

        this.setInitialFocus(this.nameField);
    }

    @Override
    protected void renderLedgerContent(GuiGraphics guiGraphics, int leftPos, int topPos, int mouseX, int mouseY, float partialTick) {
        guiGraphics.drawString(this.font, Component.translatable("villagetale.gui.townhall.village_name_label"), leftPos + 20, topPos + 28, 0x3F3F3F, false);

        int underlineY = topPos + 40 + 14;
        guiGraphics.fill(leftPos + 20, underlineY, leftPos + GUI_WIDTH - 20, underlineY + 1, 0xFF3F3F3F);
    }

    private void onSave() {
        String newName = this.nameField.getValue();
        if (newName == null || newName.trim().isEmpty()) {
            return;
        }

        if (!newName.equals(currentName)) {
            UpdateVillageInfo.send(villageId, newName);
        }

        this.onClose();
    }
}
