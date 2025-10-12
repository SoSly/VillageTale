package org.sosly.villagetale.api;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.sosly.villagetale.data.ItemOrTagMatcher;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Schedule;
import org.sosly.villagetale.entity.Villager;

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
     * @param villager The villager with this profession
     * @return List of wanted items specifications, or empty list if no specific items required
     */
    public List<IWantedItem> getAlwaysWantedItems(Villager villager);

    /**
     * Gets the tool requirements for this profession.
     * @return List of wanted tool specifications, or empty list if no tools required
     */
    public List<IWantedItem> getTools();

    /**
     * Gets the items this profession can learn recipes for.
     * @return ItemOrTagMatcher for learnable recipe outputs
     */
    public ItemOrTagMatcher getLearnableItems();

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

    /**
     * Reloads this profession's data from datapacks.
     * @param data The JSON object containing profession configuration
     */
    public void onDatapackReload(JsonObject data);
}
