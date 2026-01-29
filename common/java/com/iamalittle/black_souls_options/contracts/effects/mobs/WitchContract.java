package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;

import java.util.*;

/**
 * 女巫契约效果 - 减少50%魔法伤害
 * 使用Mixin拦截魔法伤害并实现伤害减免
 */
public class WitchContract extends ContractEffect {
    private static final String EFFECT_ID = "witch_magic_resistance";
    private static final String DISPLAY_NAME = "魔法抗性";
    private static final String DESCRIPTION = "获得女巫的魔法抗性，减少受到的魔法伤害";
    
    // 魔法伤害减免比例
    private static final float MAGIC_DAMAGE_REDUCTION = 0.50f; // 50%魔法伤害减免
    
    // 女巫契约玩家集合
    private static final Set<UUID> witchContractPlayers = new HashSet<>();
    
    public WitchContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            witchContractPlayers.add(player.getUUID());
            
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
            witchContractPlayers.remove(player.getUUID());
            
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
        // 女巫契约不需要每tick更新，伤害减免在受伤事件中处理
    }
    
    /**
     * 检查玩家是否应该减免魔法伤害
     * 这个方法需要在伤害事件处理器中调用
     */
    public static boolean shouldReduceMagicDamage(Player player, DamageSource damageSource) {
        if (player == null || damageSource == null) {
            return false;
        }
        
        // 检查玩家是否拥有女巫契约效果
        if (!hasWitchContract(player)) {
            return false;
        }
        
        // 检查伤害来源是否为魔法伤害
        return isMagicDamage(damageSource);
    }
    
    /**
     * 获取魔法伤害减免比例
     */
    public static float getMagicDamageReduction() {
        return MAGIC_DAMAGE_REDUCTION;
    }
    
    /**
     * 处理玩家受到伤害事件，检查是否需要减免魔法伤害
     * 这个方法需要在伤害事件处理器中调用
     */
    public static float onPlayerHurt(Player player, DamageSource damageSource, float originalAmount) {
        if (player == null || damageSource == null) {
            return originalAmount;
        }
        
        // 检查玩家是否拥有女巫契约效果
        if (!hasWitchContract(player)) {
            return originalAmount;
        }
        
        // 检查伤害来源是否为魔法伤害
        if (isMagicDamage(damageSource)) {
            // 计算减免后的伤害值
            float reducedAmount = originalAmount * (1.0f - MAGIC_DAMAGE_REDUCTION);
            System.out.println("[Witch Contract] Magic damage reduced from " + originalAmount + " to " + reducedAmount);
            return reducedAmount;
        }
        
        return originalAmount;
    }
    
    /**
     * 检查伤害来源是否为魔法伤害
     */
    private static boolean isMagicDamage(DamageSource damageSource) {
        if (damageSource == null) {
            return false;
        }
        
        // 检查是否为魔法伤害（药水、魔法攻击等）
        // 在Minecraft 1.20.1中，魔法伤害的标识方式
        return damageSource.is(DamageTypes.MAGIC) ||
               damageSource.is(DamageTypes.INDIRECT_MAGIC) ||
               damageSource.is(DamageTypes.WITHER) ||
               damageSource.is(DamageTypes.DRAGON_BREATH) ||
               damageSource.getMsgId().contains("magic") ||
               damageSource.getMsgId().contains("potion") ||
               damageSource.getMsgId().contains("wither") ||
               damageSource.getMsgId().contains("thorns");
    }
    
    /**
     * 检查玩家是否拥有女巫契约效果
     */
    public static boolean hasWitchContract(Player player) {
        return player != null && witchContractPlayers.contains(player.getUUID());
    }
    
    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal("§6女巫契约效果："));
        details.add(Component.literal("§7- 魔法抗性"));
        details.add(Component.literal("§7- 减少50%受到的魔法伤害"));
        details.add(Component.literal("§7- 对药水、魔法攻击、凋零效果等有效"));
        details.add(Component.literal("§7- 持续生效，无需手动激活"));
        return details;
    }
}