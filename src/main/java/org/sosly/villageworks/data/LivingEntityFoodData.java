package org.sosly.villageworks.data;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodData;

public class LivingEntityFoodData extends FoodData {
    
    public LivingEntityFoodData() {
        super();
    }
    
    public void tick(LivingEntity entity) {
        // TODO: Implement full vanilla tick logic for living entities
        // - Health regeneration when well-fed (saturation > 0, food >= 20)
        // - Slower regeneration when moderately fed (food >= 18)
        // - Starvation damage when food <= 0 (respecting difficulty)
        // - Skip Player-specific checks like GameRules.RULE_NATURAL_REGENERATION
        
        if (this.getExhaustionLevel() > 4.0F) {
            this.setExhaustion(this.getExhaustionLevel() - 4.0F);
            if (this.getSaturationLevel() > 0.0F) {
                this.setSaturation(Math.max(this.getSaturationLevel() - 1.0F, 0.0F));
            } else {
                this.setFoodLevel(Math.max(this.getFoodLevel() - 1, 0));
            }
        }
    }
}