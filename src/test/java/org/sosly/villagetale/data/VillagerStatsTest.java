package org.sosly.villagetale.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VillagerStatsTest {
    private VillagerStats stats;

    @BeforeEach
    void setUp() {
        stats = new VillagerStats();
    }

    @Test
    void testConstructorDefaultsToMinimum() {
        assertEquals(1, stats.getPhysique());
        assertEquals(1, stats.getEndurance());
        assertEquals(1, stats.getIntellect());
    }

    @Test
    void testSetPhysiqueClampsToMin() {
        stats.setPhysique(0);

        assertEquals(1, stats.getPhysique());
    }

    @Test
    void testSetPhysiqueClampsToMax() {
        stats.setPhysique(25);

        assertEquals(20, stats.getPhysique());
    }

    @Test
    void testSetPhysiqueAcceptsValidValue() {
        stats.setPhysique(10);

        assertEquals(10, stats.getPhysique());
    }

    @Test
    void testSetEnduranceClampsToMin() {
        stats.setEndurance(-5);

        assertEquals(1, stats.getEndurance());
    }

    @Test
    void testSetEnduranceClampsToMax() {
        stats.setEndurance(100);

        assertEquals(20, stats.getEndurance());
    }

    @Test
    void testSetEnduranceAcceptsValidValue() {
        stats.setEndurance(15);

        assertEquals(15, stats.getEndurance());
    }

    @Test
    void testSetIntellectClampsToMin() {
        stats.setIntellect(0);

        assertEquals(1, stats.getIntellect());
    }

    @Test
    void testSetIntellectClampsToMax() {
        stats.setIntellect(30);

        assertEquals(20, stats.getIntellect());
    }

    @Test
    void testSetIntellectAcceptsValidValue() {
        stats.setIntellect(8);

        assertEquals(8, stats.getIntellect());
    }

    @Test
    void testSerializeNBTCreatesCorrectTags() {
        stats.setPhysique(10);
        stats.setEndurance(15);
        stats.setIntellect(8);

        CompoundTag tag = stats.serializeNBT();

        assertTrue(tag.contains("Physique"));
        assertTrue(tag.contains("Endurance"));
        assertTrue(tag.contains("Intellect"));
        assertEquals(10, tag.getInt("Physique"));
        assertEquals(15, tag.getInt("Endurance"));
        assertEquals(8, tag.getInt("Intellect"));
    }

    @Test
    void testDeserializeNBTWithValidData() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Physique", 12);
        tag.putInt("Endurance", 9);
        tag.putInt("Intellect", 14);
        RandomSource random = mock(RandomSource.class);

        stats.deserializeNBT(tag, random);

        assertEquals(12, stats.getPhysique());
        assertEquals(9, stats.getEndurance());
        assertEquals(14, stats.getIntellect());
    }

    @Test
    void testDeserializeNBTClampsOutOfBoundsValues() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Physique", 0);
        tag.putInt("Endurance", 25);
        tag.putInt("Intellect", -10);
        RandomSource random = mock(RandomSource.class);

        stats.deserializeNBT(tag, random);

        assertEquals(1, stats.getPhysique());
        assertEquals(20, stats.getEndurance());
        assertEquals(1, stats.getIntellect());
    }

    @Test
    void testDeserializeNBTWithMissingStatsGeneratesRandom() {
        CompoundTag tag = new CompoundTag();
        RandomSource random = mock(RandomSource.class);
        when(random.nextInt(4)).thenReturn(2, 1, 3);

        stats.deserializeNBT(tag, random);

        assertEquals(3, stats.getPhysique());
        assertEquals(2, stats.getEndurance());
        assertEquals(4, stats.getIntellect());
    }

    @Test
    void testDeserializeNBTWithPartialStatsGeneratesRandom() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Physique", 10);
        RandomSource random = mock(RandomSource.class);
        when(random.nextInt(4)).thenReturn(2, 1, 3);

        stats.deserializeNBT(tag, random);

        assertEquals(3, stats.getPhysique());
        assertEquals(2, stats.getEndurance());
        assertEquals(4, stats.getIntellect());
    }

    @Test
    void testSerializeDeserializeRoundTrip() {
        stats.setPhysique(7);
        stats.setEndurance(13);
        stats.setIntellect(18);

        CompoundTag tag = stats.serializeNBT();

        VillagerStats deserialized = new VillagerStats();
        RandomSource random = mock(RandomSource.class);
        deserialized.deserializeNBT(tag, random);

        assertEquals(7, deserialized.getPhysique());
        assertEquals(13, deserialized.getEndurance());
        assertEquals(18, deserialized.getIntellect());
    }

    @Test
    void testRandomGenerationStaysWithinBounds() {
        CompoundTag tag = new CompoundTag();
        RandomSource random = mock(RandomSource.class);
        when(random.nextInt(4)).thenReturn(0, 3, 3);

        stats.deserializeNBT(tag, random);

        assertTrue(stats.getPhysique() >= 1 && stats.getPhysique() <= 4);
        assertTrue(stats.getEndurance() >= 1 && stats.getEndurance() <= 4);
        assertTrue(stats.getIntellect() >= 1 && stats.getIntellect() <= 4);
    }
}
