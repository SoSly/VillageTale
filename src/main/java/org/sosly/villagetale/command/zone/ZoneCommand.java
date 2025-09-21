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
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.command.Result;
import org.sosly.villagetale.command.arguments.VillageUUIDArgument;
import org.sosly.villagetale.command.arguments.VillagerUUIDArgument;
import org.sosly.villagetale.command.arguments.ZoneUUIDArgument;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.zone.ZoneRegistry;

public class ZoneCommand {

    private static final SuggestionProvider<CommandSourceStack> ZONE_TYPE_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggestResource(
                    StreamSupport.stream(ZoneRegistry.INSTANCE.getZoneTypeIDs().spliterator(), false),
                    builder
            );

    public static void register(LiteralArgumentBuilder<CommandSourceStack> parentCommand) {
        parentCommand.then(Commands.literal("zone")
                .then(Commands.argument("villageUUID", VillageUUIDArgument.villageUUID())
                        .suggests(VillageUUIDArgument::suggest)
                        .then(Commands.literal("create")
                                .then(Commands.literal("rectangle")
                                        .then(Commands.argument("pos1", BlockPosArgument.blockPos())
                                                .then(Commands.argument("pos2", BlockPosArgument.blockPos())
                                                        .then(Commands.argument("type", ResourceLocationArgument.id())
                                                                .suggests(ZONE_TYPE_SUGGESTIONS)
                                                                .executes(ZoneCommand::createRectangleZone)
                                                                .then(Commands.argument("name", StringArgumentType.string())
                                                                        .executes(ZoneCommand::createRectangleZoneWithName))))))

                                .then(Commands.literal("sphere")
                                        .then(Commands.argument("center", BlockPosArgument.blockPos())
                                                .then(Commands.argument("radius", IntegerArgumentType.integer(1))
                                                        .then(Commands.argument("type", ResourceLocationArgument.id())
                                                                .suggests(ZONE_TYPE_SUGGESTIONS)
                                                                .executes(ZoneCommand::createSphereZone)
                                                                .then(Commands.argument("name", StringArgumentType.string())
                                                                        .executes(ZoneCommand::createSphereZoneWithName))))))

                                .then(Commands.literal("point")
                                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                .then(Commands.argument("type", ResourceLocationArgument.id())
                                                        .suggests(ZONE_TYPE_SUGGESTIONS)
                                                        .executes(ZoneCommand::createPointZone)
                                                        .then(Commands.argument("name", StringArgumentType.string())
                                                                .executes(ZoneCommand::createPointZoneWithName)))))

                                .then(Commands.literal("route")
                                        .then(Commands.argument("type", ResourceLocationArgument.id())
                                                .suggests(ZONE_TYPE_SUGGESTIONS)
                                                .executes(ZoneCommand::createRouteZone)
                                                .then(Commands.argument("name", StringArgumentType.string())
                                                        .executes(ZoneCommand::createRouteZoneWithName)))))

                        .then(Commands.literal("delete")
                                .then(Commands.argument("zoneUUID", ZoneUUIDArgument.zoneUUID())
                                        .suggests(ZoneUUIDArgument::suggest)
                                        .executes(ZoneCommand::deleteZone)))

                        .then(Commands.literal("list")
                                .executes(ZoneCommand::listZones))

                        .then(Commands.literal("info")
                                .then(Commands.argument("zoneUUID", ZoneUUIDArgument.zoneUUID())
                                        .suggests(ZoneUUIDArgument::suggest)
                                        .executes(ZoneCommand::zoneInfo)))

                        .then(Commands.literal("route")
                                .then(Commands.argument("zoneUUID", ZoneUUIDArgument.zoneUUID())
                                        .suggests(ZoneUUIDArgument::suggest)
                                        .then(Commands.literal("add")
                                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                        .executes(ZoneCommand::addRoutePoint)))
                                        .then(Commands.literal("clear")
                                                .executes(ZoneCommand::clearRoute))))

                        .then(Commands.literal("assign")
                                .then(Commands.argument("zoneUUID", ZoneUUIDArgument.zoneUUID())
                                        .suggests(ZoneUUIDArgument::suggest)
                                        .then(Commands.argument("villager", VillagerUUIDArgument.villagerUUID())
                                                .suggests(VillagerUUIDArgument::suggest)
                                                .executes(ZoneCommand::assignVillager))))

                        .then(Commands.literal("unassign")
                                .then(Commands.argument("villager", VillagerUUIDArgument.villagerUUID())
                                        .suggests(VillagerUUIDArgument::suggest)
                                        .executes(ZoneCommand::unassignVillager)))));
    }

    private static int createRectangleZone(CommandContext<CommandSourceStack> ctx) {
        return createRectangleZoneInternal(ctx, null);
    }

    private static int createRectangleZoneWithName(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        return createRectangleZoneInternal(ctx, name);
    }

    private static int createRectangleZoneInternal(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            ServerLevel level = ctx.getSource().getLevel();
            UUID villageId = VillageUUIDArgument.getVillageUUID(ctx, "villageUUID");
            ResourceLocation zoneType = ResourceLocationArgument.getId(ctx, "type");
            BlockPos pos1 = BlockPosArgument.getBlockPos(ctx, "pos1");
            BlockPos pos2 = BlockPosArgument.getBlockPos(ctx, "pos2");

            Result result = ZoneService.createRectangleZone(level, villageId, pos1, pos2, zoneType, name);
            return result.send(ctx.getSource(), true);
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.create_rectangle_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }

    private static int createSphereZone(CommandContext<CommandSourceStack> ctx) {
        return createSphereZoneInternal(ctx, null);
    }

    private static int createSphereZoneWithName(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        return createSphereZoneInternal(ctx, name);
    }

    private static int createSphereZoneInternal(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            ServerLevel level = ctx.getSource().getLevel();
            UUID villageId = VillageUUIDArgument.getVillageUUID(ctx, "villageUUID");
            ResourceLocation zoneType = ResourceLocationArgument.getId(ctx, "type");
            BlockPos center = BlockPosArgument.getBlockPos(ctx, "center");
            int radius = IntegerArgumentType.getInteger(ctx, "radius");

            Result result = ZoneService.createSphereZone(level, villageId, center, radius, zoneType, name);
            return result.send(ctx.getSource(), true);
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.create_sphere_error", VillageTale.MOD_ID), e.getMessage()))
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

    private static int deleteZone(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerLevel level = ctx.getSource().getLevel();
            UUID villageId = VillageUUIDArgument.getVillageUUID(ctx, "villageUUID");
            UUID zoneId = ZoneUUIDArgument.getZoneUUID(ctx, "zoneUUID");

            Result result = ZoneService.deleteZone(level, villageId, zoneId);
            return result.send(ctx.getSource(), true);
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.delete_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }

    private static int listZones(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerLevel level = ctx.getSource().getLevel();
            UUID villageId = VillageUUIDArgument.getVillageUUID(ctx, "villageUUID");

            Result result = ZoneService.listZones(level, villageId);
            return result.send(ctx.getSource());
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.list_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }

    private static int zoneInfo(CommandContext<CommandSourceStack> ctx) {
        try {
            CommandSourceStack source = ctx.getSource();
            ServerLevel level = source.getLevel();
            UUID villageId = VillageUUIDArgument.getVillageUUID(ctx, "villageUUID");
            UUID zoneId = ZoneUUIDArgument.getZoneUUID(ctx, "zoneUUID");

            IVillageCapability capability = ZoneService.getVillageCapability(level, villageId);
            if (capability == null) {
                return Result.failure(Component.translatable(
                        String.format("%s.command.zone.village_capability_not_found", VillageTale.MOD_ID)))
                        .send(source);
            }

            IVillageZone zone = capability.getZones().stream()
                    .filter(z -> z.getUUID().equals(zoneId))
                    .findFirst()
                    .orElse(null);

            if (zone == null) {
                return Result.failure(Component.translatable(
                        String.format("%s.command.zone.not_found", VillageTale.MOD_ID), zoneId))
                        .send(source);
            }

            ZoneService.displayZoneInfo(zone, component -> source.sendSuccess(() -> component, false));
            return 1;
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.info_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }

    private static int addRoutePoint(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerLevel level = ctx.getSource().getLevel();
            UUID villageId = VillageUUIDArgument.getVillageUUID(ctx, "villageUUID");
            UUID zoneId = ZoneUUIDArgument.getZoneUUID(ctx, "zoneUUID");
            BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");

            Result result = ZoneService.addRoutePoint(level, villageId, zoneId, pos);
            return result.send(ctx.getSource(), true);
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.add_route_point_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }

    private static int clearRoute(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerLevel level = ctx.getSource().getLevel();
            UUID villageId = VillageUUIDArgument.getVillageUUID(ctx, "villageUUID");
            UUID zoneId = ZoneUUIDArgument.getZoneUUID(ctx, "zoneUUID");

            Result result = ZoneService.clearRoute(level, villageId, zoneId);
            return result.send(ctx.getSource(), true);
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.clear_route_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }

    private static int assignVillager(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerLevel level = ctx.getSource().getLevel();
            UUID villageId = VillageUUIDArgument.getVillageUUID(ctx, "villageUUID");
            UUID zoneId = ZoneUUIDArgument.getZoneUUID(ctx, "zoneUUID");
            Villager villager = VillagerUUIDArgument.getVillager(ctx, "villager");

            Result result = ZoneService.assignVillager(level, villageId, zoneId, villager);
            return result.send(ctx.getSource(), true);
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.assign_villager_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }

    private static int unassignVillager(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerLevel level = ctx.getSource().getLevel();
            UUID villageId = VillageUUIDArgument.getVillageUUID(ctx, "villageUUID");
            Villager villager = VillagerUUIDArgument.getVillager(ctx, "villager");

            Result result = ZoneService.unassignVillager(level, villageId, villager);
            return result.send(ctx.getSource(), true);
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.unassign_villager_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }
}
