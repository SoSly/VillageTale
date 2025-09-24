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

public class CreateCommand {
    
    private static final int DEFAULT_SQUADIUS = 3;
    private static final int MIN_SQUADIUS = 1;
    private static final int MAX_SQUADIUS = 16;
    
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("create")
                .then(Commands.argument("name", StringArgumentType.string())
                        .executes(ctx -> createVillage(ctx, DEFAULT_SQUADIUS))
                        .then(Commands.argument("squadius", IntegerArgumentType.integer(MIN_SQUADIUS, MAX_SQUADIUS))
                                .executes(ctx -> createVillage(ctx, IntegerArgumentType.getInteger(ctx, "squadius")))));
    }
    
    private static int createVillage(CommandContext<CommandSourceStack> ctx, int squadius) {
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
                    squadius
            );
            return result.send(source, true);
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.village.create_error", VillageTale.MOD_ID), e.getMessage())).send(source);
        }
    }
}