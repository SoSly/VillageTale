package org.sosly.villagetale.gui.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.client.VillageDataManager;
import org.sosly.villagetale.gui.LedgerScreen;
import org.sosly.villagetale.gui.components.LedgerIconButton;
import org.sosly.villagetale.network.packets.serverbound.DeleteZone;

public class ZoneListPage extends AbstractLedgerPage {
    private static final int LIST_TOP = 28;
    private static final int LIST_BOTTOM = 148;
    private static final int ADD_ZONE_BUTTON_SIZE = 9;
    private static final int DELETE_BUTTON_SIZE = 10;

    private ZoneList zoneList;

    public ZoneListPage(LedgerScreen screen, UUID villageId) {
        super(screen, villageId);
    }

    @Override
    public void attach(int uStart, int vStart) {
        super.attach(uStart, vStart);

        IVillageCapability village = VillageDataManager.getInstance().getVillageData(villageId);
        if (village != null) {
            List<IVillageZone> zones = village.getZones();
            this.zoneList = new ZoneList(
                screen.getMinecraft(),
                LedgerScreen.CONTENT_WIDTH,
                LIST_BOTTOM - LIST_TOP,
                vStart + LIST_TOP,
                vStart + LIST_BOTTOM,
                LINE_HEIGHT,
                zones
            );
            this.zoneList.setLeftPos(uStart);
            addWidget(this.zoneList);
        }

        int rightMargin = uStart + LedgerScreen.CONTENT_WIDTH;
        addRenderableWidget(LedgerIconButton.New(
                rightMargin - LedgerIconButton.NEW.width(),
                vStart + 15,
                button -> screen.setRightPage(new AddZonePage(screen, villageId)),
                Component.literal("Add a Zone")
        ));
    }

    @Override
    public void detach() {
        super.detach();
        this.zoneList = null;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        IVillageCapability village = VillageDataManager.getInstance().getVillageData(villageId);
        if (village == null) {
            guiGraphics.drawString(font, Component.translatable("villagetale.gui.error.village_not_found"), uStart, vStart + 28, 0x3F3F3F, false);
            return;
        }

        guiGraphics.drawString(font, Component.translatable("villagetale.gui.zone_list.title"), uStart, vStart + 16, 0, false);

        List<IVillageZone> zones = village.getZones();
        if (zones.isEmpty()) {
            guiGraphics.drawString(font, Component.translatable("villagetale.gui.zone_list.no_zones"), uStart, vStart + 28, 0x3F3F3F, false);
            return;
        }

        if (this.zoneList != null) {
            this.zoneList.updateEntries(zones);
            this.zoneList.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    private void confirmDeleteZone(IVillageZone zone) {
        Minecraft mc = screen.getMinecraft();
        if (mc.player == null) {
            return;
        }

        Component message = Component.translatable("villagetale.gui.zone_list.confirm_delete", zone.getName());
        ConfirmScreen confirmScreen = new ConfirmScreen(
            confirmed -> {
                if (confirmed) {
                    DeleteZone.send(villageId, zone.getUUID());
                }
                mc.setScreen(screen);
            },
            message,
            Component.empty()
        );
        mc.setScreen(confirmScreen);
    }

    private class ZoneList extends ObjectSelectionList<ZoneList.Entry> {
        private List<IVillageZone> zones;

        public ZoneList(Minecraft minecraft, int width, int height, int y, int bottom, int itemHeight, List<IVillageZone> zones) {
            super(minecraft, width, height, y, bottom, itemHeight);

            this.zones = new ArrayList<>(zones);
            this.zones.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            this.setRenderBackground(false);
            this.setRenderTopAndBottom(false);
            this.setRenderSelection(false);
            for (int i = 0; i < this.zones.size(); i++) {
                IVillageZone sortedZone = this.zones.get(i);
                int realIndex = zones.indexOf(sortedZone);
                this.addEntry(new Entry(sortedZone, realIndex));
            }
        }

        public void updateEntries(List<IVillageZone> newZones) {
            if (newZones.size() != zones.size()) {
                this.clearEntries();
                this.zones = new ArrayList<>(newZones);
                this.zones.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                for (int i = 0; i < this.zones.size(); i++) {
                    IVillageZone sortedZone = this.zones.get(i);
                    int realIndex = newZones.indexOf(sortedZone);
                    this.addEntry(new Entry(sortedZone, realIndex));
                }
            }
        }

        @Override
        public int getRowWidth() {
            return LedgerScreen.CONTENT_WIDTH;
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

                this.nameWidth = font.width(nameComponent);
                guiGraphics.drawString(font, nameComponent, nameX, nameY, 0, false);

                if (!isTownHall) {
                    if (this.deleteButton == null) {
                        this.deleteButton = LedgerIconButton.Delete(
                            left + LedgerScreen.CONTENT_WIDTH - DELETE_BUTTON_SIZE,
                            top,
                            button -> confirmDeleteZone(zone),
                            Component.translatable("villagetale.gui.zone_list.delete")
                        );
                    } else {
                        this.deleteButton.setX(left + LedgerScreen.CONTENT_WIDTH - DELETE_BUTTON_SIZE);
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
                    screen.setLeftPage(new ZoneInfoPage(screen, villageId, zoneIndex));
                    screen.setRightPage(new ZoneVillagersPage(screen, villageId, zoneIndex));
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
