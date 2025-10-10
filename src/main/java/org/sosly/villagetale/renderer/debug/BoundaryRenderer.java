package org.sosly.villagetale.renderer.debug;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IZoneShape;
import org.sosly.villagetale.client.BoundaryDataStorage;
import org.sosly.villagetale.client.ZoneCreationManager;
import org.sosly.villagetale.data.VillageBoundaryData;
import org.sosly.villagetale.data.ZoneBoundaryData;
import org.sosly.villagetale.zone.shape.Box;
import org.sosly.villagetale.zone.shape.Cylinder;
import org.sosly.villagetale.zone.shape.Point;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = VillageTale.MOD_ID, value = Dist.CLIENT)
public class BoundaryRenderer {
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        BoundaryDataStorage storage = BoundaryDataStorage.getInstance();
        ZoneCreationManager creationManager = ZoneCreationManager.getInstance();

        if (!storage.isOverlayEnabled() && !creationManager.isActive()) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        Vec3 cameraPos = event.getCamera().getPosition();
        VertexConsumer consumer = bufferSource.getBuffer(BoundaryRenderType.boundaryLines());

        if (storage.isOverlayEnabled()) {
            BoundaryDataStorage.DimensionBoundaryData data = storage.getDimensionData(mc.level.dimension());
            if (data != null) {
                for (VillageBoundaryData village : data.getVillages().values()) {
                    BoundaryOutline outline = new BoundaryOutline(village.getAABB(), 0.0f, 0.0f, 1.0f, 0.6f);
                    outline.render(poseStack, consumer, cameraPos);
                }

                for (ZoneBoundaryData zone : data.getZones().values()) {
                    IZoneShape shape = zone.toShape();
                    if (shape != null) {
                        renderShape(shape, poseStack, consumer, cameraPos, 0.4f, 0.8f, 0.4f, 0.6f);
                    }
                }
            }
        }

        if (creationManager.isActive()) {
            Vec3 cursorPos = getCursorPosition(mc);
            if (cursorPos != null) {
                IZoneShape previewShape = creationManager.getPreviewShape(cursorPos);
                if (previewShape != null) {
                    renderShape(previewShape, poseStack, consumer, cameraPos, 0.9f, 0.7f, 0.2f, 0.6f);
                }
            }
        }

        bufferSource.endBatch();
    }

    private static Vec3 getCursorPosition(Minecraft mc) {
        if (mc.hitResult == null) {
            return null;
        }

        if (mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            net.minecraft.world.phys.BlockHitResult blockHit = (net.minecraft.world.phys.BlockHitResult) mc.hitResult;
            return Vec3.atCenterOf(blockHit.getBlockPos());
        }

        if (mc.player == null) {
            return null;
        }

        return mc.player.position();
    }

    private static void renderShape(IZoneShape shape, PoseStack poseStack, VertexConsumer consumer, Vec3 cameraPos, float red, float green, float blue, float alpha) {
        if (shape instanceof Box box) {
            BoundaryOutline outline = new BoundaryOutline(box.getBounds(), red, green, blue, alpha);
            outline.render(poseStack, consumer, cameraPos);
            return;
        }

        if (shape instanceof Cylinder cylinder) {
            CylinderOutline outline = new CylinderOutline(
                cylinder.getBaseCenter(),
                cylinder.getRadius(),
                cylinder.getHeight(),
                red, green, blue, alpha
            );
            outline.render(poseStack, consumer, cameraPos);
            return;
        }

        if (shape instanceof Point point) {
            BoundaryOutline outline = new BoundaryOutline(new net.minecraft.world.phys.AABB(point.getPos()), red, green, blue, alpha);
            outline.render(poseStack, consumer, cameraPos);
            return;
        }

        if (shape instanceof org.sosly.villagetale.zone.shape.Route route) {
            if (route.getPath() == null || route.getPath().isEmpty()) {
                return;
            }
            RouteOutline outline = new RouteOutline(route.getPath(), red, green, blue, alpha);
            outline.render(poseStack, consumer, cameraPos);
        }
    }
}
