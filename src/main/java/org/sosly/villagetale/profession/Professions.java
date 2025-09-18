package org.sosly.villagetale.profession;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.event.RegisterProfessionsEvent;

@Mod.EventBusSubscriber(modid = VillageTale.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)

public class Professions {
    @SubscribeEvent
    public static void onRegisterProfessions(RegisterProfessionsEvent event) {
        event.register(new Commoner());
    }
}
