package org.sosly.villagetale.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LivingEntityFoodDataTest {
    private LivingEntityFoodData foodData;

    @BeforeEach
    void setUp() {
        foodData = new LivingEntityFoodData();
    }

    @Test
    void testTickDoesNothingWhenExhaustionBelowThreshold() {
        foodData.setExhaustion(3.0F);
        foodData.setFoodLevel(10);
        foodData.setSaturation(5.0F);

        foodData.tick();

        assertEquals(3.0F, foodData.getExhaustionLevel());
        assertEquals(10, foodData.getFoodLevel());
        assertEquals(5.0F, foodData.getSaturationLevel());
    }

    @Test
    void testTickDoesNothingWhenExhaustionAtThreshold() {
        foodData.setExhaustion(4.0F);
        foodData.setFoodLevel(10);
        foodData.setSaturation(5.0F);

        foodData.tick();

        assertEquals(4.0F, foodData.getExhaustionLevel());
        assertEquals(10, foodData.getFoodLevel());
        assertEquals(5.0F, foodData.getSaturationLevel());
    }

    @Test
    void testTickDrainsSaturationWhenExhaustionAboveThreshold() {
        foodData.setExhaustion(5.0F);
        foodData.setFoodLevel(10);
        foodData.setSaturation(5.0F);

        foodData.tick();

        assertEquals(1.0F, foodData.getExhaustionLevel());
        assertEquals(10, foodData.getFoodLevel());
        assertEquals(4.0F, foodData.getSaturationLevel());
    }

    @Test
    void testTickDrainsFoodLevelWhenSaturationIsZero() {
        foodData.setExhaustion(5.0F);
        foodData.setFoodLevel(10);
        foodData.setSaturation(0.0F);

        foodData.tick();

        assertEquals(1.0F, foodData.getExhaustionLevel());
        assertEquals(9, foodData.getFoodLevel());
        assertEquals(0.0F, foodData.getSaturationLevel());
    }

    @Test
    void testTickDoesNotDrainFoodLevelBelowZero() {
        foodData.setExhaustion(5.0F);
        foodData.setFoodLevel(0);
        foodData.setSaturation(0.0F);

        foodData.tick();

        assertEquals(1.0F, foodData.getExhaustionLevel());
        assertEquals(0, foodData.getFoodLevel());
        assertEquals(0.0F, foodData.getSaturationLevel());
    }

    @Test
    void testTickDoesNotDrainSaturationBelowZero() {
        foodData.setExhaustion(5.0F);
        foodData.setFoodLevel(10);
        foodData.setSaturation(0.5F);

        foodData.tick();

        assertEquals(1.0F, foodData.getExhaustionLevel());
        assertEquals(10, foodData.getFoodLevel());
        assertEquals(0.0F, foodData.getSaturationLevel());
    }

    @Test
    void testMultipleTicksDrainSaturationThenFood() {
        foodData.setExhaustion(10.0F);
        foodData.setFoodLevel(20);
        foodData.setSaturation(2.0F);

        foodData.tick();
        assertEquals(6.0F, foodData.getExhaustionLevel());
        assertEquals(20, foodData.getFoodLevel());
        assertEquals(1.0F, foodData.getSaturationLevel());

        foodData.tick();
        assertEquals(2.0F, foodData.getExhaustionLevel());
        assertEquals(20, foodData.getFoodLevel());
        assertEquals(0.0F, foodData.getSaturationLevel());

        foodData.setExhaustion(5.0F);
        foodData.tick();
        assertEquals(1.0F, foodData.getExhaustionLevel());
        assertEquals(19, foodData.getFoodLevel());
        assertEquals(0.0F, foodData.getSaturationLevel());
    }
}
