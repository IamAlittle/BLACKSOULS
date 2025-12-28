package com.iamalittle.black_souls_options.mixins.fabric;

import com.mojang.blaze3d.vertex.PoseStack;
import com.iamalittle.black_souls_options.common.Events;
import com.iamalittle.black_souls_options.common.events.RenderWorldLastEvent;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric平台的LevelRenderer混入类
 * 用于触发世界渲染事件
 */
@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {

    @Inject(
            method = "renderLevel",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderDebug(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/Camera;)V"))
    private void onRender(
            PoseStack matrices,
            float partialTicks,
            long limitTime,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightTexture lightmapTextureManager,
            Matrix4f projectionMatrix,
            CallbackInfo info
    ) {
        Events.RenderWorldLast.trigger(new RenderWorldLastEvent(matrices, partialTicks, projectionMatrix));
    }
}