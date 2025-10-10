package org.sosly.villagetale.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.sosly.villagetale.api.IZoneShape;
import org.sosly.villagetale.client.ZoneCreationManager;
import org.sosly.villagetale.client.gui.components.LedgerIconButton;
import org.sosly.villagetale.client.gui.components.NoShadowEditBox;
import org.sosly.villagetale.network.packets.serverbound.CreateZone;
import org.sosly.villagetale.zone.ZoneRegistry;
import org.sosly.villagetale.zone.shape.Box;
import org.sosly.villagetale.zone.shape.Cylinder;
import org.sosly.villagetale.zone.shape.Point;
import org.sosly.villagetale.zone.shape.Route;
import org.sosly.villagetale.zone.type.TownHall;

@OnlyIn(Dist.CLIENT)
public class NewZoneScreen extends AbstractLedgerScreen {
    private static final int TYPE_NAV_BUTTON_SIZE = 11;
    private static final int ACTION_BUTTON_SIZE = 9;
    private static final int ACTION_BUTTON_SPACING = 9;

    private final UUID villageId;
    private final IZoneShape shape;
    private final List<ResourceLocation> zoneTypes;
    private int currentTypeIndex;
    private NoShadowEditBox nameEditBox;
    private LedgerIconButton typeLeftButton;
    private LedgerIconButton typeRightButton;
    private LedgerIconButton saveButton;
    private LedgerIconButton cancelButton;

    public NewZoneScreen(UUID villageId, IZoneShape shape) {
        super(Component.translatable("villagetale.gui.new_zone.title"));
        this.villageId = villageId;
        this.shape = shape;
        this.zoneTypes = new ArrayList<>();
        for (ResourceLocation typeId : ZoneRegistry.INSTANCE.getZoneTypeIDs()) {
            if (!typeId.equals(TownHall.ID)) {
                this.zoneTypes.add(typeId);
            }
        }
        this.zoneTypes.sort((a, b) -> a.getPath().compareTo(b.getPath()));
        this.currentTypeIndex = 0;
    }

    @Override
    protected void init() {
        super.init();

        int leftPos = getLeftPos();
        int topPos = getTopPos();

        this.nameEditBox = new NoShadowEditBox(
            this.font,
            leftPos + CONTENT_LEFT_MARGIN,
            topPos + 16,
            CONTENT_WIDTH - TYPE_NAV_BUTTON_SIZE,
            10,
            Component.translatable("villagetale.gui.new_zone.default_name")
        );
        this.nameEditBox.setMaxLength(24);
        this.nameEditBox.setBordered(false);
        this.nameEditBox.setTextColor(0x000000);
        this.nameEditBox.setValue(Component.translatable("villagetale.gui.new_zone.default_name").getString());
        this.addRenderableWidget(this.nameEditBox);

        this.typeLeftButton = this.addRenderableWidget(LedgerIconButton.ArrowLeft(
            leftPos + CONTENT_LEFT_MARGIN + 30,
            topPos + 27,
            button -> cycleTypePrevious(),
            Component.translatable("villagetale.gui.new_zone.type")
        ));

        this.typeRightButton = this.addRenderableWidget(LedgerIconButton.ArrowRight(
            leftPos + CONTENT_LEFT_MARGIN + CONTENT_WIDTH - TYPE_NAV_BUTTON_SIZE - 5,
            topPos + 27,
            button -> cycleTypeNext(),
            Component.translatable("villagetale.gui.new_zone.type")
        ));

        int actionButtonsWidth = ACTION_BUTTON_SIZE * 2 + ACTION_BUTTON_SPACING;
        int actionButtonsStartX = leftPos + CONTENT_LEFT_MARGIN + (CONTENT_WIDTH - actionButtonsWidth) / 2;

        this.saveButton = this.addRenderableWidget(LedgerIconButton.Commit(
            actionButtonsStartX,
            topPos + 153,
            button -> saveZone(),
            Component.translatable("villagetale.gui.new_zone.save")
        ));

        this.cancelButton = this.addRenderableWidget(LedgerIconButton.Cancel(
            actionButtonsStartX + ACTION_BUTTON_SIZE + ACTION_BUTTON_SPACING,
            topPos + 153,
            button -> returnToAddZone(),
            Component.translatable("villagetale.gui.new_zone.cancel")
        ));
    }

    @Override
    protected void renderLedgerContent(GuiGraphics guiGraphics, int leftPos, int topPos, int mouseX, int mouseY, float partialTick) {
        int currentY = topPos + 30;

        guiGraphics.drawString(this.font, Component.translatable("villagetale.gui.new_zone.type"), leftPos + CONTENT_LEFT_MARGIN, currentY, 0, false);

        if (!zoneTypes.isEmpty()) {
            ResourceLocation currentType = zoneTypes.get(currentTypeIndex);
            String typeName = currentType.getPath();
            int typeNameX = leftPos + CONTENT_LEFT_MARGIN + 40;
            guiGraphics.drawString(this.font, Component.literal(typeName), typeNameX, currentY, 0, false);
        }

        currentY += 20;

        String shapeType = shape.getID().getPath();
        guiGraphics.drawString(this.font, Component.translatable("villagetale.gui.new_zone.shape", shapeType), leftPos + CONTENT_LEFT_MARGIN, currentY, 0x3F3F3F, false);
        currentY += 10;

        renderShapeInfo(guiGraphics, shape, leftPos, currentY);
    }

