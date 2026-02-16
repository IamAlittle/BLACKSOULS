package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.TextColor;

import java.util.*;

/**
 * 铁傀儡契约效果 - 百分百免疫击退
 * 玩家契约铁傀儡后获得的能力：
 * 1. 完全免疫击退效果（类似铁傀儡的特性）
 * 2. 获得100%击退抗性，使玩家无法被击退
 */
public class IronGolemContract extends ContractEffect {
    private static final String EFFECT_ID = "iron_golem_knockback_immunity";
    private static final String DISPLAY_NAME = "black_souls_options.contracts.iron_golem.display_name";
    private static final String DESCRIPTION = "black_souls_options.contracts.iron_golem.description";
    
    // 击退抗性目标值（100%免疫）
    private static final double KNOCKBACK_RESISTANCE_TARGET = 1.0;
    
    // 铁傀儡契约玩家集合
    private static final Set<UUID> ironGolemContractPlayers = new HashSet<>();
    
    // 击退抗性修改器
    private static final UUID KNOCKBACK_RESISTANCE_MODIFIER_UUID = UUID.fromString("87654321-4321-4321-4321-987654321abc");
    private static final String KNOCKBACK_RESISTANCE_MODIFIER_NAME = "iron_golem_contract_knockback_resistance";
    
    public IronGolemContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            ironGolemContractPlayers.add(player.getUUID());
            
            // 应用击退抗性增加效果
            applyKnockbackResistanceBoost(player);
            
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
            ironGolemContractPlayers.remove(player.getUUID());
            
            // 移除击退抗性增加效果
            removeKnockbackResistanceBoost(player);
            
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
        if (player == null || !player.isAlive() || player.level() == null) {
            return;
        }
        
        // 铁傀儡契约不需要每tick执行逻辑，击退抗性是永久性的
        // 但我们可以每tick检查一次确保效果正常应用
        if (ironGolemContractPlayers.contains(player.getUUID())) {
            ensureKnockbackResistanceApplied(player);
        }
    }
    
    /**
     * 应用击退抗性增加效果
     */
    private void applyKnockbackResistanceBoost(Player player) {
        AttributeInstance knockbackResistance = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (knockbackResistance != null) {
            // 移除可能存在的旧修改器
            knockbackResistance.removeModifier(KNOCKBACK_RESISTANCE_MODIFIER_UUID);
            
            // 计算需要增加的数值以达到100%击退抗性
            double currentValue = knockbackResistance.getBaseValue();
            double requiredIncrease = KNOCKBACK_RESISTANCE_TARGET - currentValue;
            
            // 使用加法修改器，直接设置到目标值
            AttributeModifier modifier = new AttributeModifier(
                KNOCKBACK_RESISTANCE_MODIFIER_UUID,
                KNOCKBACK_RESISTANCE_MODIFIER_NAME,
                requiredIncrease,
                AttributeModifier.Operation.ADDITION
            );
            knockbackResistance.addPermanentModifier(modifier);
        }
    }
    
    /**
     * 确保击退抗性效果正确应用
     */
    private void ensureKnockbackResistanceApplied(Player player) {
        AttributeInstance knockbackResistance = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (knockbackResistance != null) {
            // 检查当前击退抗性是否达到100%
            double currentValue = knockbackResistance.getValue();
            if (currentValue < KNOCKBACK_RESISTANCE_TARGET - 0.01) {
                // 如果未达到100%，重新应用效果
                applyKnockbackResistanceBoost(player);
            }
        }
    }
    
    /**
     * 移除击退抗性增加效果
     */
    private void removeKnockbackResistanceBoost(Player player) {
        AttributeInstance knockbackResistance = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (knockbackResistance != null) {
            knockbackResistance.removeModifier(KNOCKBACK_RESISTANCE_MODIFIER_UUID);
        }
    }
    
    @Override
    protected long getTickInterval() {
        return 20; // 每20tick（1秒）检测一次
    }

    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.translatable("black_souls_options.contracts.iron_golem.effect_title")
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FFFF"))));
        details.add(Component.translatable("black_souls_options.contracts.iron_golem.effect1")
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.iron_golem.effect2")
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        return details;
    }
}