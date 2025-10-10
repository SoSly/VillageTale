package org.sosly.villagetale.client.gui;

import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.client.VillageDataManager;
import org.sosly.villagetale.client.gui.components.LedgerIconButton;
import org.sosly.villagetale.client.gui.components.NoShadowEditBox;
import org.sosly.villagetale.network.packets.serverbound.UpdateZoneName;
import org.sosly.villagetale.zone.shape.Box;
import org.sosly.villagetale.zone.shape.Cylinder;
import org.sosly.villagetale.zone.shape.Point;
import org.sosly.villagetale.zone.shape.Route;

@OnlyIn(Dist.CLIENT)
public class ZoneDetailScreen extends AbstractLedgerScreen {
    private static final int TEXT_Y_OFFSET = 16;
    private static final int LINE_HEIGHT = 12;
    private static final int ICON_SIZE = 12;
    private static final int EDIT_ICON_SIZE = 10;

    private final UUID villageId;
    private int currentZoneIndex;
    private boolean isEditingName;
    private LedgerIconButton previousButton;
    private LedgerIconButton nextButton;
    private LedgerIconButton returnButton;
    private LedgerIconButton editNameButton;
    private LedgerIconButton configureItemFiltersButton;
    private LedgerIconButton configureEntityFiltersButton;
    private NoShadowEditBox nameEditBox;

    public ZoneDetailScreen(UUID villageId, int startingZoneIndex) {
        super(Component.translatable("villagetale.gui.zone_detail.title"));
        this.villageId = villageId;
        this.currentZoneIndex = startingZoneIndex;
        this.isEditingName = false;
    }

    @Override
    protected void init() {
        super.init();

        int leftPos = getLeftPos();
        int topPos = getTopPos();

        this.previousButton = this.addRenderableWidget(LedgerIconButton.PagePrev(leftPos + CONTENT_LEFT_MARGIN, topPos + 155, button -> navigateToPreviousZone(), Component.translatable("villagetale.gui.previous")));
        this.returnButton = this.addRenderableWidget(LedgerIconButton.Back(leftPos + CONTENT_LEFT_MARGIN + (CONTENT_WIDTH - 14) / 2, topPos + 153, button -> returnToVillageInfo(), Component.translatable("villagetale.gui.back")));
        this.nextButton = this.addRenderableWidget(LedgerIconButton.PageNext(leftPos + CONTENT_LEFT_MARGIN + CONTENT_WIDTH - 23, topPos + 155, button -> navigateToNextZone(), Component.translatable("villagetale.gui.next")));

        this.editNameButton = this.addRenderableWidget(LedgerIconButton.Edit(
            leftPos + CONTENT_LEFT_MARGIN + CONTENT_WIDTH - EDIT_ICON_SIZE,
            topPos + TEXT_Y_OFFSET - 1,
            button -> toggleNameEditing(),
            Component.literal("Edit Name")
        ));

        this.configureItemFiltersButton = this.addRenderableWidget(LedgerIconButton.Edit(
            leftPos + CONTENT_LEFT_MARGIN + CONTENT_WIDTH - EDIT_ICON_SIZE,
            topPos + TEXT_Y_OFFSET + 24,
            button -> openItemFilterConfig(),
            Component.literal("Configure Item Filters")
        ));

        this.configureEntityFiltersButton = this.addRenderableWidget(LedgerIconButton.Edit(
            leftPos + CONTENT_LEFT_MARGIN + CONTENT_WIDTH - EDIT_ICON_SIZE,
            topPos + TEXT_Y_OFFSET + 24,
            button -> openEntityFilterConfig(),
            Component.literal("Configure Entity Filters")
        ));

        this.nameEditBox = new NoShadowEditBox(
            this.font,
            leftPos + CONTENT_LEFT_MARGIN,
            topPos + TEXT_Y_OFFSET,
            95,
            10,
            Component.literal("Zone Name")
        );
        this.nameEditBox.setMaxLength(24);
        this.nameEditBox.setBordered(false);
        this.nameEditBox.setTextColor(0x000000);
        this.nameEditBox.setVisible(false);
        this.addRenderableWidget(this.nameEditBox);

        updateButtonVisibility();
    }

