package com.iamalittle.black_souls_options.controllers;

import com.iamalittle.black_souls_options.components.TypewriterText;
import com.iamalittle.black_souls_options.config.BlackSoulsClothConfig;
import com.iamalittle.black_souls_options.controllers.WelcomeScreenManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.Music;
import net.minecraft.client.renderer.GameRenderer;
import org.lwjgl.glfw.GLFW;
import com.iamalittle.black_souls_options.sound.ModSounds;
import net.minecraft.sounds.SoundEvent;

public class FirstTimeWelcomeScreen extends Screen {
    
    // HUD纹理
    private static final ResourceLocation HUD_TEXTURE = new ResourceLocation("black_souls_options", "textures/hud/window.png");
    
    private final Minecraft minecraft;
    private final TypewriterText typewriterText;
    private boolean textStarted;
    private long screenOpenTime;
    private float textFontSizeScale = 1.2f;
    
    // 配置常量
    private static final long DISPLAY_DELAY_MS = 200;
    private static final long TEXT_DELAY_MS = 50;
    private boolean hasDelayPassed = false;
    
    // 当前显示的段落 (0: 第一段, 1: 第二段前半部分, 2: 第二段后半部分)
    private int currentParagraph = 0;
    // 段落文本
    private final String[] paragraphs = {
        Component.translatable("black_souls_options.welcome_screen.paragraph1").getString(),
        Component.translatable("black_souls_options.welcome_screen.paragraph2").getString(),
        Component.translatable("black_souls_options.welcome_screen.paragraph3").getString()
    };
    
    // 选项相关变量
    private OptionManager optionManager;
    private float optionsMenuAnimationProgress = 0.0F;
    private boolean optionsMenuVisible = false;
    private int selectedOptionIndex = -1;
    private int confirmedOptionIndex = -1;
    private long lastBlinkTime = 0;
    private float blinkTransparency = 1.0F;
    private boolean isBlinkIncreasing = false;
    private static final float ANIMATION_SPEED = 0.25F;
    
    // 回复文本相关变量
    private boolean showingResponse = false;
    private final String[] responseTexts = {
            Component.translatable("black_souls_options.welcome_screen.response.interested").getString(),
            Component.translatable("black_souls_options.welcome_screen.response.not_interested").getString()
    };
    
    public FirstTimeWelcomeScreen() {
        super(Component.translatable("black_souls_options.welcome_screen.title"));
        this.minecraft = Minecraft.getInstance();
        this.typewriterText = new TypewriterText(this.minecraft.font, TEXT_DELAY_MS, textFontSizeScale);
        this.textStarted = false;
        this.screenOpenTime = 0;
        
        // 初始化选项管理器并添加选项
        this.optionManager = new OptionManager(HUD_TEXTURE, this.minecraft.font);
        optionManager.addOption(
            Component.translatable("black_souls_options.welcome_screen.option.interested"),
            0xFFFFFF, // 默认白色
            0xFFFFA0  // 已确认时金色
        );
        optionManager.addOption(
            Component.translatable("black_souls_options.welcome_screen.option.not_interested"),
            0xFFFFFF, // 默认白色
            0xFFFFA0  // 已确认时金色
        );
        
        // 设置第一段文本
        this.typewriterText.setText(paragraphs[currentParagraph]);
    }
    
