package org.sosly.villagetale.command.arguments;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import org.sosly.villagetale.api.data.ZoneType;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class ZoneTypeArgument {

    private static final Collection<String> ZONE_TYPE_NAMES = Arrays.asList("storage", "townhall", "home", "none");
    private static final DynamicCommandExceptionType INVALID_ZONE_TYPE = new DynamicCommandExceptionType(
        type -> Component.literal("Invalid zone type: " + type)
    );

    public static ArgumentType<String> zoneType() {
        return StringArgumentType.word();
    }

    public static ZoneType getZoneType(CommandContext<?> context, String name) throws CommandSyntaxException {
        String input = StringArgumentType.getString(context, name);
        try {
            return ZoneType.valueOf(input.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw INVALID_ZONE_TYPE.create(input);
        }
    }

    public static CompletableFuture<Suggestions> suggest(CommandContext<?> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(ZONE_TYPE_NAMES, builder);
    }
}
