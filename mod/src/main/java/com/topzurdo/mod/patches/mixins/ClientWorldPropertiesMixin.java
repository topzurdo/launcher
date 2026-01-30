package com.topzurdo.mod.patches.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.topzurdo.mod.TopZurdoMod;
import com.topzurdo.mod.modules.ModuleManager;
import com.topzurdo.mod.modules.render.TimeChangerModule;

import net.minecraft.client.world.ClientWorld;

/**
 * Mixin for ClientWorld.Properties to override getTimeOfDay().
 * This is the correct target for client-side time manipulation in 1.16.5.
 */
@Mixin(ClientWorld.Properties.class)
public class ClientWorldPropertiesMixin {

    /**
     * Override getTimeOfDay when TimeChanger module is enabled.
     * Returns the module's target time instead of the actual world time.
     */
    @Inject(method = "getTimeOfDay", at = @At("HEAD"), cancellable = true)
    private void onGetTimeOfDay(CallbackInfoReturnable<Long> cir) {
        ModuleManager mm = TopZurdoMod.getModuleManager();
        if (mm == null) return;

        TimeChangerModule module = (TimeChangerModule) mm.getModule("time_changer");
        if (module != null && module.isEnabled()) {
            cir.setReturnValue((long) module.getTargetTime());
        }
    }
}
