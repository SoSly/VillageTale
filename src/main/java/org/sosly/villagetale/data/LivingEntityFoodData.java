package org.sosly.villagetale.data;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodData;

public class LivingEntityFoodData extends FoodData {
    private static final float EXHAUSTION_THRESHOLD = 4.0F;
    private static final float EXHAUSTION_DRAIN = 4.0F;
    private static final float SATURATION_DRAIN = 1.0F;
    private static final float MINIMUM_LEVEL = 0.0F;
    private static final int FOOD_LEVEL_DRAIN = 1;
    private static final int MINIMUM_FOOD_LEVEL = 0;

    /**
     * @deprecated Entity parameter is unused. Use {@link #tick()} instead.
     * This method exists temporarily to maintain compatibility with existing code.
     */
    @Deprecated
    public void tick(LivingEntity entity) {
        this.tick();
    }

    public void tick() {
        if (this.getExhaustionLevel() <= EXHAUSTION_THRESHOLD) {
            return;
        }

        this.setExhaustion(this.getExhaustionLevel() - EXHAUSTION_DRAIN);
        if (this.getSaturationLevel() > MINIMUM_LEVEL) {
            this.setSaturation(Math.max(this.getSaturationLevel() - SATURATION_DRAIN, MINIMUM_LEVEL));
            return;
        }

        this.setFoodLevel(Math.max(this.getFoodLevel() - FOOD_LEVEL_DRAIN, MINIMUM_FOOD_LEVEL));
    }
}
