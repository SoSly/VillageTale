package org.sosly.villagetale.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector4f;

@OnlyIn(Dist.CLIENT)
public class BoundaryOutline {
    private final AABB bounds;
    private final float red;
    private final float green;
    private final float blue;
    private final float alpha;

    public BoundaryOutline(AABB bounds, float red, float green, float blue, float alpha) {
        this.bounds = bounds;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    public void render(PoseStack poseStack, VertexConsumer consumer, Vec3 camera) {
        AABB offsetBounds = bounds.move(camera.scale(-1));
        Matrix4f matrix = poseStack.last().pose();

        float minX = (float) offsetBounds.minX;
        float minY = (float) offsetBounds.minY;
        float minZ = (float) offsetBounds.minZ;
        float maxX = (float) offsetBounds.maxX;
        float maxY = (float) offsetBounds.maxY;
        float maxZ = (float) offsetBounds.maxZ;

        Vector4f[] vertices = new Vector4f[8];
        vertices[0] = new Vector4f(minX, minY, minZ, 1.0f);
        vertices[1] = new Vector4f(maxX, minY, minZ, 1.0f);
        vertices[2] = new Vector4f(maxX, minY, maxZ, 1.0f);
        vertices[3] = new Vector4f(minX, minY, maxZ, 1.0f);
        vertices[4] = new Vector4f(minX, maxY, minZ, 1.0f);
        vertices[5] = new Vector4f(maxX, maxY, minZ, 1.0f);
        vertices[6] = new Vector4f(maxX, maxY, maxZ, 1.0f);
        vertices[7] = new Vector4f(minX, maxY, maxZ, 1.0f);

        for (int i = 0; i < 8; i++) {
            vertices[i].mul(matrix);
        }

        drawEdge(consumer, vertices[0], vertices[1]);
        drawEdge(consumer, vertices[1], vertices[2]);
        drawEdge(consumer, vertices[2], vertices[3]);
        drawEdge(consumer, vertices[3], vertices[0]);

        drawEdge(consumer, vertices[4], vertices[5]);
        drawEdge(consumer, vertices[5], vertices[6]);
        drawEdge(consumer, vertices[6], vertices[7]);
        drawEdge(consumer, vertices[7], vertices[4]);

        drawEdge(consumer, vertices[0], vertices[4]);
        drawEdge(consumer, vertices[1], vertices[5]);
        drawEdge(consumer, vertices[2], vertices[6]);
        drawEdge(consumer, vertices[3], vertices[7]);
    }

    private void drawEdge(VertexConsumer consumer, Vector4f start, Vector4f end) {
        consumer.vertex(start.x(), start.y(), start.z())
            .color(red, green, blue, alpha)
            .endVertex();
        consumer.vertex(end.x(), end.y(), end.z())
            .color(red, green, blue, alpha)
            .endVertex();
    }
}
