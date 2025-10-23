package org.sosly.villagetale.zone.shape;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PointTest {
    @Test
    void testContainsPositionExact() {
        BlockPos pos = new BlockPos(10, 20, 30);
        Point point = new Point(pos);

        assertTrue(point.containsPosition(pos));
        assertTrue(point.containsPosition(new BlockPos(10, 20, 30)));
    }

    @Test
    void testContainsPositionDifferent() {
        BlockPos pos = new BlockPos(10, 20, 30);
        Point point = new Point(pos);

        assertFalse(point.containsPosition(new BlockPos(11, 20, 30)));
        assertFalse(point.containsPosition(new BlockPos(10, 21, 30)));
        assertFalse(point.containsPosition(new BlockPos(10, 20, 31)));
        assertFalse(point.containsPosition(new BlockPos(0, 0, 0)));
    }

    @Test
    void testGetStartPosition() {
        BlockPos pos = new BlockPos(5, 10, 15);
        Point point = new Point(pos);

        assertEquals(pos, point.getStartPosition());
    }

    @Test
    void testSerializeAndDeserialize() {
        BlockPos pos = new BlockPos(100, 200, 300);
        Point point = new Point(pos);

        CompoundTag tag = point.serializeNBT();
        Point deserialized = new Point();
        deserialized.deserializeNBT(tag);

        assertEquals(pos, deserialized.getPos());
    }

    @Test
    void testGetID() {
        Point point = new Point(new BlockPos(0, 0, 0));
        assertEquals("villagetale:point", point.getID().toString());
    }

    @Test
    void testSerializeAtOrigin() {
        BlockPos pos = BlockPos.ZERO;
        Point point = new Point(pos);

        CompoundTag tag = point.serializeNBT();
        Point deserialized = new Point();
        deserialized.deserializeNBT(tag);

        assertEquals(pos, deserialized.getPos());
    }

    @Test
    void testSerializeNegativeCoordinates() {
        BlockPos pos = new BlockPos(-50, -100, -150);
        Point point = new Point(pos);

        CompoundTag tag = point.serializeNBT();
        Point deserialized = new Point();
        deserialized.deserializeNBT(tag);

        assertEquals(pos, deserialized.getPos());
    }

    @Test
    void testGetPOIsMatching() {
        BlockPos pos = new BlockPos(10, 20, 30);
        Point point = new Point(pos);

        var pois = point.getPOIs(p -> true);

        assertEquals(1, pois.size());
        assertEquals(pos, pois.get(0));
    }

    @Test
    void testGetPOIsNotMatching() {
        BlockPos pos = new BlockPos(10, 20, 30);
        Point point = new Point(pos);

        var pois = point.getPOIs(p -> false);

        assertTrue(pois.isEmpty());
    }

    @Test
    void testGetPOIsWithSpecificPredicate() {
        BlockPos pos = new BlockPos(5, 10, 15);
        Point point = new Point(pos);

        var pois = point.getPOIs(p -> p.getY() == 10);

        assertEquals(1, pois.size());
        assertEquals(pos, pois.get(0));
    }
}
