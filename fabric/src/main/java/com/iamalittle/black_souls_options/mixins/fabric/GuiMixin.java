package com.iamalittle.black_souls_options.mixins.fabric;

import com.iamalittle.black_souls_options.hud.DeathTotemHud;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * GUI渲染Mixin，用于在GUI渲染之前渲染死亡图腾HUD
 * 与Forge版本的RenderGuiEvent.Pre保持一致
 */
@Mixin(Gui.class)
public class GuiMixin {
    
    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderGui(GuiGraphics guiGraphics, float partialTick, CallbackInfo ci) {
        // 在GUI渲染之前渲染死亡图腾HUD，与Forge版本保持一致
        DeathTotemHud.render(guiGraphics, partialTick);
    }
}