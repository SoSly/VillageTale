package org.sosly.villageworks.profession;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.sosly.villageworks.VillageWorks;
import org.sosly.villageworks.event.RegisterProfessionsEvent;

@Mod.EventBusSubscriber(modid= VillageWorks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)

public class Professions {
    @SubscribeEvent
    public static void onRegisterProfessions(RegisterProfessionsEvent event) {
        event.register(new Commoner());
    }
}
