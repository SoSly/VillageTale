package org.sosly.villagetale.network;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.sosly.villagetale.client.BoundaryDataStorage;
import org.sosly.villagetale.data.VillageBoundaryData;
import org.sosly.villagetale.data.ZoneBoundaryData;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class ClientPacketHandler {
    private static final Map<Integer, ResourceLocation> professionCache = new HashMap<>();

    public static void handleProfessionSync(VillagerProfessionSyncPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        Entity entity = mc.level.getEntity(packet.getEntityId());
        if (entity != null) {
            professionCache.put(packet.getEntityId(), packet.getProfessionId());
        }
    }

    public static ResourceLocation getCachedProfession(int entityId) {
        return professionCache.get(entityId);
    }

    public static void clearCache() {
        professionCache.clear();
        BoundaryDataStorage.getInstance().clearAll();
    }

    public static void handleEquipmentSync(VillagerEquipmentSyncPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        Entity entity = mc.level.getEntity(packet.getEntityId());
        if (entity instanceof LivingEntity livingEntity) {
            livingEntity.setItemInHand(packet.getHand(), packet.getItemStack());
        }
    }

    public static void handleVillageBoundary(VillageBoundaryPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        VillageBoundaryData data = new VillageBoundaryData(
            packet.getVillageId(),
            packet.getCenterChunk(),
            packet.getSquadius()
        );
        BoundaryDataStorage.getInstance().addVillage(mc.level.dimension(), data);
    }

    public static void handleZoneBoundary(ZoneBoundaryPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        ZoneBoundaryData data = new ZoneBoundaryData(
            packet.getZoneId(),
            packet.getVillageId(),
            packet.getShapeType(),
            packet.getBounds(),
            packet.getCenter(),
            packet.getRadius(),
            packet.getHeight(),
            packet.getWaypoints()
        );
        BoundaryDataStorage.getInstance().addZone(mc.level.dimension(), data);
    }
}