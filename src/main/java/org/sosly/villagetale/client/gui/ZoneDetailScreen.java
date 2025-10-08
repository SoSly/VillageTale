package org.sosly.villagetale.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.client.VillageDataManager;
import org.sosly.villagetale.client.gui.components.LedgerBackButton;
import org.sosly.villagetale.client.gui.components.LedgerPageButton;
import org.sosly.villagetale.zone.shape.Box;
import org.sosly.villagetale.zone.shape.Cylinder;
import org.sosly.villagetale.zone.shape.Point;
import org.sosly.villagetale.zone.shape.Route;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class ZoneDetailScreen extends AbstractLedgerScreen {
    private static final int TEXT_Y_OFFSET = 16;
    private static final int LINE_HEIGHT = 12;

    private final UUID villageId;
    private int currentZoneIndex;
    private LedgerPageButton backButton;
    private LedgerPageButton forwardButton;
    private LedgerBackButton returnButton;

    public ZoneDetailScreen(UUID villageId, int startingZoneIndex) {
        super(Component.translatable("villagetale.gui.zone_detail.title"));
        this.villageId = villageId;
        this.currentZoneIndex = startingZoneIndex;
    }

    @Override
    protected void init() {
        super.init();

        int leftPos = getLeftPos();
        int topPos = getTopPos();

        this.backButton = this.addRenderableWidget(new LedgerPageButton(leftPos + CONTENT_LEFT_MARGIN, topPos + 155, false, button -> navigateToPreviousZone()));
        this.returnButton = this.addRenderableWidget(new LedgerBackButton(leftPos + 62, topPos + 153, button -> returnToVillageInfo()));
        this.forwardButton = this.addRenderableWidget(new LedgerPageButton(leftPos + 100, topPos + 155, true, button -> navigateToNextZone()));
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

        guiGraphics.drawString(this.font, Component.literal(zone.getName()), textX, currentY, 0, false);
        currentY += LINE_HEIGHT + 1;

        guiGraphics.drawString(this.font, Component.translatable("villagetale.gui.zone_detail.type", getTypeName(zone)), textX, currentY, 0, false);
        currentY += LINE_HEIGHT;

        List<ItemStack> filters = zone.getFilter();
        Set<ResourceLocation> entityFilters = zone.getEntityTypeFilter();
        if (!filters.isEmpty() || !entityFilters.isEmpty()) {
            guiGraphics.drawString(this.font, Component.translatable("villagetale.gui.zone_detail.filters"), textX, currentY, 0, false);
            currentY += LINE_HEIGHT;
            currentY = renderFilters(guiGraphics, textX, currentY, filters, entityFilters);
        }

        guiGraphics.drawString(this.font, Component.translatable("villagetale.gui.zone_detail.shape", getShapeName(zone)), textX, currentY, 0, false);
        currentY += LINE_HEIGHT;

        renderShapeDetails(guiGraphics, textX, currentY, zone);
    }

    private int renderFilters(GuiGraphics guiGraphics, int textX, int currentY, List<ItemStack> filters, Set<ResourceLocation> entityFilters) {
        for (ItemStack filter : filters) {
            String filterText = String.format("- %s", filter.getHoverName().getString());
            guiGraphics.drawString(this.font, Component.literal(filterText), textX, currentY, 0, false);
            currentY += LINE_HEIGHT;
        }

        for (ResourceLocation entityType : entityFilters) {
            String filterText = String.format("- %s", entityType.toString());
            guiGraphics.drawString(this.font, Component.literal(filterText), textX, currentY, 0, false);
            currentY += LINE_HEIGHT;
        }

        return currentY;
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

        this.forwardButton.visible = currentZoneIndex < zones.size() - 1;
        this.backButton.visible = currentZoneIndex > 0;
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
            this.minecraft.setScreen(new VillageInfoScreen(villageId));
        }
    }
}
