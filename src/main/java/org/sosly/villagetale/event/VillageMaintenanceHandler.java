package org.sosly.villagetale.event;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.IVillageZone;
import org.sosly.villagetale.api.capability.IVillageCapability;
import org.sosly.villagetale.api.capability.IVillagesCapability;
import org.sosly.villagetale.capability.Capabilities;
import org.sosly.villagetale.data.VillageInfo;
import org.sosly.villagetale.entity.Villager;

@Mod.EventBusSubscriber(modid = VillageTale.MOD_ID)
public class VillageMaintenanceHandler {
    
    private static final int MAINTENANCE_INTERVAL = 6000; // 5 minutes in ticks (20 ticks/sec * 60 sec * 5 min)
    private static long lastMaintenanceTick = 0;
    
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        if (event.getServer() == null || event.getServer().getTickCount() - lastMaintenanceTick < MAINTENANCE_INTERVAL) {
            return;
        }
        
        lastMaintenanceTick = event.getServer().getTickCount();
        
        for (ServerLevel level : event.getServer().getAllLevels()) {
            performVillageMaintenance(level);
        }
    }
    
    private static void performVillageMaintenance(ServerLevel level) {
        IVillagesCapability villagesCapability = level.getCapability(Capabilities.VILLAGES_CAPABILITY).orElse(null);
        if (villagesCapability == null) {
            return;
        }
        
        VillageTale.LOGGER.debug("Performing village maintenance for dimension {}", level.dimension().location());
        
        for (VillageInfo villageInfo : villagesCapability.getVillages()) {
            maintainVillage(level, villageInfo);
        }
    }
    
    private static void maintainVillage(ServerLevel level, VillageInfo villageInfo) {
        ChunkPos villageChunk = villageInfo.getVillageStartingChunk();
        
        // Only maintain villages with loaded chunks
        if (!level.hasChunk(villageChunk.x, villageChunk.z)) {
            return;
        }
        
        LevelChunk chunk = level.getChunk(villageChunk.x, villageChunk.z);
        IVillageCapability villageCapability = chunk.getCapability(Capabilities.VILLAGE_CAPABILITY).orElse(null);
        if (villageCapability == null) {
            return;
        }
        
        // Check villager integrity
        Set<UUID> missingVillagers = new HashSet<>();
        for (UUID villagerId : villageCapability.getVillagerUUIDs()) {
            Entity entity = level.getEntity(villagerId);
            
            if (entity == null || !(entity instanceof Villager)) {
                missingVillagers.add(villagerId);
                VillageTale.LOGGER.warn("Village {} has reference to missing villager {}", 
                    villageInfo.getVillageName(), villagerId);
            } else {
                Villager villager = (Villager) entity;
                
                // Check if villager is dead
                if (!villager.isAlive() || villager.isRemoved()) {
                    missingVillagers.add(villagerId);
                    VillageTale.LOGGER.warn("Village {} has reference to dead/removed villager {}", 
                        villageInfo.getVillageName(), villagerId);
                }
                // Verify the villager still belongs to this village
                else if (villager.getVillage().isEmpty() || !villager.getVillage().get().equals(villageInfo.getVillageId())) {
                    missingVillagers.add(villagerId);
                    VillageTale.LOGGER.warn("Villager {} no longer belongs to village {}", 
                        villagerId, villageInfo.getVillageName());
                }
            }
        }
        
        // Clean up missing villagers
        for (UUID missingVillager : missingVillagers) {
            villageCapability.removeVillagerByUUID(missingVillager);
            
            // Also remove from any zone assignments
            for (IVillageZone zone : villageCapability.getZones()) {
                if (zone.removeAssignedVillager(missingVillager)) {
                    VillageTale.LOGGER.info("Removed missing villager {} from zone {}", 
                        missingVillager, zone.getName());
                }
            }
        }
        
        if (!missingVillagers.isEmpty()) {
            VillageTale.LOGGER.info("Cleaned up {} missing villager references from village {}", 
                missingVillagers.size(), villageInfo.getVillageName());
        }
        
        // Clean up expired zone claims (though they should auto-expire when accessed)
        long currentTime = level.getGameTime();
        for (IVillageZone zone : villageCapability.getZones()) {
            // This will trigger cleanup of expired claims
            zone.getClaims(currentTime);
        }
    }
}