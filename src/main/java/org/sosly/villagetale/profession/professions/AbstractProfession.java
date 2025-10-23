package org.sosly.villagetale.profession.professions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.entity.schedule.ScheduleBuilder;
import org.sosly.villagetale.api.IProfession;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.IWantedItem;
import org.sosly.villagetale.data.matchers.ItemOrTagMatcher;
import org.sosly.villagetale.data.WantedItem;
import org.sosly.villagetale.entity.Activities;
import org.sosly.villagetale.entity.Villager;

public abstract class AbstractProfession implements IProfession {
    private final ResourceLocation id;
    private final ItemOrTagMatcher learnableItems = new ItemOrTagMatcher();
    private final List<ResourceLocation> workZones = new ArrayList<>();
    private final List<IWantedItem> tools = new ArrayList<>();
    private final List<IWantedItem> wantedItems = new ArrayList<>();

    protected AbstractProfession(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public ResourceLocation getID() {
        return id;
    }

    @Override
    public String getTranslationKey() {
        return "profession." + getID().toString().replace(":", ".");
    }

    @Override
    public List<IWantedItem> getAlwaysWantedItems(Villager villager) {
        return wantedItems;
    }

    @Override
    public List<IWantedItem> getTools() {
        return tools;
    }

    @Override
    public Schedule getSchedule() {
        return new ScheduleBuilder(new Schedule())
                .changeActivityAt(0, Activities.MORNING_IDLE.get())         // 6 AM - 8 AM
                .changeActivityAt(2000, Activity.WORK)                      // 8 AM - 6 PM
                .changeActivityAt(12000, Activities.EVENING_IDLE.get())     // 6 PM - 10 PM
                .changeActivityAt(16000, Activity.REST)                     // 10 PM - 6 AM
                .build();
    }

    @Override
    public boolean isValidWorkZone(IVillageZone zone) {
        if (zone.getType() == null) {
            return false;
        }
        return workZones.stream()
            .anyMatch(wz -> wz.equals(zone.getType().getID()));
    }

    @Override
    public void registerAdditionalGoals(Brain<Villager> brain) {
    }

    public void onDatapackReload(JsonObject data) {
        if (data.has("learn")) {
            learnableItems.loadFromJson(data.getAsJsonArray("learn"));
        }

        if (data.has("work")) {
            loadWorkZones(data.getAsJsonArray("work"));
        }

        if (data.has("tools")) {
            loadToolItems(data.getAsJsonArray("tools"));
        }

        if (data.has("wanted")) {
            loadWantedItems(data.getAsJsonArray("wanted"));
        }
    }

    private void loadWorkZones(JsonArray work) {
        workZones.clear();
        for (JsonElement element : work) {
            workZones.add(new ResourceLocation(element.getAsString()));
        }
    }

    private void loadToolItems(JsonArray toolArray) {
        tools.clear();
        for (JsonElement element : toolArray) {
            tools.add(parseToolItem(element.getAsJsonObject()));
        }
    }

    private void loadWantedItems(JsonArray wanted) {
        wantedItems.clear();
        for (JsonElement element : wanted) {
            wantedItems.add(parseWantedItem(element.getAsJsonObject()));
        }
    }

    private IWantedItem parseToolItem(JsonObject toolObj) {
        ItemOrTagMatcher matcher = new ItemOrTagMatcher();
        matcher.loadFromJson(toolObj.getAsJsonArray("items"));
        int min = toolObj.has("min") ? toolObj.get("min").getAsInt() : 1;
        int amount = toolObj.has("amount") ? toolObj.get("amount").getAsInt() : 1;
        return new WantedItem(matcher::matches, amount, min);
    }

    private IWantedItem parseWantedItem(JsonObject wantedObj) {
        ItemOrTagMatcher matcher = new ItemOrTagMatcher();
        matcher.loadFromJson(wantedObj.getAsJsonArray("items"));
        int min = wantedObj.has("min") ? wantedObj.get("min").getAsInt() : 0;
        int amount = wantedObj.has("amount") ? wantedObj.get("amount").getAsInt() : 16;
        return new WantedItem(matcher::matches, amount, min);
    }

    public ItemOrTagMatcher getLearnableItems() {
        return learnableItems;
    }

    public List<ResourceLocation> getWorkZones() {
        return workZones;
    }
}
