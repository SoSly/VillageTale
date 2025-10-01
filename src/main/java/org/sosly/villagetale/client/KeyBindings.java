package org.sosly.villagetale.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;
import org.sosly.villagetale.VillageTale;

@OnlyIn(Dist.CLIENT)
public class KeyBindings {
    public static final String CATEGORY = "key.categories." + VillageTale.MOD_ID;

    public static final KeyMapping TOGGLE_VILLAGE_BOUNDARIES = new KeyMapping(
        "key." + VillageTale.MOD_ID + ".toggle_village_boundaries",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_V,
        CATEGORY
    );

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_VILLAGE_BOUNDARIES);
    }
}
