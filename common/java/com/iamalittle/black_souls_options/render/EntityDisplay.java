package com.iamalittle.black_souls_options.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.npc.Villager;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 3D实体渲染工具类，实现与ToroHealth模组中EntityDisplay相同的效果
 * 提供实体尺寸自适应、鼠标交互旋转等功能
 */
public class EntityDisplay {
    
    // 渲染区域常量
    public static final int RENDER_HEIGHT = 64;
    public static final int RENDER_WIDTH = 64;
    
    // 实例变量
    private Entity entity;
    private float entityScale = 1.0F;
    private float xOffset = 0.0F;
    private float yOffset = 0.0F;
    private float mouseX = 0.0F;
    private float mouseY = 0.0F;
    private float rotationY = 0.0F; // Y轴旋转角度（累积）
    private float rotationX = -15.0F; // X轴旋转角度（累积）
    
    /**
     * 设置要渲染的实体
     */
    public void setEntity(Entity entity) {
        this.entity = entity;
        updateScale();
    }
    
    /**
     * 设置鼠标位置，用于控制实体旋转
     * 基于鼠标移动距离来累积旋转角度
     */
    public void setMousePosition(float mouseX, float mouseY) {
        // 计算鼠标移动距离（相对于中心点）
        float deltaX = mouseX - this.mouseX;
        float deltaY = mouseY - this.mouseY;
        
        // 累积旋转角度
        this.rotationY += deltaX * 0.5F; // X轴移动控制Y轴旋转
        this.rotationX += deltaY * 0.5F; // Y轴移动控制X轴旋转
        
        // 限制X轴旋转角度范围（-45度到45度）
        this.rotationX = Math.max(-45.0F, Math.min(45.0F, this.rotationX));
        
        // 更新当前鼠标位置
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }
    
    /**
     * 绘制实体到指定位置
     */
    public void draw(GuiGraphics guiGraphics, int x, int y) {
        if (entity == null) return;
        
        drawEntity(guiGraphics, x, y, entity, entityScale, xOffset, yOffset, rotationY, rotationX);
    }
    
    /**
     * 更新实体缩放比例和偏移量
     * 根据碰撞箱最长边来缩放实体，确保实体完全适应64x64的框
     */
    private void updateScale() {
        if (entity == null) return;
        
        // 获取实体碰撞箱尺寸
        float width = entity.getBbWidth();
        float height = entity.getBbHeight();
        
        // 计算最长边
        float maxDimension = Math.max(width, height);
        
        // 根据最长边计算缩放比例，确保实体完全适应64x64的框
        // 碰撞箱越大，缩放比例应该越小，这样才能缩小实体
        float scale = (RENDER_WIDTH * 0.6F) / maxDimension; // 留出10%的边距，碰撞箱越大缩放越小
        
        
        // 限制缩放范围，确保实体不会过大或过小
        entityScale = Math.max(0.2F, Math.min(scale, 1.0F));
        
        // 计算偏移量使实体完美居中
        xOffset = (RENDER_WIDTH - width * entityScale) / 2.0F;
        yOffset = (RENDER_HEIGHT - height * entityScale) / 2.0F;
    }
    
    /**
     * 静态方法：渲染实体到GUI
     * 复制自InventoryScreen.drawEntity，但进行了修改以支持自定义变换
     */
    public static void drawEntity(GuiGraphics guiGraphics, int x, int y, Entity entity, float scale, 
                                  float xOffset, float yOffset, float rotationY, float rotationX) {
        if (entity == null) return;
        
        // 检查实体是否为末影龙，如果是则不显示3D形象
        if (entity.getClass().getName().contains("EnderDragon")) {
            return;
        }
        
        // 设置裁剪区域，只渲染64x64框内的部分
        guiGraphics.enableScissor(x, y, x + RENDER_WIDTH, y + RENDER_HEIGHT);
        
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        
        // 设置渲染位置 - 居中显示
        int entityX = x + RENDER_WIDTH / 2;
        int entityY = y + RENDER_HEIGHT / 2;
        
        // 应用变换 - 修正位置和缩放
        // 由于生物渲染从脚下开始，需要向上偏移半个碰撞箱高度来真正居中
        // 计算：scale * 20.0F 是最终缩放，所以偏移量应该是 (entity.getBbHeight() * scale * 20.0F) / 2
        float verticalOffset = entity.getBbHeight() * scale * -10.0F;
        poseStack.translate(entityX, entityY - verticalOffset, 1050.0F);
        poseStack.scale(1.0F, 1.0F, 1.0F); // 移除Z轴负缩放，防止模型倒置
        poseStack.scale(scale * 20.0F, scale * 20.0F, scale * 20.0F); // 减小缩放因子
        
        // 使用累积的旋转角度
        float yRot = rotationY; // Y轴旋转
        float xRot = rotationX; // X轴旋转
        
        // 应用旋转 - 使用指定的旋转角度
        poseStack.mulPose(new Quaternionf().rotationX((float) Math.toRadians(xRot - 180)));
        poseStack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(yRot)));
        
        // 设置光照
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        
        // 获取实体渲染器
        Minecraft minecraft = Minecraft.getInstance();
        EntityRenderDispatcher entityRenderDispatcher = minecraft.getEntityRenderDispatcher();
        
        // 设置渲染器角度 - 使用更合适的视角
        entityRenderDispatcher.overrideCameraOrientation(
            new Quaternionf().rotationXYZ((float) Math.toRadians(15.0F), 0.0F, 0.0F)
        );
        
        // 禁用阴影
        entityRenderDispatcher.setRenderShadow(false);
        
        // 创建缓冲区
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        
        boolean renderSuccess = false;
        try {
            // 保存实体状态
            float yRotO = entity.yRotO;
            float entityYRot = entity.getYRot();
            float xRotO = entity.xRotO;
            float entityXRot = entity.getXRot();
            
            // 设置实体角度 - 使用默认角度
            entity.yRotO = 0.0F;
            entity.setYRot(0.0F);
            entity.xRotO = 0.0F;
            entity.setXRot(0.0F);
            
            // 渲染实体
            entityRenderDispatcher.render(
                entity,
                0.0D, 0.0D, 0.0D,
                0.0F,
                1.0F,
                poseStack,
                bufferSource,
                0xF000F0
            );
            
            // 刷新缓冲区
            bufferSource.endBatch();
            
            // 恢复实体状态
            entity.yRotO = yRotO;
            entity.setYRot(entityYRot);
            entity.xRotO = xRotO;
            entity.setXRot(entityXRot);
            
            renderSuccess = true;
            
        } catch (Exception e) {
            // 渲染失败时，不显示3D形象，直接返回
            // 恢复阴影设置
            entityRenderDispatcher.setRenderShadow(true);
            poseStack.popPose();
            guiGraphics.disableScissor();
            return;
        }
        
        // 恢复阴影设置
        entityRenderDispatcher.setRenderShadow(true);
        
        poseStack.popPose();
        
        // 禁用裁剪区域，恢复正常的渲染
        guiGraphics.disableScissor();
    }
    
    /**
     * 获取实体缩放比例
     */
    public float getEntityScale() {
        return entityScale;
    }
    
    /**
     * 获取X轴偏移量
     */
    public float getXOffset() {
        return xOffset;
    }
    
    /**
     * 获取Y轴偏移量
     */
    public float getYOffset() {
        return yOffset;
    }
    
    /**
     * 检查是否有实体
     */
    public boolean hasEntity() {
        return entity != null;
    }
    
    /**
     * 获取当前实体
     */
    public Entity getEntity() {
        return entity;
    }
}