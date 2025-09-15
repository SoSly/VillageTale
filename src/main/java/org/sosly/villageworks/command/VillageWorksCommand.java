package org.sosly.villageworks.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class VillageWorksCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> vwCommand = Commands.literal("vw")
            .requires(source -> source.hasPermission(2));

        AssignCommand.register(vwCommand);

        dispatcher.register(vwCommand);
    }
}
