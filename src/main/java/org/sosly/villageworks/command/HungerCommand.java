package org.sosly.villageworks.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.sosly.villageworks.data.LivingEntityFoodData;
import org.sosly.villageworks.entity.Villager;

import java.util.Collection;

public class HungerCommand {
    public static void register(LiteralArgumentBuilder<CommandSourceStack> parentCommand) {
        parentCommand.then(Commands.literal("hunger")
            .then(Commands.argument("targets", EntityArgument.entities())
                .executes(HungerCommand::displayHunger)));
    }

    private static int displayHunger(CommandContext<CommandSourceStack> context) {
        try {
            Collection<? extends Entity> entities = EntityArgument.getEntities(context, "targets");
            int checkedCount = 0;

            for (Entity entity : entities) {
                if (entity instanceof Villager villager) {
                    LivingEntityFoodData foodData = villager.getFoodData();
                    
                    context.getSource().sendSuccess(() ->
                        Component.literal(String.format("Villager %s: Food=%d/20, Saturation=%.1f, Exhaustion=%.1f",
                            villager.getDisplayName().getString(),
                            foodData.getFoodLevel(),
                            foodData.getSaturationLevel(),
                            foodData.getExhaustionLevel())), false);
                    
                    checkedCount++;
                }
            }

            if (checkedCount == 0) {
                context.getSource().sendFailure(Component.literal("No villagers found in selection"));
                return 0;
            }

            return checkedCount;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to check hunger: " + e.getMessage()));
            return 0;
        }
    }
}