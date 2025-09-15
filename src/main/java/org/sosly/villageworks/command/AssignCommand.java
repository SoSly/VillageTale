package org.sosly.villageworks.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.sosly.villageworks.entity.Villager;

import java.util.Collection;

public class AssignCommand {
    public static void register(LiteralArgumentBuilder<CommandSourceStack> parentCommand) {
        parentCommand.then(Commands.literal("assign")
            .then(Commands.argument("targets", EntityArgument.entities())
                .then(Commands.literal("bed")
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(AssignCommand::assignBed)))));
    }

    private static int assignBed(CommandContext<CommandSourceStack> context) {
        try {
            Collection<? extends Entity> entities = EntityArgument.getEntities(context, "targets");
            BlockPos bedPos = BlockPosArgument.getBlockPos(context, "pos");
            int assignedCount = 0;

            for (Entity entity : entities) {
                if (entity instanceof Villager villager) {
                    villager.setHome(bedPos);
                    assignedCount++;
                }
            }

            if (assignedCount == 0) {
                context.getSource().sendFailure(Component.literal("No villagers found in selection"));
                return 0;
            }

            final int finalAssignedCount = assignedCount;
            final BlockPos finalBedPos = bedPos;
            context.getSource().sendSuccess(() ->
                Component.literal(String.format("Assigned bed at %s to %d villager(s)",
                    finalBedPos.toShortString(), finalAssignedCount)), true);

            return assignedCount;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to assign bed: " + e.getMessage()));
            return 0;
        }
    }
}
