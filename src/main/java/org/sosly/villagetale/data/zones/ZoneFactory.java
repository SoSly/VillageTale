package org.sosly.villagetale.data.zones;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.sosly.villagetale.api.data.IVillageZone;
import org.sosly.villagetale.api.data.ZoneShape;
import org.sosly.villagetale.api.data.ZoneType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ZoneFactory {

    public static IVillageZone createZone(ZoneShape shape, ZoneType type, String name) {
        UUID uuid = UUID.randomUUID();
        return createZone(shape, uuid, type, name);
    }

    public static IVillageZone createZone(ZoneShape shape, UUID uuid, ZoneType type, String name) {
        return switch (shape) {
            case AABB -> new AABBZone(uuid, type, name, new net.minecraft.world.phys.AABB(0, 0, 0, 1, 1, 1));
            case RADIUS -> new RadiusZone(uuid, type, name, BlockPos.ZERO, 1);
            case BLOCKPOS -> new BlockPosZone(uuid, type, name, BlockPos.ZERO);
            case PATH -> new PathZone(uuid, type, name, new ArrayList<>());
        };
    }

    public static IVillageZone createAABBZone(ZoneType type, String name, net.minecraft.world.phys.AABB bounds, Level level) {
        UUID uuid = UUID.randomUUID();
        return createAABBZone(uuid, type, name, bounds, level);
    }

    public static IVillageZone createAABBZone(UUID uuid, ZoneType type, String name, net.minecraft.world.phys.AABB bounds, Level level) {
        return new AABBZone(uuid, type, name, bounds, level);
    }

    public static IVillageZone createAABBZone(UUID uuid, ZoneType type, String name, net.minecraft.world.phys.AABB bounds) {
        return new AABBZone(uuid, type, name, bounds);
    }

    public static IVillageZone createRadiusZone(ZoneType type, String name, BlockPos center, int radius, Level level) {
        UUID uuid = UUID.randomUUID();
        return createRadiusZone(uuid, type, name, center, radius, level);
    }

    public static IVillageZone createRadiusZone(UUID uuid, ZoneType type, String name, BlockPos center, int radius, Level level) {
        return new RadiusZone(uuid, type, name, center, radius, level);
    }

    public static IVillageZone createRadiusZone(UUID uuid, ZoneType type, String name, BlockPos center, int radius) {
        return new RadiusZone(uuid, type, name, center, radius);
    }

    public static IVillageZone createBlockPosZone(ZoneType type, String name, BlockPos pos, Level level) {
        UUID uuid = UUID.randomUUID();
        return createBlockPosZone(uuid, type, name, pos, level);
    }

    public static IVillageZone createBlockPosZone(UUID uuid, ZoneType type, String name, BlockPos pos, Level level) {
        return new BlockPosZone(uuid, type, name, pos, level);
    }

    public static IVillageZone createPathZone(ZoneType type, String name, List<BlockPos> path, Level level) {
        UUID uuid = UUID.randomUUID();
        return createPathZone(uuid, type, name, path, level);
    }

    public static IVillageZone createPathZone(UUID uuid, ZoneType type, String name, List<BlockPos> path, Level level) {
        return new PathZone(uuid, type, name, path != null ? path : new ArrayList<>(), level);
    }

    public static IVillageZone createPathZone(UUID uuid, ZoneType type, String name, List<BlockPos> path) {
        return new PathZone(uuid, type, name, path != null ? path : new ArrayList<>());
    }


    public static IVillageZone createZone(ZoneShape shape, ZoneType type, int id, String name) {
        UUID uuid = UUID.randomUUID();
        return createZone(shape, uuid, type, id, name);
    }

    public static IVillageZone createZone(ZoneShape shape, UUID uuid, ZoneType type, int id, String name) {
        return switch (shape) {
            case AABB -> new AABBZone(uuid, type, id, name, new net.minecraft.world.phys.AABB(0, 0, 0, 1, 1, 1));
            case RADIUS -> new RadiusZone(uuid, type, id, name, BlockPos.ZERO, 1);
            case BLOCKPOS -> new BlockPosZone(uuid, type, id, name, BlockPos.ZERO);
            case PATH -> new PathZone(uuid, type, id, name, new ArrayList<>());
        };
    }

    public static IVillageZone createAABBZone(ZoneType type, int id, String name, net.minecraft.world.phys.AABB bounds, Level level) {
        UUID uuid = UUID.randomUUID();
        return createAABBZone(uuid, type, id, name, bounds, level);
    }

    public static IVillageZone createAABBZone(UUID uuid, ZoneType type, int id, String name, net.minecraft.world.phys.AABB bounds, Level level) {
        return new AABBZone(uuid, type, id, name, bounds, level);
    }

    public static IVillageZone createAABBZone(UUID uuid, ZoneType type, int id, String name, net.minecraft.world.phys.AABB bounds) {
        return new AABBZone(uuid, type, id, name, bounds);
    }

    public static IVillageZone createRadiusZone(ZoneType type, int id, String name, BlockPos center, int radius, Level level) {
        UUID uuid = UUID.randomUUID();
        return createRadiusZone(uuid, type, id, name, center, radius, level);
    }

    public static IVillageZone createRadiusZone(UUID uuid, ZoneType type, int id, String name, BlockPos center, int radius, Level level) {
        return new RadiusZone(uuid, type, id, name, center, radius, level);
    }

    public static IVillageZone createRadiusZone(UUID uuid, ZoneType type, int id, String name, BlockPos center, int radius) {
        return new RadiusZone(uuid, type, id, name, center, radius);
    }

    public static IVillageZone createBlockPosZone(ZoneType type, int id, String name, BlockPos pos, Level level) {
        UUID uuid = UUID.randomUUID();
        return createBlockPosZone(uuid, type, id, name, pos, level);
    }

    public static IVillageZone createBlockPosZone(UUID uuid, ZoneType type, int id, String name, BlockPos pos, Level level) {
        return new BlockPosZone(uuid, type, id, name, pos, level);
    }

    public static IVillageZone createPathZone(ZoneType type, int id, String name, List<BlockPos> path, Level level) {
        UUID uuid = UUID.randomUUID();
        return createPathZone(uuid, type, id, name, path, level);
    }

    public static IVillageZone createPathZone(UUID uuid, ZoneType type, int id, String name, List<BlockPos> path, Level level) {
        return new PathZone(uuid, type, id, name, path != null ? path : new ArrayList<>(), level);
    }

    public static IVillageZone createPathZone(UUID uuid, ZoneType type, int id, String name, List<BlockPos> path) {
        return new PathZone(uuid, type, id, name, path != null ? path : new ArrayList<>());
    }

    public static IVillageZone deserializeFromNBT(CompoundTag tag) {
        if (!tag.contains("UUID") || !tag.contains("Type") || !tag.contains("Shape")) {
            return null;
        }

        try {
            UUID uuid = UUID.fromString(tag.getString("UUID"));
            ZoneType type = ZoneType.values()[tag.getByte("Type")];
            ZoneShape shape = ZoneShape.values()[tag.getByte("Shape")];
            int id = tag.getShort("Id");
            String name = tag.contains("Name") ? tag.getString("Name") : null;

            IVillageZone zone = createZone(shape, uuid, type, id, name);
            zone.deserializeNBT(tag);

            if (!tag.contains("Level")) {
                return zone;
            }

            String levelKey = tag.getString("Level");
            ResourceLocation dimensionId = new ResourceLocation(levelKey);
            ResourceKey<Level> levelResourceKey = ResourceKey.create(Registries.DIMENSION, dimensionId);

            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            Level level = server != null ? server.getLevel(levelResourceKey) : null;
            if (level == null) {
                return zone;
            }

            zone.setLevel(level);
            return zone;

        } catch (Exception e) {
            return null;
        }
    }
}
