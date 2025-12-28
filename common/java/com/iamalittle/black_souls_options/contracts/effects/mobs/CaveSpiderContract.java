package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 洞穴蜘蛛契约效果 - 中毒
 * 玩家契约洞穴蜘蛛后获得的能力：
 * 1. 攻击目标时使其中毒
 * 2. 中毒效果持续时间和强度与洞穴蜘蛛相同
 */
public class CaveSpiderContract extends ContractEffect {
    private static final String EFFECT_ID = "cave_spider_poison";
    private static final String DISPLAY_NAME = "我的武器可是淬毒了的！";
    private static final String DESCRIPTION = "攻击时使目标中毒";
    
    // 中毒持续时间（秒）
    private static final int POISON_DURATION = 10;
    
    // 中毒等级（0为基础等级）
    private static final int POISON_AMPLIFIER = 0;
    
    public CaveSpiderContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            caveSpiderContractPlayers.add(player.getUUID());
            
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
            caveSpiderContractPlayers.remove(player.getUUID());
            
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
        // 洞穴蜘蛛契约不需要每tick更新，中毒逻辑在攻击时处理
    }
    
    // 契约玩家集合（用于快速检查）
    private static final Set<UUID> caveSpiderContractPlayers = new HashSet<>();
    
    /**
     * 使目标中毒
     */
    public static void poisonTarget(Entity target, Player attacker) {
        if (target != null && attacker != null && attacker.isAlive() && hasCaveSpiderContract(attacker)) {
            // 检查目标是否可以被施加效果
            if (target instanceof net.minecraft.world.entity.LivingEntity livingTarget) {
                // 施加中毒效果
                MobEffectInstance poisonEffect = new MobEffectInstance(
                    MobEffects.POISON, 
                    POISON_DURATION * 20, // 转换为tick数
                    POISON_AMPLIFIER,
                    false, // 不显示粒子效果
                    true   // 显示图标
                );
                
                livingTarget.addEffect(poisonEffect);
                
                // 显示中毒粒子效果
                if (target.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.ITEM_SLIME,
                        target.getX(), target.getY() + 1, target.getZ(),
                        8, 0.5, 0.5, 0.5, 0.1);
                }
            }
        }
    }
    
    /**
     * 检查玩家是否拥有洞穴蜘蛛契约效果
     */
    public static boolean hasCaveSpiderContract(Player player) {
        return player != null && caveSpiderContractPlayers.contains(player.getUUID());
    }
    
    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal("§6洞穴蜘蛛契约效果："));
        details.add(Component.literal("§2毒液攻击"));
        details.add(Component.literal("§2- 攻击时使目标中毒"));
        details.add(Component.literal("§2- 中毒持续" + POISON_DURATION + "秒"));
        return details;
    }
}