package org.sosly.villagetale.profession;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.entity.schedule.ScheduleBuilder;
import org.sosly.villagetale.api.IProfession;
import org.sosly.villagetale.entity.Activities;
import org.sosly.villagetale.entity.Villager;

public abstract class AbstractProfession implements IProfession {
    private final ResourceLocation id;

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
    public Schedule getSchedule() {
        return new ScheduleBuilder(new Schedule())
            .changeActivityAt(0, Activities.MORNING_IDLE.get())       // MORNING_IDLE  (6 AM - 8 AM, morning routine)
            .changeActivityAt(2000, Activity.WORK)                    // WORK  (8 AM - 5 PM, 9-hour work day)
            .changeActivityAt(11000, Activities.EVENING_IDLE.get())   // EVENING_IDLE  (5 PM - 8 PM, evening social)
            .changeActivityAt(14000, Activity.REST)                   // REST  (8 PM - 6 AM, sleep)
            .build();
    }

    @Override
    public void registerAdditionalGoals(Brain<Villager> brain) {
    }
}
