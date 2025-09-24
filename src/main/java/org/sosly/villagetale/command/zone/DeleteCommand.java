package org.sosly.villagetale.command.zone;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.command.Result;
import org.sosly.villagetale.command.arguments.VillageUUIDArgument;
import org.sosly.villagetale.command.arguments.ZoneUUIDArgument;

public class DeleteCommand {

    public static void register(ArgumentBuilder<CommandSourceStack, ?> parentCommand) {
        parentCommand.then(Commands.literal("delete")
                .then(Commands.argument("zoneUUID", ZoneUUIDArgument.zoneUUID())
                        .suggests(ZoneUUIDArgument::suggest)
                        .executes(DeleteCommand::deleteZone)));
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
}