    private void renderShapeInfo(GuiGraphics guiGraphics, IZoneShape shape, int leftPos, int currentY) {
        if (shape instanceof Box box) {
            renderBoxInfo(guiGraphics, box, leftPos, currentY);
            return;
        }

        if (shape instanceof Cylinder cylinder) {
            renderCylinderInfo(guiGraphics, cylinder, leftPos, currentY);
            return;
        }

        if (shape instanceof Point point) {
            renderPointInfo(guiGraphics, point, leftPos, currentY);
            return;
        }

        if (shape instanceof Route route) {
            renderRouteInfo(guiGraphics, route, leftPos, currentY);
        }
    }

    private int renderBoxInfo(GuiGraphics guiGraphics, Box box, int leftPos, int currentY) {
        AABB bounds = box.getBounds();
        int minX = (int) bounds.minX;
        int minY = (int) bounds.minY;
        int minZ = (int) bounds.minZ;
        int maxX = (int) bounds.maxX - 1;
        int maxY = (int) bounds.maxY - 1;
        int maxZ = (int) bounds.maxZ - 1;

        guiGraphics.drawString(this.font, Component.translatable("villagetale.gui.new_zone.shape_box_from", minX, minY, minZ), leftPos + CONTENT_LEFT_MARGIN, currentY, 0x3F3F3F, false);
        currentY += 10;
        guiGraphics.drawString(this.font, Component.translatable("villagetale.gui.new_zone.shape_box_to", maxX, maxY, maxZ), leftPos + CONTENT_LEFT_MARGIN, currentY, 0x3F3F3F, false);
        return currentY;
    }

    private int renderCylinderInfo(GuiGraphics guiGraphics, Cylinder cylinder, int leftPos, int currentY) {
        int x = cylinder.getBaseCenter().getX();
        int y = cylinder.getBaseCenter().getY();
        int z = cylinder.getBaseCenter().getZ();

        guiGraphics.drawString(this.font, Component.translatable("villagetale.gui.new_zone.shape_cylinder_center", x, y, z), leftPos + CONTENT_LEFT_MARGIN, currentY, 0x3F3F3F, false);
        currentY += 10;
        guiGraphics.drawString(this.font, Component.translatable("villagetale.gui.new_zone.shape_cylinder_radius", cylinder.getRadius()), leftPos + CONTENT_LEFT_MARGIN, currentY, 0x3F3F3F, false);
        currentY += 10;
        guiGraphics.drawString(this.font, Component.translatable("villagetale.gui.new_zone.shape_cylinder_height", cylinder.getHeight()), leftPos + CONTENT_LEFT_MARGIN, currentY, 0x3F3F3F, false);
        return currentY;
    }

    private int renderPointInfo(GuiGraphics guiGraphics, Point point, int leftPos, int currentY) {
        int x = point.getPos().getX();
        int y = point.getPos().getY();
        int z = point.getPos().getZ();

        guiGraphics.drawString(this.font, Component.translatable("villagetale.gui.new_zone.shape_point_pos", x, y, z), leftPos + CONTENT_LEFT_MARGIN, currentY, 0x3F3F3F, false);
        return currentY;
    }

    private int renderRouteInfo(GuiGraphics guiGraphics, Route route, int leftPos, int currentY) {
        guiGraphics.drawString(this.font, Component.translatable("villagetale.gui.new_zone.shape_route_waypoints", route.getPath().size()), leftPos + CONTENT_LEFT_MARGIN, currentY, 0x3F3F3F, false);
        currentY += 10;

        for (int i = 0; i < route.getPath().size(); i++) {
            net.minecraft.core.BlockPos waypoint = route.getPath().get(i);
            guiGraphics.drawString(this.font, Component.translatable("villagetale.gui.new_zone.shape_route_waypoint", i + 1, waypoint.getX(), waypoint.getY(), waypoint.getZ()), leftPos + CONTENT_LEFT_MARGIN, currentY, 0x3F3F3F, false);
            currentY += 10;
        }

        return currentY;
    }

    @Override
    public void onClose() {
        ZoneCreationManager.getInstance().cancel();
        super.onClose();
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

    private void saveZone() {
        if (zoneTypes.isEmpty()) {
            return;
        }
        String zoneName = this.nameEditBox.getValue().trim();
        if (zoneName.isEmpty()) {
            zoneName = Component.translatable("villagetale.gui.new_zone.default_name").getString();
        }
        ResourceLocation zoneType = zoneTypes.get(currentTypeIndex);

        CreateZone.send(villageId, zoneName, zoneType, shape);

        ZoneCreationManager.getInstance().cancel();
        if (this.minecraft != null) {
            this.minecraft.setScreen(new ZoneListScreen(villageId));
        }
    }

    private void returnToAddZone() {
        ZoneCreationManager.getInstance().cancel();
        if (this.minecraft != null) {
            this.minecraft.setScreen(new AddZoneScreen(villageId, new ZoneListScreen(villageId)));
        }
    }
}
