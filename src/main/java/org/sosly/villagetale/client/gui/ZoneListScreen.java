package org.sosly.villagetale.client.gui;

import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.client.VillageDataManager;
import org.sosly.villagetale.client.gui.components.LedgerIconButton;
import org.sosly.villagetale.network.packets.serverbound.DeleteZone;

@OnlyIn(Dist.CLIENT)
public class ZoneListScreen extends AbstractLedgerScreen {
    private static final int LIST_TOP = 28;
    private static final int LIST_BOTTOM = 148;
    private static final int LINE_HEIGHT = 12;
    private static final int ADD_ZONE_BUTTON_SIZE = 9;
    private static final int DELETE_BUTTON_SIZE = 10;

    private final UUID villageId;
    private ZoneList zoneList;
    private LedgerIconButton backButton;
    private LedgerIconButton addZoneButton;

    public ZoneListScreen(UUID villageId) {
        super(Component.translatable("villagetale.gui.zone_list.title"));
        this.villageId = villageId;
    }

    @Override
    protected void init() {
        super.init();

        int leftPos = getLeftPos();
        int topPos = getTopPos();

        IVillageCapability village = VillageDataManager.getInstance().getVillageData(villageId);
        if (village != null) {
            List<IVillageZone> zones = village.getZones();
            this.zoneList = new ZoneList(
                this.minecraft,
                CONTENT_WIDTH,
                LIST_BOTTOM - LIST_TOP,
                topPos + LIST_TOP,
                topPos + LIST_BOTTOM,
                LINE_HEIGHT,
                zones
            );
            this.zoneList.setLeftPos(leftPos + CONTENT_LEFT_MARGIN);
            this.addWidget(this.zoneList);
        }

        this.backButton = this.addRenderableWidget(LedgerIconButton.Back(
            leftPos + CONTENT_LEFT_MARGIN + (CONTENT_WIDTH - 14) / 2,
            topPos + 153,
            button -> returnToVillageInfo(),
            Component.translatable("villagetale.gui.back")
        ));

        this.addZoneButton = this.addRenderableWidget(LedgerIconButton.New(
            leftPos + CONTENT_LEFT_MARGIN + (CONTENT_WIDTH - ADD_ZONE_BUTTON_SIZE) / 2,
            topPos + 15,
            button -> openAddZoneScreen(),
            Component.translatable("villagetale.gui.add_zone.title")
        ));
    }

