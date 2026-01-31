package com.topzurdo.mod.patches.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.topzurdo.mod.TopZurdoMod;
import com.topzurdo.mod.modules.ModuleManager;
import com.topzurdo.mod.modules.render.NoHurtCamModule;
import com.topzurdo.mod.modules.render.ZoomModule;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Mixin for GameRenderer: hurt camera shake (NoHurtCam), zoom FOV (Zoom).
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {

    /**
     * Cancel the hurt camera bobbing effect when NoHurtCam is enabled.
     */
    @Inject(method = "bobViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void onBobViewWhenHurt(MatrixStack matrixStack, float tickDelta, CallbackInfo ci) {
        ModuleManager mm = TopZurdoMod.getModuleManager();
        if (mm == null) return;

        NoHurtCamModule module = (NoHurtCamModule) mm.getModule("no_hurt_cam");
        if (module != null && module.isEnabled()) {
            float reduction = module.getShakeReduction();
            if (reduction >= 1.0f) {
                ci.cancel();
            }
        }
    }

    /**
     * Reduce FOV when Zoom module is active (hold C). getFov(Camera, float, boolean) in 1.16.5 yarn.
     */
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void onGetFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        ModuleManager mm = TopZurdoMod.getModuleManager();
        if (mm == null) return;

        ZoomModule zoom = (ZoomModule) mm.getModule("zoom");
        if (zoom == null || !zoom.isEnabled() || !zoom.isZooming()) return;

        double baseFov = cir.getReturnValue();
        float factor = zoom.getZoomFactor();
        if (factor > 0f) {
            cir.setReturnValue(baseFov / (double) factor);
        }
    }
}
