package org.sosly.villagetale.gui.pages;

import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.client.VillageDataManager;
import org.sosly.villagetale.gui.LedgerScreen;
import org.sosly.villagetale.gui.components.LedgerIconButton;
import org.sosly.villagetale.gui.components.NoShadowEditBox;
import org.sosly.villagetale.network.packets.serverbound.UpdateZoneName;
import org.sosly.villagetale.zone.shape.Box;
import org.sosly.villagetale.zone.shape.Cylinder;
import org.sosly.villagetale.zone.shape.Point;
import org.sosly.villagetale.zone.shape.Route;

public class ZoneInfoPage extends AbstractLedgerPage {
    private static final int TEXT_Y_OFFSET = 16;

    private final int zoneIndex;
    private boolean isEditingName;
    private IVillageCapability village;
    private IVillageZone zone;
    private NoShadowEditBox nameEditBox;

    public ZoneInfoPage(LedgerScreen screen, UUID villageId, int zoneIndex) {
        super(screen, villageId);
        this.isEditingName = false;
        this.zoneIndex = zoneIndex;
    }

    @Override
    public void attach(int uStart, int vStart) {
        super.attach(uStart, vStart);

        this.village = VillageDataManager.getInstance().getVillageData(villageId);
        if (village != null) {
            List<IVillageZone> zones = village.getZones();
            if (zoneIndex >= 0 && zoneIndex < zones.size()) {
                this.zone = zones.get(zoneIndex);
            }
        }

        addRenderableWidget(LedgerIconButton.Edit(
            uStart + LedgerScreen.CONTENT_WIDTH - LedgerIconButton.EDIT.width(),
            vStart + TEXT_Y_OFFSET - 1,
            button -> toggleNameEditing(),
            Component.literal("Edit Name")
        ));

        addFilterWidget();
        addNavigationButtons();
        addBackButton();

        this.nameEditBox = new NoShadowEditBox(
            font,
            uStart,
            vStart + TEXT_Y_OFFSET,
            95,
            10,
            Component.literal("Zone Name")
        );
        this.nameEditBox.setMaxLength(24);
        this.nameEditBox.setBordered(false);
        this.nameEditBox.setTextColor(0x000000);
        this.nameEditBox.setVisible(false);
        addRenderableWidget(this.nameEditBox);
    }

    @Override
    public void detach() {
        super.detach();
        this.village = null;
        this.zone = null;
        this.nameEditBox = null;
        this.isEditingName = false;
    }

    private void addFilterWidget() {
        if (zone == null) {
            return;
        }

        boolean supportsItemFilters = zone.getType() != null && zone.getType().supportsItemFilters();
        boolean supportsEntityFilters = zone.getType() != null && zone.getType().supportsEntityFilters();

        if (!supportsItemFilters && !supportsEntityFilters) {
            return;
        }

        addRenderableWidget(LedgerIconButton.Edit(
            uStart + LedgerScreen.CONTENT_WIDTH - LedgerIconButton.EDIT.width(),
            vStart + TEXT_Y_OFFSET + 24,
            button -> openFilterConfig(),
            Component.literal("Configure Filters")
        ));
    }

    private void openFilterConfig() {
        if (zone == null) {
            return;
        }

        FilterConfigurationPage.FilterType filterType = zone.getType().supportsItemFilters()
            ? FilterConfigurationPage.FilterType.ITEM
            : FilterConfigurationPage.FilterType.ENTITY;

        screen.setRightPage(new FilterConfigurationPage(screen, villageId, zone.getUUID(), filterType, zoneIndex));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        IVillageCapability currentVillage = VillageDataManager.getInstance().getVillageData(villageId);
        if (currentVillage == null) {
            guiGraphics.drawString(font, Component.translatable("villagetale.gui.zone_detail.no_data"), uStart, vStart + 28, 0x3F3F3F, false);
            return;
        }

        List<IVillageZone> zones = currentVillage.getZones();
        if (zoneIndex < 0 || zoneIndex >= zones.size()) {
            guiGraphics.drawString(font, Component.translatable("villagetale.gui.zone_detail.no_zones"), uStart, vStart + 28, 0x3F3F3F, false);
            return;
        }

        IVillageZone currentZone = zones.get(zoneIndex);
        renderZoneDetails(guiGraphics, currentZone);
    }

    private void renderZoneDetails(GuiGraphics guiGraphics, IVillageZone zone) {
        int currentY = vStart + TEXT_Y_OFFSET;

        if (!isEditingName) {
            guiGraphics.drawString(font, Component.literal(zone.getName()), uStart, currentY, 0, false);
        }

        currentY += LINE_HEIGHT + 1;
        guiGraphics.drawString(font, Component.translatable("villagetale.gui.zone_detail.type", getTypeName(zone)), uStart, currentY, 0, false);
        currentY += LINE_HEIGHT;

        boolean supportsItemFilters = zone.getType() != null && zone.getType().supportsItemFilters();
        boolean supportsEntityFilters = zone.getType() != null && zone.getType().supportsEntityFilters();

        if (supportsItemFilters) {
            int itemFilterCount = zone.getFilter().size();
            String filterText = itemFilterCount > 0
                ? "Item Filters: " + itemFilterCount
                : "Item Filters: None";
            guiGraphics.drawString(font, Component.literal(filterText), uStart, currentY, 0, false);
            currentY += LINE_HEIGHT;
        }

        if (supportsEntityFilters) {
            int entityFilterCount = zone.getEntityTypeFilter().size();
            String filterText = entityFilterCount > 0
                ? "Entity Filters: " + entityFilterCount
                : "Entity Filters: None";
            guiGraphics.drawString(font, Component.literal(filterText), uStart, currentY, 0, false);
            currentY += LINE_HEIGHT;
        }

        currentY += LINE_HEIGHT;

        guiGraphics.drawString(font, Component.translatable("villagetale.gui.zone_detail.shape", getShapeName(zone)), uStart, currentY, 0, false);
        currentY += LINE_HEIGHT;

        renderShapeDetails(guiGraphics, currentY, zone);
    }

