package com.topzurdo.mod.patches.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.topzurdo.mod.TopZurdoMod;
import com.topzurdo.mod.modules.render.DamageNumbersModule;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;

/**
 * Mixin for LivingEntity: record damage/heal for DamageNumbersModule.
 * Uses intermediary method_5643 (Entity.damage) — LivingEntity overrides it.
 */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Unique
    private float topzurdoPrevHealth = -1f;

    @Inject(method = "method_5643", at = @At("TAIL"), remap = false)
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (TopZurdoMod.getModuleManager() == null) return;
        DamageNumbersModule mod = (DamageNumbersModule) TopZurdoMod.getModuleManager().getModule("damage_numbers");
        if (mod == null || !mod.isEnabled()) return;
        LivingEntity self = (LivingEntity) (Object) this;
        if (amount > 0) mod.recordDamage(self, amount, false);
    }

    @Inject(method = "method_6033", at = @At("HEAD"), remap = false)
    private void onSetHealthHead(float health, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        topzurdoPrevHealth = self.getHealth();
    }

    @Inject(method = "method_6033", at = @At("TAIL"), remap = false)
    private void onSetHealthTail(float health, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (topzurdoPrevHealth >= 0 && health > topzurdoPrevHealth) {
            if (TopZurdoMod.getModuleManager() != null) {
                DamageNumbersModule mod = (DamageNumbersModule) TopZurdoMod.getModuleManager().getModule("damage_numbers");
                if (mod != null && mod.isEnabled() && mod.showHeal()) {
                    mod.recordHeal(self, health - topzurdoPrevHealth);
                }
            }
        }
    }
}
