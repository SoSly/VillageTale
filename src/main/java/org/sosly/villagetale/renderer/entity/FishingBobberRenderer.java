package org.sosly.villagetale.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.sosly.villagetale.entity.FishingBobber;

public class FishingBobberRenderer extends EntityRenderer<FishingBobber> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/fishing_hook.png");
    private static final RenderType RENDER_TYPE = RenderType.entityCutout(TEXTURE);

    public FishingBobberRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(FishingBobber entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Entity owner = entity.getOwner();
        if (owner != null) {
            renderFishingLine(entity, owner, partialTicks, poseStack, buffer, packedLight);
        }

        poseStack.pushPose();

        float bobAmount = (float) Math.sin((entity.tickCount + partialTicks) * Math.PI / 40.0) * 0.05F;
        poseStack.translate(0.0, bobAmount, 0.0);

        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix4f = pose.pose();
        Matrix3f matrix3f = pose.normal();
        VertexConsumer vertexConsumer = buffer.getBuffer(RENDER_TYPE);
        vertex(vertexConsumer, matrix4f, matrix3f, packedLight, 0.0F, 0, 0, 1);
        vertex(vertexConsumer, matrix4f, matrix3f, packedLight, 1.0F, 0, 1, 1);
        vertex(vertexConsumer, matrix4f, matrix3f, packedLight, 1.0F, 1, 1, 0);
        vertex(vertexConsumer, matrix4f, matrix3f, packedLight, 0.0F, 1, 0, 0);
        poseStack.popPose();
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private void renderFishingLine(FishingBobber bobber, Entity owner, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        Vec3 ownerPos = owner.getPosition(partialTicks);
        Vec3 handOffset = calculateHandOffset(owner, partialTicks);
        Vec3 handPos = ownerPos.add(handOffset);

        float bobAmount = (float) Math.sin((bobber.tickCount + partialTicks) * Math.PI / 40.0) * 0.05F;
        Vec3 bobberPos = bobber.getPosition(partialTicks).add(0, bobAmount, 0);

        double deltaX = handPos.x - bobberPos.x;
        double deltaY = handPos.y - bobberPos.y;
        double deltaZ = handPos.z - bobberPos.z;

        VertexConsumer lineConsumer = buffer.getBuffer(RenderType.lineStrip());
        Matrix4f matrix4f = poseStack.last().pose();

        lineConsumer.vertex(matrix4f, 0.0F, 0.0F, 0.0F).color(0, 0, 0, 255).normal(0.0F, 1.0F, 0.0F).endVertex();
        lineConsumer.vertex(matrix4f, (float) deltaX, (float) deltaY, (float) deltaZ).color(0, 0, 0, 255).normal(0.0F, 1.0F, 0.0F).endVertex();

        poseStack.popPose();
    }

    private Vec3 calculateHandOffset(Entity owner, float partialTicks) {
        float yaw = Mth.lerp(partialTicks, owner.yRotO, owner.getYRot()) * Mth.DEG_TO_RAD;
        float bodyYaw = owner instanceof LivingEntity living
                ? Mth.lerp(partialTicks, living.yBodyRotO, living.yBodyRot) * Mth.DEG_TO_RAD
                : yaw;

        double handHeight = owner.getEyeHeight() * 0.6;
        double handSideOffset = 0.35;
        double handForwardOffset = 0.8;

        double offsetX = -Math.sin(bodyYaw) * handForwardOffset - Math.cos(bodyYaw) * handSideOffset;
        double offsetZ = Math.cos(bodyYaw) * handForwardOffset - Math.sin(bodyYaw) * handSideOffset;

        return new Vec3(offsetX, handHeight, offsetZ);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, int packedLight, float x, int y, int u, int v) {
        consumer.vertex(pose, x - 0.5F, (float) y - 0.5F, 0.0F)
            .color(255, 255, 255, 255)
            .uv((float) u, (float) v)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(packedLight)
            .normal(normal, 0.0F, 1.0F, 0.0F)
            .endVertex();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull FishingBobber entity) {
        return TEXTURE;
    }
}
