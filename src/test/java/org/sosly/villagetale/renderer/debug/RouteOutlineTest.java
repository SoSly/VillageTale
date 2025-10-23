package org.sosly.villagetale.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RouteOutlineTest {
    @Mock
    private VertexConsumer mockConsumer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockConsumer.vertex(anyDouble(), anyDouble(), anyDouble())).thenReturn(mockConsumer);
        when(mockConsumer.color(eq(1.0f), eq(0.0f), eq(0.0f), eq(1.0f))).thenReturn(mockConsumer);
    }

    @Test
    void testRenderWithTwoWaypoints() {
        List<BlockPos> waypoints = Arrays.asList(
            new BlockPos(0, 0, 0),
            new BlockPos(10, 0, 0)
        );
        RouteOutline outline = new RouteOutline(waypoints, 1.0f, 0.0f, 0.0f, 1.0f);
        PoseStack poseStack = new PoseStack();
        Vec3 camera = Vec3.ZERO;

        outline.render(poseStack, mockConsumer, camera);

        verify(mockConsumer, times(2)).vertex(anyDouble(), anyDouble(), anyDouble());
        verify(mockConsumer, times(2)).color(1.0f, 0.0f, 0.0f, 1.0f);
        verify(mockConsumer, times(2)).endVertex();
    }

    @Test
    void testRenderWithMultipleWaypoints() {
        List<BlockPos> waypoints = Arrays.asList(
            new BlockPos(0, 0, 0),
            new BlockPos(10, 0, 0),
            new BlockPos(10, 10, 0),
            new BlockPos(10, 10, 10)
        );
        RouteOutline outline = new RouteOutline(waypoints, 0.0f, 1.0f, 0.0f, 0.5f);
        PoseStack poseStack = new PoseStack();
        Vec3 camera = Vec3.ZERO;

        when(mockConsumer.color(eq(0.0f), eq(1.0f), eq(0.0f), eq(0.5f))).thenReturn(mockConsumer);

        outline.render(poseStack, mockConsumer, camera);

        int expectedSegments = waypoints.size() - 1;
        verify(mockConsumer, times(expectedSegments * 2)).vertex(anyDouble(), anyDouble(), anyDouble());
        verify(mockConsumer, times(expectedSegments * 2)).color(0.0f, 1.0f, 0.0f, 0.5f);
        verify(mockConsumer, times(expectedSegments * 2)).endVertex();
    }

    @Test
    void testRenderWithNullWaypoints() {
        RouteOutline outline = new RouteOutline(null, 1.0f, 1.0f, 1.0f, 1.0f);
        PoseStack poseStack = new PoseStack();
        Vec3 camera = Vec3.ZERO;

        outline.render(poseStack, mockConsumer, camera);

        verify(mockConsumer, never()).vertex(anyDouble(), anyDouble(), anyDouble());
        verify(mockConsumer, never()).color(eq(1.0f), eq(1.0f), eq(1.0f), eq(1.0f));
        verify(mockConsumer, never()).endVertex();
    }

    @Test
    void testRenderWithEmptyWaypoints() {
        List<BlockPos> waypoints = new ArrayList<>();
        RouteOutline outline = new RouteOutline(waypoints, 1.0f, 1.0f, 1.0f, 1.0f);
        PoseStack poseStack = new PoseStack();
        Vec3 camera = Vec3.ZERO;

        outline.render(poseStack, mockConsumer, camera);

        verify(mockConsumer, never()).vertex(anyDouble(), anyDouble(), anyDouble());
        verify(mockConsumer, never()).color(eq(1.0f), eq(1.0f), eq(1.0f), eq(1.0f));
        verify(mockConsumer, never()).endVertex();
    }

    @Test
    void testRenderWithSingleWaypoint() {
        List<BlockPos> waypoints = Arrays.asList(new BlockPos(0, 0, 0));
        RouteOutline outline = new RouteOutline(waypoints, 1.0f, 1.0f, 1.0f, 1.0f);
        PoseStack poseStack = new PoseStack();
        Vec3 camera = Vec3.ZERO;

        outline.render(poseStack, mockConsumer, camera);

        verify(mockConsumer, never()).vertex(anyDouble(), anyDouble(), anyDouble());
        verify(mockConsumer, never()).color(eq(1.0f), eq(1.0f), eq(1.0f), eq(1.0f));
        verify(mockConsumer, never()).endVertex();
    }

    @Test
    void testRenderWithCameraOffset() {
        List<BlockPos> waypoints = Arrays.asList(
            new BlockPos(10, 20, 30),
            new BlockPos(20, 30, 40)
        );
        RouteOutline outline = new RouteOutline(waypoints, 0.5f, 0.5f, 0.5f, 0.8f);
        PoseStack poseStack = new PoseStack();
        Vec3 camera = new Vec3(5, 10, 15);

        when(mockConsumer.color(eq(0.5f), eq(0.5f), eq(0.5f), eq(0.8f))).thenReturn(mockConsumer);

        outline.render(poseStack, mockConsumer, camera);

        verify(mockConsumer, times(2)).vertex(anyDouble(), anyDouble(), anyDouble());
        verify(mockConsumer, times(2)).color(0.5f, 0.5f, 0.5f, 0.8f);
        verify(mockConsumer, times(2)).endVertex();
    }

    @Test
    void testRenderWithDifferentColors() {
        List<BlockPos> waypoints = Arrays.asList(
            new BlockPos(0, 0, 0),
            new BlockPos(1, 1, 1)
        );

        float red = 0.25f;
        float green = 0.5f;
        float blue = 0.75f;
        float alpha = 0.9f;

        RouteOutline outline = new RouteOutline(waypoints, red, green, blue, alpha);
        PoseStack poseStack = new PoseStack();
        Vec3 camera = Vec3.ZERO;

        when(mockConsumer.color(eq(red), eq(green), eq(blue), eq(alpha))).thenReturn(mockConsumer);

        outline.render(poseStack, mockConsumer, camera);

        verify(mockConsumer, times(2)).color(red, green, blue, alpha);
    }
}
