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
import org.sosly.villagetale.entity.Villager;
import org.sosly.villagetale.item.LedgerItem;
import org.sosly.villagetale.network.packets.clientbound.OpenVillagerConversionScreen;

@Mod.EventBusSubscriber(modid = VillageTale.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class VillagerInteractionHandler {
    private static final Set<Integer> villagersInConversation = new HashSet<>();

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof net.minecraft.world.entity.npc.Villager villager)) {
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
            villagersInConversation.add(villager.getId());

            OpenVillagerConversionScreen.send(serverPlayer, event.getTarget().getId());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || villagersInConversation.isEmpty()) {
            return;
        }

        villagersInConversation.removeIf(id -> {
            for (var level : event.getServer().getAllLevels()) {
                Entity entity = level.getEntity(id);
                if (entity instanceof Villager vtVillager) {
                    vtVillager.getNavigation().stop();
                    return false;
                }
                if (entity instanceof net.minecraft.world.entity.npc.Villager villager) {
                    villager.getNavigation().stop();
                    return false;
                }
            }
            return true;
        });
    }

    public static void addVillagerToConversation(int villagerId) {
        villagersInConversation.add(villagerId);
    }

    public static void releaseVillager(int villagerId) {
        villagersInConversation.remove(villagerId);
    }
}
