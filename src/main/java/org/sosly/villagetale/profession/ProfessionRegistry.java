package org.sosly.villagetale.profession;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IProfession;
import org.sosly.villagetale.profession.professions.Butcher;
import org.sosly.villagetale.profession.professions.Carpenter;
import org.sosly.villagetale.profession.professions.Commoner;
import org.sosly.villagetale.profession.professions.Cook;
import org.sosly.villagetale.profession.professions.Farmer;
import org.sosly.villagetale.profession.professions.Fisher;
import org.sosly.villagetale.profession.professions.Forester;
import org.sosly.villagetale.profession.professions.Herder;
import org.sosly.villagetale.profession.professions.Tanner;

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

    {
        professions.put(Butcher.ID, new Butcher());
        professions.put(Carpenter.ID, new Carpenter());
        professions.put(Commoner.ID, new Commoner());
        professions.put(Cook.ID, new Cook());
        professions.put(Farmer.ID, new Farmer());
        professions.put(Fisher.ID, new Fisher());
        professions.put(Forester.ID, new Forester());
        professions.put(Herder.ID, new Herder());
        professions.put(Tanner.ID, new Tanner());
    }
}
