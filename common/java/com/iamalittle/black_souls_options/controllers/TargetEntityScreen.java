package com.iamalittle.black_souls_options.controllers;

import com.iamalittle.black_souls_options.network.ContractNetworkHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import com.iamalittle.black_souls_options.render.EntityDisplay;
import com.iamalittle.black_souls_options.components.TypewriterText;
import com.iamalittle.black_souls_options.utils.TextReader;
import com.iamalittle.black_souls_options.contracts.GlobalContractManager;
import com.iamalittle.black_souls_options.contracts.ContractManager;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.Minecraft;

public class TargetEntityScreen extends Screen {
    private static final ResourceLocation HUD_TEXTURE = new ResourceLocation("black_souls_options", "textures/hud/window.png");
    private final Entity targetEntity;
    private final Minecraft minecraft;
    private float cameraRotation; // 相机环绕旋转角度
    private final EntityDisplay entityDisplay; // EntityDisplay实例
    private double lastMouseX; // 上次鼠标X位置，用于拖动旋转
    private double lastMouseY; // 上次鼠标Y位置，用于拖动旋转
    private boolean isDragging; // 是否正在拖动旋转

    // 文本显示相关字段
    private final TypewriterText typewriterText; // 打字机效果文本
    private boolean textStarted; // 文本是否已开始显示
    private long screenOpenTime; // 界面打开时间
    private float textFontSizeScale = 1.2f; // 文本字体大小缩放比例，可调整

    // 动画状态和进度变量
    private enum AnimationState {
        OPENING,
        OPEN,
        CLOSING,
        CLOSED
    }

    private AnimationState nameBoxAnimationState = AnimationState.CLOSED;
    private AnimationState hudAnimationState = AnimationState.CLOSED;
    private AnimationState optionsMenuAnimationState = AnimationState.CLOSED;

    private float nameBoxAnimationProgress = 0.0F;
    private float hudAnimationProgress = 0.0F;
    private float optionsMenuAnimationProgress = 0.0F;

    private final float ANIMATION_SPEED = 0.25F; // 动画速度系数
    private boolean isClosing = false; // 标记界面是否正在关闭
    private long lastBlinkTime = 0; // 闪烁动画的最后时间
    private boolean isBlinkVisible = false; // 当前闪烁是否可见
    private int selectedOptionIndex = -1; // 选中的选项索引 (-1表示未选中)
    private float blinkTransparency = 1.0F; // 闪烁透明度 (0.0-1.0)
    private boolean isBlinkIncreasing = false; // 透明度是否正在增加
    private int confirmedOptionIndex = -1; // 确认的选项索引 (-1表示未确认)
    private boolean hasDelayPassed = false; // 是否已经过了延迟时间
    private static final long DISPLAY_DELAY_MS = 200; // 显示延迟时间（毫秒）

    public TargetEntityScreen(Entity targetEntity) {
        super(Component.literal("Target Entity Info"));
        this.targetEntity = targetEntity;
        this.minecraft = Minecraft.getInstance();
        this.entityDisplay = new EntityDisplay();
        this.entityDisplay.setEntity(targetEntity);

        // 初始化打字机效果文本（每个字符延迟50毫秒）
        this.typewriterText = new TypewriterText(this.minecraft.font, 50, textFontSizeScale);
        this.textStarted = false;
        this.screenOpenTime = 0;
    }

    @Override
    protected void init() {
        super.init();
        this.screenOpenTime = System.currentTimeMillis();
        initializeTextDisplay();

        // 启动动画 - 先启动HUD和选项菜单动画
        nameBoxAnimationState = AnimationState.CLOSED;
        hudAnimationState = AnimationState.OPENING;
        optionsMenuAnimationState = AnimationState.OPENING;
    }

