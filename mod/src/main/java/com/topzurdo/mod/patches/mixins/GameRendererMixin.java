package com.topzurdo.mod.patches.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.topzurdo.mod.TopZurdoMod;
import com.topzurdo.mod.modules.ModuleManager;
import com.topzurdo.mod.modules.render.NoHurtCamModule;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Mixin for GameRenderer to disable hurt camera shake.
 * Targets bobHurt method which applies the camera shake effect when damaged.
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {

    /**
     * Cancel the hurt camera bobbing effect when NoHurtCam is enabled.
     * Method: bobViewWhenHurt(MatrixStack, float)V in 1.16.5 yarn mappings.
     */
    @Inject(method = "bobViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void onBobViewWhenHurt(MatrixStack matrixStack, float tickDelta, CallbackInfo ci) {
        ModuleManager mm = TopZurdoMod.getModuleManager();
        if (mm == null) return;

        NoHurtCamModule module = (NoHurtCamModule) mm.getModule("no_hurt_cam");
        if (module != null && module.isEnabled()) {
            float reduction = module.getShakeReduction();
            if (reduction >= 1.0f) {
                // Completely cancel the hurt effect
                ci.cancel();
            }
            // For partial reduction, we'd need @ModifyVariable but full cancel is simpler
        }
    }
}
