package org.sosly.villagetale.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VillageInfoTest {
    private UUID villageId;
    private BlockPos townHallPos;
    private ChunkPos startingChunk;
    private String villageName;
    private int squadius;
    private VillageInfo villageInfo;

    @BeforeEach
    void setUp() {
        villageId = UUID.randomUUID();
        townHallPos = new BlockPos(100, 64, 200);
        startingChunk = new ChunkPos(5, 10);
        villageName = "TestVillage";
        squadius = 3;
        villageInfo = new VillageInfo(villageId, townHallPos, startingChunk, villageName, squadius);
    }

    @Test
    void testConstructorAndGetters() {
        assertEquals(villageId, villageInfo.getVillageId());
        assertEquals(townHallPos, villageInfo.getTownHallPos());
        assertEquals(startingChunk, villageInfo.getVillageStartingChunk());
        assertEquals(villageName, villageInfo.getVillageName());
        assertEquals(squadius, villageInfo.getSquadius());
    }

    @Test
    void testSetTownHallPos() {
        BlockPos newPos = new BlockPos(150, 64, 250);
        villageInfo.setTownHallPos(newPos);
        assertEquals(newPos, villageInfo.getTownHallPos());
    }

    @Test
    void testSetVillageName() {
        String newName = "NewVillageName";
        villageInfo.setVillageName(newName);
        assertEquals(newName, villageInfo.getVillageName());
    }

    @Test
    void testContainsChunkWithTownHall() {
        ChunkPos centerChunk = new ChunkPos(townHallPos);
        assertTrue(villageInfo.containsChunk(centerChunk));
        assertTrue(villageInfo.containsChunk(new ChunkPos(centerChunk.x + squadius, centerChunk.z)));
        assertTrue(villageInfo.containsChunk(new ChunkPos(centerChunk.x - squadius, centerChunk.z)));
        assertTrue(villageInfo.containsChunk(new ChunkPos(centerChunk.x, centerChunk.z + squadius)));
        assertTrue(villageInfo.containsChunk(new ChunkPos(centerChunk.x, centerChunk.z - squadius)));
    }

    @Test
    void testContainsChunkOutOfRange() {
        ChunkPos centerChunk = new ChunkPos(townHallPos);
        assertFalse(villageInfo.containsChunk(new ChunkPos(centerChunk.x + squadius + 1, centerChunk.z)));
        assertFalse(villageInfo.containsChunk(new ChunkPos(centerChunk.x - squadius - 1, centerChunk.z)));
        assertFalse(villageInfo.containsChunk(new ChunkPos(centerChunk.x, centerChunk.z + squadius + 1)));
        assertFalse(villageInfo.containsChunk(new ChunkPos(centerChunk.x, centerChunk.z - squadius - 1)));
    }

    @Test
    void testContainsChunkWithNullChunk() {
        assertFalse(villageInfo.containsChunk(null));
    }

    @Test
    void testContainsChunkWithNullTownHall() {
        VillageInfo infoWithoutTownHall = new VillageInfo(villageId, null, startingChunk, villageName, squadius);
        assertTrue(infoWithoutTownHall.containsChunk(startingChunk));
        assertTrue(infoWithoutTownHall.containsChunk(new ChunkPos(startingChunk.x + squadius, startingChunk.z)));
        assertFalse(infoWithoutTownHall.containsChunk(new ChunkPos(startingChunk.x + squadius + 1, startingChunk.z)));
    }

    @Test
    void testOverlapsWithSameVillage() {
        assertTrue(villageInfo.overlaps(villageInfo));
    }

    @Test
    void testOverlapsWithNull() {
        assertFalse(villageInfo.overlaps(null));
    }

    @Test
    void testOverlapsWithNearbyVillage() {
        ChunkPos nearbyChunk = new ChunkPos(townHallPos);
        VillageInfo nearbyVillage = new VillageInfo(UUID.randomUUID(), new BlockPos((nearbyChunk.x + 2) * 16, 64, nearbyChunk.z * 16), nearbyChunk, "NearbyVillage", 3);
        assertTrue(villageInfo.overlaps(nearbyVillage));
    }

    @Test
    void testDoesNotOverlapWithDistantVillage() {
        ChunkPos distantChunk = new ChunkPos(100, 100);
        VillageInfo distantVillage = new VillageInfo(UUID.randomUUID(), new BlockPos(distantChunk.x * 16, 64, distantChunk.z * 16), distantChunk, "DistantVillage", 3);
        assertFalse(villageInfo.overlaps(distantVillage));
    }

    @Test
    void testOverlapsWithMinDistance() {
        ChunkPos centerChunk = new ChunkPos(townHallPos);
        ChunkPos distantChunk = new ChunkPos(centerChunk.x + 10, centerChunk.z + 10);
        VillageInfo distantVillage = new VillageInfo(UUID.randomUUID(), new BlockPos(distantChunk.x * 16, 64, distantChunk.z * 16), distantChunk, "DistantVillage", 2);

        assertFalse(villageInfo.overlaps(distantVillage, 0));
        assertTrue(villageInfo.overlaps(distantVillage, 10));
    }

    @Test
    void testOverlapsWithNullTownHalls() {
        VillageInfo village1 = new VillageInfo(UUID.randomUUID(), null, new ChunkPos(0, 0), "Village1", 3);
        VillageInfo village2 = new VillageInfo(UUID.randomUUID(), null, new ChunkPos(2, 2), "Village2", 3);
        assertTrue(village1.overlaps(village2));
    }

    @Test
    void testSerializeAndDeserialize() {
        CompoundTag tag = villageInfo.serializeNBT();
        VillageInfo deserialized = VillageInfo.deserializeNBT(tag);

        assertNotNull(deserialized);
        assertEquals(villageInfo.getVillageId(), deserialized.getVillageId());
        assertEquals(villageInfo.getTownHallPos(), deserialized.getTownHallPos());
        assertEquals(villageInfo.getVillageStartingChunk().toLong(), deserialized.getVillageStartingChunk().toLong());
        assertEquals(villageInfo.getVillageName(), deserialized.getVillageName());
        assertEquals(villageInfo.getSquadius(), deserialized.getSquadius());
    }

    @Test
    void testSerializeWithoutTownHall() {
        VillageInfo infoWithoutTownHall = new VillageInfo(villageId, null, startingChunk, villageName, squadius);
        CompoundTag tag = infoWithoutTownHall.serializeNBT();

        assertFalse(tag.contains("TownHallPos"));
        assertTrue(tag.contains("VillageId"));
        assertTrue(tag.contains("VillageStartingChunk"));
        assertTrue(tag.contains("VillageName"));
        assertTrue(tag.contains("Squadius"));
    }

    @Test
    void testDeserializeWithoutTownHall() {
        CompoundTag tag = new CompoundTag();
        tag.putString("VillageId", villageId.toString());
        tag.putLong("VillageStartingChunk", startingChunk.toLong());
        tag.putString("VillageName", villageName);
        tag.putInt("Squadius", squadius);

        VillageInfo deserialized = VillageInfo.deserializeNBT(tag);

        assertNotNull(deserialized);
        assertNull(deserialized.getTownHallPos());
        assertEquals(villageId, deserialized.getVillageId());
    }

    @Test
    void testDeserializeInvalidTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("VillageId", villageId.toString());

        VillageInfo deserialized = VillageInfo.deserializeNBT(tag);

        assertNull(deserialized);
    }

    @Test
    void testDeserializeInvalidUUID() {
        CompoundTag tag = new CompoundTag();
        tag.putString("VillageId", "not-a-valid-uuid");
        tag.putLong("VillageStartingChunk", startingChunk.toLong());
        tag.putString("VillageName", villageName);
        tag.putInt("Squadius", squadius);

        VillageInfo deserialized = VillageInfo.deserializeNBT(tag);

        assertNull(deserialized);
    }

    @Test
    void testEqualsWithSameId() {
        VillageInfo other = new VillageInfo(villageId, new BlockPos(999, 999, 999), new ChunkPos(99, 99), "DifferentName", 10);
        assertEquals(villageInfo, other);
    }

    @Test
    void testEqualsWithDifferentId() {
        VillageInfo other = new VillageInfo(UUID.randomUUID(), townHallPos, startingChunk, villageName, squadius);
        assertNotEquals(villageInfo, other);
    }

    @Test
    void testEqualsWithSameInstance() {
        assertEquals(villageInfo, villageInfo);
    }

    @Test
    void testEqualsWithNull() {
        assertNotEquals(villageInfo, null);
    }

    @Test
    void testEqualsWithDifferentClass() {
        assertNotEquals(villageInfo, "not a VillageInfo");
    }

    @Test
    void testHashCode() {
        VillageInfo other = new VillageInfo(villageId, new BlockPos(999, 999, 999), new ChunkPos(99, 99), "DifferentName", 10);
        assertEquals(villageInfo.hashCode(), other.hashCode());
    }

    @Test
    void testHashCodeWithDifferentId() {
        VillageInfo other = new VillageInfo(UUID.randomUUID(), townHallPos, startingChunk, villageName, squadius);
        assertNotEquals(villageInfo.hashCode(), other.hashCode());
    }
}
