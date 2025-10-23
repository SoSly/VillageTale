package org.sosly.villagetale.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class VillagerStats {
    private static final int MIN_STAT_VALUE = 1;
    private static final int MAX_STARTING_STAT_VALUE = 4;
    private static final int MAX_STAT_VALUE = 20;

    private int physique;
    private int endurance;
    private int intellect;

    public VillagerStats() {
        this.physique = 0;
        this.endurance = 0;
        this.intellect = 0;
    }

    public int getPhysique() {
        return physique;
    }

    public void setPhysique(int physique) {
        this.physique = Mth.clamp(physique, MIN_STAT_VALUE, MAX_STAT_VALUE);
    }

    public int getEndurance() {
        return endurance;
    }

    public void setEndurance(int endurance) {
        this.endurance = Mth.clamp(endurance, MIN_STAT_VALUE, MAX_STAT_VALUE);
    }

    public int getIntellect() {
        return intellect;
    }

    public void setIntellect(int intellect) {
        this.intellect = Mth.clamp(intellect, MIN_STAT_VALUE, MAX_STAT_VALUE);
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Physique", physique);
        tag.putInt("Endurance", endurance);
        tag.putInt("Intellect", intellect);
        return tag;
    }

    public void deserializeNBT(CompoundTag nbt, RandomSource random) {
        if (!nbt.contains("Physique") || !nbt.contains("Endurance") || !nbt.contains("Intellect")) {
            this.physique = MIN_STAT_VALUE + random.nextInt(MAX_STARTING_STAT_VALUE);
            this.endurance = MIN_STAT_VALUE + random.nextInt(MAX_STARTING_STAT_VALUE);
            this.intellect = MIN_STAT_VALUE + random.nextInt(MAX_STARTING_STAT_VALUE);
            return;
        }

        this.physique = Mth.clamp(nbt.getInt("Physique"), MIN_STAT_VALUE, MAX_STAT_VALUE);
        this.endurance = Mth.clamp(nbt.getInt("Endurance"), MIN_STAT_VALUE, MAX_STAT_VALUE);
        this.intellect = Mth.clamp(nbt.getInt("Intellect"), MIN_STAT_VALUE, MAX_STAT_VALUE);
    }
}
