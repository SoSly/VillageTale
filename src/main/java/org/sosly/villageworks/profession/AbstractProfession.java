package org.sosly.villageworks.profession;

import net.minecraft.resources.ResourceLocation;
import org.sosly.villageworks.api.IProfession;

public abstract class AbstractProfession implements IProfession {
    private final ResourceLocation id;

    protected AbstractProfession(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public ResourceLocation getID() {
        return id;
    }

    @Override
    public String getTranslationKey() {
        return "profession." + getID().toString().replace(":", ".");
    }
}
