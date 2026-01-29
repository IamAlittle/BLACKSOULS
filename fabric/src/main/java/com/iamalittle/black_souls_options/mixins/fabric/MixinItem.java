package com.iamalittle.black_souls_options.mixins.fabric;

import com.iamalittle.black_souls_options.contracts.effects.mobs.AxolotlContract;
import com.iamalittle.black_souls_options.items.BambooItemHandler;
import com.iamalittle.black_souls_options.items.CactusItemHandler;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Item mixin to prevent item usage when player is feigning death
 * This intercepts both use and useOn methods to prevent all item interactions
 */
@Mixin(Item.class)
public class MixinItem {
    
    /**
     * 拦截物品的use方法，处理仙人掌右键使用动画
     * 这个方法处理右键使用物品（如吃食物、喝药水等）
     */
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void preventItemUse(Level level, Player player, net.minecraft.world.InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        ItemStack itemStack = player.getItemInHand(hand);
        
        // 检查是否是仙人掌，如果是则触发食用动画
        if (itemStack.getItem() == Items.CACTUS) {
            InteractionResultHolder<ItemStack> result = CactusItemHandler.handleCactusUse(player, hand);
            if (result.getResult() != net.minecraft.world.InteractionResult.PASS) {
                cir.setReturnValue(result);
                return;
            }
        }
        
        // 检查是否是竹子，如果是则触发食用动画（熊猫契约）
        if (itemStack.getItem() == Items.BAMBOO) {
            InteractionResultHolder<ItemStack> result = BambooItemHandler.handleBambooUse(player, hand);
            if (result.getResult() != net.minecraft.world.InteractionResult.PASS) {
                cir.setReturnValue(result);
                return;
            }
        }
        
        // 检查玩家是否正在装死
        if (AxolotlContract.isPlayerFeigningDeath(player)) {
            // 发送提示消息给玩家
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("§c装死时无法使用物品！"), true);
            // 取消物品使用
            cir.setReturnValue(InteractionResultHolder.fail(player.getItemInHand(hand)));
        }
    }
    
    /**
     * 拦截物品的useOn方法，当玩家装死时阻止在方块上使用物品
     * 这个方法处理在方块上右键使用物品（如放置方块、使用工具等）
     */
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void preventItemUseOnBlock(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        // 获取使用物品的玩家
        Player player = context.getPlayer();
        
        // 检查玩家是否存在且正在装死
        if (player != null && AxolotlContract.isPlayerFeigningDeath(player)) {
            // 发送提示消息给玩家
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("§c装死时无法在方块上使用物品！"), true);
            // 取消在方块上使用物品
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}