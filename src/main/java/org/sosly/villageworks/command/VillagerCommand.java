package org.sosly.villageworks.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.sosly.villageworks.api.capability.IVillagesCapability;
import org.sosly.villageworks.capability.Capabilities;
import org.sosly.villageworks.capability.village.VillageCapability;
import org.sosly.villageworks.command.arguments.VillageUUIDArgument;
import org.sosly.villageworks.data.VillageInfo;
import org.sosly.villageworks.entity.Villager;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public class VillagerCommand {
    public static void register(LiteralArgumentBuilder<CommandSourceStack> parentCommand) {
        parentCommand.then(Commands.literal("villager")
            .then(Commands.argument("targets", EntityArgument.entities())
                .then(Commands.literal("village")
                    .executes(VillagerCommand::queryVillageAssignment)
                    .then(Commands.argument("villageUUID", VillageUUIDArgument.villageUUID())
                        .suggests((context, builder) -> VillageUUIDArgument.suggest(context, builder))
                        .executes(VillagerCommand::assignVillage)))));
    }

    private static int queryVillageAssignment(CommandContext<CommandSourceStack> context) {
        try {
            Collection<? extends Entity> entities = EntityArgument.getEntities(context, "targets");
            int checkedCount = 0;

            for (Entity entity : entities) {
                if (entity instanceof Villager villager) {
                    Optional<UUID> villageId = villager.getVillage();
                    
                    if (villageId.isPresent()) {
                        context.getSource().sendSuccess(() ->
                            Component.literal(String.format("Villager %s is assigned to village %s",
                                villager.getDisplayName().getString(),
                                villageId.get())), false);
                    } else {
                        context.getSource().sendSuccess(() ->
                            Component.literal(String.format("Villager %s is not assigned to any village",
                                villager.getDisplayName().getString())), false);
                    }
                    
                    checkedCount++;
                }
            }

            if (checkedCount == 0) {
                context.getSource().sendFailure(Component.literal("No villagers found in selection"));
                return 0;
            }

            return checkedCount;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to query village assignment: " + e.getMessage()));
            return 0;
        }
    }

    private static int assignVillage(CommandContext<CommandSourceStack> context) {
        try {
            Collection<? extends Entity> entities = EntityArgument.getEntities(context, "targets");
            UUID villageId = VillageUUIDArgument.getVillageUUID(context, "villageUUID");
            ServerLevel level = context.getSource().getLevel();
            
            IVillagesCapability villages = level.getCapability(Capabilities.VILLAGES_CAPABILITY).orElse(null);
            if (villages == null) {
                context.getSource().sendFailure(Component.literal("Villages capability not found"));
                return 0;
            }

            VillageInfo village = villages.getVillageById(villageId);
            if (village == null) {
                context.getSource().sendFailure(Component.literal("Village " + villageId + " not found"));
                return 0;
            }

            int assignedCount = 0;
            for (Entity entity : entities) {
                if (entity instanceof Villager villager) {
                    villager.setVillage(villageId);
                    assignedCount++;
                }
            }

            if (assignedCount == 0) {
                context.getSource().sendFailure(Component.literal("No villagers found in selection"));
                return 0;
            }

            final int finalAssignedCount = assignedCount;
            final UUID finalVillageId = villageId;
            context.getSource().sendSuccess(() ->
                Component.literal(String.format("Assigned %d villager(s) to village %s",
                    finalAssignedCount, finalVillageId)), true);

            return assignedCount;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to assign village: " + e.getMessage()));
            return 0;
        }
    }
}