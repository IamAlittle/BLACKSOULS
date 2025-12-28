package com.iamalittle.black_souls_options.hud;

import com.iamalittle.black_souls_options.effects.DeathTotemEffect;
import com.iamalittle.black_souls_options.contracts.GlobalContractManager;
import com.iamalittle.black_souls_options.contracts.ContractManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

/**
 * 死亡图腾HUD渲染器
 * 在经验条中间上方显示不死图腾图标和冷却时间
 */
public class DeathTotemHud {
    
    private static final int ICON_SIZE = 16;
    private static final int TEXT_COLOR = 0xFFFFFF; // 白色
    private static final int COOLDOWN_COLOR = 0xFF5555; // 红色
    
    /**
     * 渲染死亡图腾HUD
     */
    public static void render(GuiGraphics guiGraphics, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        
        if (player == null || minecraft.options.hideGui) {
            return;
        }
        
        // 检查玩家是否激活了唤魔者契约
        ContractManager contractManager = GlobalContractManager.getInstance().getContractManager(player);
        if (contractManager == null) {
            return; // 没有契约管理器时不显示
        }
        
        // 检查是否有激活的唤魔者契约
        boolean hasActiveEvokerContract = contractManager.getAllContracts().stream()
            .anyMatch(contract -> "minecraft:evoker".equals(contract.getEntityType()) && 
                contract.getEffects().stream().anyMatch(effect -> effect.isActive()));
        
        if (!hasActiveEvokerContract) {
            return; // 没有激活的唤魔者契约时不显示
        }
        
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        
        // 计算HUD位置：经验条中间上方，血条和饥饿条之间
        int hudX = screenWidth / 2 - ICON_SIZE / 2; // 屏幕水平居中
        int hudY = screenHeight - 49; // 经验条上方位置
        
        // 获取冷却状态和剩余时间（使用优化方法）
        boolean isOnCooldown = DeathTotemEffect.isOnCooldown(player);
        float remainingSeconds = DeathTotemEffect.getRemainingCooldownSeconds(player);
        int remainingSecondsInt = DeathTotemEffect.getRemainingCooldownSecondsInt(player);

        // 渲染图腾图标（100%透明度）- 先渲染以确保图标在底层
        renderTotemIcon(guiGraphics, hudX, hudY, remainingSecondsInt);
        
        // 只有触发CD时才显示倒计时
        if (isOnCooldown && remainingSeconds > 0) {
            renderCircularCooldown(guiGraphics, hudX, hudY, remainingSecondsInt, remainingSeconds);
        }
    }
    
