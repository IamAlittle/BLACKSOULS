package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 潜影贝契约效果 - 漂浮
 * 玩家契约潜影贝后获得的能力：
 * 1. 攻击目标时使其漂浮
 * 2. 漂浮效果持续时间和强度可配置
 */
public class ShulkerContract extends ContractEffect {
    private static final String EFFECT_ID = "shulker_levitation";
    private static final String DISPLAY_NAME = "漂浮";
    private static final String DESCRIPTION = "攻击使目标漂浮";
    
    // 漂浮效果持续时间（秒）
    private static final int LEVITATION_DURATION = 5;
    
    // 漂浮效果等级（1-255，1为最低，255为最高）
    private static final int LEVITATION_AMPLIFIER = 1;
    
    // 漂浮触发概率（0.0-1.0，1.0为100%）
    private static final float TRIGGER_CHANCE = 1f;
    
    // 漂浮粒子效果显示间隔（毫秒）
    private static final long PARTICLE_INTERVAL = 200;
    
    // 上次粒子效果时间
    private long lastParticleTime;
    
    public ShulkerContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
        this.lastParticleTime = 0;
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            shulkerContractPlayers.add(player.getUUID());
            
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
            shulkerContractPlayers.remove(player.getUUID());
            
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
    }

    
    // 契约玩家集合（用于快速检查）
    private static final Set<UUID> shulkerContractPlayers = new HashSet<>();
    
    /**
     * 对目标施加漂浮效果
     */
    public static void applyLevitationToTarget(Entity target, Player attacker) {
        if (target != null && attacker != null && attacker.isAlive() && 
            hasShulkerContract(attacker) && target instanceof LivingEntity livingTarget) {
            
            // 检查触发概率
            if (attacker.getRandom().nextFloat() > TRIGGER_CHANCE) {
                return; // 未触发
            }
            
            // 施加漂浮效果
            livingTarget.addEffect(new MobEffectInstance(
                MobEffects.LEVITATION, 
                LEVITATION_DURATION * 20, // 转换为tick（20tick=1秒）
                LEVITATION_AMPLIFIER - 1, // 等级从0开始（0=等级1）
                false, // 不显示粒子效果（我们自己控制）
                true   // 显示图标
            ));
            
            // 显示漂浮粒子效果
            if (target.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,
                    target.getX(), target.getY() + 1, target.getZ(),
                    8, 0.5, 0.5, 0.5, 0.2);

            }
        }
    }
    
    /**
     * 检查玩家是否拥有潜影贝契约效果
     */
    public static boolean hasShulkerContract(Player player) {
        return player != null && shulkerContractPlayers.contains(player.getUUID());
    }
    
    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal("§6潜影贝契约效果："));
        details.add(Component.literal("§7漂浮"));
        details.add(Component.literal("§7- 攻击时有" + (int)(TRIGGER_CHANCE * 100) + "%概率使目标漂浮"));
        return details;
    }
    
    @Override
    public CompoundTag saveToNBT() {
        CompoundTag nbt = super.saveToNBT();
        nbt.putLong("lastParticleTime", lastParticleTime);
        return nbt;
    }
    
    @Override
    public void loadFromNBT(CompoundTag nbt) {
        super.loadFromNBT(nbt);
        if (nbt.contains("lastParticleTime")) {
            lastParticleTime = nbt.getLong("lastParticleTime");
        }
    }
}