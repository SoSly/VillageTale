package org.sosly.villagetale.command.zone;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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
                                .then(Commands.argument("item", ResourceLocationArgument.id())
                                        .executes(FilterCommand::addFilterItem)))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("item", ResourceLocationArgument.id())
                                        .executes(FilterCommand::removeFilterItem)))
                        .then(Commands.literal("clear")
                                .executes(FilterCommand::clearFilter))
                        .then(Commands.literal("list")
                                .executes(FilterCommand::listFilter))));
    }

    private static int addFilterItem(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerLevel level = ctx.getSource().getLevel();
            UUID villageId = VillageUUIDArgument.getVillageUUID(ctx, "villageUUID");
            UUID zoneId = ZoneUUIDArgument.getZoneUUID(ctx, "zoneUUID");
            ResourceLocation itemId = ResourceLocationArgument.getId(ctx, "item");
            
            Result result = ZoneService.addFilterItem(level, villageId, zoneId, itemId);
            return result.send(ctx.getSource(), true);
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.add_filter_item_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }
    
    private static int removeFilterItem(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerLevel level = ctx.getSource().getLevel();
            UUID villageId = VillageUUIDArgument.getVillageUUID(ctx, "villageUUID");
            UUID zoneId = ZoneUUIDArgument.getZoneUUID(ctx, "zoneUUID");
            ResourceLocation itemId = ResourceLocationArgument.getId(ctx, "item");
            
            Result result = ZoneService.removeFilterItem(level, villageId, zoneId, itemId);
            return result.send(ctx.getSource(), true);
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.remove_filter_item_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }
    
    private static int clearFilter(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerLevel level = ctx.getSource().getLevel();
            UUID villageId = VillageUUIDArgument.getVillageUUID(ctx, "villageUUID");
            UUID zoneId = ZoneUUIDArgument.getZoneUUID(ctx, "zoneUUID");
            
            Result result = ZoneService.clearFilter(level, villageId, zoneId);
            return result.send(ctx.getSource(), true);
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
            
            Result result = ZoneService.listFilter(level, villageId, zoneId);
            return result.send(ctx.getSource());
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                    String.format("%s.command.zone.list_filter_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }
}