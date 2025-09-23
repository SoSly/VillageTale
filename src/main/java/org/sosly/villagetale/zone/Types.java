package org.sosly.villagetale.zone;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.sosly.villagetale.event.RegisterZoneTypesEvent;
import org.sosly.villagetale.zone.type.Farmland;
import org.sosly.villagetale.zone.type.Home;
import org.sosly.villagetale.zone.type.Kitchen;
import org.sosly.villagetale.zone.type.Storage;
import org.sosly.villagetale.zone.type.TownHall;

//@Mod.EventBusSubscriber(modid = VillageTale.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class Types {
    @SubscribeEvent
    public static void onRegisterZoneTypes(RegisterZoneTypesEvent event) {
        event.register(Farmland.ID, Farmland::new);
        event.register(Home.ID, Home::new);
        event.register(Kitchen.ID, Kitchen::new);
        event.register(Storage.ID, Storage::new);
        event.register(TownHall.ID, TownHall::new);
    }
}