    @Override
    protected void renderLedgerContent(GuiGraphics guiGraphics, int leftPos, int topPos, int mouseX, int mouseY, float partialTick) {
        IVillageCapability village = VillageDataManager.getInstance().getVillageData(villageId);
        if (village == null) {
            guiGraphics.drawString(this.font, Component.translatable("villagetale.gui.zone_detail.no_data"), leftPos + CONTENT_LEFT_MARGIN, topPos + 28, 0x3F3F3F, false);
            return;
        }

        List<IVillageZone> zones = village.getZones();
        if (zones.isEmpty()) {
            guiGraphics.drawString(this.font, Component.translatable("villagetale.gui.zone_detail.no_zones"), leftPos + CONTENT_LEFT_MARGIN, topPos + 28, 0x3F3F3F, false);
            return;
        }

        if (currentZoneIndex < 0 || currentZoneIndex >= zones.size()) {
            currentZoneIndex = 0;
        }

        IVillageZone zone = zones.get(currentZoneIndex);
        renderZoneDetails(guiGraphics, zone, leftPos, topPos);
    }

    private void renderZoneDetails(GuiGraphics guiGraphics, IVillageZone zone, int leftPos, int topPos) {
        int currentY = topPos + TEXT_Y_OFFSET;
        int textX = leftPos + CONTENT_LEFT_MARGIN;

        if (!isEditingName) {
            guiGraphics.drawString(this.font, Component.literal(zone.getName()), textX, currentY, 0, false);
        }

        currentY += LINE_HEIGHT + 1;
        guiGraphics.drawString(this.font, Component.translatable("villagetale.gui.zone_detail.type", getTypeName(zone)), textX, currentY, 0, false);
        currentY += LINE_HEIGHT;

        boolean supportsItemFilters = zone.getType() != null && zone.getType().supportsItemFilters();
        boolean supportsEntityFilters = zone.getType() != null && zone.getType().supportsEntityFilters();

        if (supportsItemFilters) {
            int itemFilterCount = zone.getFilter().size();
            String filterText = itemFilterCount > 0
                ? "Item Filters: " + itemFilterCount
                : "Item Filters: None";
            guiGraphics.drawString(this.font, Component.literal(filterText), textX, currentY, 0, false);
            currentY += LINE_HEIGHT;
        }

        if (supportsEntityFilters) {
            int entityFilterCount = zone.getEntityTypeFilter().size();
            String filterText = entityFilterCount > 0
                ? "Entity Filters: " + entityFilterCount
                : "Entity Filters: None";
            guiGraphics.drawString(this.font, Component.literal(filterText), textX, currentY, 0, false);
            currentY += LINE_HEIGHT;
        }

        currentY += LINE_HEIGHT;

        guiGraphics.drawString(this.font, Component.translatable("villagetale.gui.zone_detail.shape", getShapeName(zone)), textX, currentY, 0, false);
        currentY += LINE_HEIGHT;

        renderShapeDetails(guiGraphics, textX, currentY, zone);
    }

    private void renderShapeDetails(GuiGraphics guiGraphics, int textX, int currentY, IVillageZone zone) {
        if (zone.getShape() instanceof Box box) {
            AABB bounds = box.getBounds();
            currentY = renderWrappedText(guiGraphics, Component.translatable("villagetale.gui.zone_detail.shape_box_start", (int)bounds.minX, (int)bounds.minY, (int)bounds.minZ), textX, currentY);
            renderWrappedText(guiGraphics, Component.translatable("villagetale.gui.zone_detail.shape_box_end", (int)bounds.maxX, (int)bounds.maxY, (int)bounds.maxZ), textX, currentY);
        }

        if (zone.getShape() instanceof Cylinder cylinder) {
            currentY = renderWrappedText(guiGraphics, Component.translatable("villagetale.gui.zone_detail.shape_cylinder_start",
                cylinder.getBaseCenter().getX(),
                cylinder.getBaseCenter().getY(),
                cylinder.getBaseCenter().getZ()), textX, currentY);
            currentY = renderWrappedText(guiGraphics, Component.translatable("villagetale.gui.zone_detail.shape_cylinder_radius", cylinder.getRadius()), textX, currentY);
            renderWrappedText(guiGraphics, Component.translatable("villagetale.gui.zone_detail.shape_cylinder_height", cylinder.getHeight()), textX, currentY);
        }

        if (zone.getShape() instanceof Point point) {
            renderWrappedText(guiGraphics, Component.translatable("villagetale.gui.zone_detail.shape_point_position",
                point.getPos().getX(),
                point.getPos().getY(),
                point.getPos().getZ()), textX, currentY);
        }

        if (zone.getShape() instanceof Route route) {
            for (int i = 0; i < route.getPath().size(); i++) {
                var waypoint = route.getPath().get(i);
                currentY = renderWrappedText(guiGraphics, Component.translatable("villagetale.gui.zone_detail.shape_route_waypoint", i + 1,
                    waypoint.getX(),
                    waypoint.getY(),
                    waypoint.getZ()), textX, currentY);
            }
        }
    }

