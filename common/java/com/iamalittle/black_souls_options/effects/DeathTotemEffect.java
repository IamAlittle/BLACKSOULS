package com.iamalittle.black_souls_options.effects;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.Util;

/**
 * 死亡图腾效果管理器
 * 功能：玩家死亡时触发不死图腾效果，冷却，重生后重置冷却
 * 使用持久化数据存储，确保服务器重启后冷却状态不丢失
 */
public class DeathTotemEffect {
    
    // 冷却时间（以秒为单位）
    public static final int COOLDOWN_SECONDS = 1200;
    
    /**
     * 检查玩家是否可以触发死亡图腾效果
     */
    public static boolean canTriggerTotem(Player player) {
        if (player == null || player.level().isClientSide()) {
            return false;
        }
        
        // 获取玩家数据
        PlayerDeathTotemData playerData = DeathTotemDataManager.getInstance().getPlayerData(player);
        if (playerData == null) {
            return false;
        }
        
        // 只检查冷却时间，不限制每条命的触发次数
        return !playerData.isOnCooldown();
    }
    
    /**
     * 触发死亡图腾效果
     */
    public static boolean triggerTotemEffect(Player player) {
        if (player == null || player.level().isClientSide()) {
            return false;
        }
        
        if (!canTriggerTotem(player)) {
            return false;
        }
        
        // 获取玩家数据
        PlayerDeathTotemData playerData = DeathTotemDataManager.getInstance().getPlayerData(player);
        if (playerData == null) {
            return false;
        }
        
        // 设置冷却时间（不再标记本条命已触发）
        playerData.setRemainingCooldownTicks(COOLDOWN_SECONDS * 20);
        
        // 触发不死图腾效果
        applyTotemEffects(player);
        
        // 发送消息
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.literal("§6不死图腾效果已触发！冷却时间：" + COOLDOWN_SECONDS + "秒"));
        }
        
        return true;
    }
    
    /**
     * 应用不死图腾效果
     */
    private static void applyTotemEffects(Player player) {
        if (player.level().isClientSide()) {
            return;
        }
        
        // 播放音效
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), 
            SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);

        // 清除负面效果
        player.removeAllEffects();

        // 设置生命值
        player.setHealth(1.0F);

        // 应用效果
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
        
        // 播放粒子效果（在服务端触发客户端效果）
        if (player instanceof ServerPlayer serverPlayer) {
            // 这里可以添加自定义的粒子效果包
        }
    }
    
    /**
     * 玩家重生时重置触发状态和冷却时间
     */
    public static void onPlayerRespawn(Player player) {
        if (player == null) {
            return;
        }
        
        // 获取玩家数据
        PlayerDeathTotemData playerData = DeathTotemDataManager.getInstance().getPlayerData(player);
        if (playerData == null) {
            return;
        }
        
        // 重置本条命的触发状态和冷却时间
        playerData.resetCooldownState();
        
        // 注意：消息发送逻辑已移至DeathTotemDataManager.onPlayerRespawn方法中
        // 避免重复发送消息
    }
    
    /**
     * 每刻更新冷却时间
     */
    public static void tick() {
        // 更新数据管理器
        DeathTotemDataManager.getInstance().tick();
    }
    
    /**
     * 获取玩家剩余的冷却时间（秒）
     * 优化版本：使用浮点数计算提高精度，添加边界检查
     */
    public static float getRemainingCooldownSeconds(Player player) {
        if (player == null) {
            return 0.0f;
        }
        
        // 获取玩家数据
        PlayerDeathTotemData playerData = DeathTotemDataManager.getInstance().getPlayerData(player);
        if (playerData == null) {
            return 0.0f;
        }
        
        int remainingTicks = playerData.getRemainingCooldownTicks();
        
        // 边界检查
        if (remainingTicks <= 0) {
            return 0.0f;
        }
        
        // 使用浮点数计算提高精度
        return Math.max(0.0f, remainingTicks / 20.0f);
    }
    
    /**
     * 获取玩家剩余的冷却时间（秒）- 整数版本
     * 用于需要整数结果的场景
     */
    public static int getRemainingCooldownSecondsInt(Player player) {
        float seconds = getRemainingCooldownSeconds(player);
        return Math.max(0, (int) Math.ceil(seconds)); // 向上取整，避免显示0秒但仍有剩余时间
    }
    
    /**
     * 检查玩家是否在冷却中
     * 优化版本：添加边界检查和缓存优化
     */
    public static boolean isOnCooldown(Player player) {
        if (player == null) {
            return false;
        }
        
        // 获取玩家数据
        PlayerDeathTotemData playerData = DeathTotemDataManager.getInstance().getPlayerData(player);
        if (playerData == null) {
            return false;
        }
        
        int remainingTicks = playerData.getRemainingCooldownTicks();
        
        // 严格检查：只有大于0的ticks才算在冷却中
        return remainingTicks > 0;
    }
    
    /**
     * 获取冷却进度百分比（0.0到1.0）
     * 用于HUD进度条显示
     */
    public static float getCooldownProgress(Player player) {
        if (player == null) {
            return 0.0f;
        }
        
        // 获取玩家数据
        PlayerDeathTotemData playerData = DeathTotemDataManager.getInstance().getPlayerData(player);
        if (playerData == null) {
            return 0.0f;
        }
        
        int remainingTicks = playerData.getRemainingCooldownTicks();
        
        // 边界检查
        if (remainingTicks <= 0) {
            return 0.0f;
        }
        
        // 计算进度百分比
        float progress = 1.0f - (remainingTicks / (float)(COOLDOWN_SECONDS * 20));
        
        // 确保进度在0.0到1.0之间
        return Math.max(0.0f, Math.min(1.0f, progress));
    }
    
    /**
     * 获取总冷却时间（秒）
     */
    public static int getTotalCooldownSeconds() {
        return COOLDOWN_SECONDS;
    }
}