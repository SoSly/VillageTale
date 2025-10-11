package org.sosly.villagetale.network.packets.serverbound;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.api.capability.IRecipeKnowledgeCapability;
import org.sosly.villagetale.capability.Capabilities;
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.network.BasePacket;
import org.sosly.villagetale.network.NetworkHandler;
import org.sosly.villagetale.network.ServerPacketHandler;

public class UpdateVillagerRecipes extends BasePacket {
    private final int villagerEntityId;
    private final Set<ResourceLocation> recipes;

    private UpdateVillagerRecipes(int villagerEntityId, Set<ResourceLocation> recipes) {
        this.villagerEntityId = villagerEntityId;
        this.recipes = recipes;
    }

    public static void send(int villagerEntityId, Set<ResourceLocation> recipes) {
        UpdateVillagerRecipes packet = new UpdateVillagerRecipes(villagerEntityId, recipes);
        NetworkHandler.CHANNEL.sendToServer(packet);
    }

    public static void encode(UpdateVillagerRecipes msg, FriendlyByteBuf buffer) {
        buffer.writeInt(msg.villagerEntityId);
        buffer.writeInt(msg.recipes.size());
        for (ResourceLocation recipe : msg.recipes) {
            buffer.writeResourceLocation(recipe);
        }
    }

    public static UpdateVillagerRecipes decode(FriendlyByteBuf buffer) {
        UpdateVillagerRecipes msg;

        try {
            int villagerEntityId = buffer.readInt();
            int recipeCount = buffer.readInt();
            Set<ResourceLocation> recipes = new HashSet<>();
            for (int i = 0; i < recipeCount; i++) {
                recipes.add(buffer.readResourceLocation());
            }
            msg = new UpdateVillagerRecipes(villagerEntityId, recipes);
        } catch (IndexOutOfBoundsException | IllegalArgumentException err) {
            VillageTale.LOGGER.error("Exception while reading UpdateVillagerRecipes: {}", err.toString());
            return null;
        }

        msg.messageIsValid = true;
        return msg;
    }

    public static void handle(UpdateVillagerRecipes msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (!ServerPacketHandler.validateBasics(msg, context)) {
            return;
        }

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            ServerLevel level = player.serverLevel();
            Entity entity = level.getEntity(msg.villagerEntityId);
            if (!(entity instanceof Villager villager)) {
                player.sendSystemMessage(Component.literal("Villager not found"));
                return;
            }

            IRecipeKnowledgeCapability knowledge = villager.getCapability(Capabilities.RECIPE_KNOWLEDGE_CAPABILITY)
                .orElse(null);

            if (knowledge == null) {
                player.sendSystemMessage(Component.literal("Failed to update recipes: capability not found"));
                return;
            }

            Set<ResourceLocation> validatedRecipes = new HashSet<>();
            for (ResourceLocation recipeId : msg.recipes) {
                Optional<?> recipe = level.getRecipeManager().byKey(recipeId);
                if (recipe.isEmpty()) {
                    continue;
                }

                if (villager.getProfession() != null) {
                    if (villager.getProfession().getLearnableItems().isEmpty() ||
                        villager.getProfession().getLearnableItems().matches(
                            level.getRecipeManager().byKey(recipeId)
                                .map(r -> r.getResultItem(level.registryAccess()))
                                .orElse(net.minecraft.world.item.ItemStack.EMPTY)
                        )) {
                        validatedRecipes.add(recipeId);
                    }
                }
            }

            Set<ResourceLocation> currentRecipes = knowledge.known();
            for (ResourceLocation recipeId : currentRecipes) {
                if (!validatedRecipes.contains(recipeId)) {
                    knowledge.forget(recipeId);
                }
            }

            for (ResourceLocation recipeId : validatedRecipes) {
                if (!currentRecipes.contains(recipeId)) {
                    knowledge.learn(level, recipeId);
                }
            }
        });
    }
}
