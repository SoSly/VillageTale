package org.sosly.villagetale.command.village;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class VillageCommand {
    
    public static void register(LiteralArgumentBuilder<CommandSourceStack> parentCommand) {
        parentCommand.then(Commands.literal("village")
                .then(CreateCommand.register())
                .then(RemoveCommand.register())
                .then(ListCommand.register())
                .then(InfoCommand.register()));
    }
}