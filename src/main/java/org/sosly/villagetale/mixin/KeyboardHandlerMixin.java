package org.sosly.villagetale.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.sosly.villagetale.VillageTale;
import org.sosly.villagetale.client.BoundaryDataStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private boolean handledDebugKey;

    @Inject(method = "keyPress", at = @At("HEAD"))
    private void enableDebugOverlay(long pWindowPointer, int pKey, int pScanCode, int pAction, int pModifiers, CallbackInfo ci) {
        if (pWindowPointer != this.minecraft.getWindow().getWindow() || this.minecraft.screen != null) {
            return;
        }

        if (!InputConstants.isKeyDown(this.minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_F3)) {
            return;
        }

        if (pAction != GLFW.GLFW_PRESS || pKey != GLFW.GLFW_KEY_V) {
            return;
        }


        VillageTale.LOGGER.debug("F3+V detected, toggling overlay");
        BoundaryDataStorage.getInstance().toggleOverlay();
        this.handledDebugKey = true;
    }
}
