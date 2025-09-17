package org.sosly.villageworks.event;

import net.minecraftforge.eventbus.api.Event;
import org.sosly.villageworks.api.IProfession;
import org.sosly.villageworks.profession.ProfessionRegistry;

public class RegisterProfessionsEvent extends Event {
    private final ProfessionRegistry registry;

    public RegisterProfessionsEvent(ProfessionRegistry professions) {
        this.registry = professions;
    }

    public void register(IProfession profession) {
        registry.register(profession);
    }
}
