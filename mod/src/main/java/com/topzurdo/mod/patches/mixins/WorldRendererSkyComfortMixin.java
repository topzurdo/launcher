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

    /**
     * Applies the SkyComfort module's configured sky color and brightness to the GL clear color before sky rendering.
     *
     * If the SkyComfort module is enabled, computes RGB from the module's configured color and brightness and calls
     * RenderSystem.clearColor with alpha fixed to 1. Does nothing if the module or module manager is unavailable or the
     * module is disabled.
     */
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

    /**
     * Prevents star rendering when the SkyComfort module is enabled and configured to disable stars.
     *
     * If the SkyComfort module is present and active and its configuration disables stars, this method cancels the original renderStars call.
     *
     * @param ci callback information used to cancel the renderStars invocation
     */
    @Inject(method = "renderStars()V", at = @At("HEAD"), cancellable = true)
    private void onRenderStars(CallbackInfo ci) {
        ModuleManager mm = TopZurdoMod.getModuleManager();
        if (mm == null) return;
        SkyComfortModule m = (SkyComfortModule) mm.getModule("sky_comfort");
        if (m != null && m.isEnabled() && m.shouldDisableStars()) {
            ci.cancel();
        }
    }

    /**
     * Prevents binding of sun and moon textures when SkyComfort is active and configured to hide them.
     *
     * If the SkyComfort module is enabled and configured to disable sun/moon, binding is skipped for
     * texture identifiers whose path contains "sun" or "moon". Otherwise this delegates to
     * TextureManager.bindTexture.
     *
     * @param textureManager the texture manager used to bind the texture
     * @param id the texture identifier to bind; binding may be skipped if its path contains "sun" or "moon"
     */
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