    /**
     * 更新所有模块的动画进度
     */
    private void updateAnimations() {
        // 更新闪烁动画 - 使用透明度渐变代替显示/隐藏
        long currentTime = System.currentTimeMillis();
        if (selectedOptionIndex >= 0) {
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

        // 更新HUD动画
        if (hudAnimationState == AnimationState.OPENING && hasDelayPassed) {
            hudAnimationProgress += ANIMATION_SPEED;
            if (hudAnimationProgress >= 1.0F) {
                hudAnimationProgress = 1.0F;
                hudAnimationState = AnimationState.OPEN;
            }
        } else if (hudAnimationState == AnimationState.CLOSING) {
            hudAnimationProgress -= ANIMATION_SPEED;
            if (hudAnimationProgress <= 0.0F) {
                hudAnimationProgress = 0.0F;
                hudAnimationState = AnimationState.CLOSED;
            }
        }

        // 更新选项菜单动画
        if (optionsMenuAnimationState == AnimationState.OPENING && hasDelayPassed) {
            optionsMenuAnimationProgress += ANIMATION_SPEED;
            if (optionsMenuAnimationProgress >= 1.0F) {
                optionsMenuAnimationProgress = 1.0F;
                optionsMenuAnimationState = AnimationState.OPEN;
            }
        } else if (optionsMenuAnimationState == AnimationState.CLOSING) {
            optionsMenuAnimationProgress -= ANIMATION_SPEED;
            if (optionsMenuAnimationProgress <= 0.0F) {
                optionsMenuAnimationProgress = 0.0F;
                optionsMenuAnimationState = AnimationState.CLOSED;
            }
        }

        // 当HUD和选项菜单动画完成后，显示名字框（不需要动画）
        if ((hudAnimationState == AnimationState.OPEN || optionsMenuAnimationState == AnimationState.OPEN) &&
            nameBoxAnimationState == AnimationState.CLOSED &&
            !(hudAnimationState == AnimationState.CLOSING || optionsMenuAnimationState == AnimationState.CLOSING)) {
            nameBoxAnimationState = AnimationState.OPEN;
            nameBoxAnimationProgress = 1.0F; // 直接显示
        }

        // 更新名字框动画（如果需要关闭）
        if (nameBoxAnimationState == AnimationState.CLOSING) {
            nameBoxAnimationProgress -= ANIMATION_SPEED * 2; // 关闭动画稍快
            if (nameBoxAnimationProgress <= 0.0F) {
                nameBoxAnimationProgress = 0.0F;
                nameBoxAnimationState = AnimationState.CLOSED;
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 检查是否已经过了延迟时间
        if (!hasDelayPassed && System.currentTimeMillis() - screenOpenTime > DISPLAY_DELAY_MS) {
            hasDelayPassed = true;
        }

        // 更新动画进度
        updateAnimations();

        // 检查是否所有动画都已关闭，如果是，则真正关闭屏幕
        if (hudAnimationState == AnimationState.CLOSED &&
            optionsMenuAnimationState == AnimationState.CLOSED &&
            nameBoxAnimationState == AnimationState.CLOSED) {
            super.onClose();
            return;
        }

        // 初始化和更新逻辑
        initializeTextDisplay();
        updateTextDisplay();
        updateCameraRotation(partialTick);
        updateEntityDisplay();

        int screenWidth = this.width;
        int screenHeight = this.height;

        // 计算HUD位置和大小 - 贴紧游戏窗口下边
        int hudWidth = screenWidth; // 宽度为屏幕宽度，不留边距
        int hudHeight = 80; // 高度
        int hudX = 0; // 左边距0像素，紧贴左边缘
        int hudY = screenHeight - hudHeight; // 底部上方0像素，紧贴底部

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // 只有在动画状态不是关闭的情况下才绘制
        if (hudAnimationProgress > 0) {
            // 绘制各个UI部件
            drawHudBackground(guiGraphics, hudX, hudY, hudWidth, hudHeight);
            drawEntityRenderArea(guiGraphics, hudX, hudY, hudHeight);
            drawHudBorder(guiGraphics, hudX, hudY, hudWidth, hudHeight);
            drawTypewriterText(guiGraphics, hudX, hudY, hudWidth, hudHeight);
            drawCloseHint(guiGraphics, hudX, hudY, hudWidth, hudHeight);
        }

        // 绘制名字框（独立动画）
        if (nameBoxAnimationProgress > 0) {
            drawNameBox(guiGraphics, hudX, hudY, hudWidth);
            drawEntityName(guiGraphics, hudX, hudY, hudWidth);
        }

        // 绘制右侧选项菜单（独立动画）
        if (optionsMenuAnimationProgress > 0) {
            drawOptionsMenu(guiGraphics, mouseX, mouseY, hudX, hudY, hudWidth, hudHeight);
        }

        RenderSystem.disableBlend();

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    /**
     * 初始化文本显示
     */
    private void initializeTextDisplay() {
        if (screenOpenTime == 0) {
            screenOpenTime = System.currentTimeMillis();
        }
    }

    /**
     * 更新文本显示状态
     */
    private void updateTextDisplay() {
        // 延迟500毫秒后开始显示文本
        if (!textStarted && System.currentTimeMillis() - screenOpenTime > 500) {
            String randomText = TextReader.getRandomText("entity_descriptions");
            if (!randomText.isEmpty()) {
                typewriterText.setText(randomText);
                typewriterText.start();
                textStarted = true;
            }
        }

        // 更新打字机效果文本
        if (textStarted) {
            typewriterText.update();
        }
    }

    /**
     * 更新相机旋转角度
     */
    private void updateCameraRotation(float partialTick) {
        // 更新相机旋转角度（每次渲染增加1度，实现缓慢环绕）
        this.cameraRotation = (this.cameraRotation + 1.0F * partialTick) % 360.0F;
    }

    /**
     * 更新实体显示状态
     */
    private void updateEntityDisplay() {
        // 只有在拖动时才设置鼠标位置用于旋转
        if (!this.isDragging) {
            // 非拖动状态下，使用固定位置让实体保持默认角度
            this.entityDisplay.setMousePosition(0, 0);
        }
    }

    /**
     * 绘制HUD背景
     */
    private void drawHudBackground(GuiGraphics guiGraphics, int hudX, int hudY, int hudWidth, int hudHeight) {
        // 应用动画效果 - 从矩形中心往上下两边展开
        float progress = hudAnimationProgress;
        int animatedHudHeight = (int) (hudHeight * progress);
        int centerY = hudY + hudHeight / 2;
        int animatedHudY = centerY - animatedHudHeight / 2;

        if (animatedHudHeight <= 0) return;

        // 先绘制黑色背景（在贴图下方）
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        guiGraphics.fill(hudX + 5, animatedHudY + 5, hudX + hudWidth - 5, animatedHudY + animatedHudHeight - 5, 0xCC000000); // 半透明黑色覆盖，整体缩小5像素

        // 绘制图片背景 - 使用方格平铺并添加颜色叠加
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, HUD_TEXTURE);

        // 设置颜色叠加效果 - 使用更暗的红色
        RenderSystem.setShaderColor(0.5F, 0.1F, 0.1F, 1.0F);

        // 绘制平铺的背景贴图
        int textureSrcX = 0;
        int textureSrcY = 48;
        int textureWidth = 48;
        int textureHeight = 48;

        int tilesX = (int) Math.ceil((double) (hudWidth - 10) / textureWidth);
        int tilesY = (int) Math.ceil((double) (animatedHudHeight - 10) / textureHeight);

        for (int x = 0; x < tilesX; x++) {
            for (int y = 0; y < tilesY; y++) {
                int tileX = hudX + 5 + x * textureWidth;
                int tileY = animatedHudY + 5 + y * textureHeight;
                int tileWidth = Math.min(textureWidth, hudWidth - 10 - x * textureWidth);
                int tileHeight = Math.min(textureHeight, animatedHudHeight - 10 - y * textureHeight);

                guiGraphics.blit(HUD_TEXTURE, tileX, tileY, textureSrcX, textureSrcY, tileWidth, tileHeight, 96, 96);
            }
        }

        // 重置颜色设置
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * 绘制实体渲染区域
     */
    private void drawEntityRenderArea(GuiGraphics guiGraphics, int hudX, int hudY, int hudHeight) {
        // 应用动画效果 - 随着HUD一起动画
        float progress = hudAnimationProgress;
        if (progress <= 0) return;

        int entityRenderWidth = 64; // 实体渲染区域宽度
        int entityRenderHeight = 64; // 实体渲染区域高度

        // 计算动画位置 - 从矩形中心往上下两边展开
        int entityRenderX = hudX + 15; // 在HUD内部，距离左侧15像素
        int centerY = hudY + hudHeight / 2; // HUD中心Y坐标
        int animatedRenderHeight = (int) (entityRenderHeight * progress);
        int entityRenderY = centerY - animatedRenderHeight / 2;

        if (animatedRenderHeight <= 0) return;

        // 绘制实体渲染区域的边框
        int renderBorderSize = 4;
        guiGraphics.fill(entityRenderX - renderBorderSize, entityRenderY - renderBorderSize,
                         entityRenderX + entityRenderWidth + renderBorderSize,
                         entityRenderY + animatedRenderHeight + renderBorderSize, 0x80000000);

        // 绘制实体渲染区域的背景
        guiGraphics.fill(entityRenderX, entityRenderY,
                         entityRenderX + entityRenderWidth,
                         entityRenderY + animatedRenderHeight, 0x40000000);

        // 渲染3D实体 - 使用完整宽度和动画高度
        renderEntityInInventory(guiGraphics, entityRenderX, entityRenderY, entityRenderWidth, animatedRenderHeight);
    }

    /**
     * 绘制名字框
     */
    private void drawNameBox(GuiGraphics guiGraphics, int hudX, int hudY, int hudWidth) {
        // 不再使用动画效果，直接显示完整名字框，但仅在名字框状态为OPEN时显示
        if (nameBoxAnimationState != AnimationState.OPEN) return;

        Component entityName = targetEntity.getName();
        int nameWidth = this.font.width(entityName);

        // 动态计算名字框尺寸和位置
        int namePadding = 20; // 名字两侧的边距
        int nameBoxWidth = Math.max(64, nameWidth + namePadding * 2); // 最小宽度64像素
        int nameBoxHeight = 28;
        int nameBoxX = hudX + (hudWidth - nameBoxWidth) / 4;
        int nameBoxY = hudY - 10;

        // 名字框边框尺寸
        int nameBorderSize = 14;
        int nameRightBorderSize = 16;

        // 名字框四个角的纹理坐标
        int nameTopLeftSrcX = 64;
        int nameTopLeftSrcY = 0;
        int nameTopRightSrcX = 112;
        int nameTopRightSrcY = 0;
        int nameBottomLeftSrcX = 64;
        int nameBottomLeftSrcY = 50;
        int nameBottomRightSrcX = 112;
        int nameBottomRightSrcY = 50;

        // 绘制名字框四个角
        guiGraphics.blit(HUD_TEXTURE, nameBoxX, nameBoxY, nameTopLeftSrcX, nameTopLeftSrcY, nameBorderSize, nameBorderSize, 128, 128);
        guiGraphics.blit(HUD_TEXTURE, nameBoxX + nameBoxWidth - nameRightBorderSize, nameBoxY, nameTopRightSrcX, nameTopRightSrcY, nameRightBorderSize, nameBorderSize, 128, 128);
        guiGraphics.blit(HUD_TEXTURE, nameBoxX, nameBoxY + nameBoxHeight - nameBorderSize, nameBottomLeftSrcX, nameBottomLeftSrcY, nameBorderSize, nameBorderSize, 128, 128);
        guiGraphics.blit(HUD_TEXTURE, nameBoxX + nameBoxWidth - nameRightBorderSize, nameBoxY + nameBoxHeight - nameBorderSize, nameBottomRightSrcX, nameBottomRightSrcY, nameRightBorderSize, nameBorderSize, 128, 128);

        // 绘制名字框边框线（根据宽度平铺）
        int nameTopBorderSrcX = 78;
        int nameTopBorderSrcY = 0;
        int nameTopBorderWidth = 34;
        int nameTopBorderTiles = (int) Math.ceil((double) (nameBoxWidth - nameBorderSize - nameRightBorderSize) / nameTopBorderWidth);
        for (int i = 0; i < nameTopBorderTiles; i++) {
            int tileX = nameBoxX + nameBorderSize + i * nameTopBorderWidth;
            int tileWidth = Math.min(nameTopBorderWidth, nameBoxWidth - nameBorderSize - nameRightBorderSize - i * nameTopBorderWidth);
            guiGraphics.blit(HUD_TEXTURE, tileX, nameBoxY, nameTopBorderSrcX, nameTopBorderSrcY, tileWidth, nameBorderSize, 128, 128);
        }

        int nameBottomBorderSrcX = 78;
        int nameBottomBorderSrcY = 50;
        int nameBottomBorderWidth = 34;
        int nameBottomBorderTiles = (int) Math.ceil((double) (nameBoxWidth - nameBorderSize - nameRightBorderSize) / nameBottomBorderWidth);
        for (int i = 0; i < nameBottomBorderTiles; i++) {
            int tileX = nameBoxX + nameBorderSize + i * nameBottomBorderWidth;
            int tileWidth = Math.min(nameBottomBorderWidth, nameBoxWidth - nameBorderSize - nameRightBorderSize - i * nameBottomBorderWidth);
            guiGraphics.blit(HUD_TEXTURE, tileX, nameBoxY + nameBoxHeight - nameBorderSize, nameBottomBorderSrcX, nameBottomBorderSrcY, tileWidth, nameBorderSize, 128, 128);
        }

        // 绘制名字框背景（应用颜色叠加）
        RenderSystem.setShaderColor(0.5F, 0.1F, 0.1F, 1.0F);
        int nameBgSrcX = 0;
        int nameBgSrcY = 48;
        int nameBgWidth = nameBoxWidth - nameBorderSize - nameRightBorderSize;
        int nameBgHeight = nameBoxHeight - 2 * nameBorderSize;
        guiGraphics.blit(HUD_TEXTURE, nameBoxX + nameBorderSize, nameBoxY + nameBorderSize, nameBgSrcX, nameBgSrcY, nameBgWidth, nameBgHeight, 128, 128);

        // 重置颜色设置
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * 绘制HUD边框
     */
    private void drawHudBorder(GuiGraphics guiGraphics, int hudX, int hudY, int hudWidth, int hudHeight) {
        // 应用动画效果 - 从矩形中心往上下两边展开
        float progress = hudAnimationProgress;
        int animatedHudHeight = (int) (hudHeight * progress);
        int centerY = hudY + hudHeight / 2;
        int animatedHudY = centerY - animatedHudHeight / 2;

        if (animatedHudHeight <= 0) return;

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
        guiGraphics.blit(HUD_TEXTURE, hudX, animatedHudY, topLeftSrcX, topLeftSrcY, borderSize, borderSize, 128, 128);
        guiGraphics.blit(HUD_TEXTURE, hudX + hudWidth - rightBorderSize, animatedHudY, topRightSrcX, topRightSrcY, rightBorderSize, borderSize, 128, 128);
        guiGraphics.blit(HUD_TEXTURE, hudX, animatedHudY + animatedHudHeight - borderSize, bottomLeftSrcX, bottomLeftSrcY, borderSize, borderSize, 128, 128);
        guiGraphics.blit(HUD_TEXTURE, hudX + hudWidth - rightBorderSize, animatedHudY + animatedHudHeight - borderSize, bottomRightSrcX, bottomRightSrcY, rightBorderSize, borderSize, 128, 128);

        // 平铺上边框线
        int topBorderTiles = (int) Math.ceil((double) (hudWidth - borderSize - rightBorderSize) / topBorderWidth);
        for (int i = 0; i < topBorderTiles; i++) {
            int tileX = hudX + borderSize + i * topBorderWidth;
            int tileWidth = Math.min(topBorderWidth, hudWidth - borderSize - rightBorderSize - i * topBorderWidth);
            guiGraphics.blit(HUD_TEXTURE, tileX, animatedHudY, topBorderSrcX, topBorderSrcY, tileWidth, borderSize, 128, 128);
        }

        // 平铺下边框线
        int bottomBorderTiles = (int) Math.ceil((double) (hudWidth - borderSize - rightBorderSize) / bottomBorderWidth);
        for (int i = 0; i < bottomBorderTiles; i++) {
            int tileX = hudX + borderSize + i * bottomBorderWidth;
            int tileWidth = Math.min(bottomBorderWidth, hudWidth - borderSize - rightBorderSize - i * bottomBorderWidth);
            guiGraphics.blit(HUD_TEXTURE, tileX, animatedHudY + animatedHudHeight - borderSize, bottomBorderSrcX, bottomBorderSrcY, tileWidth, borderSize, 128, 128);
        }

        // 平铺左边框线
        int leftBorderTiles = (int) Math.ceil((double) (animatedHudHeight - 2 * borderSize) / leftBorderHeight);
        for (int i = 0; i < leftBorderTiles; i++) {
            int tileY = animatedHudY + borderSize + i * leftBorderHeight;
            int tileHeight = Math.min(leftBorderHeight, animatedHudHeight - borderSize * 2 - i * leftBorderHeight);
            guiGraphics.blit(HUD_TEXTURE, hudX, tileY, leftBorderSrcX, leftBorderSrcY, leftBorderWidth, tileHeight, 128, 128);
        }

        // 平铺右边框线
        int rightBorderTiles = (int) Math.ceil((double) (animatedHudHeight - 2 * borderSize) / rightBorderHeight);
        for (int i = 0; i < rightBorderTiles; i++) {
            int tileY = animatedHudY + borderSize + i * rightBorderHeight;
            int tileHeight = Math.min(rightBorderHeight, animatedHudHeight - borderSize * 2 - i * rightBorderHeight);
            guiGraphics.blit(HUD_TEXTURE, hudX + hudWidth - rightBorderSize, tileY, rightBorderSrcX, rightBorderSrcY, rightBorderSize, tileHeight, 128, 128);
        }
    }

    /**
     * 绘制实体名称
     */
    private void drawEntityName(GuiGraphics guiGraphics, int hudX, int hudY, int hudWidth) {
        // 在关闭过程中不渲染实体名称
        if (isClosing) return;

        Component entityName = targetEntity.getName();
        int nameWidth = this.font.width(entityName);

        // 动态计算名字框尺寸和位置
        int namePadding = 20;
        int nameBoxWidth = Math.max(64, nameWidth + namePadding * 2);
        int nameBoxHeight = 28;
        int nameBoxX = hudX + (hudWidth - nameBoxWidth) / 4;
        int nameBoxY = hudY - 10;

        // 计算动态缩放比例
        float scale = calculateNameScale(entityName.getString().length());

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scale, scale, scale);

        // 修正坐标计算
        int scaledNameX = (int)((nameBoxX + (nameBoxWidth - nameWidth * scale) / 2) / scale);
        int scaledNameY = (int)((nameBoxY + (nameBoxHeight - this.font.lineHeight * scale) / 2) / scale);

        guiGraphics.drawString(
                this.font,
                entityName,
                scaledNameX,
                scaledNameY,
                0xFFFFFF
        );

        guiGraphics.pose().popPose();
    }

    /**
     * 根据名字长度计算缩放比例
     */
    private float calculateNameScale(int nameLength) {
        // 基础缩放比例
        float baseScale = 1.5f;

        // 根据字符数量动态调整缩放比例
        if (nameLength <= 4) {
            return baseScale;
        } else if (nameLength <= 8) {
            return baseScale * 0.9f;
        } else if (nameLength <= 12) {
            return baseScale * 0.8f;
        } else if (nameLength <= 16) {
            return baseScale * 0.8f;
        } else {
            return baseScale * 0.8f;
        }
        // 确保缩放比例不会太小（最小为0.5倍）
        // 这个最小值在上面的条件中已经被满足，因为 baseScale * 0.8f >= 0.6f (当 baseScale = 1.5f 时)
    }

    /**
     * 绘制打字机效果文本
     */
    private void drawTypewriterText(GuiGraphics guiGraphics, int hudX, int hudY, int hudWidth, int hudHeight) {
        // 在关闭过程中不渲染文本
        if (!isClosing && textStarted && typewriterText != null) {
            // 计算文本显示区域（HUD内部，实体渲染区域右侧）
            int textX = hudX + 15 + 64 + 15; // 实体渲染区域右侧15像素
            int textY = hudY + 25; // HUD顶部15像素
            int textWidth = hudWidth - (15 + 64 + 15 + 20); // 剩余宽度
            int textHeight = hudHeight - 30; // 上下各15像素边距

            typewriterText.render(guiGraphics, textX, textY, textWidth, textHeight);
        }
    }

    /**
     * 绘制关闭提示
     */
    private void drawCloseHint(GuiGraphics guiGraphics, int hudX, int hudY, int hudWidth, int hudHeight) {
        // 在关闭过程中不渲染关闭提示
        if (!isClosing) {
            String closeHint = "Press ESC to close";
            int hintWidth = this.font.width(closeHint);
            guiGraphics.drawString(
                    this.font,
                    closeHint,
                    hudX + (hudWidth - hintWidth) / 2,
                    hudY + hudHeight - 15,
                    0xAAAAAA
            );
        }
    }

    /**
     * 在界面中渲染3D实体
     * 使用EntityDisplay类实现与ToroHealth模组相同的效果
     */
    private void renderEntityInInventory(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        if (targetEntity == null) return;

        // 使用EntityDisplay来渲染实体
        this.entityDisplay.draw(guiGraphics, x, y);
    }

    @Override
    public boolean isPauseScreen() {
        return false; // 不暂停游戏
    }

    @Override
    public void onClose() {
        // 标记界面正在关闭
        isClosing = true;

        // 停止文本打字机效果
        if (textStarted && typewriterText != null) {
            textStarted = false;
        }

        // 启动关闭动画
        if (nameBoxAnimationState == AnimationState.OPEN) {
            nameBoxAnimationState = AnimationState.CLOSING; // 名字框先关闭
        }

        // 启动HUD和选项菜单的关闭动画
        if (hudAnimationState == AnimationState.OPEN) {
            hudAnimationState = AnimationState.CLOSING;
        }
        if (optionsMenuAnimationState == AnimationState.OPEN) {
            optionsMenuAnimationState = AnimationState.CLOSING;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC键
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 左键点击时检查是否在实体渲染区域内
        if (button == 0) { // 左键
            // 首先检查是否点击了选项菜单中的选项
            if (optionsMenuAnimationProgress > 0 && !isClosing) {
                int hudWidth = this.width;
                int hudHeight = 80;
                int hudX = 0;
                int hudY = this.height - hudHeight;

                // 重新计算选项框尺寸和位置
                String option1 = "契约";
                String option2 = "选项 2";
                int option1Width = this.font.width(option1);
                int option2Width = this.font.width(option2);
                int maxOptionWidth = Math.max(option1Width, option2Width);

                int optionPadding = 10; // 左右边距
                int optionBoxWidth = Math.max(50, maxOptionWidth + optionPadding * 2);
                int optionSpacing = 10; // 选项间距
                int optionHeight = this.font.lineHeight; // 选项文本高度
                int verticalPadding = 16; // 上下边距
                int optionCount = 2; // 当前选项数量
                int optionBoxHeight = optionHeight * optionCount + optionSpacing * (optionCount - 1) + verticalPadding * 2;

                int optionBoxX = hudX + hudWidth - optionBoxWidth - 1;
                int centerY = hudY - optionBoxHeight / 2 - 3; // 向上移动3像素，增加与下方文本框的间距
                int optionBoxY = centerY - optionBoxHeight / 2;

                // 计算选项位置
                int totalOptionsHeight = optionHeight * optionCount + optionSpacing * (optionCount - 1);
                int startY = optionBoxY + (optionBoxHeight - totalOptionsHeight) / 2;
                int option1Y = startY;
                int option2Y = startY + optionHeight + optionSpacing;

                int optionClickHeight = optionHeight + 4; // 选项点击区域高度

                // 检查点击的是哪个选项
                if (mouseX >= optionBoxX && mouseX <= optionBoxX + optionBoxWidth &&
                    mouseY >= option1Y - 2 && mouseY <= option1Y + optionClickHeight) {
                    // 第一次点击选中，第二次点击相同选项则确认
                    if (selectedOptionIndex == 0) {
                        // 第二次点击相同选项，确认选择
                        confirmedOptionIndex = 0;
                        // 创建契约 - 发送网络请求到服务器
                            if (targetEntity != null) {
                                // 从实体中提取所需信息创建契约
                                UUID entityId = targetEntity.getUUID();
                                ResourceLocation entityTypeKey = BuiltInRegistries.ENTITY_TYPE.getKey(targetEntity.getType());
                                String entityType = entityTypeKey.toString(); // 使用资源位置ID，如"minecraft:rabbit"
                                
                                // 获取实体的友好名称：优先使用自定义名称，其次使用显示名称，最后使用本地化类型名称
                                String entityName;
                                if (targetEntity.hasCustomName()) {
                                    // 获取原始的自定义名称Component
                                    Component customName = targetEntity.getCustomName();
                                    // 使用JSON序列化来完整保留所有样式信息
                                    entityName = Component.Serializer.toJson(customName);
                                } else {
                                    // 使用实体的显示名称（会返回本地化的名称）
                                    entityName = targetEntity.getDisplayName().getString();
                                    
                                    // 如果显示名称仍然是技术名称，尝试使用类型名称
                                    if (entityName.contains(":") || entityName.startsWith("entity.")) {
                                        // 获取本地化的实体类型名称
                                        entityName = targetEntity.getType().getDescription().getString();
                                    }
                                }
                                
                                Vec3 position = targetEntity.position();
                                String dimension = targetEntity.level().dimension().location().toString();
                                
                                // 发送契约创建请求到服务器
                                ContractNetworkHandler.sendContractCreateRequest(entityId, entityType, entityName, position, dimension);
                            }
                        // 关闭界面
                        this.onClose();
                    } else {
                        // 第一次点击或点击不同选项，选择但不确认
                        selectedOptionIndex = 0;
                        confirmedOptionIndex = -1; // 重置确认状态
                    }
                    lastBlinkTime = System.currentTimeMillis(); // 重置闪烁时间
                    return true;
                } else if (mouseX >= optionBoxX && mouseX <= optionBoxX + optionBoxWidth &&
                           mouseY >= option2Y - 2 && mouseY <= option2Y + optionClickHeight) {
                    // 第一次点击选中，第二次点击相同选项则确认
                    if (selectedOptionIndex == 1) {
                        // 第二次点击相同选项，确认选择
                        confirmedOptionIndex = 1;
                        // 关闭界面
                        this.onClose();
                    } else {
                        // 第一次点击或点击不同选项，选择但不确认
                        selectedOptionIndex = 1;
                        confirmedOptionIndex = -1; // 重置确认状态
                    }
                    lastBlinkTime = System.currentTimeMillis(); // 重置闪烁时间
                    return true;
                }
            }

            // 获取实体渲染区域坐标
            int entityRenderX = 15; // 距离左侧20像素
            int entityRenderY = this.height - 80 + 10; // 在HUD内部，距离顶部10像素
            int entityRenderWidth = 64;
            int entityRenderHeight = 64;

            // 检查鼠标是否在实体渲染区域内
            if (mouseX >= entityRenderX && mouseX <= entityRenderX + entityRenderWidth &&
                mouseY >= entityRenderY && mouseY <= entityRenderY + entityRenderHeight) {
                // 在区域内，记录鼠标位置用于拖动旋转
                this.lastMouseX = mouseX;
                this.lastMouseY = mouseY;
                this.isDragging = true;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isDragging && button == 0) {
            // 计算鼠标移动距离
            double deltaX = mouseX - this.lastMouseX;
            double deltaY = mouseY - this.lastMouseY;

            // 更新鼠标位置
            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;

            // 设置EntityDisplay的鼠标位置用于旋转（传递当前鼠标位置）
            this.entityDisplay.setMousePosition((float)mouseX, (float)mouseY);

            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.isDragging) {
            this.isDragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /**
     * 绘制右侧选项菜单
     */
    private void drawOptionsMenu(GuiGraphics guiGraphics, int mouseX, int mouseY, int hudX, int hudY, int hudWidth, int hudHeight) {
        // 应用动画效果 - 从矩形中心往上下两边展开
        float progress = optionsMenuAnimationProgress;
        if (progress <= 0) return;

        int screenWidth = this.width;
        int screenHeight = this.height;

        // 绘制选项文本
        String option1 = "契约";
        String option2 = "选项 2";

        // 计算最长选项的宽度，用于动态调整选项框宽度
        int option1Width = this.font.width(option1);
        int option2Width = this.font.width(option2);
        int maxOptionWidth = Math.max(option1Width, option2Width);

        // 动态调整选项框宽度，确保有足够的边距
        int minOptionBoxWidth = 55; // 最小宽度
        int optionPadding = 10; // 左右边距
        int optionBoxWidth = Math.max(minOptionBoxWidth, maxOptionWidth + optionPadding * 2);

        // 动态调整选项框高度，根据选项数量和间距计算
        int optionSpacing = 10; // 选项间距
        int optionHeight = this.font.lineHeight; // 选项文本高度
        int verticalPadding = 16; // 上下边距（与左右边距一致）
        int optionCount = 2; // 当前选项数量
        int optionBoxHeight = optionHeight * optionCount + optionSpacing * (optionCount - 1) + verticalPadding * 2;

        int optionBoxX = hudX + hudWidth - optionBoxWidth - 1; // 距离游戏框右侧边缘像素
        int centerY = hudY - optionBoxHeight / 2 - 3; // 向上移动3像素，增加与下方文本框的间距
        
        // 计算动画位置和高度
        int animatedOptionBoxHeight = (int) (optionBoxHeight * progress);
        int animatedOptionBoxY = centerY - animatedOptionBoxHeight / 2;
        
        if (animatedOptionBoxHeight <= 0) return;
        
        // 更新变量名以反映新的动画方式
        int optionBoxY = animatedOptionBoxY;
        
        // 先绘制黑色背景（半透明）
        guiGraphics.fill(optionBoxX + 5, optionBoxY + 5, optionBoxX + optionBoxWidth - 5, optionBoxY + animatedOptionBoxHeight - 5, 0xCC000000);
        
        // 绘制选项框背景贴图 - 与主HUD风格一致
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, HUD_TEXTURE);
        
        // 设置暗红色调
        RenderSystem.setShaderColor(0.5F, 0.1F, 0.1F, 1.0F);
        
        // 平铺背景贴图
        int textureSrcX = 0;
        int textureSrcY = 48;
        int textureWidth = 48;
        int textureHeight = 48;
        
        int tilesX = (int) Math.ceil((double) (optionBoxWidth - 10) / textureWidth);
        int tilesY = (int) Math.ceil((double) (animatedOptionBoxHeight - 10) / textureHeight);
        
        for (int x = 0; x < tilesX; x++) {
            for (int y = 0; y < tilesY; y++) {
                int tileX = optionBoxX + 5 + x * textureWidth;
                int tileY = optionBoxY + 5 + y * textureHeight;
                int tileWidth = Math.min(textureWidth, optionBoxWidth - 10 - x * textureWidth);
                int tileHeight = Math.min(textureHeight, animatedOptionBoxHeight - 10 - y * textureHeight);
                
                guiGraphics.blit(HUD_TEXTURE, tileX, tileY, textureSrcX, textureSrcY, tileWidth, tileHeight, 96, 96);
            }
        }
        
        // 重置颜色设置
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        
        // 绘制选项框边框 - 复用主HUD的边框逻辑
        drawMenuBorder(guiGraphics, optionBoxX, optionBoxY, optionBoxWidth, animatedOptionBoxHeight);
        
        // 计算选项位置 - 垂直居中对齐（选项+间隔作为一个整体居中）
        int totalOptionsHeight = optionHeight * optionCount + optionSpacing * (optionCount - 1);
        int startY = optionBoxY + (optionBoxHeight - totalOptionsHeight) / 2;
        int option1Y = startY;
        int option2Y = startY + optionHeight + optionSpacing;
        
        // 检查鼠标悬停状态 - 确保选项区域不会重叠
        int optionClickHeight = optionHeight + 4; // 选项点击区域高度
        boolean isOption1Hovered = mouseX >= optionBoxX && mouseX <= optionBoxX + optionBoxWidth &&
                                  mouseY >= option1Y - 2 && mouseY <= option1Y + optionClickHeight &&
                                  mouseY >= optionBoxY; // 确保在动画展开范围内
        boolean isOption2Hovered = mouseX >= optionBoxX && mouseX <= optionBoxX + optionBoxWidth &&
                                  mouseY >= option2Y - 2 && mouseY <= option2Y + optionClickHeight &&
                                  mouseY >= optionBoxY; // 确保在动画展开范围内
        
        // 在关闭过程中不渲染选项文本和光标
            if (!isClosing) {
                // 绘制选中选项的闪烁效果
                if (selectedOptionIndex >= 0) {
                    // 设置使用HUD纹理
                    RenderSystem.setShader(GameRenderer::getPositionTexShader);
                    RenderSystem.setShaderTexture(0, HUD_TEXTURE);
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, blinkTransparency); // 使用闪烁透明度
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    
                    // 闪烁框的UV坐标 (64,64) 到 (95,95)
                    int blinkU = 64;
                    int blinkV = 64;
                    int blinkWidth = 32;
                    int blinkHeight = 32;
                    
                    if (selectedOptionIndex == 0) {
                        // 绘制选项1的闪烁框
                        int blinkBoxX = optionBoxX + (optionBoxWidth - option1Width) / 2 - 5;
                        int blinkBoxY = option1Y - 2;
                        int blinkBoxWidth = option1Width + 10;
                        int blinkBoxHeight = optionHeight + 4;
                        
                        // 绘制缩放的闪烁框，保持原始纹理的UV比例
                        guiGraphics.blit(
                            HUD_TEXTURE,
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
                    } else if (selectedOptionIndex == 1) {
                        // 绘制选项2的闪烁框
                        int blinkBoxX = optionBoxX + (optionBoxWidth - option2Width) / 2 - 5;
                        int blinkBoxY = option2Y - 2;
                        int blinkBoxWidth = option2Width + 10;
                        int blinkBoxHeight = optionHeight + 4;
                        
                        guiGraphics.blit(
                            HUD_TEXTURE,
                            blinkBoxX,
                            blinkBoxY,
                            blinkBoxWidth,
                            blinkBoxHeight,
                            blinkU,
                            blinkV,
                            blinkWidth,
                            blinkHeight,
                            128,
                            128
                        );
                    }
                    
                    RenderSystem.disableBlend();
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                }
                
                // 绘制选项1 - 根据状态显示不同颜色：悬停（红色）、已确认（金色）、普通（白色）
                int option1Color = 0xFFFFFF; // 默认白色
                if (isOption1Hovered) {
                    option1Color = 0xFFA0A0; // 悬停时红色
                }
                if (confirmedOptionIndex == 0) {
                    option1Color = 0xFFFFA0; // 已确认时金色
                }
                
                guiGraphics.drawString(
                        this.font,
                        option1,
                        optionBoxX + (optionBoxWidth - option1Width) / 2, // 水平居中
                        option1Y,
                        option1Color
                );
                
                // 绘制选项2 - 根据状态显示不同颜色：悬停（红色）、已确认（金色）、普通（白色）
                int option2Color = 0xFFFFFF; // 默认白色
                if (isOption2Hovered) {
                    option2Color = 0xFFA0A0; // 悬停时红色
                }
                if (confirmedOptionIndex == 1) {
                    option2Color = 0xFFFFA0; // 已确认时金色
                }
                
                guiGraphics.drawString(
                        this.font,
                        option2,
                        optionBoxX + (optionBoxWidth - option2Width) / 2, // 水平居中
                        option2Y,
                        option2Color
                );
                
                // 在选项前绘制三角形光标（RPG Maker风格）
                drawTriangleCursor(guiGraphics, optionBoxX + (optionBoxWidth - maxOptionWidth) / 2 - 15, isOption1Hovered ? option1Y : option2Y);
            }
    }
    
    /**
     * 绘制菜单边框 - 复用主HUD的边框逻辑
     */
    private void drawMenuBorder(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        // 确保宽度足够大以显示边框
        if (width <= 24) return;
        
        // 边框尺寸

        int borderSize = 12;
        int rightBorderSize = 16;
        
        // 四个角的边框坐标
        int topLeftSrcX = 64;
        int topLeftSrcY = 0;
        int topRightSrcX = 112;
        int topRightSrcY = 0;
        int bottomLeftSrcX = 64;
        int bottomLeftSrcY = 52;
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
        
        // 绘制四个角
        guiGraphics.blit(HUD_TEXTURE, x, y, topLeftSrcX, topLeftSrcY, borderSize, borderSize, 128, 128);
        guiGraphics.blit(HUD_TEXTURE, x + width - rightBorderSize, y, topRightSrcX, topRightSrcY, rightBorderSize, borderSize, 128, 128);
        guiGraphics.blit(HUD_TEXTURE, x, y + height - borderSize, bottomLeftSrcX, bottomLeftSrcY, borderSize, borderSize, 128, 128);
        guiGraphics.blit(HUD_TEXTURE, x + width - rightBorderSize, y + height - borderSize, bottomRightSrcX, bottomRightSrcY, rightBorderSize, borderSize, 128, 128);
        
        // 平铺上边框线
        int topBorderTiles = (int) Math.ceil((double) (width - 2 * borderSize) / topBorderWidth);
        for (int i = 0; i < topBorderTiles; i++) {
            int tileX = x + borderSize + i * topBorderWidth;
            int tileWidth = Math.min(topBorderWidth, width - borderSize * 2 - i * topBorderWidth);
            guiGraphics.blit(HUD_TEXTURE, tileX, y, topBorderSrcX, topBorderSrcY, tileWidth, topBorderHeight, 128, 128);
        }
        
        // 平铺下边框线
        int bottomBorderTiles = (int) Math.ceil((double) (width - 2 * borderSize) / bottomBorderWidth);
        for (int i = 0; i < bottomBorderTiles; i++) {
            int tileX = x + borderSize + i * bottomBorderWidth;
            int tileWidth = Math.min(bottomBorderWidth, width - borderSize * 2 - i * bottomBorderWidth);
            guiGraphics.blit(HUD_TEXTURE, tileX, y + height - bottomBorderHeight , bottomBorderSrcX, bottomBorderSrcY, tileWidth, bottomBorderHeight, 128, 128);
        }

        // 平铺左边框线
        int leftBorderTiles = (int) Math.ceil((double) (height - 2 * borderSize) / leftBorderHeight);
        for (int i = 0; i < leftBorderTiles; i++) {
            int tileY = y + borderSize + i * leftBorderHeight;
            int tileHeight = Math.min(leftBorderHeight, height - borderSize * 2 - i * leftBorderHeight);
            guiGraphics.blit(HUD_TEXTURE, x, tileY, leftBorderSrcX, leftBorderSrcY, leftBorderWidth, tileHeight, 128, 128);
        }
        
        // 平铺右边框线
        int rightBorderTiles = (int) Math.ceil((double) (height - 2 * borderSize) / rightBorderHeight);
        for (int i = 0; i < rightBorderTiles; i++) {
            int tileY = y + borderSize + i * rightBorderHeight;
            int tileHeight = Math.min(rightBorderHeight, height - borderSize * 2 - i * rightBorderHeight);
            guiGraphics.blit(HUD_TEXTURE, x + width - rightBorderWidth, tileY, rightBorderSrcX, rightBorderSrcY, rightBorderWidth, tileHeight, 128, 128);
        }
    }
    
    /**
     * 绘制三角形光标
     */
    private void drawTriangleCursor(GuiGraphics guiGraphics, int x, int y) {
        // 使用填充矩形绘制简单的三角形光标
        int cursorSize = 6;
        
        // 绘制三角形（使用几个矩形模拟）
        for (int i = 0; i < cursorSize; i++) {
            int width = cursorSize - i;
            int offsetX = x + i;
            int offsetY = y + i;
            
            guiGraphics.fill(offsetX, offsetY, offsetX + width, offsetY + 1, 0xFFFFFF);
        }
    }
    
    /**
     * 检查鼠标是否在选项区域内
     */
    private boolean isMouseInOption(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}