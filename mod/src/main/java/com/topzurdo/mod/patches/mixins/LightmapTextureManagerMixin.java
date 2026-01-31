package com.topzurdo.mod.patches.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.topzurdo.mod.TopZurdoMod;
import com.topzurdo.mod.modules.Module;
import com.topzurdo.mod.modules.render.FullBrightModule;

import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.LightmapTextureManager;

/**
 * When FullBright is enabled, override gamma read during lightmap update
 * so the game uses our brightness instead of vanilla (clamped) gamma.
 */
@Mixin(LightmapTextureManager.class)
public class LightmapTextureManagerMixin {

    /** Minimum gamma to avoid / by zero in lightmap code (e.g. Sodium/vanilla). */
    private static final double MIN_GAMMA = 0.01;

    @Redirect(
        method = "update",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;gamma:D")
    )
    private double onReadGamma(GameOptions options) {
        double vanilla = options.gamma;
        TopZurdoMod mod = TopZurdoMod.getInstance();
        if (mod == null || mod.getModuleManager() == null)
            return Math.max(MIN_GAMMA, vanilla);
        Module m = mod.getModuleManager().getModule("fullbright");
        if (!(m instanceof FullBrightModule) || !m.isEnabled())
            return Math.max(MIN_GAMMA, vanilla);
        float br = ((FullBrightModule) m).getBrightness();
        return Math.max(MIN_GAMMA, Math.min(16.0, 1.0 * (10.0 + 6.0 * br)));
    }
}
