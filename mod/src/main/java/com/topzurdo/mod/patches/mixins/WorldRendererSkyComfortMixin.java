package com.topzurdo.mod.patches.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.topzurdo.mod.TopZurdoMod;
import com.topzurdo.mod.modules.ModuleManager;
import com.topzurdo.mod.modules.render.SkyComfortModule;

import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;

/**
 * SkyComfort: custom sky color, brightness, hide stars, hide sun/moon.
 */
@Mixin(WorldRenderer.class)
public class WorldRendererSkyComfortMixin {

    @Inject(method = "renderSky", at = @At("HEAD"))
    private void onRenderSkyHead(net.minecraft.client.util.math.MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        ModuleManager mm = TopZurdoMod.getModuleManager();
        if (mm == null) return;
        SkyComfortModule m = (SkyComfortModule) mm.getModule("sky_comfort");
        if (m == null || !m.isEnabled()) return;
        int sky = m.getSkyColor();
        float br = m.getBrightness();
        float r = ((sky >> 16) & 0xFF) / 255f * br;
        float g = ((sky >> 8) & 0xFF) / 255f * br;
        float b = (sky & 0xFF) / 255f * br;
        com.mojang.blaze3d.systems.RenderSystem.clearColor(r, g, b, 1f);
    }

    @Inject(method = "renderStars()V", at = @At("HEAD"), cancellable = true)
    private void onRenderStars(CallbackInfo ci) {
        ModuleManager mm = TopZurdoMod.getModuleManager();
        if (mm == null) return;
        SkyComfortModule m = (SkyComfortModule) mm.getModule("sky_comfort");
        if (m != null && m.isEnabled() && m.shouldDisableStars()) {
            ci.cancel();
        }
    }

    @Redirect(method = "renderSky", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/texture/TextureManager;bindTexture(Lnet/minecraft/util/Identifier;)V"))
    private void onBindTextureInSky(TextureManager textureManager, Identifier id) {
        ModuleManager mm = TopZurdoMod.getModuleManager();
        if (mm != null) {
            SkyComfortModule m = (SkyComfortModule) mm.getModule("sky_comfort");
            if (m != null && m.isEnabled() && m.shouldDisableSunMoon()) {
                String path = id != null ? id.getPath() : "";
                if (path.contains("sun") || path.contains("moon")) {
                    return;
                }
            }
        }
        textureManager.bindTexture(id);
    }
}
