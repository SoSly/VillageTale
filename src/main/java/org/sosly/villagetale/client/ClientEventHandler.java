package org.sosly.villagetale.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.item.LedgerItem;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = VillageTale.MOD_ID, value = Dist.CLIENT)
public class ClientEventHandler {
    private static boolean wasActive = false;

    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientDataManager.clearAll();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        ZoneCreationManager manager = ZoneCreationManager.getInstance();
        boolean isActive = manager.isActive();

        if (isActive != wasActive) {
            wasActive = isActive;
        }

        if (!isActive) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        playAmbientSound(mc.player);
        createParticleSpark(mc.level, mc.player);

        ItemStack mainHand = mc.player.getMainHandItem();
        if (!(mainHand.getItem() instanceof LedgerItem)) {
            manager.cancel();
        }
    }

    private static void createParticleSpark(Level level, Player player) {
        if (level == null || player == null || player.getRandom().nextFloat() > 0.25f) {
            return;
        }

        double yawRadians = Math.toRadians(-player.getYRot());
        double forwardX = Math.sin(yawRadians) * 0.6;
        double forwardZ = Math.cos(yawRadians) * 0.6;
        double rightX = Math.sin(yawRadians - Math.PI / 2) * 0.3;
        double rightZ = Math.cos(yawRadians - Math.PI / 2) * 0.3;

        double handX = player.getX() + forwardX + rightX;
        double handY = player.getEyeY() - 0.4;
        double handZ = player.getZ() + forwardZ + rightZ;

        float r = 0.5f + player.getRandom().nextFloat() * 0.5f;
        float g = 0.5f + player.getRandom().nextFloat() * 0.5f;
        float b = 0.5f + player.getRandom().nextFloat() * 0.5f;
        DustParticleOptions particleOptions = new DustParticleOptions(new Vector3f(r, g, b), 1.0f);

        level.addParticle(
                particleOptions,
                handX + (player.getRandom().nextDouble() - 0.5) * 0.3,
                handY + (player.getRandom().nextDouble() - 0.5) * 0.3,
                handZ + (player.getRandom().nextDouble() - 0.5) * 0.3,
                0, 0.02, 0
        );
    }

    private static void playAmbientSound(Player player) {
        if (player.getRandom().nextFloat() < 0.075f) {
            player.playNotifySound(SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.AMBIENT, 0.1f, 1.0f);
        }

        if (player.getRandom().nextFloat() < 0.25f) {
            float pitch = 1.0f + (player.getRandom().nextFloat() - 0.5f) * 0.4f;
            player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.AMBIENT, 1.25f, pitch);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide) {
            return;
        }

        ZoneCreationManager manager = ZoneCreationManager.getInstance();
        if (!manager.isActive()) {
            return;
        }

        Player player = event.getEntity();
        if (!(player.getMainHandItem().getItem() instanceof LedgerItem)) {
            return;
        }

        BlockPos pos = event.getPos();

        if (manager.handleClick(pos)) {
            playClickEffects(player, pos);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    private static void playClickEffects(Player player, BlockPos pos) {
        if (!player.level().isClientSide) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        player.playNotifySound(SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.BLOCKS, 0.5f, 1.2f);

            for (int i = 0; i < 8; i++) {
                mc.level.addParticle(
                    ParticleTypes.END_ROD,
                    pos.getX() + 0.5 + (Math.random() - 0.5) * 0.3,
                    pos.getY() + 0.5 + (Math.random() - 0.5) * 0.3,
                    pos.getZ() + 0.5 + (Math.random() - 0.5) * 0.3,
                    0, 0.05, 0
                );
            }
        }
    }
}
