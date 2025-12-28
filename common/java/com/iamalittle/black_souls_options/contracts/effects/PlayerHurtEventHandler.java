package com.iamalittle.black_souls_options.contracts.effects;

import com.iamalittle.black_souls_options.contracts.effects.mobs.GuardianThornsContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.EnderManContract;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * 玩家受伤事件处理器，用于处理玩家受伤时触发的契约效果
 * 包括守卫者契约的反伤效果和末影人契约的弹射物免疫效果
 */
public class PlayerHurtEventHandler {
    
    /**
     * 处理玩家受伤事件
     * @param player 受伤的玩家
     * @param damageSource 伤害来源
     * @param damageAmount 伤害值
     * @return 是否处理了伤害事件（true表示已处理，false表示未处理）
     */
    public static boolean onPlayerHurt(Player player, DamageSource damageSource, float damageAmount) {
        if (player == null || player.level().isClientSide()) {
            return false;
        }
        
        boolean handled = false;
        
        // 检查并触发守卫者契约的反伤效果
        if (GuardianThornsContract.hasGuardianThornsContract(player)) {
            boolean thornsTriggered = GuardianThornsContract.onPlayerHurt(player, damageSource, damageAmount);
            if (thornsTriggered) {
                handled = true;
            }
        }
        
        // 检查并触发末影人契约的弹射物免疫效果
        if (EnderManContract.hasEnderManContract(player)) {
            boolean immune = EnderManContract.onPlayerHurt(player, damageSource);
            if (immune) {
                // 如果免疫了伤害，返回true表示已处理
                return true;
            }
        }
        
        return handled;
    }
    
    /**
     * 处理玩家受伤事件（简化版，不包含伤害值）
     * @param player 受伤的玩家
     * @param damageSource 伤害来源
     * @return 是否处理了伤害事件
     */
    public static boolean onPlayerHurt(Player player, DamageSource damageSource) {
        return onPlayerHurt(player, damageSource, 0.0f);
    }
}