    @Override
    protected void renderLedgerContent(GuiGraphics guiGraphics, int leftPos, int topPos, int mouseX, int mouseY, float partialTick) {
        IVillageCapability village = VillageDataManager.getInstance().getVillageData(villageId);
        if (village == null) {
            guiGraphics.drawString(this.font, Component.translatable("villagetale.gui.error.village_not_found"), leftPos + CONTENT_LEFT_MARGIN, topPos + 28, 0x3F3F3F, false);
            return;
        }

        guiGraphics.drawString(this.font, Component.translatable("villagetale.gui.zone_list.title"), leftPos + CONTENT_LEFT_MARGIN, topPos + 16, 0, false);

        List<IVillageZone> zones = village.getZones();
        if (zones.isEmpty()) {
            guiGraphics.drawString(this.font, Component.translatable("villagetale.gui.zone_list.no_zones"), leftPos + CONTENT_LEFT_MARGIN, topPos + 28, 0x3F3F3F, false);
            return;
        }

        if (this.zoneList != null) {
            this.zoneList.updateEntries(zones);
            this.zoneList.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    private void returnToVillageInfo() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new VillageInfoScreen(villageId));
        }
    }

    private void openZoneDetail(int zoneIndex) {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new ZoneDetailScreen(villageId, zoneIndex));
        }
    }

    private void confirmDeleteZone(IVillageZone zone) {
        if (this.minecraft == null) {
            return;
        }

        Component message = Component.translatable("villagetale.gui.zone_list.confirm_delete", zone.getName());
        ConfirmScreen confirmScreen = new ConfirmScreen(
            confirmed -> {
                if (confirmed) {
                    DeleteZone.send(villageId, zone.getUUID());
                }
                if (this.minecraft != null) {
                    this.minecraft.setScreen(new ZoneListScreen(villageId));
                }
            },
            message,
            Component.empty()
        );
        this.minecraft.setScreen(confirmScreen);
    }

    private void openAddZoneScreen() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new AddZoneScreen(villageId, this));
        }
    }

    private class ZoneList extends ObjectSelectionList<ZoneList.Entry> {
        private List<IVillageZone> zones;

        public ZoneList(net.minecraft.client.Minecraft minecraft, int width, int height, int y, int bottom, int itemHeight, List<IVillageZone> zones) {
            super(minecraft, width, height, y, bottom, itemHeight);
            this.zones = zones;
            this.setRenderBackground(false);
            this.setRenderTopAndBottom(false);
            this.setRenderSelection(false);
            for (int i = 0; i < zones.size(); i++) {
                this.addEntry(new Entry(zones.get(i), i));
            }
        }

        public void updateEntries(List<IVillageZone> newZones) {
            if (newZones.size() != zones.size()) {
                this.clearEntries();
                this.zones = newZones;
                for (int i = 0; i < zones.size(); i++) {
                    this.addEntry(new Entry(zones.get(i), i));
                }
            }
        }

        @Override
        public int getRowWidth() {
            return CONTENT_WIDTH;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getRowLeft() + this.getRowWidth() - 6;
        }

        public class Entry extends ObjectSelectionList.Entry<Entry> {
            private final IVillageZone zone;
            private final int zoneIndex;
            private LedgerIconButton deleteButton;
            private int nameWidth;
            private int nameX;
            private int nameY;

            public Entry(IVillageZone zone, int zoneIndex) {
                this.zone = zone;
                this.zoneIndex = zoneIndex;
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
                this.nameX = left;
                this.nameY = top;

                boolean isTownHall = zone.getType() != null && "townhall".equals(zone.getType().getID().getPath());

                Component nameComponent = Component.literal(zone.getName());
                boolean nameHovered = isMouseOverName(mouseX, mouseY);
                if (nameHovered) {
                    nameComponent = nameComponent.copy().withStyle(ChatFormatting.UNDERLINE);
                }
                nameComponent = nameComponent.copy().withStyle(ChatFormatting.BLUE);

                this.nameWidth = ZoneListScreen.this.font.width(nameComponent);
                guiGraphics.drawString(ZoneListScreen.this.font, nameComponent, nameX, nameY, 0, false);

                if (!isTownHall) {
                    if (this.deleteButton == null) {
                        this.deleteButton = LedgerIconButton.Delete(
                            left + CONTENT_WIDTH - DELETE_BUTTON_SIZE,
                            top,
                            button -> confirmDeleteZone(zone),
                            Component.translatable("villagetale.gui.zone_list.delete")
                        );
                    } else {
                        this.deleteButton.setX(left + CONTENT_WIDTH - DELETE_BUTTON_SIZE);
                        this.deleteButton.setY(top);
                    }
                    this.deleteButton.render(guiGraphics, mouseX, mouseY, partialTick);
                }
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button != 0) {
                    return false;
                }

                boolean isTownHall = zone.getType() != null && "townhall".equals(zone.getType().getID().getPath());
                if (!isTownHall && this.deleteButton != null && this.deleteButton.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }

                if (isMouseOverName(mouseX, mouseY)) {
                    openZoneDetail(zoneIndex);
                    return true;
                }

                return false;
            }

            @Override
            public Component getNarration() {
                return Component.literal(zone.getName());
            }

            private boolean isMouseOverName(double mouseX, double mouseY) {
                if (this.nameWidth == 0) {
                    return false;
                }
                return mouseX >= nameX && mouseX <= nameX + nameWidth &&
                       mouseY >= nameY && mouseY <= nameY + LINE_HEIGHT;
            }
        }
    }
}
