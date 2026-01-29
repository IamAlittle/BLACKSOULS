package com.iamalittle.black_souls_options.mixins.forge;

import com.iamalittle.black_souls_options.contracts.effects.mobs.SheepContract;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;

/**
 * PlayerRenderer Mixin - 实现绵羊契约的彩虹变色效果
 * 修改玩家渲染逻辑，为拥有绵羊契约的玩家添加彩虹颜色变换
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    
    // 不需要构造函数，因为这是Mixin类
    
    /**
     * 注入到render方法中，在渲染玩家前应用彩虹颜色
     */
    @Inject(method = "render", at = @At("HEAD"))
    public void onRender(AbstractClientPlayer player, float entityYaw, float partialTicks, PoseStack poseStack, 
                        MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        
        // 检查玩家是否拥有绵羊契约
        if (SheepContract.hasSheepContract(player)) {
            // 获取当前彩虹颜色
            float[] rainbowColor = SheepContract.getRainbowColor(player);
            if (rainbowColor != null) {
                // 应用彩虹颜色到渲染状态
                applyRainbowColor(rainbowColor);
            }
        }
    }
    
    /**
     * 注入到render方法末尾，恢复原始颜色状态
     */
    @Inject(method = "render", at = @At("TAIL"))
    public void onRenderEnd(AbstractClientPlayer player, float entityYaw, float partialTicks, PoseStack poseStack, 
                           MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        
        // 恢复默认颜色状态
        if (SheepContract.hasSheepContract(player)) {
            restoreDefaultColor();
        }
    }

    
    /**
     * 应用彩虹颜色到渲染状态
     */
    private void applyRainbowColor(float[] rgbColor) {
        if (rgbColor == null || rgbColor.length < 3) return;
        
        // 设置颜色乘数，实现彩虹效果
        RenderSystem.setShaderColor(rgbColor[0], rgbColor[1], rgbColor[2], 1.0f);
    }
    
    /**
     * 恢复默认颜色状态
     */
    private void restoreDefaultColor() {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
}