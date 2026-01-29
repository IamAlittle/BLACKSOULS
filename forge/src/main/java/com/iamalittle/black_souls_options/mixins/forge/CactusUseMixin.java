package com.iamalittle.black_souls_options.mixins.forge;

import com.iamalittle.black_souls_options.contracts.effects.mobs.CamelContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.PandaContract;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 契约物品使用动画结束监听Mixin
 * 监听玩家使用物品完成事件，当使用契约相关物品动画结束时触发对应契约效果
 */
@Mixin(LivingEntity.class)
public class CactusUseMixin {
    
    @Inject(method = "completeUsingItem", at = @At("HEAD"))
    private void onCompleteUsingItem(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        
        // 只处理玩家
        if (!(entity instanceof Player player)) {
            return;
        }
        
        ItemStack usingItem = player.getUseItem();
        
        // 检查是否正在使用仙人掌（骆驼契约）
        if (usingItem.getItem() == Items.CACTUS) {
            // 消耗仙人掌（减少数量）
            if (!player.isCreative()) {
                usingItem.shrink(1);
            }
            
            // 触发骆驼契约的仙人掌食用完成效果
            CamelContract.onCactusEatComplete(player);
        }
        // 检查是否正在使用竹子（熊猫契约）
        else if (usingItem.getItem() == Items.BAMBOO) {
            // 消耗竹子（减少数量）
            if (!player.isCreative()) {
                usingItem.shrink(1);
            }
            
            // 触发熊猫契约的竹子食用完成效果
            PandaContract.onBambooEatComplete(player);
        }
    }
}