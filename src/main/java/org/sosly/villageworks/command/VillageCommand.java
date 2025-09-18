package org.sosly.villageworks.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.sosly.villageworks.api.capability.IVillageCapability;
import org.sosly.villageworks.api.capability.IVillagesCapability;
import org.sosly.villageworks.capability.Capabilities;
import org.sosly.villageworks.capability.village.VillageCapability;
import org.sosly.villageworks.data.VillageInfo;

import java.util.Collection;
import java.util.UUID;

public class VillageCommand {

    private static final int DEFAULT_SQUADIUS = 3;
    private static final int MIN_SQUADIUS = 1;
    private static final int MAX_SQUADIUS = 16;

    public static void register(LiteralArgumentBuilder<CommandSourceStack> parentCommand) {
        parentCommand.then(Commands.literal("village")
            .then(Commands.literal("create")
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(ctx -> createVillage(ctx, DEFAULT_SQUADIUS))
                    .then(Commands.argument("squadius", IntegerArgumentType.integer(MIN_SQUADIUS, MAX_SQUADIUS))
                        .executes(ctx -> createVillage(ctx, IntegerArgumentType.getInteger(ctx, "squadius"))))))
            .then(Commands.literal("remove")
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(VillageCommand::removeVillage)))
            .then(Commands.literal("list")
                .executes(VillageCommand::listVillages))
            .then(Commands.literal("info")
                .executes(VillageCommand::showVillageInfo)
                .then(Commands.argument("village", StringArgumentType.string())
                    .executes(VillageCommand::showVillageInfoByName))));
    }

    private static int createVillage(CommandContext<CommandSourceStack> context, int squadius) {
        try {
            CommandSourceStack source = context.getSource();
            Entity entity = source.getEntity();
            if (entity == null) {
                source.sendFailure(Component.literal("Command must be executed by an entity"));
                return 0;
            }

            String villageName = StringArgumentType.getString(context, "name");
            if (villageName.trim().isEmpty()) {
                source.sendFailure(Component.literal("Village name cannot be empty"));
                return 0;
            }

            ServerLevel level = source.getLevel();
            BlockPos pos = entity.blockPosition();

            IVillagesCapability villages = level.getCapability(Capabilities.VILLAGES_CAPABILITY).orElse(null);
            if (villages == null) {
                source.sendFailure(Component.literal("Village not found"));
                return 0;
            }

            UUID villageId = villages.createVillage(pos, villageName, squadius);
            if (villageId == null) {
                source.sendFailure(Component.literal("Village creation failed"));
                return 0;
            }

            ChunkPos chunkPos = new ChunkPos(pos);
            IVillageCapability cap = level.getChunk(chunkPos.x, chunkPos.z).getCapability(Capabilities.VILLAGE_CAPABILITY).orElse(null);
            if (cap == null) {
                source.sendFailure(Component.literal("Village creation failed"));
                return 0;
            }
            ((VillageCapability) cap).initializeVillage(villageId);

            source.sendSuccess(() ->
                    Component.literal("Created village '" + villageName + "' with squadius " + squadius +
                            " (covers " + ((squadius * 2 + 1) * (squadius * 2 + 1)) + " chunks)"), true);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to create village: " + e.getMessage()));
            return 0;
        }
    }

    private static int removeVillage(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            String villageName = StringArgumentType.getString(context, "name");
            ServerLevel level = source.getLevel();

            return level.getCapability(Capabilities.VILLAGES_CAPABILITY)
                .map(manager -> {
                    VillageInfo village = manager.getVillageByName(villageName);
                    if (village == null) {
                        source.sendFailure(Component.literal("Village '" + villageName + "' not found"));
                        return 0;
                    }

                    if (manager.removeVillage(village.getVillageId())) {
                        source.sendSuccess(() ->
                            Component.literal("Removed village '" + villageName + "'"), true);
                        return 1;
                    } else {
                        source.sendFailure(Component.literal("Failed to remove village '" + villageName + "'"));
                        return 0;
                    }
                })
                .orElse(0);

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to remove village: " + e.getMessage()));
            return 0;
        }
    }

    private static int listVillages(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            ServerLevel level = source.getLevel();

            return level.getCapability(Capabilities.VILLAGES_CAPABILITY)
                .map(manager -> {
                    Collection<VillageInfo> villages = manager.getVillages();

                    if (villages.isEmpty()) {
                        source.sendSuccess(() ->
                            Component.literal("No villages found in this dimension"), false);
                        return 1;
                    }

                    source.sendSuccess(() ->
                        Component.literal("Villages in " + level.dimension().location() + ":"), false);

                    for (VillageInfo village : villages) {
                        BlockPos townHall = village.getTownHallPos();
                        ChunkPos chunkPos = new ChunkPos(townHall);
                        source.sendSuccess(() ->
                            Component.literal("- " + village.getVillageName() +
                                            " at block (" + townHall.getX() + ", " + townHall.getY() + ", " + townHall.getZ() + ") " +
                                            "chunk (" + chunkPos.x + ", " + chunkPos.z + ") " +
                                            "squadius " + village.getSquadius()), false);
                    }

                    return villages.size();
                })
                .orElse(0);

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to list villages: " + e.getMessage()));
            return 0;
        }
    }

    private static int showVillageInfo(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            Entity entity = source.getEntity();
            if (entity == null) {
                source.sendFailure(Component.literal("Command must be executed by an entity"));
                return 0;
            }

            ServerLevel level = source.getLevel();
            BlockPos pos = entity.blockPosition();
            ChunkPos chunkPos = new ChunkPos(pos);

            return level.getCapability(Capabilities.VILLAGES_CAPABILITY)
                .map(manager -> {
                    VillageInfo village = manager.getVillageAt(chunkPos);

                    if (village == null) {
                        source.sendSuccess(() ->
                            Component.literal("No village at this location"), false);
                        return 1;
                    }

                    BlockPos townHall = village.getTownHallPos();
                    ChunkPos townHallChunk = new ChunkPos(townHall);
                    ChunkPos villageChunk = village.getVillageStartingChunk();
                    int squadius = village.getSquadius();
                    int minX = townHallChunk.x - squadius;
                    int maxX = townHallChunk.x + squadius;
                    int minZ = townHallChunk.z - squadius;
                    int maxZ = townHallChunk.z + squadius;

                    source.sendSuccess(() ->
                        Component.literal("Village: " + village.getVillageName()), false);
                    source.sendSuccess(() ->
                        Component.literal("UUID: (" + village.getVillageId().toString() + ")"), false);
                    source.sendSuccess(() ->
                        Component.literal("Town Hall: block (" + townHall.getX() + ", " + townHall.getY() + ", " + townHall.getZ() + ") " +
                                        "chunk (" + townHallChunk.x + ", " + townHallChunk.z + ")"), false);
                    source.sendSuccess(() ->
                        Component.literal("Squadius: " + squadius + " (covers " +
                                        ((squadius * 2 + 1) * (squadius * 2 + 1)) + " chunks)"), false);
                    source.sendSuccess(() ->
                        Component.literal("Boundaries: chunks (" + minX + ", " + minZ + ") to (" + maxX + ", " + maxZ + ")"), false);

                    LevelChunk chunk = level.getChunk(villageChunk.x, villageChunk.z);
                    var villageCapability = chunk.getCapability(Capabilities.VILLAGE_CAPABILITY).orElse(null);
                    if (villageCapability != null) {
                        int villagerCount = villageCapability.getVillagerIds().size();
                        source.sendSuccess(() ->
                            Component.literal("Villagers: " + villagerCount), false);
                    }

                    return 1;
                })
                .orElse(0);

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to show village info: " + e.getMessage()));
            return 0;
        }
    }

    private static int showVillageInfoByName(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            String villageIdentifier = StringArgumentType.getString(context, "village");
            ServerLevel level = source.getLevel();

            return level.getCapability(Capabilities.VILLAGES_CAPABILITY)
                .map(manager -> {
                    VillageInfo village;

                    try {
                        UUID villageId = UUID.fromString(villageIdentifier);
                        village = manager.getVillageById(villageId);
                    } catch (IllegalArgumentException e) {
                        village = manager.getVillageByName(villageIdentifier);
                    }

                    if (village == null) {
                        source.sendFailure(Component.literal("Village '" + villageIdentifier + "' not found"));
                        return 0;
                    }

                    final VillageInfo villageData = village;

                    BlockPos townHall = villageData.getTownHallPos();
                    ChunkPos townHallChunk = new ChunkPos(townHall);
                    ChunkPos villageChunk = villageData.getVillageStartingChunk();
                    int squadius = villageData.getSquadius();
                    int minX = townHallChunk.x - squadius;
                    int maxX = townHallChunk.x + squadius;
                    int minZ = townHallChunk.z - squadius;
                    int maxZ = townHallChunk.z + squadius;

                    source.sendSuccess(() ->
                        Component.literal("Village: " + villageData.getVillageName()), false);
                    source.sendSuccess(() ->
                        Component.literal("UUID: (" + villageData.getVillageId().toString() + ")"), false);
                    source.sendSuccess(() ->
                        Component.literal("Town Hall: block (" + townHall.getX() + ", " + townHall.getY() + ", " + townHall.getZ() + ") " +
                                        "chunk (" + townHallChunk.x + ", " + townHallChunk.z + ")"), false);
                    source.sendSuccess(() ->
                        Component.literal("Squadius: " + squadius + " (covers " +
                                        ((squadius * 2 + 1) * (squadius * 2 + 1)) + " chunks)"), false);
                    source.sendSuccess(() ->
                        Component.literal("Boundaries: chunks (" + minX + ", " + minZ + ") to (" + maxX + ", " + maxZ + ")"), false);

                    LevelChunk chunk = level.getChunk(villageChunk.x, villageChunk.z);
                    var villageCapability = chunk.getCapability(Capabilities.VILLAGE_CAPABILITY).orElse(null);
                    if (villageCapability != null) {
                        int villagerCount = villageCapability.getVillagerIds().size();
                        source.sendSuccess(() ->
                            Component.literal("Villagers: " + villagerCount), false);
                    }

                    return 1;
                })
                .orElse(0);

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to show village info: " + e.getMessage()));
            return 0;
        }
    }
}
