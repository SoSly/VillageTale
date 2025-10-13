package org.sosly.villagetale.network.packets.serverbound;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.api.capability.IVillagesCapability;
import org.sosly.villagetale.capability.Capabilities;
import org.sosly.villagetale.data.VillageInfo;
import org.sosly.villagetale.entity.MemoryModuleTypes;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.network.BasePacket;
import org.sosly.villagetale.network.NetworkHandler;
import org.sosly.villagetale.network.ServerPacketHandler;
import org.sosly.villagetale.profession.ProfessionRegistry;

public class UpdateVillagerAssignment extends BasePacket {
    private final int villagerEntityId;
    @Nullable
    private final ResourceLocation professionId;
    @Nullable
    private final UUID homeZoneId;
    @Nullable
    private final UUID workZoneId;
    private final boolean clearWorkZone;

    private UpdateVillagerAssignment(int villagerEntityId, @Nullable ResourceLocation professionId, @Nullable UUID homeZoneId, @Nullable UUID workZoneId, boolean clearWorkZone) {
        this.villagerEntityId = villagerEntityId;
        this.professionId = professionId;
        this.homeZoneId = homeZoneId;
        this.workZoneId = workZoneId;
        this.clearWorkZone = clearWorkZone;
    }

    public static void send(int villagerEntityId, @Nullable ResourceLocation professionId, @Nullable UUID homeZoneId, @Nullable UUID workZoneId, boolean clearWorkZone) {
        UpdateVillagerAssignment packet = new UpdateVillagerAssignment(villagerEntityId, professionId, homeZoneId, workZoneId, clearWorkZone);
        NetworkHandler.CHANNEL.sendToServer(packet);
    }

    public static void encode(UpdateVillagerAssignment msg, FriendlyByteBuf buffer) {
        buffer.writeInt(msg.villagerEntityId);
        buffer.writeBoolean(msg.professionId != null);
        if (msg.professionId != null) {
            buffer.writeResourceLocation(msg.professionId);
        }
        buffer.writeBoolean(msg.homeZoneId != null);
        if (msg.homeZoneId != null) {
            buffer.writeUUID(msg.homeZoneId);
        }
        buffer.writeBoolean(msg.workZoneId != null);
        if (msg.workZoneId != null) {
            buffer.writeUUID(msg.workZoneId);
        }
        buffer.writeBoolean(msg.clearWorkZone);
    }

    public static UpdateVillagerAssignment decode(FriendlyByteBuf buffer) {
        UpdateVillagerAssignment msg;

        try {
            int villagerEntityId = buffer.readInt();
            ResourceLocation professionId = buffer.readBoolean() ? buffer.readResourceLocation() : null;
            UUID homeZoneId = buffer.readBoolean() ? buffer.readUUID() : null;
            UUID workZoneId = buffer.readBoolean() ? buffer.readUUID() : null;
            boolean clearWorkZone = buffer.readBoolean();
            msg = new UpdateVillagerAssignment(villagerEntityId, professionId, homeZoneId, workZoneId, clearWorkZone);
        } catch (IndexOutOfBoundsException | IllegalArgumentException err) {
            VillageTale.LOGGER.error("Exception while reading UpdateVillagerAssignment: {}", err.toString());
            return null;
        }

        msg.messageIsValid = true;
        return msg;
    }

    public static void handle(UpdateVillagerAssignment msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (!ServerPacketHandler.validateBasics(msg, context)) {
            return;
        }

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            ServerLevel level = player.serverLevel();
            Entity entity = level.getEntity(msg.villagerEntityId);
            if (!(entity instanceof Villager villager)) {
                player.sendSystemMessage(Component.literal("Villager not found"));
                return;
            }

            Optional<UUID> villageId = villager.getVillage();
            if (villageId.isEmpty()) {
                player.sendSystemMessage(Component.literal("Villager is not assigned to a village"));
                return;
            }

            IVillagesCapability villagesCapability = level.getCapability(Capabilities.VILLAGES_CAPABILITY).orElse(null);
            if (villagesCapability == null) {
                player.sendSystemMessage(Component.literal("Failed to update villager: capability not found"));
                return;
            }

            VillageInfo village = villagesCapability.getVillageById(villageId.get());
            if (village == null) {
                player.sendSystemMessage(Component.literal("Village not found"));
                return;
            }

            IVillageCapability villageCapability = level.getChunk(village.getVillageStartingChunk().x, village.getVillageStartingChunk().z)
                .getCapability(Capabilities.VILLAGE_CAPABILITY).orElse(null);

            if (villageCapability == null) {
                player.sendSystemMessage(Component.literal("Failed to update villager: village capability not found"));
                return;
            }

            if (msg.professionId != null) {
                if (ProfessionRegistry.INSTANCE.getProfession(msg.professionId).isEmpty()) {
                    player.sendSystemMessage(Component.literal("Invalid profession"));
                    return;
                }
                villager.setProfession(msg.professionId);
            }

            if (msg.homeZoneId != null) {
                IVillageZone homeZone = villageCapability.getZones().stream()
                    .filter(z -> z.getUUID().equals(msg.homeZoneId))
                    .findFirst()
                    .orElse(null);

                if (homeZone == null) {
                    player.sendSystemMessage(Component.literal("Home zone not found"));
                    return;
                }

                villager.getBrain().setMemory(MemoryModuleTypes.HOME_ZONE.get(), msg.homeZoneId);
            }

            if (msg.clearWorkZone) {
                villager.getBrain().eraseMemory(MemoryModuleTypes.WORK_ZONE.get());
            } else if (msg.workZoneId != null) {
                IVillageZone workZone = villageCapability.getZones().stream()
                    .filter(z -> z.getUUID().equals(msg.workZoneId))
                    .findFirst()
                    .orElse(null);

                if (workZone == null) {
                    player.sendSystemMessage(Component.literal("Work zone not found"));
                    return;
                }

                if (!villager.getProfession().isValidWorkZone(workZone)) {
                    player.sendSystemMessage(Component.literal("Work zone is not valid for this profession"));
                    return;
                }

                villager.getBrain().setMemory(MemoryModuleTypes.WORK_ZONE.get(), msg.workZoneId);
            }

            villager.refreshBrain(level);
        });
    }
}