    @Override
    public void init() {
        super.init();
        this.screenOpenTime = System.currentTimeMillis();
        this.lastBlinkTime = System.currentTimeMillis();
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 检查延迟
        if (!hasDelayPassed && System.currentTimeMillis() - screenOpenTime > DISPLAY_DELAY_MS) {
            hasDelayPassed = true;
            startTextDisplay();
        }
        
        // 更新文本显示
        if (textStarted) {
            typewriterText.update();
        }
        
        // 更新选项菜单动画
        updateOptionsMenuAnimation();
        
        // 更新闪烁动画
        updateBlinkAnimation();
        
        int screenWidth = this.width;
        int screenHeight = this.height;
        
        // 绘制半透明黑色矩形（从左到右，覆盖屏幕中间部分）
        int rectHeight = 80;
        int rectY = (screenHeight - rectHeight) / 2;
        
        // 使用半透明黑色填充矩形
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.fill(0, rectY, screenWidth, rectY + rectHeight, 0x80000000);
        
        // 绘制文字
        if (typewriterText != null && hasDelayPassed) {
            int textX = 50;
            int textY = rectY + 20;
            int textWidth = screenWidth - 100;
            int textHeight = rectHeight - 40;
            
            typewriterText.render(guiGraphics, textX, textY, textWidth, textHeight);
        }
        
        // 绘制选项菜单
        drawOptionsMenu(guiGraphics, mouseX, mouseY, screenWidth, screenHeight);
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 选项菜单导航
        if (optionsMenuVisible) {
            int optionCount = optionManager.getOptionCount();
            
            if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN) {
                if (selectedOptionIndex == -1 && optionCount > 0) {
                    // 如果还没有选择任何选项，选择第一个
                    selectedOptionIndex = 0;
                } else if (optionCount > 0) {
                    // 根据方向键移动选择
                    if (keyCode == GLFW.GLFW_KEY_UP) {
                        selectedOptionIndex = (selectedOptionIndex - 1 + optionCount) % optionCount;
                    } else if (keyCode == GLFW.GLFW_KEY_DOWN) {
                        selectedOptionIndex = (selectedOptionIndex + 1) % optionCount;
                    }
                }
                
                // 播放光标移动音效
                playSound(ModSounds.CURSOR1);
                
                // 重置确认状态
                confirmedOptionIndex = -1;
                
                // 重置闪烁时间
                lastBlinkTime = System.currentTimeMillis();
                
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                // 回车键直接确认当前选择的选项（不需要两次确认）
                if (selectedOptionIndex != -1) {
                    // 直接处理选择
                    handleOptionSelection(selectedOptionIndex);
                    playSound(ModSounds.SWORD1); // 确认音效
                    return true;
                }
            }
        } else if (showingResponse) {
            // 如果正在显示回复文本，回车键或空格键可以跳过
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) {
                if (!typewriterText.isComplete()) {
                    typewriterText.complete();
                } else {
                    this.onClose();
                }
                return true;
            }
        }
        
        // 仅处理回车键(Return键)或空格键用于文本显示
        if (keyCode == 257 || keyCode == 32) { // 257是Return键, 32是空格键
            handleUserInteraction();
            return true;
        }
        
        // 阻止ESC键关闭界面
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return true;
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 检查是否点击了选项菜单
        if (optionsMenuVisible && button == 0) { // 左键点击
            int screenWidth = this.width;
            int screenHeight = this.height;
            
            // 计算选项框尺寸和位置
            int optionBoxWidth = optionManager.calculateOptionBoxWidth();
            int optionBoxHeight = optionManager.calculateOptionBoxHeight();
            
            int optionBoxX = screenWidth - optionBoxWidth - 20;
            int optionBoxY = screenHeight - optionBoxHeight - 20;
            
            // 检测点击的选项
            int clickedOptionIndex = optionManager.getClickedOption((int)mouseX, (int)mouseY, optionBoxX, optionBoxY, optionBoxWidth, optionBoxHeight, this.font.lineHeight);
            if (clickedOptionIndex != -1) {
                if (selectedOptionIndex == clickedOptionIndex) {
                    // 已经选中，直接处理选择
                    handleOptionSelection(clickedOptionIndex);
                    playSound(ModSounds.SWORD1); // 确认音效
                } else {
                    // 首次点击，选中选项
                    selectedOptionIndex = clickedOptionIndex;
                    confirmedOptionIndex = -1; // 重置确认状态
                    playSound(ModSounds.CURSOR1); // 选中音效
                    lastBlinkTime = System.currentTimeMillis();
                }
                return true;
            }
        } else if (showingResponse && button == 0) {
            // 如果正在显示回复文本，点击可以跳过
            if (!typewriterText.isComplete()) {
                typewriterText.complete();
            } else {
                this.onClose();
            }
            return true;
        }
        
        // 点击任意位置处理文本交互
        handleUserInteraction();
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    private void startTextDisplay() {
        if (!textStarted && typewriterText != null) {
            typewriterText.start();
            textStarted = true;
        }
    }
    
    /**
     * 处理用户交互（点击或回车）
     */
    private void handleUserInteraction() {
        if (typewriterText != null) {
            if (!typewriterText.isComplete()) {
                // 如果当前段落文字还没显示完，就完成它
                typewriterText.complete();
            } else {
                if (showingResponse) {
                    // 如果正在显示回复文本且已经完成，关闭界面
                    this.onClose();
                } else {
                    // 如果当前段落文字已显示完，进入下一段
                    currentParagraph++;
                    if (currentParagraph < paragraphs.length) {
                        if (currentParagraph == 2) {
                            // 对于第二段后半部分，将其追加到当前文本的后面
                            // 先获取当前已显示的完整文本
                            String currentFullText = paragraphs[1] + "\n" + paragraphs[2];
                            // 保留段落1的文本，开始段落2后半部分的打字机效果
                            typewriterText.setText(currentFullText);
                            // 设置当前索引到段落1结束的位置
                            typewriterText.setCurrentIndex(paragraphs[1].length());
                            typewriterText.start();
                        } else {
                            // 设置下一段文字
                            typewriterText.setText(paragraphs[currentParagraph]);
                            typewriterText.start();
                        }
                    } else {
                        // 所有段落都已显示完，显示选项菜单
                        optionsMenuVisible = true;
                    }
                }
            }
        }
    }
    
    /**
     * 更新选项菜单动画
     */
    private void updateOptionsMenuAnimation() {
        if (optionsMenuVisible && optionsMenuAnimationProgress < 1.0F) {
            optionsMenuAnimationProgress += ANIMATION_SPEED;
            if (optionsMenuAnimationProgress >= 1.0F) {
                optionsMenuAnimationProgress = 1.0F;
                // 动画完成后不自动选择任何选项，需要用户手动选择
            }
        }
    }
    
    /**
     * 更新闪烁动画
     */
    private void updateBlinkAnimation() {
        long currentTime = System.currentTimeMillis();
        if (selectedOptionIndex >= 0 && optionsMenuVisible) {
            // 计算自上次更新以来的时间
            float deltaTime = (currentTime - lastBlinkTime) / 300.0F; // 基于300ms的动画周期
            lastBlinkTime = currentTime;
            
            // 更新透明度
            if (isBlinkIncreasing) {
                blinkTransparency += deltaTime;
                if (blinkTransparency >= 1.0F) {
                    blinkTransparency = 1.0F;
                    isBlinkIncreasing = false;
                }
            } else {
                blinkTransparency -= deltaTime;
                if (blinkTransparency <= 0.5F) { // 最小透明度为0.5，确保始终可见但有变化
                    blinkTransparency = 0.5F;
                    isBlinkIncreasing = true;
                }
            }
        } else {
            // 重置闪烁状态
            blinkTransparency = 1.0F;
            isBlinkIncreasing = false;
        }
    }
    
    /**
     * 绘制菜单边框
     */
    private void drawMenuBorder(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, HUD_TEXTURE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // 边框尺寸定义
        int borderSize = 13;
        int rightBorderSize = 16;

        // 四个角的边框坐标
        int topLeftSrcX = 64;
        int topLeftSrcY = 0;
        int topRightSrcX = 112;
        int topRightSrcY = 0;
        int bottomLeftSrcX = 64;
        int bottomLeftSrcY = 51;
        int bottomRightSrcX = 112;
        int bottomRightSrcY = 51;

        // 边框线部分
        int topBorderSrcX = 78;
        int topBorderSrcY = 0;
        int topBorderWidth = 34;
        int topBorderHeight = 12;

        int bottomBorderSrcX = 78;
        int bottomBorderSrcY = 50;
        int bottomBorderWidth = 34;
        int bottomBorderHeight = 14;

        int leftBorderSrcX = 64;
        int leftBorderSrcY = 12;
        int leftBorderWidth = 14;
        int leftBorderHeight = 37;

        int rightBorderSrcX = 112;
        int rightBorderSrcY = 12;
        int rightBorderWidth = 16;
        int rightBorderHeight = 37;

        // 绘制四个角的边框
        guiGraphics.blit(HUD_TEXTURE, x, y, topLeftSrcX, topLeftSrcY, borderSize, borderSize, 128, 128);
        guiGraphics.blit(HUD_TEXTURE, x + width - rightBorderSize, y, topRightSrcX, topRightSrcY, rightBorderSize, borderSize, 128, 128);
        guiGraphics.blit(HUD_TEXTURE, x, y + height - borderSize, bottomLeftSrcX, bottomLeftSrcY, borderSize, borderSize, 128, 128);
        guiGraphics.blit(HUD_TEXTURE, x + width - rightBorderSize, y + height - borderSize, bottomRightSrcX, bottomRightSrcY, rightBorderSize, borderSize, 128, 128);

        // 平铺上边框线
        int topBorderTiles = (int) Math.ceil((double) (width - borderSize - rightBorderSize) / topBorderWidth);
        for (int i = 0; i < topBorderTiles; i++) {
            int tileX = x + borderSize + i * topBorderWidth;
            int tileWidth = Math.min(topBorderWidth, width - borderSize - rightBorderSize - i * topBorderWidth);
            guiGraphics.blit(HUD_TEXTURE, tileX, y, topBorderSrcX, topBorderSrcY, tileWidth, borderSize, 128, 128);
        }

        // 平铺下边框线
        int bottomBorderTiles = (int) Math.ceil((double) (width - borderSize - rightBorderSize) / bottomBorderWidth);
        for (int i = 0; i < bottomBorderTiles; i++) {
            int tileX = x + borderSize + i * bottomBorderWidth;
            int tileWidth = Math.min(bottomBorderWidth, width - borderSize - rightBorderSize - i * bottomBorderWidth);
            guiGraphics.blit(HUD_TEXTURE, tileX, y + height - borderSize, bottomBorderSrcX, bottomBorderSrcY, tileWidth, borderSize, 128, 128);
        }

        // 平铺左边框线
        int leftBorderTiles = (int) Math.ceil((double) (height - 2 * borderSize) / leftBorderHeight);
        for (int i = 0; i < leftBorderTiles; i++) {
            int tileY = y + borderSize + i * leftBorderHeight;
            int tileHeight = Math.min(leftBorderHeight, height - 2 * borderSize - i * leftBorderHeight);
            guiGraphics.blit(HUD_TEXTURE, x, tileY, leftBorderSrcX, leftBorderSrcY, borderSize, tileHeight, 128, 128);
        }

        // 平铺右边框线
        int rightBorderTiles = (int) Math.ceil((double) (height - 2 * borderSize) / rightBorderHeight);
        for (int i = 0; i < rightBorderTiles; i++) {
            int tileY = y + borderSize + i * rightBorderHeight;
            int tileHeight = Math.min(rightBorderHeight, height - 2 * borderSize - i * rightBorderHeight);
            guiGraphics.blit(HUD_TEXTURE, x + width - rightBorderSize, tileY, rightBorderSrcX, rightBorderSrcY, rightBorderSize, tileHeight, 128, 128);
        }
    }

    /**
     * 绘制选项菜单
     */
    private void drawOptionsMenu(GuiGraphics guiGraphics, int mouseX, int mouseY, int screenWidth, int screenHeight) {
        if (!optionsMenuVisible || optionsMenuAnimationProgress <= 0) return;
        
        // 计算选项框尺寸和位置
        int optionBoxWidth = optionManager.calculateOptionBoxWidth();
        int optionBoxHeight = optionManager.calculateOptionBoxHeight();
        
        // 右下角位置
        int optionBoxX = screenWidth - optionBoxWidth - 20;
        int optionBoxY = screenHeight - optionBoxHeight - 20;
        
        // 应用动画效果 - 从底部向上展开
        int animatedOptionBoxHeight = (int) (optionBoxHeight * optionsMenuAnimationProgress);
        int animatedBoxY = optionBoxY + optionBoxHeight - animatedOptionBoxHeight;
        
        if (animatedOptionBoxHeight <= 0) return;
        
        // 设置使用HUD纹理
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, HUD_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        // 先绘制黑色背景（在贴图下方）
        guiGraphics.fill(optionBoxX + 5, animatedBoxY + 5, optionBoxX + optionBoxWidth - 5, animatedBoxY + animatedOptionBoxHeight - 5, 0xCC000000); // 半透明黑色覆盖，整体缩小5像素
        
        // 设置暗红色调
        RenderSystem.setShaderColor(0.5F, 0.1F, 0.1F, 1.0F);
        
        // 绘制平铺的背景贴图
        int textureSrcX = 0;
        int textureSrcY = 48;
        int textureWidth = 48;
        int textureHeight = 48;
        
        int tilesX = (int) Math.ceil((double) (optionBoxWidth - 10) / textureWidth);
        int tilesY = (int) Math.ceil((double) (animatedOptionBoxHeight - 10) / textureHeight);
        
        for (int x = 0; x < tilesX; x++) {
            for (int y = 0; y < tilesY; y++) {
                int tileX = optionBoxX + 5 + x * textureWidth;
                int tileY = animatedBoxY + 5 + y * textureHeight;
                int tileWidth = Math.min(textureWidth, optionBoxWidth - 10 - x * textureWidth);
                int tileHeight = Math.min(textureHeight, animatedOptionBoxHeight - 10 - y * textureHeight);
                
                guiGraphics.blit(HUD_TEXTURE, tileX, tileY, textureSrcX, textureSrcY, tileWidth, tileHeight, 96, 96);
            }
        }
        
        // 重置颜色设置
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        
        // 绘制选项框边框
        drawMenuBorder(guiGraphics, optionBoxX, animatedBoxY, optionBoxWidth, animatedOptionBoxHeight);
        
        // 设置OptionManager的动画状态和选中状态
        optionManager.setAnimationProgress(optionsMenuAnimationProgress);
        optionManager.setSelectedOptionIndex(selectedOptionIndex);
        optionManager.setConfirmedOptionIndex(confirmedOptionIndex);
        
        // 绘制选项
        optionManager.drawOptions(guiGraphics, optionBoxX, animatedBoxY, optionBoxWidth, animatedOptionBoxHeight, blinkTransparency);
        
        RenderSystem.disableBlend();
    }
    
    /**
     * 播放音效
     */
    private void playSound(SoundEvent sound) {
        if (this.minecraft.level != null && this.minecraft.player != null) {
            this.minecraft.player.playSound(sound, 1.0F, 1.0F);
        }
    }

    /**
     * 处理选项选择
     */
    private void handleOptionSelection(int optionIndex) {
        // 播放选择音效
        playSound(ModSounds.SWORD1);
        
        // 获取配置实例
        BlackSoulsClothConfig config = BlackSoulsClothConfig.getInstance();
        
        // 根据选择的选项执行不同操作
        switch (optionIndex) {
            case 0: // 感兴趣
                config.enableUncensoredMode = true;
                break;
            case 1: // 不感兴趣
                config.enableUncensoredMode = false;
                break;
        }
        
        // 保存配置
        BlackSoulsClothConfig.save();
        
        // 标记欢迎界面已完成
        WelcomeScreenManager.getInstance().markWelcomeScreenCompleted();
        
        // 显示回复文本
        showingResponse = true;
        optionsMenuVisible = false;
        typewriterText.setText(responseTexts[optionIndex]);
        typewriterText.start();
    }
    
    @Override
    public void onClose() {
        super.onClose();
    }

    public Music getBackgroundMusic() {
        return null;
    }

    @Override
    public boolean isPauseScreen() {
        // 不暂停游戏
        return false;
    }
}