package org.sosly.villagetale.zone.shape;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoxTest {
    @Test
    void testContainsPositionInside() {
        AABB bounds = new AABB(0, 0, 0, 10, 10, 10);
        Box box = new Box(bounds);

        assertTrue(box.containsPosition(new BlockPos(5, 5, 5)));
        assertTrue(box.containsPosition(new BlockPos(0, 0, 0)));
        assertTrue(box.containsPosition(new BlockPos(9, 9, 9)));
    }

    @Test
    void testContainsPositionOutside() {
        AABB bounds = new AABB(0, 0, 0, 10, 10, 10);
        Box box = new Box(bounds);

        assertFalse(box.containsPosition(new BlockPos(11, 5, 5)));
        assertFalse(box.containsPosition(new BlockPos(5, 11, 5)));
        assertFalse(box.containsPosition(new BlockPos(5, 5, 11)));
        assertFalse(box.containsPosition(new BlockPos(-1, 5, 5)));
    }

    @Test
    void testContainsPositionWithBuffer() {
        AABB bounds = new AABB(0, 0, 0, 10, 10, 10);
        Box box = new Box(bounds);

        assertFalse(box.containsPosition(new BlockPos(12, 5, 5), 0));
        assertTrue(box.containsPosition(new BlockPos(12, 5, 5), 3));
        assertFalse(box.containsPosition(new BlockPos(5, 12, 5), 1));
    }

    @Test
    void testGetStartPositionCenter() {
        AABB bounds = new AABB(0, 0, 0, 10, 10, 10);
        Box box = new Box(bounds);

        BlockPos start = box.getStartPosition();
        assertEquals(5, start.getX());
        assertEquals(0, start.getY());
        assertEquals(5, start.getZ());
    }

    @Test
    void testGetStartPositionOffsetBounds() {
        AABB bounds = new AABB(10, 20, 30, 20, 30, 40);
        Box box = new Box(bounds);

        BlockPos start = box.getStartPosition();
        assertEquals(15, start.getX());
        assertEquals(20, start.getY());
        assertEquals(35, start.getZ());
    }

    @Test
    void testSerializeAndDeserialize() {
        AABB bounds = new AABB(5, 10, 15, 25, 30, 35);
        Box box = new Box(bounds);

        CompoundTag tag = box.serializeNBT();
        Box deserialized = new Box();
        deserialized.deserializeNBT(tag);

        AABB deserializedBounds = deserialized.getBounds();
        assertEquals(5, (int) deserializedBounds.minX);
        assertEquals(10, (int) deserializedBounds.minY);
        assertEquals(15, (int) deserializedBounds.minZ);
        assertEquals(25, (int) deserializedBounds.maxX);
        assertEquals(30, (int) deserializedBounds.maxY);
        assertEquals(35, (int) deserializedBounds.maxZ);
    }

    @Test
    void testSerializeEmptyBox() {
        Box box = new Box(new AABB(0, 0, 0, 0, 0, 0));

        CompoundTag tag = box.serializeNBT();
        assertNotNull(tag);
        assertTrue(tag.contains("min"));
        assertTrue(tag.contains("max"));
    }

    @Test
    void testGetID() {
        Box box = new Box(new AABB(0, 0, 0, 1, 1, 1));
        assertEquals("villagetale:box", box.getID().toString());
    }

    @Test
    void testContainsPositionEdgeCases() {
        AABB bounds = new AABB(0, 0, 0, 10, 10, 10);
        Box box = new Box(bounds);

        assertTrue(box.containsPosition(new BlockPos(9, 9, 9)));
        assertFalse(box.containsPosition(new BlockPos(10, 10, 10)));
        assertFalse(box.containsPosition(new BlockPos(10, 10, 11)));
    }

    @Test
    void testGetPOIsWithAllMatching() {
        AABB bounds = new AABB(0, 0, 0, 3, 1, 3);
        Box box = new Box(bounds);

        var pois = box.getPOIs(pos -> true);

        assertEquals(9, pois.size());
    }

    @Test
    void testGetPOIsWithNoneMatching() {
        AABB bounds = new AABB(0, 0, 0, 3, 1, 3);
        Box box = new Box(bounds);

        var pois = box.getPOIs(pos -> false);

        assertTrue(pois.isEmpty());
    }

    @Test
    void testGetPOIsWithSelectiveMatching() {
        AABB bounds = new AABB(0, 0, 0, 10, 1, 10);
        Box box = new Box(bounds);

        var pois = box.getPOIs(pos -> pos.getX() == 5 && pos.getZ() == 5);

        assertEquals(1, pois.size());
        assertEquals(new BlockPos(5, 0, 5), pois.get(0));
    }
}