    private void renderShapeDetails(GuiGraphics guiGraphics, int currentY, IVillageZone zone) {
        if (zone.getShape() instanceof Box box) {
            AABB bounds = box.getBounds();
            currentY = renderWrappedText(guiGraphics, Component.translatable("villagetale.gui.zone_detail.shape_box_start", (int)bounds.minX, (int)bounds.minY, (int)bounds.minZ), currentY);
            renderWrappedText(guiGraphics, Component.translatable("villagetale.gui.zone_detail.shape_box_end", (int)bounds.maxX, (int)bounds.maxY, (int)bounds.maxZ), currentY);
        }

        if (zone.getShape() instanceof Cylinder cylinder) {
            currentY = renderWrappedText(guiGraphics, Component.translatable("villagetale.gui.zone_detail.shape_cylinder_start",
                cylinder.getBaseCenter().getX(),
                cylinder.getBaseCenter().getY(),
                cylinder.getBaseCenter().getZ()), currentY);
            currentY = renderWrappedText(guiGraphics, Component.translatable("villagetale.gui.zone_detail.shape_cylinder_radius", cylinder.getRadius()), currentY);
            renderWrappedText(guiGraphics, Component.translatable("villagetale.gui.zone_detail.shape_cylinder_height", cylinder.getHeight()), currentY);
        }

        if (zone.getShape() instanceof Point point) {
            renderWrappedText(guiGraphics, Component.translatable("villagetale.gui.zone_detail.shape_point_position",
                point.getPos().getX(),
                point.getPos().getY(),
                point.getPos().getZ()), currentY);
        }

        if (zone.getShape() instanceof Route route) {
            for (int i = 0; i < route.getPath().size(); i++) {
                var waypoint = route.getPath().get(i);
                currentY = renderWrappedText(guiGraphics, Component.translatable("villagetale.gui.zone_detail.shape_route_waypoint", i + 1,
                    waypoint.getX(),
                    waypoint.getY(),
                    waypoint.getZ()), currentY);
            }
        }
    }

    private int renderWrappedText(GuiGraphics guiGraphics, Component text, int y) {
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(text, LedgerScreen.CONTENT_WIDTH);
        for (int i = 0; i < lines.size(); i++) {
            int lineX = uStart;
            if (i > 0) {
                lineX += LedgerScreen.INDENT_WIDTH;
            }
            guiGraphics.drawString(font, lines.get(i), lineX, y, 0, false);
            y += LINE_HEIGHT;
        }
        return y;
    }

    private String getTypeName(IVillageZone zone) {
        if (zone.getType() == null) {
            return "Unknown";
        }
        return zone.getType().getID().getPath();
    }

    private String getShapeName(IVillageZone zone) {
        if (zone.getShape() == null) {
            return "Unknown";
        }
        return zone.getShape().getID().getPath();
    }

    private void toggleNameEditing() {
        if (zone == null) {
            return;
        }

        if (!isEditingName) {
            this.nameEditBox.setValue(zone.getName());
            this.nameEditBox.setVisible(true);
            this.nameEditBox.setFocused(true);
            this.nameEditBox.moveCursorToEnd();
            this.isEditingName = true;
        } else {
            String newName = this.nameEditBox.getValue().trim();
            if (!newName.isEmpty() && !newName.equals(zone.getName())) {
                UpdateZoneName.send(villageId, zone.getUUID(), newName);
            }
            this.nameEditBox.setVisible(false);
            this.nameEditBox.setFocused(false);
            this.isEditingName = false;
        }
    }

    private void addNavigationButtons() {
        if (village == null) {
            return;
        }

        List<IVillageZone> zones = village.getZones();
        int zoneCount = zones.size();

        if (zoneIndex > 0) {
            addRenderableWidget(LedgerIconButton.PagePrev(
                uStart,
                vStart + LedgerScreen.CONTENT_HEIGHT - LedgerIconButton.PAGE_NEXT.height(),
                button -> navigateToZone(zoneIndex - 1),
                Component.literal("Previous Zone")
            ));
        }

        if (zoneIndex < zoneCount - 1) {
            int guiLeft = uStart - LedgerScreen.CONTENT_LEFT_START;
            int guiRight = guiLeft + LedgerScreen.GUI_WIDTH;
            int rightOffset = LedgerScreen.CONTENT_LEFT_START + LedgerIconButton.PAGE_NEXT.width();
            addRenderableWidget(LedgerIconButton.PageNext(
                guiRight - rightOffset,
                vStart + LedgerScreen.CONTENT_HEIGHT - LedgerIconButton.PAGE_NEXT.height(),
                button -> navigateToZone(zoneIndex + 1),
                Component.literal("Next Zone")
            ));
        }
    }

    private void navigateToZone(int newZoneIndex) {
        screen.setLeftPage(new ZoneInfoPage(screen, villageId, newZoneIndex));
        screen.setRightPage(new ZoneVillagersPage(screen, villageId, newZoneIndex));
    }

    private void addBackButton() {
        addRenderableWidget(LedgerIconButton.Back(
            uStart + LedgerScreen.CONTENT_WIDTH - LedgerIconButton.BACK.width(),
            vStart + LedgerScreen.CONTENT_HEIGHT - LedgerIconButton.BACK.height(),
            button -> {
                screen.setLeftPage(new VillageInfoPage(screen, villageId));
                screen.setRightPage(new ZoneListPage(screen, villageId));
            },
            Component.literal("Back to Zone List")
        ));
    }
}
