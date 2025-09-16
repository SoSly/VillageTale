package org.sosly.villageworks.command.arguments;

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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.sosly.villageworks.api.capability.IVillageCapability;
import org.sosly.villageworks.api.capability.IVillagesCapability;
import org.sosly.villageworks.api.data.IVillageZone;
import org.sosly.villageworks.capability.Capabilities;
import org.sosly.villageworks.data.VillageInfo;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ZoneUUIDArgument {
    
    private static final DynamicCommandExceptionType INVALID_UUID = new DynamicCommandExceptionType(
        uuid -> Component.literal("Invalid zone UUID: " + uuid)
    );
    
    public static ArgumentType<String> zoneUUID() {
        return StringArgumentType.word();
    }
    
    public static UUID getZoneUUID(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        String input = StringArgumentType.getString(context, name);
        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException e) {
            throw INVALID_UUID.create(input);
        }
    }
    
    public static CompletableFuture<Suggestions> suggest(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        try {
            String villageUUIDStr = StringArgumentType.getString(context, "villageUUID");
            UUID villageId = UUID.fromString(villageUUIDStr);
            
            ServerLevel level = context.getSource().getLevel();
            
            IVillagesCapability villages = level.getCapability(Capabilities.VILLAGES_CAPABILITY).orElse(null);
            if (villages == null) {
                return Suggestions.empty();
            }

            VillageInfo village = villages.getVillageById(villageId);
            if (village == null) {
                return Suggestions.empty();
            }
            
            ChunkPos villageChunk = village.getVillageStartingChunk();
            LevelChunk chunk = level.getChunk(villageChunk.x, villageChunk.z);
            IVillageCapability villageCapability = chunk.getCapability(Capabilities.VILLAGE_CAPABILITY).orElse(null);
            if (villageCapability == null) {
                return Suggestions.empty();
            }

            List<IVillageZone> zones = villageCapability.getZones();
            Collection<String> zoneUUIDs = zones.stream()
                .map(zone -> zone.getUUID().toString())
                .collect(Collectors.toList());
            
            return SharedSuggestionProvider.suggest(zoneUUIDs, builder);
        } catch (Exception e) {
            return Suggestions.empty();
        }
    }
}