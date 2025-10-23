package org.sosly.villagetale.zone.shape;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteTest {
    private Route route;

    @BeforeEach
    void setUp() {
        route = new Route();
    }

    @Test
    void testAddPoint() {
        BlockPos pos1 = new BlockPos(0, 0, 0);
        BlockPos pos2 = new BlockPos(10, 0, 0);

        route.addPoint(pos1);
        route.addPoint(pos2);

        List<BlockPos> path = route.getPath();
        assertEquals(2, path.size());
        assertEquals(pos1, path.get(0));
        assertEquals(pos2, path.get(1));
    }

    @Test
    void testClearPath() {
        route.addPoint(new BlockPos(0, 0, 0));
        route.addPoint(new BlockPos(10, 0, 0));

        route.clearPath();

        assertTrue(route.getPath().isEmpty());
    }

    @Test
    void testContainsPositionInRoute() {
        BlockPos pos1 = new BlockPos(0, 0, 0);
        BlockPos pos2 = new BlockPos(10, 0, 0);

        route.addPoint(pos1);
        route.addPoint(pos2);

        assertTrue(route.containsPosition(pos1));
        assertTrue(route.containsPosition(pos2));
    }

    @Test
    void testContainsPositionNotInRoute() {
        route.addPoint(new BlockPos(0, 0, 0));
        route.addPoint(new BlockPos(10, 0, 0));

        assertFalse(route.containsPosition(new BlockPos(5, 0, 0)));
        assertFalse(route.containsPosition(new BlockPos(20, 0, 0)));
    }

    @Test
    void testGetStartPositionWithPoints() {
        BlockPos first = new BlockPos(5, 10, 15);
        BlockPos second = new BlockPos(20, 30, 40);

        route.addPoint(first);
        route.addPoint(second);

        assertEquals(first, route.getStartPosition());
    }

    @Test
    void testGetStartPositionEmpty() {
        assertEquals(BlockPos.ZERO, route.getStartPosition());
    }

    @Test
    void testSerializeAndDeserialize() {
        BlockPos pos1 = new BlockPos(10, 20, 30);
        BlockPos pos2 = new BlockPos(40, 50, 60);
        BlockPos pos3 = new BlockPos(70, 80, 90);

        route.addPoint(pos1);
        route.addPoint(pos2);
        route.addPoint(pos3);

        CompoundTag tag = route.serializeNBT();
        Route deserialized = new Route();
        deserialized.deserializeNBT(tag);

        List<BlockPos> deserializedPath = deserialized.getPath();
        assertEquals(3, deserializedPath.size());
        assertEquals(pos1, deserializedPath.get(0));
        assertEquals(pos2, deserializedPath.get(1));
        assertEquals(pos3, deserializedPath.get(2));
    }

    @Test
    void testSerializeEmptyRoute() {
        CompoundTag tag = route.serializeNBT();
        Route deserialized = new Route();
        deserialized.deserializeNBT(tag);

        assertTrue(deserialized.getPath().isEmpty());
    }

    @Test
    void testGetID() {
        assertEquals("villagetale:route", route.getID().toString());
    }

    @Test
    void testAddMultiplePoints() {
        for (int i = 0; i < 10; i++) {
            route.addPoint(new BlockPos(i * 10, 0, 0));
        }

        assertEquals(10, route.getPath().size());
    }

    @Test
    void testClearAndAddAgain() {
        route.addPoint(new BlockPos(0, 0, 0));
        route.addPoint(new BlockPos(10, 0, 0));
        route.clearPath();

        BlockPos newPos = new BlockPos(20, 0, 0);
        route.addPoint(newPos);

        assertEquals(1, route.getPath().size());
        assertEquals(newPos, route.getPath().get(0));
    }

    @Test
    void testDeserializeOverwritesExistingPath() {
        route.addPoint(new BlockPos(0, 0, 0));
        route.addPoint(new BlockPos(10, 0, 0));

        Route other = new Route();
        BlockPos newPos = new BlockPos(100, 100, 100);
        other.addPoint(newPos);

        CompoundTag tag = other.serializeNBT();
        route.deserializeNBT(tag);

        assertEquals(1, route.getPath().size());
        assertEquals(newPos, route.getPath().get(0));
    }

    @Test
    void testGetPOIsWithAllMatching() {
        route.addPoint(new BlockPos(0, 0, 0));
        route.addPoint(new BlockPos(10, 0, 0));
        route.addPoint(new BlockPos(20, 0, 0));

        var pois = route.getPOIs(pos -> true);

        assertEquals(3, pois.size());
    }

    @Test
    void testGetPOIsWithNoneMatching() {
        route.addPoint(new BlockPos(0, 0, 0));
        route.addPoint(new BlockPos(10, 0, 0));

        var pois = route.getPOIs(pos -> false);

        assertTrue(pois.isEmpty());
    }

    @Test
    void testGetPOIsWithSelectiveMatching() {
        BlockPos match1 = new BlockPos(5, 10, 15);
        BlockPos noMatch = new BlockPos(20, 30, 40);
        BlockPos match2 = new BlockPos(5, 20, 25);

        route.addPoint(match1);
        route.addPoint(noMatch);
        route.addPoint(match2);

        var pois = route.getPOIs(pos -> pos.getX() == 5);

        assertEquals(2, pois.size());
        assertTrue(pois.contains(match1));
        assertTrue(pois.contains(match2));
    }

    @Test
    void testGetPOIsEmptyRoute() {
        var pois = route.getPOIs(pos -> true);

        assertTrue(pois.isEmpty());
    }
}
