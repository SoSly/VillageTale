package org.sosly.villagetale.command.zone;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.command.Result;
import org.sosly.villagetale.command.arguments.VillageUUIDArgument;
import org.sosly.villagetale.command.arguments.ZoneUUIDArgument;

public class InfoCommand {

    public static void register(LiteralArgumentBuilder<CommandSourceStack> parentCommand) {
        parentCommand.then(Commands.literal("info")
                .then(Commands.argument("zoneUUID", ZoneUUIDArgument.zoneUUID())
                        .suggests(ZoneUUIDArgument::suggest)
                        .executes(InfoCommand::zoneInfo)));
    }

    private static int zoneInfo(CommandContext<CommandSourceStack> ctx) {
        try {
            CommandSourceStack source = ctx.getSource();
            ServerLevel level = source.getLevel();
            UUID villageId = VillageUUIDArgument.getVillageUUID(ctx, "villageUUID");
            UUID zoneId = ZoneUUIDArgument.getZoneUUID(ctx, "zoneUUID");

            IVillageCapability capability = ZoneService.getVillageCapability(level, villageId);
            if (capability == null) {
                return Result.failure(Component.translatable(
                        String.format("%s.command.zone.village_capability_not_found", VillageTale.MOD_ID)))
                        .send(source);
            }

            IVillageZone zone = capability.getZones().stream()
                    .filter(z -> z.getUUID().equals(zoneId))
                    .findFirst()
                    .orElse(null);

            if (zone == null) {
                return Result.failure(Component.translatable(
                        String.format("%s.command.zone.not_found", VillageTale.MOD_ID), zoneId))
                        .send(source);
            }

            ZoneService.displayZoneInfo(zone, component -> source.sendSuccess(() -> component, false));
            return 1;
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.info_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }
}