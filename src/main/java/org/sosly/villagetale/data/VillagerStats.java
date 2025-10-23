package org.sosly.villagetale.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;

public class VillagerStats {
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

    public int getEndurance() {
        return endurance;
    }

    public int getIntellect() {
        return intellect;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Physique", physique);
        tag.putInt("Endurance", endurance);
        tag.putInt("Intellect", intellect);
        return tag;
    }

    public void deserializeInto(CompoundTag nbt, RandomSource random) {
        if (nbt.contains("Physique") && nbt.contains("Endurance") && nbt.contains("Intellect")) {
            this.physique = nbt.getInt("Physique");
            this.endurance = nbt.getInt("Endurance");
            this.intellect = nbt.getInt("Intellect");
            return;
        }

        this.physique = 1 + random.nextInt(4);
        this.endurance = 1 + random.nextInt(4);
        this.intellect = 1 + random.nextInt(4);
    }
}
