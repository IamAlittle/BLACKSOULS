package com.iamalittle.black_souls_options.mixins.forge;

import com.iamalittle.black_souls_options.items.BambooItemHandler;
import com.iamalittle.black_souls_options.items.CactusItemHandler;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 仙人掌物品Mixin - 为仙人掌添加使用动画属性
 */
@Mixin(Item.class)
public class MixinCactusItem {
    
    /**
     * 为仙人掌设置使用动画
     */
    @Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
    private void setCactusUseAnimation(ItemStack stack, CallbackInfoReturnable<net.minecraft.world.item.UseAnim> cir) {
        if (stack.getItem() == Items.CACTUS) {
            cir.setReturnValue(CactusItemHandler.getCactusUseAnimation());
        }
        
        // 为竹子设置使用动画（熊猫契约）
        if (stack.getItem() == Items.BAMBOO) {
            cir.setReturnValue(BambooItemHandler.getBambooUseAnimation());
        }
    }
    
    /**
     * 为仙人掌设置使用持续时间
     */
    @Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
    private void setCactusUseDuration(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (stack.getItem() == Items.CACTUS) {
            cir.setReturnValue(CactusItemHandler.getCactusUseDuration());
        }
        
        // 为竹子设置使用持续时间（熊猫契约）
        if (stack.getItem() == Items.BAMBOO) {
            cir.setReturnValue(BambooItemHandler.getBambooUseDuration());
        }
    }
}