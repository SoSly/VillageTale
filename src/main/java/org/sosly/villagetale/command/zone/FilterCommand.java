package org.sosly.villagetale.command.zone;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.command.Result;
import org.sosly.villagetale.command.arguments.VillageUUIDArgument;
import org.sosly.villagetale.command.arguments.ZoneUUIDArgument;

public class FilterCommand {

    public static void register(ArgumentBuilder<CommandSourceStack, ?> parentCommand) {
        parentCommand.then(Commands.literal("filter")
                .then(Commands.argument("zoneUUID", ZoneUUIDArgument.zoneUUID())
                        .suggests(ZoneUUIDArgument::suggest)
                        .then(Commands.literal("add")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .executes(FilterCommand::addFilter)))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .executes(FilterCommand::removeFilter)))
                        .then(Commands.literal("clear")
                                .executes(FilterCommand::clearFilter))
                        .then(Commands.literal("list")
                                .executes(FilterCommand::listFilter))));
    }

    private static int addFilter(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerLevel level = ctx.getSource().getLevel();
            UUID villageId = VillageUUIDArgument.getVillageUUID(ctx, "villageUUID");
            UUID zoneId = ZoneUUIDArgument.getZoneUUID(ctx, "zoneUUID");
            ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");
            
            if (BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
                Result result = ZoneService.addEntityTypeFilter(level, villageId, zoneId, id);
                return result.send(ctx.getSource(), true);
            }
            
            if (BuiltInRegistries.ITEM.containsKey(id) && BuiltInRegistries.ITEM.get(id) != Items.AIR) {
                Result result = ZoneService.addFilterItem(level, villageId, zoneId, id);
                return result.send(ctx.getSource(), true);
            }
            
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.invalid_filter_id", VillageTale.MOD_ID), id))
                    .send(ctx.getSource());
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.add_filter_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }
    
    private static int removeFilter(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerLevel level = ctx.getSource().getLevel();
            UUID villageId = VillageUUIDArgument.getVillageUUID(ctx, "villageUUID");
            UUID zoneId = ZoneUUIDArgument.getZoneUUID(ctx, "zoneUUID");
            ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");
            
            if (BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
                Result result = ZoneService.removeEntityTypeFilter(level, villageId, zoneId, id);
                if (result.isSuccess()) {
                    return result.send(ctx.getSource(), true);
                }
            }
            
            if (BuiltInRegistries.ITEM.containsKey(id) && BuiltInRegistries.ITEM.get(id) != Items.AIR) {
                Result result = ZoneService.removeFilterItem(level, villageId, zoneId, id);
                return result.send(ctx.getSource(), true);
            }
            
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.filter_not_found", VillageTale.MOD_ID), id))
                    .send(ctx.getSource());
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.remove_filter_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }
    
    private static int clearFilter(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerLevel level = ctx.getSource().getLevel();
            UUID villageId = VillageUUIDArgument.getVillageUUID(ctx, "villageUUID");
            UUID zoneId = ZoneUUIDArgument.getZoneUUID(ctx, "zoneUUID");
            
            Result itemResult = ZoneService.clearFilter(level, villageId, zoneId);
            Result entityResult = ZoneService.clearEntityTypeFilter(level, villageId, zoneId);
            
            if (itemResult.isSuccess() || entityResult.isSuccess()) {
                return Result.success(Component.translatable(
                        String.format("%s.command.zone.filters_cleared", VillageTale.MOD_ID)))
                        .send(ctx.getSource(), true);
            }
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.clear_filter_failed", VillageTale.MOD_ID)))
                    .send(ctx.getSource());
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.clear_filter_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }
    
    private static int listFilter(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerLevel level = ctx.getSource().getLevel();
            UUID villageId = VillageUUIDArgument.getVillageUUID(ctx, "villageUUID");
            UUID zoneId = ZoneUUIDArgument.getZoneUUID(ctx, "zoneUUID");
            
            Result itemResult = ZoneService.listFilter(level, villageId, zoneId);
            Result entityResult = ZoneService.listEntityTypeFilter(level, villageId, zoneId);
            
            itemResult.send(ctx.getSource());
            entityResult.send(ctx.getSource());
            
            return 1;
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.list_filter_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }
}
