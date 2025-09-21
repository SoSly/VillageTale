package org.sosly.villagetale.command.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.sosly.villagetale.entity.EntityTypes;
import org.sosly.villagetale.entity.Villager;

public class VillagerUUIDArgument {

    private static final DynamicCommandExceptionType VILLAGER_NOT_FOUND = new DynamicCommandExceptionType(
        id -> Component.literal("Villager not found: " + id)
    );

    private static final DynamicCommandExceptionType NOT_A_VILLAGER = new DynamicCommandExceptionType(
        entity -> Component.literal("Entity is not a villager: " + entity)
    );

    public static ArgumentType<String> villagerUUID() {
        return StringArgumentType.string();
    }

    public static Villager getVillager(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        String input = StringArgumentType.getString(context, name);
        ServerLevel level = context.getSource().getLevel();

        try {
            UUID uuid = UUID.fromString(input);
            Entity entity = level.getEntity(uuid);
            if (entity == null) {
                throw VILLAGER_NOT_FOUND.create(uuid);
            }
            if (!(entity instanceof Villager villager)) {
                throw NOT_A_VILLAGER.create(uuid);
            }
            return villager;
        } catch (IllegalArgumentException e) {
            EntitySelectorParser parser = new EntitySelectorParser(new StringReader(input));
            EntitySelector selector = parser.parse();

            Collection<? extends Entity> entities = selector.findEntities(context.getSource());
            if (entities.isEmpty()) {
                throw VILLAGER_NOT_FOUND.create(input);
            }

            Entity entity = entities.iterator().next();
            if (!(entity instanceof Villager villager)) {
                throw NOT_A_VILLAGER.create(input);
            }
            return villager;
        }
    }

    public static CompletableFuture<Suggestions> suggest(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        ServerLevel level = context.getSource().getLevel();

        // Get villager UUIDs
        Collection<String> villagerUUIDs = level.getEntities(EntityTypes.VILLAGER.get(), entity -> true).stream()
            .map(villager -> villager.getUUID().toString())
            .collect(Collectors.toList());

        // Also suggest entity selectors
        Collection<String> selectors = Stream.of("@e[type=villagetale:villager]", "@p", "@r", "@a")
            .collect(Collectors.toList());

        // Combine both suggestions
        Collection<String> allSuggestions = Stream.concat(villagerUUIDs.stream(), selectors.stream())
            .collect(Collectors.toList());

        return SharedSuggestionProvider.suggest(allSuggestions, builder);
    }
}
