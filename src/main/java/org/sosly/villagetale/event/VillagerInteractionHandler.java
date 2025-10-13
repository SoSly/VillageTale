package org.sosly.villagetale.event;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.item.LedgerItem;
import org.sosly.villagetale.network.packets.clientbound.OpenVillagerConversionScreen;

@Mod.EventBusSubscriber(modid = VillageTale.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class VillagerInteractionHandler {
    private static final Set<Integer> VILLAGERS_IN_CONVERSATION = new HashSet<>();

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        // CHECKSTYLE:OFF RegexpSingleline - legitimate FQCN due to name collision with org.sosly.villagetale.entity.Villager
        if (!(event.getTarget() instanceof net.minecraft.world.entity.npc.Villager villager)) {
        // CHECKSTYLE:ON
            return;
        }

        Player player = event.getEntity();
        InteractionHand hand = event.getHand();
        ItemStack stack = player.getItemInHand(hand);

        if (!(stack.getItem() instanceof LedgerItem)) {
            return;
        }

        if (player.level().isClientSide) {
            return;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            villager.getNavigation().stop();
            villager.lookAt(player, 180.0F, 180.0F);
            VILLAGERS_IN_CONVERSATION.add(villager.getId());

            OpenVillagerConversionScreen.send(serverPlayer, event.getTarget().getId());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || VILLAGERS_IN_CONVERSATION.isEmpty()) {
            return;
        }

        VILLAGERS_IN_CONVERSATION.removeIf(id -> {
            for (var level : event.getServer().getAllLevels()) {
                Entity entity = level.getEntity(id);
                // CHECKSTYLE:OFF RegexpSingleline - legitimate FQCNs due to name collision
                if (entity instanceof org.sosly.villagetale.entity.Villager vtVillager) {
                    vtVillager.getNavigation().stop();
                    return false;
                }
                if (entity instanceof net.minecraft.world.entity.npc.Villager villager) {
                    villager.getNavigation().stop();
                    return false;
                }
                // CHECKSTYLE:ON
            }
            return true;
        });
    }

    public static void addVillagerToConversation(int villagerId) {
        VILLAGERS_IN_CONVERSATION.add(villagerId);
    }

    public static void releaseVillager(int villagerId) {
        VILLAGERS_IN_CONVERSATION.remove(villagerId);
    }
}
