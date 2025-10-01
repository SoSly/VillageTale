package org.sosly.villagetale.capability.village;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.network.NetworkHandler;
import org.sosly.villagetale.network.ZoneBoundaryPacket;
import org.sosly.villagetale.zone.shape.Box;
import net.minecraftforge.network.PacketDistributor;

public class VillageCapability implements IVillageCapability {

    @Nullable
    private UUID id;

    private ResourceKey<Level> dimension;
    private Map<ResourceLocation, Integer> ordinals = Collections.synchronizedMap(new HashMap<>());
    private String name;
    private Map<UUID, IVillageZone> zones = Collections.synchronizedMap(new HashMap<>());
    private Set<UUID> villagers = Collections.synchronizedSet(new HashSet<>());
    private Map<UUID, Permission> players = Collections.synchronizedMap(new HashMap<>());
    private WeakReference<LevelChunk> chunk;

    @Override
    public UUID getUUID() {
        return id;
    }

    @Override
    public void setUUID(UUID uuid) {
        this.id = uuid;
    }

    @Override
    public LevelChunk getChunk() {
        return chunk == null ? null : chunk.get();
    }

    @Override
    public void setChunk(LevelChunk chunk) {
        this.chunk = new WeakReference<>(chunk);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public List<IVillageZone> getZones() {
        return zones.values().stream().toList();
    }

    @Override
    public void addZone(IVillageZone zone) {
        zones.put(zone.getUUID(), zone);
        markDirty();

        if (chunk == null || chunk.get() == null) {
            return;
        }

        if (chunk.get().getLevel().isClientSide) {
            return;
        }

        if (!(zone.getShape() instanceof Box)) {
            return;
        }

        Box box = (Box) zone.getShape();
        ZoneBoundaryPacket packet = new ZoneBoundaryPacket(
            zone.getUUID(),
            this.id,
            zone.getShape().getID(),
            box.getBounds()
        );
        NetworkHandler.CHANNEL.send(
            PacketDistributor.DIMENSION.with(() -> chunk.get().getLevel().dimension()),
            packet
        );
    }

    @Override
    public boolean removeZone(UUID zoneId) {
        if (!zones.containsKey(zoneId)) {
            return false;
        }

        zones.remove(zoneId);
        markDirty();
        return true;
    }

    @Override
    public IVillageZone getZoneAt(BlockPos pos) {
        return zones.values().stream()
                .filter(zone -> zone.containsPosition(pos))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Set<UUID> getVillagerUUIDs() {
        return villagers;
    }

    @Override
    public void addVillagerByUUID(UUID villager) {
        if (villagers.contains(villager)) {
            return;
        }

        villagers.add(villager);
        markDirty();
    }

    @Override
    public boolean removeVillagerByUUID(UUID villager) {
        if (!villagers.contains(villager)) {
            return false;
        }

        villagers.remove(villager);
        markDirty();
        return true;
    }

    @Override
    public Map<UUID, Permission> getPlayerPermissions() {
        return players;
    }

    @Override
    public void setPlayerPermission(UUID player, Permission permission) {
        players.put(player, permission);
        markDirty();
    }

    @Override
    public boolean hasPermission(UUID player, Permission required) {
        return players.containsKey(player) && players.get(player).equals(required);
    }

    private void markDirty() {
        LevelChunk chunk = this.chunk.get();
        if (chunk == null) {
            return;
        }

        chunk.setUnsaved(true);
    }

    public void destroy() {
        markDirty();
        this.id = null;
        this.dimension = null;
        this.ordinals = null;
        this.zones = null;
        this.villagers = null;
        this.players = null;
        this.chunk = null;
    }
}
