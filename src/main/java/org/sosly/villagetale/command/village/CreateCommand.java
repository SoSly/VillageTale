package org.sosly.villagetale.command.village;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.command.Result;
import org.sosly.villagetale.config.CommonConfig;

public class CreateCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("create")
                .then(Commands.argument("name", StringArgumentType.string())
                        .executes(CreateCommand::createVillage));
    }

    private static int createVillage(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        Entity entity = source.getEntity();

        if (entity == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.requires_entity", VillageTale.MOD_ID))).send(source);
        }

        try {
            String villageName = StringArgumentType.getString(ctx, "name");
            Result result = VillageService.createVillage(
                    source.getLevel(),
                    entity.blockPosition(),
                    villageName,
                    CommonConfig.defaultSquadius
            );
            return result.send(source, true);
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.village.create_error", VillageTale.MOD_ID), e.getMessage())).send(source);
        }
    }
}