package com.topzurdo.mod.patches.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.topzurdo.mod.TopZurdoMod;
import com.topzurdo.mod.modules.render.WeatherControlModule;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;

/**
 * World mixin for client-side overrides:
 * - getRainGradient/getThunderGradient: WeatherControl module
 * Only applies to ClientWorld instances.
 * Note: TimeChanger uses ClientWorldPropertiesMixin instead.
 */
@Mixin(World.class)
public class WorldMixin {

    @Inject(method = "getRainGradient", at = @At("RETURN"), cancellable = true)
    private void onGetRainGradient(float tickDelta, CallbackInfoReturnable<Float> cir) {
        if (!((Object) this instanceof ClientWorld)) return;
        TopZurdoMod inst = TopZurdoMod.getInstance();
        if (inst == null || inst.getModuleManager() == null) return;
        WeatherControlModule m = (WeatherControlModule) inst.getModuleManager().getModule("weather_control");
        if (m == null) return;
        float f = m.getRainFactor();
        if (f >= 1f) return;
        cir.setReturnValue(cir.getReturnValue() * f);
    }

    @Inject(method = "getThunderGradient", at = @At("RETURN"), cancellable = true)
    private void onGetThunderGradient(float tickDelta, CallbackInfoReturnable<Float> cir) {
        if (!((Object) this instanceof ClientWorld)) return;
        TopZurdoMod inst = TopZurdoMod.getInstance();
        if (inst == null || inst.getModuleManager() == null) return;
        WeatherControlModule m = (WeatherControlModule) inst.getModuleManager().getModule("weather_control");
        if (m == null) return;
        float f = m.getThunderFactor();
        if (f >= 1f) return;
        cir.setReturnValue(cir.getReturnValue() * f);
    }
}
