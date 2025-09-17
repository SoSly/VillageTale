package org.sosly.villageworks.profession;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.sosly.villageworks.VillageWorks;
import org.sosly.villageworks.api.profession.IProfession;

public class ProfessionRegistry {
    public static final ProfessionRegistry INSTANCE = new ProfessionRegistry();
    private final Map<ResourceLocation, IProfession> professions = new HashMap<ResourceLocation, IProfession>();
    private ProfessionRegistry() {}

    public void register(IProfession profession) {
        ResourceLocation id = profession.getID();
        if (professions.containsKey(id)) {
            VillageWorks.LOGGER.warn("Attempted to register profession " + profession.getID() + " twice");
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
}
