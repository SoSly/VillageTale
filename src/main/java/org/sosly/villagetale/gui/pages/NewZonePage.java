package org.sosly.villagetale.gui.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import org.sosly.villagetale.api.IZoneShape;
import org.sosly.villagetale.client.ZoneCreationManager;
import org.sosly.villagetale.gui.LedgerScreen;
import org.sosly.villagetale.gui.components.LedgerIconButton;
import org.sosly.villagetale.gui.components.NoShadowEditBox;
import org.sosly.villagetale.network.packets.serverbound.CreateZone;
import org.sosly.villagetale.zone.ZoneRegistry;
import org.sosly.villagetale.zone.shape.Box;
import org.sosly.villagetale.zone.shape.Cylinder;
import org.sosly.villagetale.zone.shape.Point;
import org.sosly.villagetale.zone.shape.Route;
import org.sosly.villagetale.zone.type.TownHall;

public class NewZonePage extends AbstractLedgerPage {
    private final IZoneShape shape;
    private final List<ResourceLocation> zoneTypes;
    private int currentTypeIndex;
    private NoShadowEditBox name;

    public NewZonePage(LedgerScreen screen, UUID villageId, IZoneShape shape) {
        super(screen, villageId);
        this.currentTypeIndex = 0;
        this.shape = shape;
        this.zoneTypes = new ArrayList<>();
        for (ResourceLocation typeId : ZoneRegistry.INSTANCE.getZoneTypeIDs()) {
            if (!typeId.equals(TownHall.ID)) {
                this.zoneTypes.add(typeId);
            }
        }
        this.zoneTypes.sort((a, b) -> a.getPath().compareTo(b.getPath()));
    }

    @Override
    public void attach(int uStart, int vStart) {
        super.attach(uStart, vStart);

        name = new NoShadowEditBox(
                font,
                uStart,
                vStart + 16,
                LedgerScreen.CONTENT_WIDTH,
                LINE_HEIGHT,
                Component.literal("Zone Name")
        );
        name.setValue(Component.translatable("villagetale.gui.new_zone.default_name").getString());
        addRenderableWidget(name);

        addRenderableWidget(LedgerIconButton.ArrowLeft(
                uStart + 25,
                vStart + 28,
                button -> cycleTypePrevious(),
                Component.translatable("villagetale.gui.new_zone.type")
        ));

        addRenderableWidget(LedgerIconButton.ArrowRight(
                uStart + LedgerScreen.CONTENT_WIDTH - LedgerIconButton.ARROW_RIGHT.width(),
                vStart + 28,
                button -> cycleTypeNext(),
                Component.translatable("villagetale.gui.new_zone.type")
        ));

        addRenderableWidget(LedgerIconButton.Commit(
                uStart + (LedgerScreen.CONTENT_WIDTH / 2) - (LedgerIconButton.COMMIT.width() * 2),
                vStart + 153,
                button -> saveZone(),
                Component.translatable("villagetale.gui.new_zone.save")
        ));

        addRenderableWidget(LedgerIconButton.Cancel(
                uStart + (LedgerScreen.CONTENT_WIDTH / 2) + (LedgerIconButton.COMMIT.width() * 2),
                vStart + 153,
                button -> screen.setRightPage(new ZoneListPage(screen, villageId)),
                Component.translatable("villagetale.gui.new_zone.cancel")
        ));
    }

    @Override
    public void detach() {
        super.detach();
        name = null;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int currentY = vStart + 30;

        guiGraphics.drawString(this.font, Component.translatable("villagetale.gui.new_zone.type"), uStart, currentY, 0, false);

        if (!zoneTypes.isEmpty()) {
            ResourceLocation currentType = zoneTypes.get(currentTypeIndex);
            String typeName = currentType.getPath();
            int typeNameX = uStart + 25 + LedgerIconButton.ARROW_LEFT.width() + 5;
            guiGraphics.drawString(this.font, Component.literal(typeName), typeNameX, currentY, 0, false);
        }

        currentY += LINE_HEIGHT + 4;

        String shapeType = shape.getID().getPath();
        guiGraphics.drawString(this.font, Component.translatable("villagetale.gui.new_zone.shape", shapeType), uStart, currentY, 0x3F3F3F, false);

        currentY += LINE_HEIGHT + 2;

        renderShapeInfo(guiGraphics, shape, currentY);
    }

    private void cycleTypePrevious() {
        if (zoneTypes.isEmpty()) {
            return;
        }
        currentTypeIndex--;
        if (currentTypeIndex < 0) {
            currentTypeIndex = zoneTypes.size() - 1;
        }
    }

