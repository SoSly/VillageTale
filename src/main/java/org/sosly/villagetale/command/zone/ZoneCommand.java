package org.sosly.villagetale.command.zone;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.sosly.villagetale.command.arguments.VillageUUIDArgument;

public class ZoneCommand {

    public static void register(LiteralArgumentBuilder<CommandSourceStack> parentCommand) {
        var villageArgument = Commands.argument("villageUUID", VillageUUIDArgument.villageUUID())
                .suggests(VillageUUIDArgument::suggest);

        CreateCommand.register(villageArgument);
        DeleteCommand.register(villageArgument);
        ListCommand.register(villageArgument);
        InfoCommand.register(villageArgument);
        RouteCommand.register(villageArgument);
        AssignCommand.register(villageArgument);
        UnassignCommand.register(villageArgument);
        FilterCommand.register(villageArgument);

        parentCommand.then(Commands.literal("zone").then(villageArgument));
    }
}
