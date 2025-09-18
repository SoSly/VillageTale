package org.sosly.villagetale.command.arguments;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import org.sosly.villagetale.api.capability.IVillagesCapability;
import org.sosly.villagetale.capability.Capabilities;
import org.sosly.villagetale.data.VillageInfo;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class VillageUUIDArgument {

    private static final DynamicCommandExceptionType INVALID_UUID = new DynamicCommandExceptionType(
        uuid -> Component.literal("Invalid village UUID: " + uuid)
    );

    public static ArgumentType<String> villageUUID() {
        return StringArgumentType.word();
    }

    public static UUID getVillageUUID(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        String input = StringArgumentType.getString(context, name);
        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException e) {
            throw INVALID_UUID.create(input);
        }
    }

    public static CompletableFuture<Suggestions> suggest(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        ServerLevel level = context.getSource().getLevel();

        IVillagesCapability villages = level.getCapability(Capabilities.VILLAGES_CAPABILITY).orElse(null);
        if (villages == null) {
            return Suggestions.empty();
        }

        Collection<VillageInfo> villageInfos = villages.getVillages();
        Collection<String> villageUUIDs = villageInfos.stream()
            .map(village -> village.getVillageId().toString())
            .collect(Collectors.toList());

        return SharedSuggestionProvider.suggest(villageUUIDs, builder);
    }
}
