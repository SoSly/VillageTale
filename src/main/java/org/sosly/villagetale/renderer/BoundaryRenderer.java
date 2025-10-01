package org.sosly.villagetale.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.client.BoundaryDataStorage;
import org.sosly.villagetale.data.VillageBoundaryData;
import org.sosly.villagetale.data.ZoneBoundaryData;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = VillageTale.MOD_ID, value = Dist.CLIENT)
public class BoundaryRenderer {
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        BoundaryDataStorage storage = BoundaryDataStorage.getInstance();
        if (!storage.isOverlayEnabled()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        BoundaryDataStorage.DimensionBoundaryData data = storage.getDimensionData(mc.level.dimension());
        if (data == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        Vec3 cameraPos = event.getCamera().getPosition();
        VertexConsumer consumer = bufferSource.getBuffer(BoundaryRenderType.boundaryLines());

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        for (VillageBoundaryData village : data.getVillages().values()) {
            BoundaryOutline outline = new BoundaryOutline(village.getAABB(), 0.0f, 0.0f, 1.0f, 0.6f);
            outline.render(poseStack, consumer, cameraPos);
        }

        for (ZoneBoundaryData zone : data.getZones().values()) {
            renderZone(zone, poseStack, consumer, cameraPos);
        }

        bufferSource.endBatch();

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    private static void renderZone(ZoneBoundaryData zone, PoseStack poseStack, VertexConsumer consumer, Vec3 cameraPos) {
        ResourceLocation shapeType = zone.getShapeType();
        float red = 0.4f, green = 0.8f, blue = 0.4f, alpha = 0.6f;

        if (shapeType.equals(new ResourceLocation(VillageTale.MOD_ID, "box"))) {
            BoundaryOutline outline = new BoundaryOutline(zone.getBounds(), red, green, blue, alpha);
            outline.render(poseStack, consumer, cameraPos);
            return;
        }

        if (shapeType.equals(new ResourceLocation(VillageTale.MOD_ID, "cylinder"))) {
            if (zone.getCenter() == null) {
                return;
            }

            CylinderOutline outline = new CylinderOutline(
                zone.getCenter(), zone.getRadius(), zone.getHeight(),
                red, green, blue, alpha
            );
            outline.render(poseStack, consumer, cameraPos);
            return;
        }

        if (shapeType.equals(new ResourceLocation(VillageTale.MOD_ID, "point"))) {
            BoundaryOutline outline = new BoundaryOutline(zone.getBounds(), red, green, blue, alpha);
            outline.render(poseStack, consumer, cameraPos);
            return;
        }

        if (shapeType.equals(new ResourceLocation(VillageTale.MOD_ID, "route"))) {
            if (zone.getWaypoints() == null || zone.getWaypoints().isEmpty()) {
                return;
            }

            RouteOutline outline = new RouteOutline(zone.getWaypoints(), red, green, blue, alpha);
            outline.render(poseStack, consumer, cameraPos);
        }
    }
}
