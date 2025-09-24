package org.sosly.villagetale.command.zone;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.sosly.villagetale.command.arguments.VillageUUIDArgument;

public class ZoneCommand {

    public static void register(LiteralArgumentBuilder<CommandSourceStack> parentCommand) {
        var zoneCommand = Commands.literal("zone")
                .then(Commands.argument("villageUUID", VillageUUIDArgument.villageUUID())
                        .suggests(VillageUUIDArgument::suggest));

        CreateCommand.register(zoneCommand);
        DeleteCommand.register(zoneCommand);
        ListCommand.register(zoneCommand);
        InfoCommand.register(zoneCommand);
        RouteCommand.register(zoneCommand);
        AssignCommand.register(zoneCommand);
        UnassignCommand.register(zoneCommand);
        FilterCommand.register(zoneCommand);

        parentCommand.then(zoneCommand);
    }
}
