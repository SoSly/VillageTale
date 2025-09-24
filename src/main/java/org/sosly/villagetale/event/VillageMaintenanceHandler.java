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
    
    private static final int MAINTENANCE_INTERVAL = 6000;
    private static long lastMaintenanceTick = 0;
    
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        if (event.getServer() == null) {
            return;
        }
        
        if (event.getServer().getTickCount() - lastMaintenanceTick < MAINTENANCE_INTERVAL) {
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
        
        if (!level.hasChunk(villageChunk.x, villageChunk.z)) {
            return;
        }
        
        LevelChunk chunk = level.getChunk(villageChunk.x, villageChunk.z);
        IVillageCapability villageCapability = chunk.getCapability(Capabilities.VILLAGE_CAPABILITY).orElse(null);
        if (villageCapability == null) {
            return;
        }
        
        Set<UUID> missingVillagers = findMissingVillagers(level, villageInfo, villageCapability);
        cleanupMissingVillagers(villageInfo, villageCapability, missingVillagers);
        cleanupExpiredZoneClaims(level, villageCapability);
    }
    
    private static Set<UUID> findMissingVillagers(ServerLevel level, VillageInfo villageInfo, IVillageCapability villageCapability) {
        Set<UUID> missingVillagers = new HashSet<>();
        
        for (UUID villagerId : villageCapability.getVillagerUUIDs()) {
            if (shouldRemoveVillager(level, villageInfo, villagerId)) {
                missingVillagers.add(villagerId);
            }
        }
        
        return missingVillagers;
    }
    
    private static boolean shouldRemoveVillager(ServerLevel level, VillageInfo villageInfo, UUID villagerId) {
        Entity entity = level.getEntity(villagerId);
        
        if (!(entity instanceof Villager villager)) {
            VillageTale.LOGGER.warn("Village {} has reference to missing villager {}", 
                villageInfo.getVillageName(), villagerId);
            return true;
        }
        
        if (!villager.isAlive() || villager.isRemoved()) {
            VillageTale.LOGGER.warn("Village {} has reference to dead/removed villager {}", 
                villageInfo.getVillageName(), villagerId);
            return true;
        }
        
        if (villager.getVillage().isEmpty() || !villager.getVillage().get().equals(villageInfo.getVillageId())) {
            VillageTale.LOGGER.warn("Villager {} no longer belongs to village {}", 
                villagerId, villageInfo.getVillageName());
            return true;
        }
        
        return false;
    }
    
    private static void cleanupMissingVillagers(VillageInfo villageInfo, IVillageCapability villageCapability, Set<UUID> missingVillagers) {
        if (missingVillagers.isEmpty()) {
            return;
        }
        
        for (UUID missingVillager : missingVillagers) {
            villageCapability.removeVillagerByUUID(missingVillager);
            removeVillagerFromZones(villageCapability, missingVillager);
        }
        
        VillageTale.LOGGER.info("Cleaned up {} missing villager references from village {}", 
            missingVillagers.size(), villageInfo.getVillageName());
    }
    
    private static void removeVillagerFromZones(IVillageCapability villageCapability, UUID villagerId) {
        for (IVillageZone zone : villageCapability.getZones()) {
            if (zone.removeAssignedVillager(villagerId)) {
                VillageTale.LOGGER.info("Removed missing villager {} from zone {}", 
                    villagerId, zone.getName());
            }
        }
    }
    
    private static void cleanupExpiredZoneClaims(ServerLevel level, IVillageCapability villageCapability) {
        long currentTime = level.getGameTime();
        for (IVillageZone zone : villageCapability.getZones()) {
            zone.getClaims(currentTime);
        }
    }
}