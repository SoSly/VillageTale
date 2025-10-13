package org.sosly.villagetale.command.villager;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.command.Result;
import org.sosly.villagetale.entity.Villager;

public class InfoCommand {
    
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("info")
                .executes(InfoCommand::displayInfo);
    }
    
    private static int displayInfo(CommandContext<CommandSourceStack> ctx) {
        try {
            Villager villager = VillagerCommand.getTargetVillager(ctx);
            CommandSourceStack source = ctx.getSource();
            ServerLevel level = source.getLevel();

            VillagerService.displayDetailedInfo(
                    villager,
                    level,
                    Component.literal("=== Villager Information ==="),
                    component -> source.sendSuccess(() -> component, false)
            );

            return 1;
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.villager.display_info_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }
}
