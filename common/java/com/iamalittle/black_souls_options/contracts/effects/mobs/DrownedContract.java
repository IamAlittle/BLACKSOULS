package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import java.util.*;

/**
 * 溺尸契约效果 - 三叉戟缓慢
 * 玩家契约溺尸后获得的能力：
 * 1. 使用三叉戟攻击时让目标获得缓慢效果
 * 2. 缓慢效果持续时间和强度与溺尸相同
 */
public class DrownedContract extends ContractEffect {
    private static final String EFFECT_ID = "drowned_trident_slowness";
    private static final String DISPLAY_NAME = "溺尸";
    private static final String DESCRIPTION = "使用三叉戟攻击时让目标获得缓慢效果,村民感染";
    
    // 缓慢效果持续时间（秒）
    private static final int SLOWNESS_DURATION = 5;
    
    // 缓慢效果等级（0为基础等级）
    private static final int SLOWNESS_AMPLIFIER = 0;
    
    // 溺尸契约玩家集合
    private static final Set<UUID> drownedContractPlayers = new HashSet<>();
    
    public DrownedContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            drownedContractPlayers.add(player.getUUID());
            
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
            drownedContractPlayers.remove(player.getUUID());
            
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
        // 检查玩家是否在太阳底下并着火
        if (player != null && player.isAlive() && hasDrownedContract(player)) {
            // 只在服务端执行
            if (!player.level().isClientSide()) {
                // 检查是否在白天、户外、没有遮挡
                if (isInSunlight(player)) {
                    // 如果玩家戴着头盔，消耗耐久度
                    if (isWearingHelmet(player)) {
                        consumeHelmetDurability(player);
                    } else {
                        // 没有戴头盔，设置玩家着火
                        player.setSecondsOnFire(8); // 着火8秒
                    }
                }
            }
        }
    }
    
    /**
     * 检查玩家是否在太阳底下
     */
    private static boolean isInSunlight(Player player) {
        // 检查是否为白天
        if (player.level().isDay()) {
            // 检查玩家是否在户外（没有方块遮挡）
            if (player.level().canSeeSky(player.blockPosition())) {
                // 检查玩家是否在水中或雨中没有遮挡（溺尸在水中不会着火）
                if (!player.isInWaterRainOrBubble() && !player.isInPowderSnow) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * 检查玩家是否戴着头盔
     */
    private static boolean isWearingHelmet(Player player) {
        // 检查头盔槽位是否有物品
        if (!player.getInventory().armor.get(3).isEmpty()) {
            return true;
        }
        return false;
    }
    
    /**
     * 消耗头盔耐久度
     */
    private static void consumeHelmetDurability(Player player) {
        // 获取头盔物品
        var helmet = player.getInventory().armor.get(3);
        if (!helmet.isEmpty() && helmet.isDamageableItem()) {
            // 每次消耗1点耐久度
            helmet.hurtAndBreak(1, player, (p) -> {
                // 头盔损坏时的回调
                p.broadcastBreakEvent(net.minecraft.world.entity.EquipmentSlot.HEAD);
            });
        }
    }
    
    /**
     * 检查玩家是否使用三叉戟攻击（手持检测）
     */
    public static boolean isUsingTrident(Player player) {
        if (player == null) return false;
        
        // 只检查主手是否持有三叉戟（攻击时使用的武器）
        ItemStack mainHand = player.getMainHandItem();
        
        return mainHand.getItem() == Items.TRIDENT;
    }
    
    /**
     * 检查伤害来源是否为三叉戟（伤害类型检测）
     */
    public static boolean isTridentDamage(DamageSource damageSource) {
        if (damageSource == null) return false;
        
        // 检查是否为三叉戟伤害类型
        return damageSource.is(DamageTypes.TRIDENT) || 
               damageSource.getMsgId().contains("trident");
    }
    
    /**
     * 对目标施加缓慢效果（Forge版本，使用伤害来源检测）
     */
    public static void applySlownessToTarget(Entity target, Player attacker, DamageSource damageSource) {
        if (target != null && attacker != null && attacker.isAlive() && hasDrownedContract(attacker)) {
            // 只在服务端执行
            if (!target.level().isClientSide()) {
                // 检查是否为三叉戟伤害（投掷或近战）
                boolean isTridentAttack = isTridentDamage(damageSource) || isUsingTrident(attacker);
                
                if (isTridentAttack) {
                    // 检查目标是否为LivingEntity（只有生物才能获得效果）
                    if (target instanceof LivingEntity livingTarget) {
                        // 施加缓慢效果
                        MobEffectInstance slowness = new MobEffectInstance(
                            MobEffects.MOVEMENT_SLOWDOWN, 
                            SLOWNESS_DURATION * 20, // 转换为游戏刻（1秒=20刻）
                            SLOWNESS_AMPLIFIER
                        );
                        livingTarget.addEffect(slowness);
                    }
                }
            }
        }
    }
    
    /**
     * 对目标施加缓慢效果（Fabric版本，使用手持检测）
     */
    public static void applySlownessToTarget(Entity target, Player attacker) {
        if (target != null && attacker != null && attacker.isAlive() && hasDrownedContract(attacker)) {
            // 只在服务端执行
            if (!target.level().isClientSide()) {
                // 检查攻击者是否使用三叉戟（手持检测）
                if (isUsingTrident(attacker)) {
                    // 检查目标是否为LivingEntity（只有生物才能获得效果）
                    if (target instanceof LivingEntity livingTarget) {
                        // 施加缓慢效果
                        MobEffectInstance slowness = new MobEffectInstance(
                            MobEffects.MOVEMENT_SLOWDOWN, 
                            SLOWNESS_DURATION * 20, // 转换为游戏刻（1秒=20刻）
                            SLOWNESS_AMPLIFIER
                        );
                        livingTarget.addEffect(slowness);
                    }
                }
            }
        }
    }
    
    /**
     * 尝试将村民转化为僵尸村民（继承僵尸契约效果）
     */
    public static void tryConvertVillager(Entity target, Player attacker) {
        if (target != null && attacker != null && attacker.isAlive() && hasDrownedContract(attacker)) {
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
     * 检查玩家是否拥有溺尸契约效果
     */
    public static boolean hasDrownedContract(Player player) {
        return player != null && drownedContractPlayers.contains(player.getUUID());
    }
    
    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal("§7三叉戟缓慢"));
        details.add(Component.literal("§7- 使用三叉戟攻击时让目标获得缓慢效果"));
        details.add(Component.literal("§7- 缓慢效果持续" + SLOWNESS_DURATION + "秒"));
        details.add(Component.literal("§7- 缓慢效果等级：" + (SLOWNESS_AMPLIFIER + 1)));
        details.add(Component.literal("§c- 白天在太阳底下会着火，持续8秒"));
        details.add(Component.literal("§c- 戴头盔可以避免太阳着火效果，但会消耗头盔耐久度"));
        return details;
    }
}