package org.sosly.villagetale.command.zone;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.command.Result;
import org.sosly.villagetale.command.arguments.VillageUUIDArgument;
import org.sosly.villagetale.command.arguments.ZoneUUIDArgument;

public class RouteCommand {

    public static void register(ArgumentBuilder<CommandSourceStack, ?> parentCommand) {
        parentCommand.then(Commands.literal("route")
                .then(Commands.argument("zoneUUID", ZoneUUIDArgument.zoneUUID())
                        .suggests(ZoneUUIDArgument::suggest)
                        .then(Commands.literal("add")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(RouteCommand::addRoutePoint)))
                        .then(Commands.literal("clear")
                                .executes(RouteCommand::clearRoute))));
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
}
