package org.sosly.villagetale.profession;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.sosly.villagetale.event.RegisterProfessionsEvent;
import org.sosly.villagetale.profession.professions.Commoner;
import org.sosly.villagetale.profession.professions.Cook;
import org.sosly.villagetale.profession.professions.Farmer;
import org.sosly.villagetale.profession.professions.Forester;

//@Mod.EventBusSubscriber(modid = VillageTale.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class Professions {
    @SubscribeEvent
    public static void onRegisterProfessions(RegisterProfessionsEvent event) {
        event.register(new Commoner());
        event.register(new Cook());
        event.register(new Farmer());
        event.register(new Forester());
    }
}
