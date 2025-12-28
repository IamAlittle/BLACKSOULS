package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

/**
 * 史莱姆契约效果 - 分裂重生
 * 玩家契约史莱姆后可以获得死亡拦截和生命值上限调整效果
 */
public class SlimeContract extends ContractEffect {
    private static final String EFFECT_ID = "slime_split_rebirth";
    private static final String DISPLAY_NAME = "史莱姆分裂重生";
    private static final String DESCRIPTION = "死亡时分裂重生，每次死亡扣除一半生命值上限";
    
    // 存储玩家死亡次数和生命值上限数据
    private static final Map<UUID, SlimePlayerData> playerDataMap = new HashMap<>();
    
    public SlimeContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            // 初始化玩家数据
            UUID playerUuid = player.getUUID();
            if (!playerDataMap.containsKey(playerUuid)) {
                playerDataMap.put(playerUuid, new SlimePlayerData());
            }
            
            // 使用契约目标名称发送消息（仅在需要时发送）
            if (sendMessage) {
                String entityName = effectData.getString("contractEntityName");
                if (entityName.isEmpty()) {
                    entityName = displayName; // 回退到效果名称
                }
                sendActivationMessage(player, entityName);
            }
        }
    }
    
    @Override
    protected void onDeactivate(Player player) {
        if (player != null) {
            // 移除玩家数据
            playerDataMap.remove(player.getUUID());
            
            // 使用契约目标名称发送消息
            String entityName = effectData.getString("contractEntityName");
            if (entityName.isEmpty()) {
                entityName = displayName; // 回退到效果名称
            }
            sendDeactivationMessage(player, entityName);
        }
    }
    
    @Override
    protected void onTick(Player player) {
        // 史莱姆契约不需要每tick更新
    }
    
    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal("§b分裂重生效果："));
        details.add(Component.literal("§7第一次死亡：拦截死亡，扣除一半生命值上限并恢复满血"));
        details.add(Component.literal("§7第二次死亡：再扣一半上限并恢复满血"));
        details.add(Component.literal("§7第三次死亡：正常死亡"));
        details.add(Component.literal("§7优先级低于唤魔者契约"));
        return details;
    }
    
    /**
     * 检查是否可以触发史莱姆分裂重生效果
     */
    public static boolean canTriggerSlimeRebirth(Player player) {
        if (player == null || player.level().isClientSide()) {
            return false;
        }
        
        // 检查玩家是否拥有激活的史莱姆契约
        if (!hasSlimeContract(player)) {
            return false;
        }
        
        // 获取玩家数据
        SlimePlayerData playerData = playerDataMap.get(player.getUUID());
        if (playerData == null) {
            return false;
        }
        
        // 检查死亡次数，最多触发2次
        return playerData.getDeathCount() < 2;
    }
    
    /**
     * 触发史莱姆分裂重生效果
     */
    public static boolean triggerSlimeRebirth(Player player) {
        if (player == null || player.level().isClientSide()) {
            return false;
        }
        
        if (!canTriggerSlimeRebirth(player)) {
            return false;
        }
        
        // 获取玩家数据
        SlimePlayerData playerData = playerDataMap.get(player.getUUID());
        if (playerData == null) {
            return false;
        }
        
        // 增加死亡次数
        int deathCount = playerData.incrementDeathCount();
        
        // 计算新的生命值上限（每次死亡扣除一半）
        float currentMaxHealth = player.getMaxHealth();
        float newMaxHealth = currentMaxHealth / 2.0f;
        
        // 确保生命值上限不低于1点
        if (newMaxHealth < 1.0f) {
            newMaxHealth = 1.0f;
        }
        
        // 设置新的生命值上限
        player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(newMaxHealth);
        
        // 恢复满血
        player.setHealth(newMaxHealth);
        
        // 应用重生效果
        applyRebirthEffects(player);
        
        // 发送消息
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.literal("§a史莱姆分裂重生效果触发！第" + deathCount + "次死亡，生命值上限降至" + String.format("%.1f", newMaxHealth) + "点"));
        }
        
        return true;
    }
    
    /**
     * 应用重生效果
     */
    private static void applyRebirthEffects(Player player) {
        if (player.level().isClientSide()) {
            return;
        }
        
        // 播放音效
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), 
            SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.0F, 1.0F);

        // 清除负面效果
        player.removeAllEffects();

        // 应用重生效果
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 0));
        
        // 播放粒子效果（在服务端触发客户端效果）
        if (player instanceof ServerPlayer serverPlayer) {
            // 这里可以添加自定义的粒子效果包
        }
    }
    
    /**
     * 检查玩家是否拥有激活的史莱姆契约
     */
    public static boolean hasSlimeContract(Player player) {
        if (player == null) {
            return false;
        }
        
        // 检查玩家数据中是否有史莱姆契约
        return playerDataMap.containsKey(player.getUUID());
    }
    
    /**
     * 获取玩家当前的死亡次数
     */
    public static int getPlayerDeathCount(Player player) {
        if (player == null) {
            return 0;
        }
        
        SlimePlayerData playerData = playerDataMap.get(player.getUUID());
        if (playerData == null) {
            return 0;
        }
        
        return playerData.getDeathCount();
    }
    
    /**
     * 重置玩家的史莱姆契约数据（玩家重生时调用）
     */
    public static void resetPlayerData(Player player) {
        if (player == null) {
            return;
        }
        
        playerDataMap.remove(player.getUUID());
    }
    
    /**
     * 玩家史莱姆契约数据类
     */
    private static class SlimePlayerData {
        private int deathCount;
        
        public SlimePlayerData() {
            this.deathCount = 0;
        }
        
        public int getDeathCount() {
            return deathCount;
        }
        
        public int incrementDeathCount() {
            deathCount++;
            return deathCount;
        }
        
        public void reset() {
            deathCount = 0;
        }
    }
}