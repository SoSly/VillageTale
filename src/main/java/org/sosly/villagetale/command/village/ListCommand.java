package org.sosly.villagetale.command.village;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.capability.IVillagesCapability;
import org.sosly.villagetale.command.Result;
import org.sosly.villagetale.data.VillageInfo;

public class ListCommand {
    
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("list")
                .executes(ListCommand::listVillages);
    }
    
    private static int listVillages(CommandContext<CommandSourceStack> ctx) {
        try {
            CommandSourceStack source = ctx.getSource();
            ServerLevel level = source.getLevel();

            IVillagesCapability villages = VillageService.getVillagesCapability(level);
            if (villages == null || villages.getVillages().isEmpty()) {
                return Result.failure(Component.translatable(
                        String.format("%s.command.village.no_villages_dimension", VillageTale.MOD_ID))).send(source);
            }

            source.sendSuccess(() ->
                    Component.translatable(String.format("%s.command.village.list_header", VillageTale.MOD_ID),
                            level.dimension().location()), false);

            for (VillageInfo village : villages.getVillages()) {
                BlockPos townHall = village.getTownHallPos();
                ChunkPos chunkPos = new ChunkPos(townHall);

                source.sendSuccess(() ->
                        Component.translatable(String.format("%s.command.village.list_entry", VillageTale.MOD_ID),
                                village.getVillageName(),
                                townHall.getX(), townHall.getY(), townHall.getZ(),
                                chunkPos.x, chunkPos.z,
                                village.getSquadius()), false);
            }

            return villages.getVillages().size();
        } catch (Exception e) {
            return Result.failure(Component.translatable(
                            String.format("%s.command.village.list_error", VillageTale.MOD_ID), e.getMessage()))
                    .send(ctx.getSource());
        }
    }
}