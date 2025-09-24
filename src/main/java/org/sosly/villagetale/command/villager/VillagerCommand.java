package org.sosly.villagetale.command.villager;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.sosly.villagetale.command.arguments.VillagerUUIDArgument;
import org.sosly.villagetale.entity.Villager;

public class VillagerCommand {

    public static void register(LiteralArgumentBuilder<CommandSourceStack> parentCommand) {
        parentCommand.then(Commands.literal("villager")
                .then(Commands.argument("target", VillagerUUIDArgument.villagerUUID())
                        .suggests(VillagerUUIDArgument::suggest)
                        .then(VillageCommand.register())
                        .then(ProfessionCommand.register())
                        .then(HungerCommand.register())
                        .then(InfoCommand.register())
                        .then(RecipeCommand.register())));
    }

    public static Villager getTargetVillager(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return VillagerUUIDArgument.getVillager(ctx, "target");
    }
}
