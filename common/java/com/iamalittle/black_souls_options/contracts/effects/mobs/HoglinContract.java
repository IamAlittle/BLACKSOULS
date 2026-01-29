package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * 疣猪兽契约效果 - 减少受到的击退效果50%，周围有诡异菇时缓慢
 * 玩家契约疣猪兽后获得的能力：
 * 1. 减少50%受到的击退效果（更难被击退）
 * 2. 周围有诡异菇时会获得缓慢效果（类似疣猪兽怕诡异菇的特性）
 */
public class HoglinContract extends ContractEffect {
    private static final String EFFECT_ID = "hoglin_knockback_resistance";
    private static final String DISPLAY_NAME = "疣猪兽";
    private static final String DESCRIPTION = "减少50%受到的击退效果，周围有诡异菇时缓慢";
    
    // 击退抗性增加比例（减少50%受到的击退效果）
    private static final double KNOCKBACK_RESISTANCE_BOOST = 0.5;
    
    // 诡异菇检测范围（以玩家为中心的半径）
    private static final int WARPED_FUNGUS_DETECTION_RANGE = 5;
    
    // 缓慢效果持续时间（秒）
    private static final int SLOWNESS_DURATION = 3;
    
    // 疣猪兽契约玩家集合
    private static final Set<UUID> hoglinContractPlayers = new HashSet<>();
    
    // 击退抗性修改器
    private static final UUID KNOCKBACK_RESISTANCE_MODIFIER_UUID = UUID.fromString("12345678-1234-1234-1234-123456789abc");
    private static final String KNOCKBACK_RESISTANCE_MODIFIER_NAME = "hoglin_contract_knockback_resistance";
    
    // 记录玩家是否在诡异菇附近
    private static final Map<UUID, Boolean> playerNearWarpedFungus = new HashMap<>();
    
    public HoglinContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            hoglinContractPlayers.add(player.getUUID());
            playerNearWarpedFungus.put(player.getUUID(), false);
            
            // 应用击退抗性减少效果
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
            hoglinContractPlayers.remove(player.getUUID());
            playerNearWarpedFungus.remove(player.getUUID());
            
            // 移除击退抗性减少效果
            removeKnockbackResistanceReduction(player);
            
            // 移除可能存在的缓慢效果
            if (player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
                player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            }
            
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
        
        // 客户端不执行逻辑
        if (player.level().isClientSide()) {
            return;
        }
        
        // 确保击退抗性减少效果正确应用
        ensureKnockbackResistanceApplied(player);
        
        // 检测周围是否有诡异菇
        boolean nearWarpedFungus = isNearWarpedFungus(player);
        boolean wasNearWarpedFungus = playerNearWarpedFungus.getOrDefault(player.getUUID(), false);
        
        // 更新状态
        playerNearWarpedFungus.put(player.getUUID(), nearWarpedFungus);
        
        // 如果状态发生变化，应用或移除缓慢效果
        if (nearWarpedFungus && !wasNearWarpedFungus) {
            // 进入诡异菇范围，应用缓慢效果
            applySlownessEffect(player);
        } else if (!nearWarpedFungus && wasNearWarpedFungus) {
            // 离开诡异菇范围，移除缓慢效果
            removeSlownessEffect(player);
        }
    }
    
    /**
     * 应用击退抗性增加效果（减少50%受到的击退效果）
     */
    private void applyKnockbackResistanceBoost(Player player) {
        AttributeInstance knockbackResistance = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (knockbackResistance != null) {
            // 移除可能存在的旧修改器
            knockbackResistance.removeModifier(KNOCKBACK_RESISTANCE_MODIFIER_UUID);
            
            // 计算需要增加的数值以达到50%击退抗性增加
            double currentValue = knockbackResistance.getBaseValue();
            double requiredIncrease = currentValue * KNOCKBACK_RESISTANCE_BOOST;
            
            // 使用加法修改器，直接设置到目标值
            AttributeModifier modifier = new AttributeModifier(
                KNOCKBACK_RESISTANCE_MODIFIER_UUID,
                KNOCKBACK_RESISTANCE_MODIFIER_NAME,
                requiredIncrease, // 增加50%
                AttributeModifier.Operation.ADDITION
            );
            knockbackResistance.addPermanentModifier(modifier);
        }
    }
    
    /**
     * 确保击退抗性增加效果正确应用
     */
    private void ensureKnockbackResistanceApplied(Player player) {
        AttributeInstance knockbackResistance = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (knockbackResistance != null) {
            // 获取当前值（包括所有修改器）
            double currentTotalValue = knockbackResistance.getValue();
            // 基础值
            double baseValue = knockbackResistance.getBaseValue();
            // 预期值：基础值增加50%
            double expectedValue = baseValue * (1 + KNOCKBACK_RESISTANCE_BOOST);
            
            // 如果当前值与预期值差异超过0.01，重新应用效果
            if (Math.abs(currentTotalValue - expectedValue) > 0.01) {
                applyKnockbackResistanceBoost(player);
            }
        }
    }
    
    /**
     * 移除击退抗性减少效果
     */
    private void removeKnockbackResistanceReduction(Player player) {
        AttributeInstance knockbackResistance = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (knockbackResistance != null) {
            knockbackResistance.removeModifier(KNOCKBACK_RESISTANCE_MODIFIER_UUID);
        }
    }
    
    /**
     * 检测玩家周围是否有诡异菇
     */
    private boolean isNearWarpedFungus(Player player) {
        BlockPos playerPos = player.blockPosition();
        
        // 检测以玩家为中心的立方体范围内是否有诡异菇
        for (int x = -WARPED_FUNGUS_DETECTION_RANGE; x <= WARPED_FUNGUS_DETECTION_RANGE; x++) {
            for (int y = -WARPED_FUNGUS_DETECTION_RANGE; y <= WARPED_FUNGUS_DETECTION_RANGE; y++) {
                for (int z = -WARPED_FUNGUS_DETECTION_RANGE; z <= WARPED_FUNGUS_DETECTION_RANGE; z++) {
                    BlockPos checkPos = playerPos.offset(x, y, z);
                    BlockState blockState = player.level().getBlockState(checkPos);
                    
                    if (blockState.is(Blocks.WARPED_FUNGUS)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * 应用缓慢效果
     */
    private void applySlownessEffect(Player player) {
        if (!player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
            MobEffectInstance slowness = new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                -1, // 转换为tick
                0, // 等级0
                false, // 不显示粒子效果
                true // 不显示图标
            );
            player.addEffect(slowness);
        }
    }
    
    /**
     * 移除缓慢效果
     */
    private void removeSlownessEffect(Player player) {
        if (player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
    }
    
    /**
     * 检查玩家是否拥有疣猪兽契约效果
     */
    public static boolean hasHoglinContract(Player player) {
        return player != null && hoglinContractPlayers.contains(player.getUUID());
    }
    
    @Override
    protected long getTickInterval() {
        return 1000; // 1秒检测一次
    }

    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal("§6疣猪兽契约效果："));
        details.add(Component.literal("§7- 减少50%击退抗性"));
        details.add(Component.literal("§c- 周围" + WARPED_FUNGUS_DETECTION_RANGE + "格内有诡异菇时获得缓慢效果"));
        return details;
    }
}