    private int renderWrappedText(GuiGraphics guiGraphics, Component text, int x, int y) {
        List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(text, CONTENT_WIDTH);
        for (int i = 0; i < lines.size(); i++) {
            int lineX = x;
            if (i > 0) {
                lineX += INDENT_WIDTH;
            }
            guiGraphics.drawString(this.font, lines.get(i), lineX, y, 0, false);
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

    private void updateButtonVisibility() {
        IVillageCapability village = VillageDataManager.getInstance().getVillageData(villageId);
        if (village == null) {
            return;
        }

        List<IVillageZone> zones = village.getZones();
        if (zones.isEmpty()) {
            return;
        }

        IVillageZone zone = zones.get(currentZoneIndex);

        this.nextButton.visible = currentZoneIndex < zones.size() - 1;
        this.previousButton.visible = currentZoneIndex > 0;

        boolean supportsItemFilters = zone.getType() != null && zone.getType().supportsItemFilters();
        boolean supportsEntityFilters = zone.getType() != null && zone.getType().supportsEntityFilters();

        this.configureItemFiltersButton.visible = supportsItemFilters;
        this.configureEntityFiltersButton.visible = supportsEntityFilters;
    }

    private void navigateToPreviousZone() {
        if (currentZoneIndex > 0) {
            currentZoneIndex--;
            updateButtonVisibility();
        }
    }

    private void navigateToNextZone() {
        IVillageCapability village = VillageDataManager.getInstance().getVillageData(villageId);
        if (village == null) {
            return;
        }

        List<IVillageZone> zones = village.getZones();
        if (currentZoneIndex < zones.size() - 1) {
            currentZoneIndex++;
            updateButtonVisibility();
        }
    }

    private void returnToVillageInfo() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new ZoneListScreen(villageId));
        }
    }

    private void toggleNameEditing() {
        IVillageCapability village = VillageDataManager.getInstance().getVillageData(villageId);
        if (village == null) {
            return;
        }

        List<IVillageZone> zones = village.getZones();
        if (zones.isEmpty() || currentZoneIndex < 0 || currentZoneIndex >= zones.size()) {
            return;
        }

        IVillageZone zone = zones.get(currentZoneIndex);

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

    private void openItemFilterConfig() {
        IVillageCapability village = VillageDataManager.getInstance().getVillageData(villageId);
        if (village == null) {
            return;
        }

        List<IVillageZone> zones = village.getZones();
        if (zones.isEmpty() || currentZoneIndex < 0 || currentZoneIndex >= zones.size()) {
            return;
        }

        IVillageZone zone = zones.get(currentZoneIndex);
        if (zone.getType() == null || !zone.getType().supportsItemFilters()) {
            return;
        }

        if (this.minecraft != null) {
            this.minecraft.setScreen(new FilterConfigurationScreen(villageId, zone.getUUID(), FilterConfigurationScreen.FilterType.ITEM, currentZoneIndex));
        }
    }

    private void openEntityFilterConfig() {
        IVillageCapability village = VillageDataManager.getInstance().getVillageData(villageId);
        if (village == null) {
            return;
        }

        List<IVillageZone> zones = village.getZones();
        if (zones.isEmpty() || currentZoneIndex < 0 || currentZoneIndex >= zones.size()) {
            return;
        }

        IVillageZone zone = zones.get(currentZoneIndex);
        if (zone.getType() == null || !zone.getType().supportsEntityFilters()) {
            return;
        }

        if (this.minecraft != null) {
            this.minecraft.setScreen(new FilterConfigurationScreen(villageId, zone.getUUID(), FilterConfigurationScreen.FilterType.ENTITY, currentZoneIndex));
        }
    }
}
