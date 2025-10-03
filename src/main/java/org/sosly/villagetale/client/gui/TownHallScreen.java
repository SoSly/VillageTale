package org.sosly.villagetale.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.network.NetworkHandler;
import org.sosly.villagetale.network.packets.serverbound.UpdateVillageInfoPacket;

import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class TownHallScreen extends Screen {
    private static final ResourceLocation LEDGER_TEXTURE = new ResourceLocation(VillageTale.MOD_ID, "textures/gui/ledger.png");
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;
    private static final int GUI_WIDTH = 146;
    private static final int GUI_HEIGHT = 180;
    private static final int VERTICAL_OFFSET = 20;
    private static final int HORIZONTAL_OFFSET = 1;

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

        int leftPos = (this.width - GUI_WIDTH) / 2;
        int topPos = (this.height - GUI_HEIGHT) / 2;

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
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        int leftPos = (this.width - GUI_WIDTH) / 2;
        int topPos = (this.height - GUI_HEIGHT) / 2;

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(LEDGER_TEXTURE, leftPos, topPos, VERTICAL_OFFSET, HORIZONTAL_OFFSET, GUI_WIDTH, GUI_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        guiGraphics.drawString(this.font, Component.translatable("villagetale.gui.townhall.village_name_label"), leftPos + 20, topPos + 28, 0x3F3F3F, false);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int underlineY = topPos + 40 + 14;
        guiGraphics.fill(leftPos + 20, underlineY, leftPos + GUI_WIDTH - 20, underlineY + 1, 0xFF3F3F3F);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void onSave() {
        String newName = this.nameField.getValue();
        if (newName == null || newName.trim().isEmpty()) {
            return;
        }

        if (!newName.equals(currentName)) {
            UpdateVillageInfoPacket packet = new UpdateVillageInfoPacket(villageId, newName);
            NetworkHandler.CHANNEL.sendToServer(packet);
        }

        this.onClose();
    }
}
