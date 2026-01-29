package com.iamalittle.black_souls_options.items;

import com.iamalittle.black_souls_options.contracts.effects.mobs.PandaContract;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * 竹子物品处理器 - 处理手持竹子右键触发食用动画
 */
public class BambooItemHandler {
    
    /**
     * 检查玩家是否手持竹子
     */
    public static boolean isHoldingBamboo(Player player) {
        if (player == null) return false;
        
        // 检查主手和副手是否持有竹子
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        
        return (mainHand.getItem() == Items.BAMBOO) || (offHand.getItem() == Items.BAMBOO);
    }
    
    /**
     * 处理竹子右键使用
     */
    public static InteractionResultHolder<ItemStack> handleBambooUse(Player player, InteractionHand hand) {
        if (player == null) return InteractionResultHolder.pass(ItemStack.EMPTY);
        
        ItemStack itemStack = player.getItemInHand(hand);
        
        // 检查是否是竹子
        if (itemStack.getItem() != Items.BAMBOO) {
            return InteractionResultHolder.pass(itemStack);
        }
        
        // 检查玩家是否拥有熊猫契约
        if (!PandaContract.hasPandaContract(player)) {
            return InteractionResultHolder.pass(itemStack);
        }
        
        // 触发食用动画
        player.startUsingItem(hand);
        
        // 返回成功结果，并消耗物品
        return InteractionResultHolder.consume(itemStack);
    }
    
    /**
     * 获取竹子的使用动画
     */
    public static UseAnim getBambooUseAnimation() {
        return UseAnim.EAT;
    }
    
    /**
     * 获取竹子的使用持续时间
     */
    public static int getBambooUseDuration() {
        return 20; // 熊猫食用竹子比标准更快
    }
}