package org.sosly.villagetale.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BoundaryOutlineTest {
    @Mock
    private VertexConsumer mockConsumer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockConsumer.vertex(anyDouble(), anyDouble(), anyDouble())).thenReturn(mockConsumer);
        when(mockConsumer.color(eq(1.0f), eq(0.0f), eq(0.0f), eq(1.0f))).thenReturn(mockConsumer);
    }

    @Test
    void testRenderDraws24Vertices() {
        AABB bounds = new AABB(0, 0, 0, 10, 10, 10);
        BoundaryOutline outline = new BoundaryOutline(bounds, 1.0f, 0.0f, 0.0f, 1.0f);
        PoseStack poseStack = new PoseStack();
        Vec3 camera = new Vec3(0, 0, 0);

        outline.render(poseStack, mockConsumer, camera);

        verify(mockConsumer, times(24)).vertex(anyDouble(), anyDouble(), anyDouble());
        verify(mockConsumer, times(24)).color(1.0f, 0.0f, 0.0f, 1.0f);
        verify(mockConsumer, times(24)).endVertex();
    }

    @Test
    void testRenderWithCameraOffset() {
        AABB bounds = new AABB(10, 20, 30, 20, 30, 40);
        BoundaryOutline outline = new BoundaryOutline(bounds, 0.0f, 1.0f, 0.0f, 0.5f);
        PoseStack poseStack = new PoseStack();
        Vec3 camera = new Vec3(5, 10, 15);

        when(mockConsumer.color(eq(0.0f), eq(1.0f), eq(0.0f), eq(0.5f))).thenReturn(mockConsumer);

        outline.render(poseStack, mockConsumer, camera);

        verify(mockConsumer, times(24)).vertex(anyDouble(), anyDouble(), anyDouble());
        verify(mockConsumer, times(24)).color(0.0f, 1.0f, 0.0f, 0.5f);
        verify(mockConsumer, times(24)).endVertex();
    }

    @Test
    void testRenderWithDifferentColors() {
        AABB bounds = new AABB(0, 0, 0, 1, 1, 1);

        float red = 0.25f;
        float green = 0.5f;
        float blue = 0.75f;
        float alpha = 0.9f;

        BoundaryOutline outline = new BoundaryOutline(bounds, red, green, blue, alpha);
        PoseStack poseStack = new PoseStack();
        Vec3 camera = Vec3.ZERO;

        when(mockConsumer.color(eq(red), eq(green), eq(blue), eq(alpha))).thenReturn(mockConsumer);

        outline.render(poseStack, mockConsumer, camera);

        verify(mockConsumer, times(24)).color(red, green, blue, alpha);
    }

    @Test
    void testRenderVerifiesVertexPositions() {
        AABB bounds = new AABB(0, 0, 0, 10, 10, 10);
        BoundaryOutline outline = new BoundaryOutline(bounds, 1.0f, 1.0f, 1.0f, 1.0f);
        PoseStack poseStack = new PoseStack();
        Vec3 camera = Vec3.ZERO;

        when(mockConsumer.color(eq(1.0f), eq(1.0f), eq(1.0f), eq(1.0f))).thenReturn(mockConsumer);

        outline.render(poseStack, mockConsumer, camera);

        ArgumentCaptor<Double> xCaptor = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> yCaptor = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> zCaptor = ArgumentCaptor.forClass(Double.class);

        verify(mockConsumer, times(24)).vertex(xCaptor.capture(), yCaptor.capture(), zCaptor.capture());

        for (Double x : xCaptor.getAllValues()) {
            assert x >= 0.0 && x <= 10.0 : "X coordinate out of bounds: " + x;
        }
        for (Double y : yCaptor.getAllValues()) {
            assert y >= 0.0 && y <= 10.0 : "Y coordinate out of bounds: " + y;
        }
        for (Double z : zCaptor.getAllValues()) {
            assert z >= 0.0 && z <= 10.0 : "Z coordinate out of bounds: " + z;
        }
    }

    @Test
    void testConstructorStoresValues() {
        AABB bounds = new AABB(1, 2, 3, 4, 5, 6);
        float red = 0.1f;
        float green = 0.2f;
        float blue = 0.3f;
        float alpha = 0.4f;

        BoundaryOutline outline = new BoundaryOutline(bounds, red, green, blue, alpha);
        PoseStack poseStack = new PoseStack();
        Vec3 camera = Vec3.ZERO;

        when(mockConsumer.color(eq(red), eq(green), eq(blue), eq(alpha))).thenReturn(mockConsumer);

        outline.render(poseStack, mockConsumer, camera);

        verify(mockConsumer, times(24)).color(red, green, blue, alpha);
    }
}
