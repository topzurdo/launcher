package com.topzurdo.mod.patches.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.topzurdo.mod.TopZurdoMod;
import com.topzurdo.mod.modules.ModuleManager;
import com.topzurdo.mod.modules.render.CustomCrosshairModule;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Mixin for InGameHud to hide vanilla crosshair when CustomCrosshair is enabled.
 */
@Mixin(InGameHud.class)
public class InGameHudMixin {

    /**
     * Cancel vanilla crosshair rendering when CustomCrosshair module is active.
     * Target method: renderCrosshair(MatrixStack)V in 1.16.5 yarn mappings.
     */
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void onRenderCrosshair(MatrixStack matrices, CallbackInfo ci) {
        ModuleManager mm = TopZurdoMod.getModuleManager();
        if (mm == null) return;

        CustomCrosshairModule module = (CustomCrosshairModule) mm.getModule("custom_crosshair");
        if (module != null && module.isEnabled()) {
            // Cancel vanilla crosshair rendering - our custom one will be drawn instead
            ci.cancel();
        }
    }
}
