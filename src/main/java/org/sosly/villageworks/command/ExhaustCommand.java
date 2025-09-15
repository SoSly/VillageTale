package org.sosly.villageworks.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.sosly.villageworks.entity.Villager;

import java.util.Collection;

public class ExhaustCommand {
    public static void register(LiteralArgumentBuilder<CommandSourceStack> parentCommand) {
        parentCommand.then(Commands.literal("exhaust")
            .then(Commands.argument("targets", EntityArgument.entities())
                .then(Commands.argument("amount", FloatArgumentType.floatArg(0.0F, 40.0F))
                    .executes(ExhaustCommand::addExhaustion))));
    }

    private static int addExhaustion(CommandContext<CommandSourceStack> context) {
        try {
            Collection<? extends Entity> entities = EntityArgument.getEntities(context, "targets");
            float amount = FloatArgumentType.getFloat(context, "amount");
            int exhaustedCount = 0;

            for (Entity entity : entities) {
                if (entity instanceof Villager villager) {
                    villager.getFoodData().addExhaustion(amount);
                    exhaustedCount++;
                }
            }

            if (exhaustedCount == 0) {
                context.getSource().sendFailure(Component.literal("No villagers found in selection"));
                return 0;
            }

            final int finalExhaustedCount = exhaustedCount;
            final float finalAmount = amount;
            context.getSource().sendSuccess(() ->
                Component.literal(String.format("Added %.1f exhaustion to %d villager(s)",
                    finalAmount, finalExhaustedCount)), true);

            return exhaustedCount;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to add exhaustion: " + e.getMessage()));
            return 0;
        }
    }
}