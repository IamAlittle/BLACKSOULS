package com.iamalittle.black_souls_options.mixins.forge;

import com.iamalittle.black_souls_options.contracts.effects.mobs.AxolotlContract;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class MixinBlockItem {
    
    /**
     * 拦截方块物品的使用，当玩家装死时阻止放置方块
     */
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void preventBlockPlacement(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        // 检查玩家是否正在装死
        if (context.getPlayer() != null && AxolotlContract.isPlayerFeigningDeath(context.getPlayer())) {
            // 取消放置方块的动作
            cir.setReturnValue(InteractionResult.PASS);
            context.getPlayer().displayClientMessage(net.minecraft.network.chat.Component.literal("§c装死时无法放置方块！"), true);
        }
    }
}