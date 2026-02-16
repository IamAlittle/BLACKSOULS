package com.iamalittle.black_souls_options.controllers;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class OptionManager {
    private List<Option> options;
    private int selectedOptionIndex;
    private int confirmedOptionIndex;
    private ResourceLocation hudTexture;
    private Font font;
    
    // 选项布局参数
    private int optionSpacing = 10;
    private int verticalPadding = 16;
    private int optionPadding = 10;
    private int minOptionBoxWidth = 55;
    
    // 动画参数
    private float animationProgress;
    private boolean isClosing;
    
    public OptionManager(ResourceLocation hudTexture, Font font) {
        this.options = new ArrayList<>();
        this.selectedOptionIndex = -1;
        this.confirmedOptionIndex = -1;
        this.hudTexture = hudTexture;
        this.font = font;
        this.animationProgress = 0.0f;
        this.isClosing = false;
    }
    
    public void addOption(Component textComponent, int defaultColor, int confirmedColor) {
        Option option = new Option(textComponent, defaultColor, confirmedColor);
        option.setIndex(options.size());
        options.add(option);
    }
    
    public void addOption(Component textComponent, int defaultColor, int confirmedColor, int shadowColor) {
        Option option = new Option(textComponent, defaultColor, confirmedColor);
        option.setIndex(options.size());
        option.setShadowColor(shadowColor);
        options.add(option);
    }
    
    public List<Option> getOptions() {
        return options;
    }
    
    public Option getOption(int index) {
        if (index >= 0 && index < options.size()) {
            return options.get(index);
        }
        return null;
    }
    
    public int getSelectedOptionIndex() {
        return selectedOptionIndex;
    }
    
    public void setSelectedOptionIndex(int index) {
        if (index >= -1 && index < options.size()) {
            this.selectedOptionIndex = index;
        }
    }
    
    public int getConfirmedOptionIndex() {
        return confirmedOptionIndex;
    }
    
    public void setConfirmedOptionIndex(int index) {
        this.confirmedOptionIndex = index;
        
        // 更新对应选项的确认状态
        for (int i = 0; i < options.size(); i++) {
            options.get(i).setConfirmed(i == index);
        }
    }
    
    public int getOptionCount() {
        return options.size();
    }
    
    public float getAnimationProgress() {
        return animationProgress;
    }
    
    public void setAnimationProgress(float progress) {
        this.animationProgress = Math.max(0.0f, Math.min(1.0f, progress));
    }
    
    public boolean isClosing() {
        return isClosing;
    }
    
    public void setClosing(boolean closing) {
        this.isClosing = closing;
    }
    
    public int calculateMaxOptionWidth() {
        int maxWidth = 0;
        for (Option option : options) {
            int width = font.width(option.getText());
            if (width > maxWidth) {
                maxWidth = width;
            }
        }
        return maxWidth;
    }
    
    public int calculateOptionBoxWidth() {
        int maxOptionWidth = calculateMaxOptionWidth();
        return Math.max(minOptionBoxWidth, maxOptionWidth + optionPadding * 2);
    }
    
    public int calculateOptionBoxHeight() {
        int optionCount = options.size();
        int optionHeight = font.lineHeight;
        return optionHeight * optionCount + optionSpacing * (optionCount - 1) + verticalPadding * 2;
    }
    
    public boolean isMouseOverOption(int mouseX, int mouseY, int optionBoxX, int optionBoxY, int optionBoxWidth, int optionHeight) {
        for (int i = 0; i < options.size(); i++) {
            Option option = options.get(i);
            int startY = optionBoxY + verticalPadding + i * (optionHeight + optionSpacing);
            int endY = startY + optionHeight;
            
            if (mouseX >= optionBoxX && mouseX <= optionBoxX + optionBoxWidth &&
                mouseY >= startY - 2 && mouseY <= endY + 2 &&
                mouseY >= optionBoxY) {
                return true;
            }
        }
        return false;
    }
    
    public int getHoveredOptionIndex(int mouseX, int mouseY, int optionBoxX, int optionBoxY, int optionBoxWidth, int optionHeight) {
        for (int i = 0; i < options.size(); i++) {
            Option option = options.get(i);
            int startY = optionBoxY + verticalPadding + i * (optionHeight + optionSpacing);
            int endY = startY + optionHeight;
            
            if (mouseX >= optionBoxX && mouseX <= optionBoxX + optionBoxWidth &&
                mouseY >= startY - 2 && mouseY <= endY + 2 &&
                mouseY >= optionBoxY) {
                return i;
            }
        }
        return -1;
    }
    
    public int getClickedOption(int mouseX, int mouseY, int optionBoxX, int optionBoxY, int optionBoxWidth, int optionBoxHeight, int optionHeight) {
        // 首先检查鼠标是否在选项框内
        if (mouseX < optionBoxX || mouseX > optionBoxX + optionBoxWidth ||
            mouseY < optionBoxY || mouseY > optionBoxY + optionBoxHeight) {
            return -1;
        }
        
        // 然后检查点击的是哪个选项
        return getHoveredOptionIndex(mouseX, mouseY, optionBoxX, optionBoxY, optionBoxWidth, optionHeight);
    }
    
    public void drawOptions(GuiGraphics guiGraphics, int optionBoxX, int optionBoxY, int optionBoxWidth, int animatedOptionBoxHeight, float blinkTransparency) {
        if (isClosing) return;
        
        int optionHeight = font.lineHeight;
        
        // 绘制选中选项的闪烁效果
        if (selectedOptionIndex >= 0 && selectedOptionIndex < options.size()) {
            Option option = options.get(selectedOptionIndex);
            int optionWidth = font.width(option.getText());
            int blinkBoxX = optionBoxX + (optionBoxWidth - optionWidth) / 2 - 5;
            int startY = optionBoxY + verticalPadding + selectedOptionIndex * (optionHeight + optionSpacing);
            int blinkBoxY = startY - 2;
            int blinkBoxWidth = optionWidth + 10;
            int blinkBoxHeight = optionHeight + 4;
            
            // 设置使用HUD纹理
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, hudTexture);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, blinkTransparency); // 使用闪烁透明度
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            
            // 闪烁框的UV坐标 (64,64) 到 (95,95)
            int blinkU = 64;
            int blinkV = 64;
            int blinkWidth = 32;
            int blinkHeight = 32;
            
            guiGraphics.blit(
                hudTexture,
                blinkBoxX,
                blinkBoxY,
                blinkBoxWidth,
                blinkBoxHeight,
                blinkU,
                blinkV,
                blinkWidth,
                blinkHeight,
                128, // 纹理总宽度
                128  // 纹理总高度
            );
            
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
        
        // 绘制所有选项
        for (int i = 0; i < options.size(); i++) {
            Option option = options.get(i);
            int startY = optionBoxY + verticalPadding + i * (optionHeight + optionSpacing);
            
            // 确保选项在动画范围内
            if (startY + optionHeight > optionBoxY + animatedOptionBoxHeight) {
                break;
            }
            
            // 设置选项的确认状态
            option.setConfirmed(i == confirmedOptionIndex);
            
            int optionWidth = font.width(option.getText());
            int centerX = optionBoxX + (optionBoxWidth - optionWidth) / 2;
            
            // 绘制主文字（无阴影）
            guiGraphics.drawString(
                    font,
                    option.getText(),
                    centerX, // 水平居中
                    startY,
                    option.getCurrentColor() // 当前颜色（普通或确认状态）
            );
        }
    }
    
    // 重置选项管理器
    public void reset() {
        options.clear();
        selectedOptionIndex = -1;
        confirmedOptionIndex = -1;
    }
}