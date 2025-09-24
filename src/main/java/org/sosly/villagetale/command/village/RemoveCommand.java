package org.sosly.villagetale.command.village;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.command.Result;

public class RemoveCommand {
    
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("remove")
                .then(Commands.argument("name", StringArgumentType.string())
                        .executes(RemoveCommand::removeVillage));
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
}