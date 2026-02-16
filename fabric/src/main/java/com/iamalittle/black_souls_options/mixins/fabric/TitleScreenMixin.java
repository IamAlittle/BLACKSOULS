package com.iamalittle.black_souls_options.mixins.fabric;

import com.iamalittle.black_souls_options.client.TitleScreenCharacterRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 标题界面Mixin，用于在标题界面渲染像素小人
 */
@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    
    /**
     * 在标题界面渲染完成后，渲染像素小人
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void renderPixelCharacter(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        TitleScreenCharacterRenderer.render(guiGraphics, partialTick);
    }
    
    /**
     * 处理鼠标点击事件
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void onMouseClicked(double d, double e, int i, CallbackInfoReturnable<Boolean> cir) {
        // 将点击事件传递给角色渲染器的处理方法
        TitleScreenCharacterRenderer.handleMouseClick(Minecraft.getInstance(), d, e);
    }
    
    /**
     * 在tick方法中阻止原版标题音乐播放
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(CallbackInfo ci) {
        // 清空音乐管理器的当前音乐，阻止原版标题音乐播放
        Minecraft minecraft = Minecraft.getInstance();
        MusicManager musicManager = minecraft.getMusicManager();
        if (musicManager != null) {
            musicManager.stopPlaying();
        }
    }
}