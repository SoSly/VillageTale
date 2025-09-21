package org.sosly.villagetale.command.village;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.api.capability.IVillagesCapability;
import org.sosly.villagetale.capability.Capabilities;
import org.sosly.villagetale.capability.village.VillageCapability;
import org.sosly.villagetale.command.Result;
import org.sosly.villagetale.data.VillageInfo;

public class VillageService {

    public static IVillagesCapability getVillagesCapability(ServerLevel level) {
        return level.getCapability(Capabilities.VILLAGES_CAPABILITY).orElse(null);
    }

    public static IVillageCapability getVillageCapability(ServerLevel level, ChunkPos chunkPos) {
        return level.getChunk(chunkPos.x, chunkPos.z)
                .getCapability(Capabilities.VILLAGE_CAPABILITY)
                .orElse(null);
    }

    public static Result createVillage(ServerLevel level, BlockPos position,
                                       String name, int squadius) {
        // Validate input
        if (name == null || name.trim().isEmpty()) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.village.empty_name", VillageTale.MOD_ID)));
        }

        // Get capabilities
        IVillagesCapability villages = getVillagesCapability(level);
        if (villages == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.village.capability_not_found", VillageTale.MOD_ID)));
        }

        // Create village
        UUID villageId = villages.createVillage(position, name, squadius);
        if (villageId == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.village.creation_failed", VillageTale.MOD_ID)));
        }

        // Set chunk capability
        ChunkPos chunkPos = new ChunkPos(position);
        IVillageCapability chunkCapability = getVillageCapability(level, chunkPos);
        if (chunkCapability == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.village.creation_failed", VillageTale.MOD_ID)));
        }

        chunkCapability.setUUID(villageId);
        chunkCapability.setName(name);

        int chunksCovered = (squadius * 2 + 1) * (squadius * 2 + 1);
        return Result.success(Component.translatable(
                String.format("%s.command.village.created", VillageTale.MOD_ID), name, squadius, chunksCovered));
    }

    public static Result removeVillage(ServerLevel level, String name) {
        IVillagesCapability villages = getVillagesCapability(level);
        if (villages == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.village.capability_not_found", VillageTale.MOD_ID)));
        }

        VillageInfo info = villages.getVillageByName(name);
        if (info == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.village.not_found", VillageTale.MOD_ID), name));
        }

        boolean removed = villages.removeVillage(info.getVillageId());
        if (!removed) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.village.removal_failed", VillageTale.MOD_ID)));
        }

        // Clear from chunk capability
        VillageCapability village = (VillageCapability) level
                .getCapability(Capabilities.VILLAGE_CAPABILITY)
                .orElse(null);
        if (village != null) {
            village.destroy();
        }

        return Result.success(Component.translatable(
                String.format("%s.command.village.removed", VillageTale.MOD_ID), name));
    }

    public static VillageInfo findVillage(ServerLevel level, String identifier) {
        IVillagesCapability villages = getVillagesCapability(level);
        if (villages == null) {
            return null;
        }

        try {
            UUID villageId = UUID.fromString(identifier);
            return villages.getVillageById(villageId);
        } catch (IllegalArgumentException e) {
            return villages.getVillageByName(identifier);
        }
    }
}
