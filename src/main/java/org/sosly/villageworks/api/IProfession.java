package org.sosly.villageworks.api;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Schedule;
import org.sosly.villageworks.api.data.IVillageZone;
import org.sosly.villageworks.api.data.IWantedItem;
import org.sosly.villageworks.entity.Villager;

/**
 * Defines a villager profession with its tools, preferences, and work zone requirements.
 */
public interface IProfession {
    /**
     * Gets the unique identifier for this profession.
     * @return The profession's resource location ID
     */
    public ResourceLocation getID();

    /**
     * Gets the items this profession always wants to keep in inventory.
     * @return Optional containing the wanted items specification, or empty if no specific items required
     */
    public Optional<IWantedItem> getAlwaysWantedItems();

    /**
     * Gets the tool requirement for this profession.
     * @return Optional containing the wanted tool specification, or empty if no tools required
     */
    public Optional<IWantedItem> getTool();

    /**
     * Gets the translation key for displaying this profession's name.
     * @return The translation key string
     */
    public String getTranslationKey();

    /**
     * Checks if a zone is valid for this profession to work in.
     * @param zone The zone to validate
     * @return True if this profession can work in the zone
     */
    public boolean isValidWorkZone(IVillageZone zone);

    /**
     * Gets the memory modules this profession requires.
     * @return List of memory modules to add to the villager's brain
     */
    public ImmutableList<MemoryModuleType<?>> getMemoryModules();

    /**
     * Gets the sensors this profession requires.
     * @return List of sensors to add to the villager's brain
     */
    public ImmutableList<SensorType<? extends Sensor<? super Villager>>> getSensors();

    /**
     * Gets the work activity behaviors for this profession.
     * @param speedModifier The speed modifier for movement behaviors
     * @return List of prioritized behaviors for the WORK activity
     */
    public ImmutableList<? extends Pair<Integer, ? extends BehaviorControl<? super Villager>>> getWorkPackage(float speedModifier);

    /**
     * Registers profession-specific behaviors to the villager's brain.
     * @param brain The villager's brain to add behaviors to
     */
    public void registerAdditionalGoals(Brain<Villager> brain);

    /**
     * Gets the work schedule for this profession.
     * @return The schedule defining when this profession works, rests, and socializes
     */
    public Schedule getSchedule();
}
