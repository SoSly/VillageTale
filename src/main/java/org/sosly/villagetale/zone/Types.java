package org.sosly.villagetale.zone;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.event.RegisterZoneTypesEvent;
import org.sosly.villagetale.zone.type.Butchery;
import org.sosly.villagetale.zone.type.Dock;
import org.sosly.villagetale.zone.type.Farmland;
import org.sosly.villagetale.zone.type.Forest;
import org.sosly.villagetale.zone.type.Home;
import org.sosly.villagetale.zone.type.Kitchen;
import org.sosly.villagetale.zone.type.Pen;
import org.sosly.villagetale.zone.type.Storage;
import org.sosly.villagetale.zone.type.Tannery;
import org.sosly.villagetale.zone.type.TownHall;
import org.sosly.villagetale.zone.type.Woodshop;

@Mod.EventBusSubscriber(modid = VillageTale.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class Types {
    @SubscribeEvent
    public static void onRegisterZoneTypes(RegisterZoneTypesEvent event) {
        event.register(Butchery.ID, new Butchery());
        event.register(Dock.ID, new Dock());
        event.register(Farmland.ID, new Farmland());
        event.register(Forest.ID, new Forest());
        event.register(Home.ID, new Home());
        event.register(Kitchen.ID, new Kitchen());
        event.register(Pen.ID, new Pen());
        event.register(Storage.ID, new Storage());
        event.register(Tannery.ID, new Tannery());
        event.register(TownHall.ID, new TownHall());
        event.register(Woodshop.ID, new Woodshop());
    }
}
