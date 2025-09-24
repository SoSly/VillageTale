package org.sosly.villagetale.command.villager;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.command.Result;
import org.sosly.villagetale.entity.Villager;

public class HungerCommand {
    
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("hunger")
                .executes(HungerCommand::displayHunger)
                .then(Commands.argument("exhaustion", FloatArgumentType.floatArg(0.0F, 40.0F))
                        .executes(HungerCommand::addExhaustion));
    }
    
    private static int displayHunger(CommandContext<CommandSourceStack> ctx) {
        try {
            Villager villager = VillagerCommand.getTargetVillager(ctx);
            Result result = VillagerService.displayHunger(villager);
            return result.send(ctx.getSource());
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.villager.display_hunger_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }

    private static int addExhaustion(CommandContext<CommandSourceStack> ctx) {
        try {
            Villager villager = VillagerCommand.getTargetVillager(ctx);
            float exhaustion = FloatArgumentType.getFloat(ctx, "exhaustion");
            
            Result result = VillagerService.addExhaustion(villager, exhaustion);
            return result.send(ctx.getSource(), true);
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.villager.add_exhaustion_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }
}