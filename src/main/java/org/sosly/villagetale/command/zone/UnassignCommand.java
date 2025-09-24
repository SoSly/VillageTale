package org.sosly.villagetale.command.zone;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.command.Result;
import org.sosly.villagetale.command.arguments.VillageUUIDArgument;
import org.sosly.villagetale.command.arguments.VillagerUUIDArgument;
import org.sosly.villagetale.entity.Villager;

public class UnassignCommand {

    public static void register(LiteralArgumentBuilder<CommandSourceStack> parentCommand) {
        parentCommand.then(Commands.literal("unassign")
                .then(Commands.argument("villager", VillagerUUIDArgument.villagerUUID())
                        .suggests(VillagerUUIDArgument::suggest)
                        .executes(UnassignCommand::unassignVillager)));
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