package org.sosly.villagetale.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;
import org.sosly.villagetale.api.IZoneType;
import org.sosly.villagetale.zone.ZoneRegistry;

public class RegisterZoneTypesEvent extends Event {
    private final ZoneRegistry registry;

    public RegisterZoneTypesEvent(ZoneRegistry zoneTypes) {
        this.registry = zoneTypes;
    }

    public void register(ResourceLocation id, IZoneType type) {
        registry.register(id, type);
    }
}
