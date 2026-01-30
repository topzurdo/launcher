package com.topzurdo.mod.patches.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.topzurdo.mod.TopZurdoMod;
import com.topzurdo.mod.modules.render.ViewModelChangerModule;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

/**
 * ViewModel: push + transform + swing progress modification.
 * applyTransform: position, rotation, scale for hands.
 * getModifiedSwingProgress: swing speed and smooth animation.
 *
 * Targets HeldItemRenderer which handles first-person item rendering in 1.16.5.
 */
@Mixin(HeldItemRenderer.class)
public class ViewModelMixin {

    private static final ThreadLocal<Boolean> viewModelPushed = ThreadLocal.withInitial(() -> false);

    /**
     * renderFirstPersonItem in HeldItemRenderer (1.16.5 yarn mappings).
     * Signature: (AbstractClientPlayerEntity, float, float, Hand, float, ItemStack, float, MatrixStack, VertexConsumerProvider, int)V
     */
    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), require = 0)
    private void onRenderItemInFirstPersonHEAD(AbstractClientPlayerEntity player, float tickDelta, float pitch,
            Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices,
            VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        ViewModelChangerModule m = getModule();
        if (m != null && m.isEnabled()) {
            matrices.push();
            m.applyTransform(matrices, hand);
            viewModelPushed.set(true);
        }
    }

    @Inject(method = "renderFirstPersonItem", at = @At("TAIL"), require = 0)
    private void onRenderItemInFirstPersonTAIL(AbstractClientPlayerEntity player, float tickDelta, float pitch,
            Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices,
            VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (viewModelPushed.get()) {
            matrices.pop();
            viewModelPushed.set(false);
        }
    }

    @ModifyVariable(method = "renderFirstPersonItem", at = @At("HEAD"), ordinal = 0, argsOnly = true, require = 0)
    private float modifySwingProgress(float swingProgress) {
        ViewModelChangerModule m = getModule();
        if (m != null && m.isEnabled()) {
            return m.getModifiedSwingProgress(swingProgress);
        }
        return swingProgress;
    }

    private static ViewModelChangerModule getModule() {
        com.topzurdo.mod.modules.ModuleManager mm = TopZurdoMod.getModuleManager();
        return mm != null ? (ViewModelChangerModule) mm.getModule("viewmodel_changer") : null;
    }
}
