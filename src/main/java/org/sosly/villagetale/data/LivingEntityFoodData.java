package org.sosly.villagetale.data;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodData;

public class LivingEntityFoodData extends FoodData {

    public void tick(LivingEntity entity) {
        if (this.getExhaustionLevel() <= 4.0F) {
            return;
        }

        this.setExhaustion(this.getExhaustionLevel() - 4.0F);
        if (this.getSaturationLevel() > 0.0F) {
            this.setSaturation(Math.max(this.getSaturationLevel() - 1.0F, 0.0F));
            return;
        }

        this.setFoodLevel(Math.max(this.getFoodLevel() - 1, 0));
    }
}
