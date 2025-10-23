package org.sosly.villagetale.event;

import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;
import org.sosly.villagetale.api.IProfession;
import org.sosly.villagetale.profession.ProfessionRegistry;

public class RegisterProfessionsEvent extends Event implements IModBusEvent {
    private final ProfessionRegistry registry;

    public RegisterProfessionsEvent(ProfessionRegistry professions) {
        this.registry = professions;
    }

    public void register(IProfession profession) {
        registry.register(profession);
    }
}
