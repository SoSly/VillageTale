package org.sosly.villagetale.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IZoneShape;
import org.sosly.villagetale.renderer.debug.BoundaryOutline;
import org.sosly.villagetale.renderer.debug.BoundaryRenderType;
import org.sosly.villagetale.zone.shape.Box;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = VillageTale.MOD_ID, value = Dist.CLIENT)
public class ZoneCreationRenderer {
    private static final float PREVIEW_RED = 0.9f;
    private static final float PREVIEW_GREEN = 0.7f;
    private static final float PREVIEW_BLUE = 0.2f;
    private static final float PREVIEW_ALPHA = 0.6f;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        ZoneCreationManager manager = ZoneCreationManager.getInstance();
        if (!manager.isActive()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        Vec3 cursorPos = getCursorPosition(mc);
        if (cursorPos == null) {
            return;
        }

        IZoneShape previewShape = manager.getPreviewShape(cursorPos);
        if (previewShape == null) {
            return;
        }

        if (!(previewShape instanceof Box box)) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();
        VertexConsumer consumer = mc.renderBuffers().bufferSource().getBuffer(BoundaryRenderType.boundaryLines());

        AABB bounds = box.getBounds();
        BoundaryOutline outline = new BoundaryOutline(bounds, PREVIEW_RED, PREVIEW_GREEN, PREVIEW_BLUE, PREVIEW_ALPHA);
        outline.render(poseStack, consumer, cameraPos);

        mc.renderBuffers().bufferSource().endBatch();
    }

    private static Vec3 getCursorPosition(Minecraft mc) {
        HitResult hitResult = mc.hitResult;
        if (hitResult == null) {
            return null;
        }

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hitResult;
            return Vec3.atCenterOf(blockHit.getBlockPos());
        }

        if (mc.player == null) {
            return null;
        }

        return mc.player.position();
    }
}