    /**
     * 渲染不死图腾（使用原版纹理图片）
     */
    private static void renderTotemIcon(GuiGraphics guiGraphics, int x, int y, int remainingSeconds) {
        // 使用原版不死图腾纹理路径
        ResourceLocation totemTexture = new ResourceLocation("textures/item/totem_of_undying.png");
        
        // 渲染纹理图片
        guiGraphics.blit(totemTexture, x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
    }
    
    /**
     * 渲染圆形冷却倒计时
     * 优化版本：使用新的进度计算方法
     */
    private static void renderCircularCooldown(GuiGraphics guiGraphics, int x, int y, int remainingSecondsInt, float remainingSeconds) {
        Minecraft minecraft = Minecraft.getInstance();
        
        if (remainingSeconds <= 0) {
            return; // 冷却完成时不显示
        }
        
        // 获取冷却进度百分比（使用优化方法）
        float progress = DeathTotemEffect.getCooldownProgress(minecraft.player);
        
        // 计算圆形半径和中心点
        int centerX = x + ICON_SIZE / 2;
        int centerY = y + ICON_SIZE / 2;
        int radius = ICON_SIZE / 2 + 2; // 比图标稍大
        
        // 在图标中心显示剩余秒数（如果剩余时间较短）
        if (remainingSecondsInt <= DeathTotemEffect.COOLDOWN_SECONDS) {
            String timeText = String.valueOf(remainingSecondsInt);
            
            // 根据图标宽度自动缩放文本
            float scale = calculateTextScale(minecraft.font, timeText, ICON_SIZE - 4); // 留出2像素边距
            int textWidth = (int) (minecraft.font.width(timeText) * scale);
            int textHeight = (int) (minecraft.font.lineHeight * scale);
            
            int textX = centerX - textWidth / 2;
            int textY = centerY - textHeight / 2;
            
            // 保存当前变换矩阵
            guiGraphics.pose().pushPose();
            // 应用缩放变换
            guiGraphics.pose().translate(textX, textY, 0);
            guiGraphics.pose().scale(scale, scale, 1.0f);
            
            // 渲染文本阴影
            guiGraphics.drawString(minecraft.font, timeText, 1, 1, 0x000000, false);
            // 渲染主文本
            guiGraphics.drawString(minecraft.font, timeText, 0, 0, COOLDOWN_COLOR, false);
            
            // 恢复变换矩阵
            guiGraphics.pose().popPose();
        }
        
        // 设置渲染颜色（半透明红色）
        RenderSystem.setShaderColor(1.0f, 0.0f, 0.0f, 0.7f);
        
        // 绘制圆形进度条（反转进度：冷却时间越短，进度条显示越少）
        drawCircularProgress(guiGraphics, centerX, centerY, radius, 1.0f - progress);
        
        // 重置颜色
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
    
    /**
     * 绘制圆形进度条
     */
    private static void drawCircularProgress(GuiGraphics guiGraphics, int centerX, int centerY, int radius, float progress) {
        // 计算起始角度（从顶部开始，顺时针方向）
        float startAngle = -90.0f; // 顶部
        float endAngle = startAngle + 360.0f * progress;
        
        // 绘制圆形进度条（使用更平滑的线条）
        int segments = 36; // 36个线段，每10度一个
        float angleStep = 360.0f / segments;
        
        int prevX = centerX + (int) (radius * Math.cos(Math.toRadians(startAngle)));
        int prevY = centerY + (int) (radius * Math.sin(Math.toRadians(startAngle)));
        
        for (float angle = startAngle + angleStep; angle <= endAngle; angle += angleStep) {
            int currentX = centerX + (int) (radius * Math.cos(Math.toRadians(angle)));
            int currentY = centerY + (int) (radius * Math.sin(Math.toRadians(angle)));
            
            // 绘制线段
            drawLine(guiGraphics, prevX, prevY, currentX, currentY, 0xFFFF0000);
            
            prevX = currentX;
            prevY = currentY;
        }
        
        // 绘制最后一段（如果进度不是完整的）
        if (endAngle < startAngle + 360.0f) {
            int finalX = centerX + (int) (radius * Math.cos(Math.toRadians(endAngle)));
            int finalY = centerY + (int) (radius * Math.sin(Math.toRadians(endAngle)));
            drawLine(guiGraphics, prevX, prevY, finalX, finalY, 0xFFFF0000);
        }
    }
    
    /**
     * 绘制线段（Bresenham算法简化版）
     */
    private static void drawLine(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;
        
        while (true) {
            guiGraphics.fill(x1, y1, x1 + 1, y1 + 1, color);
            
            if (x1 == x2 && y1 == y2) break;
            
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x1 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y1 += sy;
            }
        }
    }
    
    /**
     * 格式化时间显示（分钟:秒）
     */
    private static String formatTime(int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        
        if (minutes > 0) {
            return String.format("%d:%02d", minutes, remainingSeconds);
        } else {
            return String.format("%ds", remainingSeconds);
        }
    }
    
    /**
     * 计算文本缩放比例，确保文本不超过指定宽度
     */
    private static float calculateTextScale(net.minecraft.client.gui.Font font, String text, int maxWidth) {
        int textWidth = font.width(text);
        if (textWidth <= maxWidth) {
            return 1.0f; // 文本宽度合适，不需要缩放
        }
        
        // 计算缩放比例，确保文本不超过最大宽度
        float scale = (float) maxWidth / textWidth;
        
        // 设置最小缩放比例，避免文本过小
        float minScale = 0.5f;
        return Math.max(scale, minScale);
    }
}