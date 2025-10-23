package org.sosly.villagetale.network.packets.clientbound;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.client.ClientDataManager;
import org.sosly.villagetale.gui.LedgerScreen;
import org.sosly.villagetale.gui.pages.VillagerManagementPage;
import org.sosly.villagetale.gui.pages.VillagerStatsPage;
import org.sosly.villagetale.network.BasePacket;
import org.sosly.villagetale.network.ClientPacketHandler;
import org.sosly.villagetale.network.NetworkHandler;

public class OpenVillagerManagementScreen extends BasePacket {
    private final int villagerEntityId;
    private final UUID villageId;
    private final UUID homeZoneId;
    private final UUID workZoneId;
    private final List<ItemStack> inventory;
    private final float health;
    private final int hunger;
    private final int physique;
    private final int endurance;
    private final int intellect;
    private final List<ResourceLocation> knownRecipes;

    private OpenVillagerManagementScreen(int villagerEntityId, UUID villageId, UUID homeZoneId, UUID workZoneId, List<ItemStack> inventory, float health, int hunger, int physique, int endurance, int intellect, List<ResourceLocation> knownRecipes) {
        this.villagerEntityId = villagerEntityId;
        this.villageId = villageId;
        this.homeZoneId = homeZoneId;
        this.workZoneId = workZoneId;
        this.inventory = inventory;
        this.health = health;
        this.hunger = hunger;
        this.physique = physique;
        this.endurance = endurance;
        this.intellect = intellect;
        this.knownRecipes = knownRecipes;
    }

    public static void send(ServerPlayer player, int villagerEntityId, UUID villageId, UUID homeZoneId, UUID workZoneId, List<ItemStack> inventory, float health, int hunger, int physique, int endurance, int intellect, List<ResourceLocation> knownRecipes) {
        OpenVillagerManagementScreen packet = new OpenVillagerManagementScreen(villagerEntityId, villageId, homeZoneId, workZoneId, inventory, health, hunger, physique, endurance, intellect, knownRecipes);
        NetworkHandler.CHANNEL.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void encode(OpenVillagerManagementScreen msg, FriendlyByteBuf buffer) {
        buffer.writeInt(msg.villagerEntityId);
        buffer.writeUUID(msg.villageId);
        buffer.writeBoolean(msg.homeZoneId != null);
        if (msg.homeZoneId != null) {
            buffer.writeUUID(msg.homeZoneId);
        }
        buffer.writeBoolean(msg.workZoneId != null);
        if (msg.workZoneId != null) {
            buffer.writeUUID(msg.workZoneId);
        }
        buffer.writeInt(msg.inventory.size());
        for (ItemStack stack : msg.inventory) {
            buffer.writeItem(stack);
        }
        buffer.writeFloat(msg.health);
        buffer.writeInt(msg.hunger);
        buffer.writeInt(msg.physique);
        buffer.writeInt(msg.endurance);
        buffer.writeInt(msg.intellect);
        buffer.writeInt(msg.knownRecipes.size());
        for (ResourceLocation recipe : msg.knownRecipes) {
            buffer.writeResourceLocation(recipe);
        }
    }

    public static OpenVillagerManagementScreen decode(FriendlyByteBuf buffer) {
        OpenVillagerManagementScreen msg;

        try {
            int villagerEntityId = buffer.readInt();
            UUID villageId = buffer.readUUID();
            UUID homeZoneId = buffer.readBoolean() ? buffer.readUUID() : null;
            UUID workZoneId = buffer.readBoolean() ? buffer.readUUID() : null;
            int inventorySize = buffer.readInt();
            List<ItemStack> inventory = new ArrayList<>();
            for (int i = 0; i < inventorySize; i++) {
                inventory.add(buffer.readItem());
            }
            float health = buffer.readFloat();
            int hunger = buffer.readInt();
            int physique = buffer.readInt();
            int endurance = buffer.readInt();
            int intellect = buffer.readInt();
            int recipeCount = buffer.readInt();
            List<ResourceLocation> knownRecipes = new ArrayList<>();
            for (int i = 0; i < recipeCount; i++) {
                knownRecipes.add(buffer.readResourceLocation());
            }
            msg = new OpenVillagerManagementScreen(villagerEntityId, villageId, homeZoneId, workZoneId, inventory, health, hunger, physique, endurance, intellect, knownRecipes);
        } catch (IndexOutOfBoundsException | IllegalArgumentException err) {
            VillageTale.LOGGER.error("Exception while reading OpenVillagerManagementScreen: {}", err.toString());
            return null;
        }

        msg.messageIsValid = true;
        return msg;
    }

    public static void handle(OpenVillagerManagementScreen msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (!ClientPacketHandler.validateBasics(msg, context)) {
            return;
        }

        context.enqueueWork(() -> {
            ClientDataManager.cacheRecipes(msg.villagerEntityId, new HashSet<>(msg.knownRecipes));
            Minecraft mc = Minecraft.getInstance();
            LedgerScreen screen = new LedgerScreen(Component.translatable("villagetale.gui.villager_management.title"));
            screen.setLeftPage(new VillagerManagementPage(screen, msg.villagerEntityId, msg.villageId, msg.homeZoneId, msg.workZoneId));
            screen.setRightPage(new VillagerStatsPage(screen, msg.villagerEntityId, msg.villageId, msg.health, msg.hunger, msg.physique, msg.endurance, msg.intellect));
            mc.setScreen(screen);
        });
    }

    public int getVillagerEntityId() {
        return villagerEntityId;
    }

    public UUID getVillageId() {
        return villageId;
    }

    public UUID getHomeZoneId() {
        return homeZoneId;
    }

    public UUID getWorkZoneId() {
        return workZoneId;
    }

    public List<ItemStack> getInventory() {
        return inventory;
    }
}
