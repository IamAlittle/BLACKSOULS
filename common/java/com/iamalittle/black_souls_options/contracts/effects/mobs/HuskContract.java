package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import java.util.*;

/**
 * 尸壳契约效果 - 继承僵尸契约并添加额外效果
 * 玩家契约尸壳后获得的能力：
 * 1. 继承僵尸契约的感染村民效果
 * 2. 不会因为晒太阳着火（尸壳特性）
 * 3. 攻击时使目标获得饥饿效果
 */
public class HuskContract extends ContractEffect {
    private static final String EFFECT_ID = "husk_hunger_effect";
    private static final String DISPLAY_NAME = "尸壳之怒";
    private static final String DESCRIPTION = "攻击时使目标饥饿，且不会因太阳着火";
    
    // 饥饿效果持续时间（秒）
    private static final int HUNGER_DURATION = 10;
    
    // 饥饿效果等级（0为基础等级）
    private static final int HUNGER_AMPLIFIER = 0;
    
    // 饥饿效果触发概率（百分比）
    private static final float HUNGER_CHANCE = 0.5f; // 50%概率触发
    
    // 尸壳契约玩家集合
    private static final Set<UUID> huskContractPlayers = new HashSet<>();
    
    public HuskContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            huskContractPlayers.add(player.getUUID());
            
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
            huskContractPlayers.remove(player.getUUID());
            
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
        // 尸壳契约不会因为晒太阳着火，所以不需要处理太阳着火逻辑
        // 继承僵尸契约的其他逻辑（如感染村民）通过攻击事件处理器处理
        // 检查是否在水中，如果在水中则受到伤害
        checkWaterDamage(player);
    }
    
    /**
     * 对目标施加饥饿效果
     */
    public static void applyHungerToTarget(Entity target, Player attacker) {
        if (target != null && attacker != null && attacker.isAlive() && hasHuskContract(attacker)) {
            // 只在服务端执行
            if (!target.level().isClientSide()) {
                // 检查饥饿效果触发概率
                if (attacker.getRandom().nextFloat() < HUNGER_CHANCE) {
                    // 检查目标是否为LivingEntity（只有生物才能获得效果）
                    if (target instanceof LivingEntity livingTarget) {
                        // 施加饥饿效果
                        MobEffectInstance hunger = new MobEffectInstance(
                            MobEffects.HUNGER, 
                            HUNGER_DURATION * 20, // 转换为游戏刻（1秒=20刻）
                            HUNGER_AMPLIFIER
                        );
                        livingTarget.addEffect(hunger);
                        
                        // 给攻击者发送效果消息
                        attacker.sendSystemMessage(Component.literal("§6你的攻击让目标感到饥饿！"));
                    }
                }
            }
        }
    }
    
    /**
     * 尝试将村民转化为僵尸村民（继承僵尸契约效果）
     */
    public static void tryConvertVillager(Entity target, Player attacker) {
        if (target != null && attacker != null && attacker.isAlive() && hasHuskContract(attacker)) {
            // 检查目标是否为村民
            if (target instanceof net.minecraft.world.entity.npc.Villager villager) {
                // 检查转化概率（30%，与僵尸契约相同）
                if (attacker.getRandom().nextFloat() < 0.3f) {
                    convertToZombieVillager(villager, attacker);
                }
            }
        }
    }
    
    /**
     * 将村民转化为僵尸村民（与僵尸契约相同的实现）
     */
    private static void convertToZombieVillager(net.minecraft.world.entity.npc.Villager villager, Player attacker) {
        if (villager.level().isClientSide()) {
            return; // 只在服务端执行
        }
        
        // 创建僵尸村民
        net.minecraft.world.entity.monster.ZombieVillager zombieVillager = new net.minecraft.world.entity.monster.ZombieVillager(
            net.minecraft.world.entity.EntityType.ZOMBIE_VILLAGER, villager.level());
        
        // 设置僵尸村民的位置
        zombieVillager.setPos(villager.getX(), villager.getY(), villager.getZ());
        
        // 复制村民的数据到僵尸村民
        zombieVillager.setVillagerData(villager.getVillagerData());
        zombieVillager.setVillagerXp(villager.getVillagerXp());
        
        // 移除原村民，生成僵尸村民
        villager.discard();
        villager.level().addFreshEntity(zombieVillager);
        
        // 添加转化视觉效果
        if (villager.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            // 播放转化粒子效果
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                villager.getX(), villager.getY() + 1, villager.getZ(),
                20, 0.5, 0.5, 0.5, 0.1);
            
            // 播放转化音效
            serverLevel.playSound(null, villager.getX(), villager.getY(), villager.getZ(),
                net.minecraft.sounds.SoundEvents.ZOMBIE_VILLAGER_CONVERTED, 
                net.minecraft.sounds.SoundSource.HOSTILE, 1.0f, 1.0f);
        }
        
        // 给攻击者发送转化消息
        attacker.sendSystemMessage(Component.literal("§c你成功感染了一个村民！"));
    }
    
    /**
     * 检查玩家是否拥有尸壳契约效果
     */
    public static boolean hasHuskContract(Player player) {
        return player != null && huskContractPlayers.contains(player.getUUID());
    }
    
    /**
     * 检查玩家是否在水中，如果在水中则受到伤害（尸壳怕水）
     */
    private static void checkWaterDamage(Player player) {
        if (player != null && player.isAlive() && player.isInWater()) {
            player.hurt(player.damageSources().drown(), 1.0F);
        }
    }
    
    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal("§6尸壳契约效果："));
        details.add(Component.literal("§7继承僵尸契约的感染村民效果"));
        details.add(Component.literal("§7- 攻击村民时有30%概率将其转化为僵尸村民"));
        details.add(Component.literal("§7尸壳专属效果："));
        details.add(Component.literal("§7- 不会因为晒太阳着火"));
        details.add(Component.literal("§7- 攻击时有" + (int)(HUNGER_CHANCE * 100) + "%概率使目标获得饥饿效果"));
        details.add(Component.literal("§c- 碰水会掉血"));
        return details;
    }
}