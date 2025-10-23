package org.sosly.villagetale.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CylinderOutlineTest {
    private static final int SEGMENTS = 32;

    @Mock
    private VertexConsumer mockConsumer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockConsumer.vertex(anyDouble(), anyDouble(), anyDouble())).thenReturn(mockConsumer);
        when(mockConsumer.color(eq(1.0f), eq(0.0f), eq(0.0f), eq(1.0f))).thenReturn(mockConsumer);
    }

    @Test
    void testRenderDrawsCorrectNumberOfVertices() {
        BlockPos center = new BlockPos(0, 64, 0);
        CylinderOutline outline = new CylinderOutline(center, 10, 5, 1.0f, 0.0f, 0.0f, 1.0f);
        PoseStack poseStack = new PoseStack();
        Vec3 camera = Vec3.ZERO;

        outline.render(poseStack, mockConsumer, camera);

        int expectedVertices = (SEGMENTS * 2 * 2) + (4 * 2);
        verify(mockConsumer, times(expectedVertices)).vertex(anyDouble(), anyDouble(), anyDouble());
        verify(mockConsumer, times(expectedVertices)).color(1.0f, 0.0f, 0.0f, 1.0f);
        verify(mockConsumer, times(expectedVertices)).endVertex();
    }

    @Test
    void testRenderWithDifferentRadius() {
        BlockPos center = new BlockPos(10, 20, 30);
        int radius = 15;
        int height = 10;
        CylinderOutline outline = new CylinderOutline(center, radius, height, 0.0f, 1.0f, 0.0f, 0.5f);
        PoseStack poseStack = new PoseStack();
        Vec3 camera = new Vec3(5, 10, 15);

        when(mockConsumer.color(eq(0.0f), eq(1.0f), eq(0.0f), eq(0.5f))).thenReturn(mockConsumer);

        outline.render(poseStack, mockConsumer, camera);

        int expectedVertices = (SEGMENTS * 2 * 2) + (4 * 2);
        verify(mockConsumer, times(expectedVertices)).vertex(anyDouble(), anyDouble(), anyDouble());
        verify(mockConsumer, times(expectedVertices)).color(0.0f, 1.0f, 0.0f, 0.5f);
    }

    @Test
    void testRenderWithZeroRadius() {
        BlockPos center = new BlockPos(0, 0, 0);
        CylinderOutline outline = new CylinderOutline(center, 0, 10, 1.0f, 1.0f, 1.0f, 1.0f);
        PoseStack poseStack = new PoseStack();
        Vec3 camera = Vec3.ZERO;

        when(mockConsumer.color(eq(1.0f), eq(1.0f), eq(1.0f), eq(1.0f))).thenReturn(mockConsumer);

        outline.render(poseStack, mockConsumer, camera);

        int expectedVertices = (SEGMENTS * 2 * 2) + (4 * 2);
        verify(mockConsumer, times(expectedVertices)).vertex(anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    void testRenderWithZeroHeight() {
        BlockPos center = new BlockPos(0, 0, 0);
        CylinderOutline outline = new CylinderOutline(center, 10, 0, 1.0f, 1.0f, 1.0f, 1.0f);
        PoseStack poseStack = new PoseStack();
        Vec3 camera = Vec3.ZERO;

        when(mockConsumer.color(eq(1.0f), eq(1.0f), eq(1.0f), eq(1.0f))).thenReturn(mockConsumer);

        outline.render(poseStack, mockConsumer, camera);

        int expectedVertices = (SEGMENTS * 2 * 2) + (4 * 2);
        verify(mockConsumer, times(expectedVertices)).vertex(anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    void testRenderWithDifferentColors() {
        BlockPos center = new BlockPos(0, 0, 0);
        float red = 0.25f;
        float green = 0.5f;
        float blue = 0.75f;
        float alpha = 0.9f;

        CylinderOutline outline = new CylinderOutline(center, 5, 5, red, green, blue, alpha);
        PoseStack poseStack = new PoseStack();
        Vec3 camera = Vec3.ZERO;

        when(mockConsumer.color(eq(red), eq(green), eq(blue), eq(alpha))).thenReturn(mockConsumer);

        outline.render(poseStack, mockConsumer, camera);

        int expectedVertices = (SEGMENTS * 2 * 2) + (4 * 2);
        verify(mockConsumer, times(expectedVertices)).color(red, green, blue, alpha);
    }
}
