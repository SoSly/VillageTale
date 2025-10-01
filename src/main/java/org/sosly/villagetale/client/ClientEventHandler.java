package org.sosly.villagetale.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.network.ClientPacketHandler;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = VillageTale.MOD_ID, value = Dist.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientPacketHandler.clearCache();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        if (KeyBindings.TOGGLE_VILLAGE_BOUNDARIES.consumeClick()) {
            long window = mc.getWindow().getWindow();
            boolean f3Pressed = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_F3) == GLFW.GLFW_PRESS;

            if (f3Pressed) {
                BoundaryDataStorage.getInstance().toggleOverlay();
            }
        }
    }
}