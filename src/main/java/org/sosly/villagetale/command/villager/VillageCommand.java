package org.sosly.villagetale.command.villager;

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
import org.sosly.villagetale.entity.Villager;

public class VillageCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("village")
                .executes(VillageCommand::queryVillageAssignment)
                .then(Commands.argument("villageUUID", VillageUUIDArgument.villageUUID())
                        .suggests(VillageUUIDArgument::suggest)
                        .executes(VillageCommand::assignVillage));
    }

    private static int queryVillageAssignment(CommandContext<CommandSourceStack> ctx) {
        try {
            Villager villager = VillagerCommand.getTargetVillager(ctx);
            Result result = VillagerService.queryVillageAssignment(villager);
            return result.send(ctx.getSource());
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.villager.query_village_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }

    private static int assignVillage(CommandContext<CommandSourceStack> ctx) {
        try {
            Villager villager = VillagerCommand.getTargetVillager(ctx);
            UUID villageId = VillageUUIDArgument.getVillageUUID(ctx, "villageUUID");
            ServerLevel level = ctx.getSource().getLevel();

            Result result = VillagerService.assignVillage(level, villager, villageId);
            return result.send(ctx.getSource(), true);
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.villager.assign_village_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }
}
