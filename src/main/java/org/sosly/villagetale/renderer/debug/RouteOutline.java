package org.sosly.villagetale.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class RouteOutline {
    private final List<BlockPos> waypoints;
    private final float red;
    private final float green;
    private final float blue;
    private final float alpha;

    public RouteOutline(List<BlockPos> waypoints, float red, float green, float blue, float alpha) {
        this.waypoints = waypoints;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    public void render(PoseStack poseStack, VertexConsumer consumer, Vec3 camera) {
        if (waypoints == null || waypoints.size() < 2) {
            return;
        }

        Matrix4f matrix = poseStack.last().pose();

        for (int i = 0; i < waypoints.size() - 1; i++) {
            BlockPos start = waypoints.get(i);
            BlockPos end = waypoints.get(i + 1);

            Vec3 startOffset = new Vec3(start.getX() + 0.5, start.getY() + 0.5, start.getZ() + 0.5).subtract(camera);
            Vec3 endOffset = new Vec3(end.getX() + 0.5, end.getY() + 0.5, end.getZ() + 0.5).subtract(camera);

            Vector4f v1 = new Vector4f((float) startOffset.x, (float) startOffset.y, (float) startOffset.z, 1.0f).mul(matrix);
            Vector4f v2 = new Vector4f((float) endOffset.x, (float) endOffset.y, (float) endOffset.z, 1.0f).mul(matrix);

            consumer.vertex(v1.x(), v1.y(), v1.z())
                .color(red, green, blue, alpha)
                .endVertex();
            consumer.vertex(v2.x(), v2.y(), v2.z())
                .color(red, green, blue, alpha)
                .endVertex();
        }
    }
}
