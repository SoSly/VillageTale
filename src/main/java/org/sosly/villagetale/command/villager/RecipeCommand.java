package org.sosly.villagetale.command.villager;

import com.google.common.collect.ImmutableSet;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.capability.IRecipeKnowledgeCapability;
import org.sosly.villagetale.capability.Capabilities;
import org.sosly.villagetale.command.Result;
import org.sosly.villagetale.command.arguments.VillagerUUIDArgument;
import org.sosly.villagetale.entity.Villager;

public class RecipeCommand {
    
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("recipes")
                .then(Commands.literal("list")
                        .executes(RecipeCommand::listRecipesCommand))
                .then(Commands.literal("add")
                        .then(Commands.argument("recipe", ResourceLocationArgument.id())
                                .suggests(RECIPE_SUGGESTIONS)
                                .executes(RecipeCommand::addRecipeCommand)))
                .then(Commands.literal("remove")
                        .then(Commands.argument("recipe", ResourceLocationArgument.id())
                                .suggests(KNOWN_RECIPE_SUGGESTIONS)
                                .executes(RecipeCommand::removeRecipeCommand)));
    }
    
    private static int listRecipesCommand(CommandContext<CommandSourceStack> ctx) {
        try {
            Villager villager = VillagerCommand.getTargetVillager(ctx);
            ServerLevel level = ctx.getSource().getLevel();
            Result result = listRecipes(level, villager);
            return result.send(ctx.getSource());
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.villager.recipes.list_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }
    
    private static int addRecipeCommand(CommandContext<CommandSourceStack> ctx) {
        try {
            Villager villager = VillagerCommand.getTargetVillager(ctx);
            ResourceLocation recipeId = ResourceLocationArgument.getId(ctx, "recipe");
            ServerLevel level = ctx.getSource().getLevel();
            Result result = addRecipe(level, villager, recipeId);
            return result.send(ctx.getSource(), true);
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.villager.recipes.add_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }
    
    private static int removeRecipeCommand(CommandContext<CommandSourceStack> ctx) {
        try {
            Villager villager = VillagerCommand.getTargetVillager(ctx);
            ResourceLocation recipeId = ResourceLocationArgument.getId(ctx, "recipe");
            ServerLevel level = ctx.getSource().getLevel();
            Result result = removeRecipe(level, villager, recipeId);
            return result.send(ctx.getSource(), true);
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.villager.recipes.remove_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }
    
    public static final SuggestionProvider<CommandSourceStack> RECIPE_SUGGESTIONS = 
            (context, builder) -> {
                ServerLevel level = context.getSource().getLevel();
                RecipeManager recipeManager = level.getRecipeManager();
                Stream<ResourceLocation> recipes = recipeManager.getRecipes().stream()
                        .map(Recipe::getId);
                return SharedSuggestionProvider.suggestResource(recipes, builder);
            };
    
    public static final SuggestionProvider<CommandSourceStack> KNOWN_RECIPE_SUGGESTIONS = 
            (CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) -> {
                try {
                    Villager villager = VillagerUUIDArgument.getVillager(context, "target");
                    IRecipeKnowledgeCapability knowledge = villager.getCapability(Capabilities.RECIPE_KNOWLEDGE_CAPABILITY)
                            .orElse(null);
                    
                    if (knowledge == null) {
                        return Suggestions.empty();
                    }
                    
                    ImmutableSet<ResourceLocation> knownRecipes = knowledge.known();
                    return SharedSuggestionProvider.suggestResource(knownRecipes.stream(), builder);
                } catch (Exception e) {
                    return Suggestions.empty();
                }
            };
    
    public static Result listRecipes(ServerLevel level, Villager villager) {
        IRecipeKnowledgeCapability knowledge = villager.getCapability(Capabilities.RECIPE_KNOWLEDGE_CAPABILITY)
                .orElse(null);
        
        if (knowledge == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.villager.recipes.no_capability", VillageTale.MOD_ID)));
        }
        
        ImmutableSet<ResourceLocation> knownRecipes = knowledge.known();
        
        if (knownRecipes.isEmpty()) {
            return Result.success(Component.literal(String.format(
                    "%s knows no recipes", villager.getDisplayName().getString())));
        }
        
        StringBuilder message = new StringBuilder();
        message.append(String.format("%s knows %d recipe(s):\n", 
                villager.getDisplayName().getString(), knownRecipes.size()));
        
        RecipeManager recipeManager = level.getRecipeManager();
        for (ResourceLocation recipeId : knownRecipes) {
            recipeManager.byKey(recipeId).ifPresentOrElse(
                    recipe -> {
                        String output = recipe.getResultItem(level.registryAccess()).getDisplayName().getString();
                        message.append(String.format("- %s (%s)\n", recipeId, output));
                    },
                    () -> message.append(String.format("- %s (invalid recipe)\n", recipeId))
            );
        }
        
        return Result.success(Component.literal(message.toString().trim()));
    }
    
    public static Result addRecipe(ServerLevel level, Villager villager, ResourceLocation recipeId) {
        IRecipeKnowledgeCapability knowledge = villager.getCapability(Capabilities.RECIPE_KNOWLEDGE_CAPABILITY)
                .orElse(null);
        
        if (knowledge == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.villager.recipes.no_capability", VillageTale.MOD_ID)));
        }
        
        RecipeManager recipeManager = level.getRecipeManager();
        if (recipeManager.byKey(recipeId).isEmpty()) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.villager.recipes.unknown_recipe", VillageTale.MOD_ID), recipeId));
        }
        
        if (knowledge.knows(level, recipeId)) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.villager.recipes.already_knows", VillageTale.MOD_ID), 
                    villager.getDisplayName().getString(), recipeId));
        }
        
        if (!knowledge.learn(level, recipeId)) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.villager.recipes.learn_failed", VillageTale.MOD_ID), recipeId));
        }
        
        return Result.success(Component.translatable(
                String.format("%s.command.villager.recipes.learned", VillageTale.MOD_ID), 
                villager.getDisplayName().getString(), recipeId));
    }
    
    public static Result removeRecipe(ServerLevel level, Villager villager, ResourceLocation recipeId) {
        IRecipeKnowledgeCapability knowledge = villager.getCapability(Capabilities.RECIPE_KNOWLEDGE_CAPABILITY)
                .orElse(null);
        
        if (knowledge == null) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.villager.recipes.no_capability", VillageTale.MOD_ID)));
        }
        
        if (!knowledge.knows(level, recipeId)) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.villager.recipes.doesnt_know", VillageTale.MOD_ID), 
                    villager.getDisplayName().getString(), recipeId));
        }
        
        knowledge.known().stream()
                .filter(id -> id.equals(recipeId))
                .findFirst()
                .ifPresent(id -> {
                    if (knowledge instanceof org.sosly.villagetale.capability.recipeknowledge.RecipeKnowledgeCapability impl) {
                        impl.getRecipes().remove(id);
                    }
                });
        
        return Result.success(Component.translatable(
                String.format("%s.command.villager.recipes.forgotten", VillageTale.MOD_ID), 
                villager.getDisplayName().getString(), recipeId));
    }
}