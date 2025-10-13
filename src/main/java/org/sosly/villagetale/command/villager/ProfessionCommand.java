package org.sosly.villagetale.command.villager;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.command.Result;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.profession.ProfessionRegistry;

public class ProfessionCommand {
    
    public static final SuggestionProvider<CommandSourceStack> PROFESSION_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggestResource(
                    ProfessionRegistry.INSTANCE.getProfessionIDs().stream(),
                    builder
            );
    
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("profession")
                .executes(ProfessionCommand::queryProfession)
                .then(Commands.argument("profession", ResourceLocationArgument.id())
                        .suggests(PROFESSION_SUGGESTIONS)
                        .executes(ProfessionCommand::setProfession));
    }
    
    private static int queryProfession(CommandContext<CommandSourceStack> ctx) {
        try {
            Villager villager = VillagerCommand.getTargetVillager(ctx);
            Result result = VillagerService.queryProfession(villager);
            return result.send(ctx.getSource());
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.villager.query_profession_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }

    private static int setProfession(CommandContext<CommandSourceStack> ctx) {
        try {
            Villager villager = VillagerCommand.getTargetVillager(ctx);
            ResourceLocation professionId = ResourceLocationArgument.getId(ctx, "profession");
            ServerLevel level = ctx.getSource().getLevel();
            
            Result result = VillagerService.setProfession(villager, professionId);
            return result.send(ctx.getSource(), true);
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.villager.set_profession_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }
}
