package com.iamalittle.black_souls_options.mixins.forge;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for PauseScreen to add a custom button in the top-left corner
 */
@Mixin(PauseScreen.class)
public class MixinPauseScreen extends Screen {
    
    protected MixinPauseScreen(Component title) {
        super(title);
    }
    
    /**
     * 在PauseScreen初始化时添加自定义按钮
     */
    @Inject(method = "init", at = @At("HEAD"))
    private void onInit(CallbackInfo ci) {
        // 在左上角添加一个自定义按钮
        int buttonWidth = 100;
        int buttonHeight = 20;
        int buttonX = 10; // 距离左侧10像素
        int buttonY = 10; // 距离顶部10像素
        
        // 创建契约列表按钮
        Button contractButton = Button.builder(
            Component.translatable("black_souls_options.pause_screen.contracts_button"),
            button -> {
                // 打开契约列表界面
                minecraft.setScreen(new com.iamalittle.black_souls_options.controllers.ContractsScreen());
            }
        ).bounds(buttonX, buttonY, buttonWidth, buttonHeight).build();
        
        // 将按钮添加到屏幕
        this.addRenderableWidget(contractButton);
    }
    
    /**
     * 在渲染时确保按钮正确显示
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // 可以在这里添加额外的渲染逻辑
    }
}