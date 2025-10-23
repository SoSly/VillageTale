package org.sosly.villagetale.profession;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IProfession;
import org.sosly.villagetale.event.RegisterProfessionsEvent;
import org.sosly.villagetale.profession.professions.Butcher;
import org.sosly.villagetale.profession.professions.Carpenter;
import org.sosly.villagetale.profession.professions.Commoner;
import org.sosly.villagetale.profession.professions.Cook;
import org.sosly.villagetale.profession.professions.Farmer;
import org.sosly.villagetale.profession.professions.Fisher;
import org.sosly.villagetale.profession.professions.Forester;
import org.sosly.villagetale.profession.professions.Herder;
import org.sosly.villagetale.profession.professions.Tanner;

@Mod.EventBusSubscriber(modid = VillageTale.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ProfessionRegistry {
    public static final ProfessionRegistry INSTANCE = new ProfessionRegistry();
    private final Map<ResourceLocation, IProfession> professions = new HashMap<ResourceLocation, IProfession>();
    private ProfessionRegistry() {}

    public void register(IProfession profession) {
        ResourceLocation id = profession.getID();
        if (professions.containsKey(id)) {
            VillageTale.LOGGER.warn("Attempted to register profession " + profession.getID() + " twice");
            return;
        }
        professions.put(id, profession);
        VillageTale.LOGGER.debug("Registered profession: {} (total: {})", profession.getID(), professions.size());
    }

    public Optional<IProfession> getProfession(ResourceLocation id) {
        return Optional.ofNullable(professions.get(id));
    }

    public Collection<IProfession> getProfessions() {
        return professions.values();
    }

    public Set<ResourceLocation> getProfessionIDs() {
        return professions.keySet();
    }

    @SubscribeEvent
    public static void onRegisterProfessions(RegisterProfessionsEvent event) {
        event.register(new Butcher());
        event.register(new Carpenter());
        event.register(new Commoner());
        event.register(new Cook());
        event.register(new Farmer());
        event.register(new Fisher());
        event.register(new Forester());
        event.register(new Herder());
        event.register(new Tanner());
    }
}
