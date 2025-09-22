package org.sosly.villagetale.command.villager;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Collection;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.command.Result;
import org.sosly.villagetale.command.arguments.VillageUUIDArgument;
import org.sosly.villagetale.command.arguments.VillagerUUIDArgument;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.profession.ProfessionRegistry;

public class VillagerCommand {
    
    private static final SuggestionProvider<CommandSourceStack> PROFESSION_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggestResource(
                    ProfessionRegistry.INSTANCE.getProfessionIDs().stream(),
                    builder
            );

    public static void register(LiteralArgumentBuilder<CommandSourceStack> parentCommand) {
        parentCommand.then(Commands.literal("villager")
                .then(Commands.argument("target", VillagerUUIDArgument.villagerUUID())
                        .suggests(VillagerUUIDArgument::suggest)
                        .then(Commands.literal("village")
                                .executes(VillagerCommand::queryVillageAssignment)
                                .then(Commands.argument("villageUUID", VillageUUIDArgument.villageUUID())
                                        .suggests(VillageUUIDArgument::suggest)
                                        .executes(VillagerCommand::assignVillage)))
                        
                        .then(Commands.literal("profession")
                                .executes(VillagerCommand::queryProfession)
                                .then(Commands.argument("profession", ResourceLocationArgument.id())
                                        .suggests(PROFESSION_SUGGESTIONS)
                                        .executes(VillagerCommand::setProfession)))
                        
                        .then(Commands.literal("hunger")
                                .executes(VillagerCommand::displayHunger)
                                .then(Commands.argument("exhaustion", FloatArgumentType.floatArg(0.0F, 40.0F))
                                        .executes(VillagerCommand::addExhaustion)))
                        
                        .then(Commands.literal("info")
                                .executes(VillagerCommand::displayInfo))));
    }

    private static int queryVillageAssignment(CommandContext<CommandSourceStack> ctx) {
        try {
            Villager villager = VillagerUUIDArgument.getVillager(ctx, "target");
            Result result = VillagerService.queryVillageAssignment(villager);
            return result.send(ctx.getSource());
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.villager.query_village_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }

    private static int assignVillage(CommandContext<CommandSourceStack> ctx) {
        try {
            Villager villager = VillagerUUIDArgument.getVillager(ctx, "target");
            UUID villageId = VillageUUIDArgument.getVillageUUID(ctx, "villageUUID");
            ServerLevel level = ctx.getSource().getLevel();
            
            Result result = VillagerService.assignVillage(level, villager, villageId);
            return result.send(ctx.getSource(), true);
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.villager.assign_village_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }

    private static int queryProfession(CommandContext<CommandSourceStack> ctx) {
        try {
            Villager villager = VillagerUUIDArgument.getVillager(ctx, "target");
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
            Villager villager = VillagerUUIDArgument.getVillager(ctx, "target");
            ResourceLocation professionId = ResourceLocationArgument.getId(ctx, "profession");
            
            Result result = VillagerService.setProfession(villager, professionId);
            return result.send(ctx.getSource(), true);
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.villager.set_profession_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }

    private static int displayHunger(CommandContext<CommandSourceStack> ctx) {
        try {
            Villager villager = VillagerUUIDArgument.getVillager(ctx, "target");
            Result result = VillagerService.displayHunger(villager);
            return result.send(ctx.getSource());
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.villager.hunger_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }

    private static int addExhaustion(CommandContext<CommandSourceStack> ctx) {
        try {
            Villager villager = VillagerUUIDArgument.getVillager(ctx, "target");
            float amount = FloatArgumentType.getFloat(ctx, "exhaustion");
            
            Result result = VillagerService.addExhaustion(villager, amount);
            return result.send(ctx.getSource(), true);
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.villager.exhaustion_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }

    private static int displayInfo(CommandContext<CommandSourceStack> ctx) {
        try {
            Villager villager = VillagerUUIDArgument.getVillager(ctx, "target");
            CommandSourceStack source = ctx.getSource();
            ServerLevel level = source.getLevel();

            VillagerService.displayDetailedInfo(
                    villager,
                    level,
                    Component.literal("=== Villager Information ==="),
                    component -> source.sendSuccess(() -> component, false)
            );

            return 1;
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.villager.info_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }
}