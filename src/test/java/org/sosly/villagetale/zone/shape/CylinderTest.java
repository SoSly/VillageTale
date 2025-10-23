package org.sosly.villagetale.zone.shape;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CylinderTest {
    @Test
    void testContainsPositionInside() {
        BlockPos center = new BlockPos(0, 0, 0);
        Cylinder cylinder = new Cylinder(center, 5, 10);

        assertTrue(cylinder.containsPosition(new BlockPos(0, 0, 0)));
        assertTrue(cylinder.containsPosition(new BlockPos(3, 5, 3)));
        assertTrue(cylinder.containsPosition(new BlockPos(0, 9, 0)));
    }

    @Test
    void testContainsPositionOutsideRadius() {
        BlockPos center = new BlockPos(0, 0, 0);
        Cylinder cylinder = new Cylinder(center, 5, 10);

        assertFalse(cylinder.containsPosition(new BlockPos(6, 5, 0)));
        assertFalse(cylinder.containsPosition(new BlockPos(0, 5, 6)));
        assertFalse(cylinder.containsPosition(new BlockPos(4, 5, 4)));
    }

    @Test
    void testContainsPositionOutsideHeight() {
        BlockPos center = new BlockPos(0, 0, 0);
        Cylinder cylinder = new Cylinder(center, 5, 10);

        assertFalse(cylinder.containsPosition(new BlockPos(0, -1, 0)));
        assertFalse(cylinder.containsPosition(new BlockPos(0, 10, 0)));
        assertFalse(cylinder.containsPosition(new BlockPos(0, 15, 0)));
    }

    @Test
    void testContainsPositionWithBuffer() {
        BlockPos center = new BlockPos(0, 0, 0);
        Cylinder cylinder = new Cylinder(center, 5, 10);

        assertFalse(cylinder.containsPosition(new BlockPos(6, 5, 0), 0));
        assertTrue(cylinder.containsPosition(new BlockPos(6, 5, 0), 2));
        assertFalse(cylinder.containsPosition(new BlockPos(0, -2, 0), 1));
        assertTrue(cylinder.containsPosition(new BlockPos(0, -2, 0), 3));
    }

    @Test
    void testContainsPositionWithNullCenter() {
        Cylinder cylinder = new Cylinder(null, 5, 10);
        assertFalse(cylinder.containsPosition(new BlockPos(0, 0, 0)));
    }

    @Test
    void testContainsPositionWithNullPos() {
        BlockPos center = new BlockPos(0, 0, 0);
        Cylinder cylinder = new Cylinder(center, 5, 10);
        assertFalse(cylinder.containsPosition(null));
    }

    @Test
    void testGetStartPosition() {
        BlockPos center = new BlockPos(10, 20, 30);
        Cylinder cylinder = new Cylinder(center, 5, 10);

        BlockPos start = cylinder.getStartPosition();
        assertEquals(center, start);
    }

    @Test
    void testSerializeAndDeserialize() {
        BlockPos center = new BlockPos(10, 20, 30);
        Cylinder cylinder = new Cylinder(center, 7, 12);

        CompoundTag tag = cylinder.serializeNBT();
        Cylinder deserialized = new Cylinder();
        deserialized.deserializeNBT(tag);

        assertEquals(center, deserialized.getBaseCenter());
        assertEquals(7, deserialized.getRadius());
        assertEquals(12, deserialized.getHeight());
    }

    @Test
    void testGetID() {
        Cylinder cylinder = new Cylinder(new BlockPos(0, 0, 0), 1, 1);
        assertEquals("villagetale:cylinder", cylinder.getID().toString());
    }

    @Test
    void testContainsPositionAtEdgeOfRadius() {
        BlockPos center = new BlockPos(0, 0, 0);
        Cylinder cylinder = new Cylinder(center, 5, 10);

        assertTrue(cylinder.containsPosition(new BlockPos(5, 5, 0)));
        assertTrue(cylinder.containsPosition(new BlockPos(0, 5, 5)));
        assertTrue(cylinder.containsPosition(new BlockPos(-5, 5, 0)));
        assertTrue(cylinder.containsPosition(new BlockPos(0, 5, -5)));
    }

    @Test
    void testContainsPositionOffsetCenter() {
        BlockPos center = new BlockPos(100, 64, 200);
        Cylinder cylinder = new Cylinder(center, 10, 20);

        assertTrue(cylinder.containsPosition(new BlockPos(100, 64, 200)));
        assertTrue(cylinder.containsPosition(new BlockPos(105, 70, 200)));
        assertFalse(cylinder.containsPosition(new BlockPos(111, 70, 200)));
        assertFalse(cylinder.containsPosition(new BlockPos(100, 84, 200)));
    }

    @Test
    void testGetPOIsWithAllMatching() {
        BlockPos center = new BlockPos(0, 0, 0);
        Cylinder cylinder = new Cylinder(center, 2, 2);

        var pois = cylinder.getPOIs(pos -> true);

        assertTrue(pois.size() > 0, "Should have at least one POI");
    }

    @Test
    void testGetPOIsWithNoneMatching() {
        BlockPos center = new BlockPos(0, 0, 0);
        Cylinder cylinder = new Cylinder(center, 5, 5);

        var pois = cylinder.getPOIs(pos -> false);

        assertTrue(pois.isEmpty());
    }

    @Test
    void testGetPOIsOnlyCenter() {
        BlockPos center = new BlockPos(10, 20, 30);
        Cylinder cylinder = new Cylinder(center, 5, 5);

        var pois = cylinder.getPOIs(pos -> pos.equals(center));

        assertEquals(1, pois.size());
        assertEquals(center, pois.get(0));
    }
}
