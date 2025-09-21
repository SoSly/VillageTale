package org.sosly.villagetale.network;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
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
    }
}