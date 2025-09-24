package org.sosly.villagetale.command.zone;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.UUID;
import java.util.stream.StreamSupport;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.command.Result;
import org.sosly.villagetale.command.arguments.VillageUUIDArgument;
import org.sosly.villagetale.zone.ZoneRegistry;

public class CreateCommand {

    private static final SuggestionProvider<CommandSourceStack> ZONE_TYPE_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggestResource(
                    StreamSupport.stream(ZoneRegistry.INSTANCE.getZoneTypeIDs().spliterator(), false),
                    builder
            );

    public static void register(LiteralArgumentBuilder<CommandSourceStack> parentCommand) {
        parentCommand.then(Commands.literal("create")
                .then(Commands.literal("box")
                        .then(Commands.argument("pos1", BlockPosArgument.blockPos())
                                .then(Commands.argument("pos2", BlockPosArgument.blockPos())
                                        .then(Commands.argument("type", ResourceLocationArgument.id())
                                                .suggests(ZONE_TYPE_SUGGESTIONS)
                                                .executes(CreateCommand::createBoxZone)
                                                .then(Commands.argument("name", StringArgumentType.string())
                                                        .executes(CreateCommand::createBoxZoneWithName))))))

                .then(Commands.literal("cylinder")
                        .then(Commands.argument("center", BlockPosArgument.blockPos())
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("height", IntegerArgumentType.integer(1))
                                            .then(Commands.argument("type", ResourceLocationArgument.id())
                                                    .suggests(ZONE_TYPE_SUGGESTIONS)
                                                    .executes(CreateCommand::createCylinderZone)
                                                    .then(Commands.argument("name", StringArgumentType.string())
                                                            .executes(CreateCommand::createCylinderZoneWithName))))))

                .then(Commands.literal("point")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.argument("type", ResourceLocationArgument.id())
                                        .suggests(ZONE_TYPE_SUGGESTIONS)
                                        .executes(CreateCommand::createPointZone)
                                        .then(Commands.argument("name", StringArgumentType.string())
                                                .executes(CreateCommand::createPointZoneWithName)))))

                .then(Commands.literal("route")
                        .then(Commands.argument("type", ResourceLocationArgument.id())
                                .suggests(ZONE_TYPE_SUGGESTIONS)
                                .executes(CreateCommand::createRouteZone)
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .executes(CreateCommand::createRouteZoneWithName))))));
    }

    private static int createBoxZone(CommandContext<CommandSourceStack> ctx) {
        return createBoxZoneInterval(ctx, null);
    }

    private static int createBoxZoneWithName(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        return createBoxZoneInterval(ctx, name);
    }

    private static int createBoxZoneInterval(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            ServerLevel level = ctx.getSource().getLevel();
            UUID villageId = VillageUUIDArgument.getVillageUUID(ctx, "villageUUID");
            ResourceLocation zoneType = ResourceLocationArgument.getId(ctx, "type");
            BlockPos pos1 = BlockPosArgument.getBlockPos(ctx, "pos1");
            BlockPos pos2 = BlockPosArgument.getBlockPos(ctx, "pos2");

            Result result = ZoneService.createBoxZone(level, villageId, pos1, pos2, zoneType, name);
            return result.send(ctx.getSource(), true);
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.create_box_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }

    private static int createCylinderZone(CommandContext<CommandSourceStack> ctx) {
        return createCylinderZoneInterval(ctx, null);
    }

    private static int createCylinderZoneWithName(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        return createCylinderZoneInterval(ctx, name);
    }

    private static int createCylinderZoneInterval(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            ServerLevel level = ctx.getSource().getLevel();
            UUID villageId = VillageUUIDArgument.getVillageUUID(ctx, "villageUUID");
            ResourceLocation zoneType = ResourceLocationArgument.getId(ctx, "type");
            BlockPos center = BlockPosArgument.getBlockPos(ctx, "center");
            int radius = IntegerArgumentType.getInteger(ctx, "radius");
            int height = IntegerArgumentType.getInteger(ctx, "height");

            Result result = ZoneService.createCylinderZone(level, villageId, center, radius, height, zoneType, name);
            return result.send(ctx.getSource(), true);
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.create_cylinder_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }

    private static int createPointZone(CommandContext<CommandSourceStack> ctx) {
        return createPointZoneInternal(ctx, null);
    }

    private static int createPointZoneWithName(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        return createPointZoneInternal(ctx, name);
    }

    private static int createPointZoneInternal(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            ServerLevel level = ctx.getSource().getLevel();
            UUID villageId = VillageUUIDArgument.getVillageUUID(ctx, "villageUUID");
            ResourceLocation zoneType = ResourceLocationArgument.getId(ctx, "type");
            BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");

            Result result = ZoneService.createPointZone(level, villageId, pos, zoneType, name);
            return result.send(ctx.getSource(), true);
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.create_point_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }

    private static int createRouteZone(CommandContext<CommandSourceStack> ctx) {
        return createRouteZoneInternal(ctx, null);
    }

    private static int createRouteZoneWithName(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        return createRouteZoneInternal(ctx, name);
    }

    private static int createRouteZoneInternal(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            ServerLevel level = ctx.getSource().getLevel();
            UUID villageId = VillageUUIDArgument.getVillageUUID(ctx, "villageUUID");
            ResourceLocation zoneType = ResourceLocationArgument.getId(ctx, "type");

            Result result = ZoneService.createRouteZone(level, villageId, zoneType, name);
            return result.send(ctx.getSource(), true);
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.create_route_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }
}
