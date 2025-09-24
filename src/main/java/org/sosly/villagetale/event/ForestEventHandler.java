package org.sosly.villagetale.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.SaplingGrowTreeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.capability.Capabilities;
import org.sosly.villagetale.zone.type.Forest;

@Mod.EventBusSubscriber(modid = VillageTale.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForestEventHandler {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockState state = event.getState();

        if (!state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES)) {
            return;
        }

        processBlockChange(level, pos);
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockState state = event.getPlacedBlock();

        if (!state.is(BlockTags.LOGS)) {
            return;
        }

        processBlockChange(level, pos);
    }

    @SubscribeEvent
    public static void onSaplingGrow(SaplingGrowTreeEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        BlockPos pos = event.getPos();
        processBlockChange(level, pos);
    }

    private static void processBlockChange(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        IVillageCapability villageCapability = chunk.getCapability(Capabilities.VILLAGE_CAPABILITY).orElse(null);
        
        if (villageCapability == null) {
            return;
        }

        for (IVillageZone zone : villageCapability.getZones()) {
            if (!(zone.getType() instanceof Forest forest)) {
                continue;
            }
            
            if (!zone.containsPosition(pos, 5)) {
                continue;
            }
            
            forest.markDirty(pos);
            
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        BlockPos adjacent = pos.offset(dx, dy, dz);
                        if (!zone.containsPosition(adjacent)) {
                            continue;
                        }
                        
                        BlockState adjacentState = level.getBlockState(adjacent);
                        if (!adjacentState.is(BlockTags.LOGS) && !adjacentState.is(BlockTags.LEAVES)) {
                            continue;
                        }
                        
                        forest.markDirty(adjacent);
                    }
                }
            }
            
            forest.revalidateDirtyPositions(level, zone.getShape());
        }
    }
}