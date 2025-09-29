package org.sosly.villagetale.zone;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.event.RegisterZoneTypesEvent;
import org.sosly.villagetale.zone.type.Farmland;
import org.sosly.villagetale.zone.type.Forest;
import org.sosly.villagetale.zone.type.Home;
import org.sosly.villagetale.zone.type.Kitchen;
import org.sosly.villagetale.zone.type.Storage;
import org.sosly.villagetale.zone.type.TownHall;

@Mod.EventBusSubscriber(modid = VillageTale.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class Types {
    @SubscribeEvent
    public static void onRegisterZoneTypes(RegisterZoneTypesEvent event) {
        event.register(Farmland.ID, new Farmland());
        event.register(Forest.ID, new Forest());
        event.register(Home.ID, new Home());
        event.register(Kitchen.ID, new Kitchen());
        event.register(Storage.ID, new Storage());
        event.register(TownHall.ID, new TownHall());
    }
}
