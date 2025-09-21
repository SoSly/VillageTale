package org.sosly.villagetale.command.village;

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
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.api.capability.IVillagesCapability;
import org.sosly.villagetale.capability.Capabilities;
import org.sosly.villagetale.command.Result;
import org.sosly.villagetale.data.VillageInfo;

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

    private static int createVillage(CommandContext<CommandSourceStack> ctx, int squadius) {
        CommandSourceStack source = ctx.getSource();
        Entity entity = source.getEntity();

        if (entity == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.requires_entity", VillageTale.MOD_ID))).send(source);
        }

        try {
            String villageName = StringArgumentType.getString(ctx, "name");
            Result result = VillageService.createVillage(
                    source.getLevel(),
                    entity.blockPosition(),
                    villageName,
                    squadius
            );
            return result.send(source, true);
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.village.create_error", VillageTale.MOD_ID), e.getMessage())).send(source);
        }
    }

    private static int removeVillage(CommandContext<CommandSourceStack> ctx) {
        try {
            CommandSourceStack source = ctx.getSource();
            String villageName = StringArgumentType.getString(ctx, "name");

            Result result = VillageService.removeVillage(source.getLevel(), villageName);
            return result.send(source, true);
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                            String.format("%s.command.village.remove_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }

    private static int listVillages(CommandContext<CommandSourceStack> ctx) {
        try {
            CommandSourceStack source = ctx.getSource();
            ServerLevel level = source.getLevel();

            IVillagesCapability villages = VillageService.getVillagesCapability(level);
            if (villages == null || villages.getVillages().isEmpty()) {
                return Result.failure(Component.translatable(
                        String.format("%s.command.village.no_villages_dimension", VillageTale.MOD_ID))).send(source);
            }

            source.sendSuccess(() ->
                    Component.translatable(String.format("%s.command.village.list_header", VillageTale.MOD_ID),
                            level.dimension().location()), false);

            for (VillageInfo village : villages.getVillages()) {
                BlockPos townHall = village.getTownHallPos();
                ChunkPos chunkPos = new ChunkPos(townHall);

                source.sendSuccess(() ->
                        Component.translatable(String.format("%s.command.village.list_entry", VillageTale.MOD_ID),
                                village.getVillageName(),
                                townHall.getX(), townHall.getY(), townHall.getZ(),
                                chunkPos.x, chunkPos.z,
                                village.getSquadius()), false);
            }

            return villages.getVillages().size();
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                            String.format("%s.command.village.list_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }

    private static int showVillageInfo(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        Entity entity = source.getEntity();

        if (entity == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.requires_entity", VillageTale.MOD_ID))).send(source);
        }

        try {
            ChunkPos chunkPos = new ChunkPos(entity.blockPosition());
            IVillagesCapability villages = VillageService.getVillagesCapability(source.getLevel());

            if (villages == null) {
                return Result.failure(Component.translatable(
                        String.format("%s.command.village.no_villages_dimension", VillageTale.MOD_ID))).send(source);
            }

            VillageInfo info = villages.getVillageAt(chunkPos);
            if (info == null) {
                return Result.failure(Component.translatable(
                        String.format("%s.command.village.no_village_location", VillageTale.MOD_ID))).send(source);
            }

            return displayVillageInfo(source, source.getLevel(), info);
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.village.info_error", VillageTale.MOD_ID), e.getMessage())).send(source);
        }
    }

    private static int showVillageInfoByName(CommandContext<CommandSourceStack> ctx) {
        try {
            CommandSourceStack source = ctx.getSource();
            String identifier = StringArgumentType.getString(ctx, "village");

            VillageInfo village = VillageService.findVillage(source.getLevel(), identifier);
            if (village == null) {
                return Result.failure(Component.translatable(
                                String.format("%s.command.village.not_found", VillageTale.MOD_ID), identifier))
                        .send(source);
            }

            return displayVillageInfo(source, source.getLevel(), village);
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                            String.format("%s.command.village.info_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }

    private static int displayVillageInfo(CommandSourceStack source, ServerLevel level,
                                          VillageInfo info) {
        BlockPos townHall = info.getTownHallPos();
        ChunkPos townHallChunk = new ChunkPos(townHall);
        ChunkPos startingChunk = info.getVillageStartingChunk();
        int squadius = info.getSquadius();

        // Send basic info
        source.sendSuccess(() ->
                Component.translatable(String.format("%s.command.village.info_header", VillageTale.MOD_ID),
                        info.getVillageName()), false);
        source.sendSuccess(() ->
                Component.translatable(String.format("%s.command.village.info_uuid", VillageTale.MOD_ID),
                        info.getVillageId()), false);
        source.sendSuccess(() ->
                Component.translatable(String.format("%s.command.village.info_townhall", VillageTale.MOD_ID),
                        townHall.getX(), townHall.getY(), townHall.getZ(),
                        townHallChunk.x, townHallChunk.z), false);
        source.sendSuccess(() ->
                Component.translatable(String.format("%s.command.village.info_squadius", VillageTale.MOD_ID),
                        squadius, (squadius * 2 + 1) * (squadius * 2 + 1)), false);
        source.sendSuccess(() ->
                Component.translatable(String.format("%s.command.village.info_boundaries", VillageTale.MOD_ID),
                        townHallChunk.x - squadius, townHallChunk.z - squadius,
                        townHallChunk.x + squadius, townHallChunk.z + squadius), false);

        // Get village capability for additional info
        LevelChunk chunk = level.getChunk(startingChunk.x, startingChunk.z);
        IVillageCapability villageCapability = chunk.getCapability(Capabilities.VILLAGE_CAPABILITY)
                .orElse(null);

        if (villageCapability != null) {
            source.sendSuccess(() ->
                    Component.translatable(String.format("%s.command.village.info_zones", VillageTale.MOD_ID),
                            villageCapability.getZones().size()), false);
            source.sendSuccess(() ->
                    Component.translatable(String.format("%s.command.village.info_villagers", VillageTale.MOD_ID),
                            villageCapability.getVillagerUUIDs().size()), false);
        }

        return 1;
    }
}
