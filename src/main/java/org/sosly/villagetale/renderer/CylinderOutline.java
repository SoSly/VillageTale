package org.sosly.villagetale.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector4f;

@OnlyIn(Dist.CLIENT)
public class CylinderOutline {
    private final BlockPos center;
    private final int radius;
    private final int height;
    private final float red;
    private final float green;
    private final float blue;
    private final float alpha;
    private static final int SEGMENTS = 32;

    public CylinderOutline(BlockPos center, int radius, int height, float red, float green, float blue, float alpha) {
        this.center = center;
        this.radius = radius;
        this.height = height;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    public void render(PoseStack poseStack, VertexConsumer consumer, Vec3 camera) {
        Matrix4f matrix = poseStack.last().pose();

        Vec3 offsetCenter = new Vec3(center.getX(), center.getY(), center.getZ()).subtract(camera);
        float centerX = (float) offsetCenter.x;
        float centerY = (float) offsetCenter.y;
        float centerZ = (float) offsetCenter.z;

        renderCircle(matrix, consumer, centerX, centerY, centerZ, radius);
        renderCircle(matrix, consumer, centerX, centerY + height, centerZ, radius);

        for (int i = 0; i < 4; i++) {
            double angle = (Math.PI * 2.0 / 4) * i;
            float x = centerX + (float) (Math.cos(angle) * radius);
            float z = centerZ + (float) (Math.sin(angle) * radius);

            Vector4f bottom = new Vector4f(x, centerY, z, 1.0f).mul(matrix);
            Vector4f top = new Vector4f(x, centerY + height, z, 1.0f).mul(matrix);

            consumer.vertex(bottom.x(), bottom.y(), bottom.z())
                .color(red, green, blue, alpha)
                .endVertex();
            consumer.vertex(top.x(), top.y(), top.z())
                .color(red, green, blue, alpha)
                .endVertex();
        }
    }

    private void renderCircle(Matrix4f matrix, VertexConsumer consumer, float centerX, float centerY, float centerZ, float radius) {
        for (int i = 0; i < SEGMENTS; i++) {
            double angle1 = (Math.PI * 2.0 / SEGMENTS) * i;
            double angle2 = (Math.PI * 2.0 / SEGMENTS) * ((i + 1) % SEGMENTS);

            float x1 = centerX + (float) (Math.cos(angle1) * radius);
            float z1 = centerZ + (float) (Math.sin(angle1) * radius);
            float x2 = centerX + (float) (Math.cos(angle2) * radius);
            float z2 = centerZ + (float) (Math.sin(angle2) * radius);

            Vector4f v1 = new Vector4f(x1, centerY, z1, 1.0f).mul(matrix);
            Vector4f v2 = new Vector4f(x2, centerY, z2, 1.0f).mul(matrix);

            consumer.vertex(v1.x(), v1.y(), v1.z())
                .color(red, green, blue, alpha)
                .endVertex();
            consumer.vertex(v2.x(), v2.y(), v2.z())
                .color(red, green, blue, alpha)
                .endVertex();
        }
    }
}
