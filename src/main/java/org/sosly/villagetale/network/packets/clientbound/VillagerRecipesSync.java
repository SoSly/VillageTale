package org.sosly.villagetale.network.packets.clientbound;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.client.ClientDataManager;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.network.BasePacket;
import org.sosly.villagetale.network.ClientPacketHandler;
import org.sosly.villagetale.network.NetworkHandler;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class VillagerRecipesSync extends BasePacket {
    private final int entityId;
    private final Set<ResourceLocation> recipes;

    private VillagerRecipesSync(int entityId, Set<ResourceLocation> recipes) {
        this.entityId = entityId;
        this.recipes = recipes;
    }

    public static void sendToPlayer(Villager villager, Set<ResourceLocation> recipes, ServerPlayer player) {
        VillagerRecipesSync packet = new VillagerRecipesSync(villager.getId(), recipes);
        NetworkHandler.CHANNEL.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void encode(VillagerRecipesSync msg, FriendlyByteBuf buffer) {
        buffer.writeVarInt(msg.entityId);
        buffer.writeInt(msg.recipes.size());
        for (ResourceLocation recipe : msg.recipes) {
            buffer.writeResourceLocation(recipe);
        }
    }

    public static VillagerRecipesSync decode(FriendlyByteBuf buffer) {
        VillagerRecipesSync msg;

        try {
            int entityId = buffer.readVarInt();
            int recipeCount = buffer.readInt();
            Set<ResourceLocation> recipes = new HashSet<>();
            for (int i = 0; i < recipeCount; i++) {
                recipes.add(buffer.readResourceLocation());
            }
            msg = new VillagerRecipesSync(entityId, recipes);
        } catch (IndexOutOfBoundsException | IllegalArgumentException err) {
            VillageTale.LOGGER.error("Exception while reading VillagerRecipesSync: {}", err.toString());
            return null;
        }

        msg.messageIsValid = true;
        return msg;
    }

    public static void handle(VillagerRecipesSync msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (ClientPacketHandler.validateBasics(msg, context)) {
            context.enqueueWork(() -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level == null) {
                    return;
                }

                Entity entity = mc.level.getEntity(msg.entityId);
                if (entity != null) {
                    ClientDataManager.cacheRecipes(msg.entityId, msg.recipes);
                }
            }).whenComplete((r, e) -> {
                if (e != null) {
                    throw new RuntimeException("Failed to handle VillagerRecipesSync", e);
                }
            });
        }
    }

    public int getEntityId() {
        return entityId;
    }

    public Set<ResourceLocation> getRecipes() {
        return recipes;
    }
}
