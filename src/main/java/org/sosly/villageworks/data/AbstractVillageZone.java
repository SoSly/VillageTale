package org.sosly.villageworks.data;

import net.minecraft.network.chat.Component;
import org.sosly.villageworks.api.data.IVillageZone;
import org.sosly.villageworks.api.data.ZoneType;

import java.util.Objects;
import java.util.UUID;

public abstract class AbstractVillageZone implements IVillageZone {
    private final UUID uuid;
    private final ZoneType type;
    private final int id;
    private String name;

    public AbstractVillageZone(UUID uuid, ZoneType type, int id, String name) {
        this.uuid = Objects.requireNonNull(uuid, "UUID cannot be null");
        this.type = Objects.requireNonNull(type, "Zone type cannot be null");
        this.id = id;
        this.name = name;
    }

    public AbstractVillageZone(ZoneType type, int id, String name) {
        this(UUID.randomUUID(), type, id, name);
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public String getName() {
        if (name != null) {
            return name;
        }
        
        String translationKey = "villageworks.zone." + type.name().toLowerCase();
        String zoneName = Component.translatable(translationKey).getString();
        return Component.translatable("villageworks.zone.numbered", zoneName, id).getString();
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public ZoneType getType() {
        return type;
    }
}