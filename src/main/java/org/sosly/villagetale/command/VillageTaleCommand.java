package org.sosly.villagetale.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.sosly.villagetale.command.village.VillageCommand;
import org.sosly.villagetale.command.villager.VillagerCommand;
import org.sosly.villagetale.command.zone.ZoneCommand;

public class VillageTaleCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> vtCommand = Commands.literal("vt")
            .requires(source -> source.hasPermission(2));


        VillageCommand.register(vtCommand);
        VillagerCommand.register(vtCommand);
        ZoneCommand.register(vtCommand);

        dispatcher.register(vtCommand);
    }
}