    private void cycleTypeNext() {
        if (zoneTypes.isEmpty()) {
            return;
        }
        currentTypeIndex++;
        if (currentTypeIndex >= zoneTypes.size()) {
            currentTypeIndex = 0;
        }
    }

    private void renderShapeInfo(GuiGraphics guiGraphics, IZoneShape shape, int currentY) {
        if (shape instanceof Box box) {
            renderBoxInfo(guiGraphics, box, currentY);
            return;
        }

        if (shape instanceof Cylinder cylinder) {
            renderCylinderInfo(guiGraphics, cylinder, currentY);
            return;
        }

        if (shape instanceof Point point) {
            renderPointInfo(guiGraphics, point, currentY);
            return;
        }

        if (shape instanceof Route route) {
            renderRouteInfo(guiGraphics, route, currentY);
        }
    }

    private int renderBoxInfo(GuiGraphics guiGraphics, Box box, int currentY) {
        AABB bounds = box.getBounds();
        int minX = (int) bounds.minX;
        int minY = (int) bounds.minY;
        int minZ = (int) bounds.minZ;
        int maxX = (int) bounds.maxX - 1;
        int maxY = (int) bounds.maxY - 1;
        int maxZ = (int) bounds.maxZ - 1;

        guiGraphics.drawString(this.font, Component.translatable("villagetale.gui.new_zone.shape_box_from", minX, minY, minZ), uStart, currentY, 0x3F3F3F, false);
        currentY += 10;
        guiGraphics.drawString(this.font, Component.translatable("villagetale.gui.new_zone.shape_box_to", maxX, maxY, maxZ), uStart, currentY, 0x3F3F3F, false);
        return currentY;
    }

    private int renderCylinderInfo(GuiGraphics guiGraphics, Cylinder cylinder, int currentY) {
        int x = cylinder.getBaseCenter().getX();
        int y = cylinder.getBaseCenter().getY();
        int z = cylinder.getBaseCenter().getZ();

        guiGraphics.drawString(this.font, Component.translatable("villagetale.gui.new_zone.shape_cylinder_center", x, y, z), uStart, currentY, 0x3F3F3F, false);
        currentY += 10;
        guiGraphics.drawString(this.font, Component.translatable("villagetale.gui.new_zone.shape_cylinder_radius", cylinder.getRadius()), uStart, currentY, 0x3F3F3F, false);
        currentY += 10;
        guiGraphics.drawString(this.font, Component.translatable("villagetale.gui.new_zone.shape_cylinder_height", cylinder.getHeight()), uStart, currentY, 0x3F3F3F, false);
        return currentY;
    }

    private int renderPointInfo(GuiGraphics guiGraphics, Point point, int currentY) {
        int x = point.getPos().getX();
        int y = point.getPos().getY();
        int z = point.getPos().getZ();

        guiGraphics.drawString(this.font, Component.translatable("villagetale.gui.new_zone.shape_point_pos", x, y, z), uStart, currentY, 0x3F3F3F, false);
        return currentY;
    }

    private int renderRouteInfo(GuiGraphics guiGraphics, Route route, int currentY) {
        guiGraphics.drawString(this.font, Component.translatable("villagetale.gui.new_zone.shape_route_waypoints", route.getPath().size()), uStart, currentY, 0x3F3F3F, false);
        currentY += 10;

        for (int i = 0; i < route.getPath().size(); i++) {
            net.minecraft.core.BlockPos waypoint = route.getPath().get(i);
            guiGraphics.drawString(this.font, Component.translatable("villagetale.gui.new_zone.shape_route_waypoint", i + 1, waypoint.getX(), waypoint.getY(), waypoint.getZ()), uStart, currentY, 0x3F3F3F, false);
            currentY += 10;
        }

        return currentY;
    }

    private void saveZone() {
        if (zoneTypes.isEmpty()) {
            return;
        }
        String zoneName = name.getValue().trim();
        if (zoneName.isEmpty()) {
            zoneName = Component.translatable("villagetale.gui.new_zone.default_name").getString();
        }
        ResourceLocation zoneType = zoneTypes.get(currentTypeIndex);

        CreateZone.send(villageId, zoneName, zoneType, shape);

        ZoneCreationManager.getInstance().cancel();
        screen.setLeftPage(new VillageInfoPage(screen, villageId));
        screen.setRightPage(new ZoneListPage(screen, villageId));
    }
}
