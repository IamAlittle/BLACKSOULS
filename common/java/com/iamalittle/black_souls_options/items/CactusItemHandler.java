package com.iamalittle.black_souls_options.items;

import com.iamalittle.black_souls_options.contracts.effects.mobs.CamelContract;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * 仙人掌物品处理器 - 处理手持仙人掌右键触发食用动画
 */
public class CactusItemHandler {
    
    /**
     * 检查玩家是否手持仙人掌
     */
    public static boolean isHoldingCactus(Player player) {
        if (player == null) return false;
        
        // 检查主手和副手是否持有仙人掌
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        
        return (mainHand.getItem() == Items.CACTUS) || (offHand.getItem() == Items.CACTUS);
    }
    
    /**
     * 处理仙人掌右键使用
     */
    public static InteractionResultHolder<ItemStack> handleCactusUse(Player player, InteractionHand hand) {
        if (player == null) return InteractionResultHolder.pass(ItemStack.EMPTY);
        
        ItemStack itemStack = player.getItemInHand(hand);
        
        // 检查是否是仙人掌
        if (itemStack.getItem() != Items.CACTUS) {
            return InteractionResultHolder.pass(itemStack);
        }
        
        // 检查玩家是否拥有骆驼契约
        if (!CamelContract.hasCamelContract(player)) {
            return InteractionResultHolder.pass(itemStack);
        }
        
        // 触发食用动画
        player.startUsingItem(hand);
        
        // 返回成功结果，并消耗物品
        return InteractionResultHolder.consume(itemStack);
    }
    
    /**
     * 获取仙人掌的使用动画
     */
    public static UseAnim getCactusUseAnimation() {
        return UseAnim.EAT;
    }
    
    /**
     * 获取仙人掌的使用持续时间
     */
    public static int getCactusUseDuration() {
        return 32; // 标准食用动画持续时间
    }
}