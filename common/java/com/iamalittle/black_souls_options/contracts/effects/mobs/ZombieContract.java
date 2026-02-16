package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import java.util.*;

/**
 * 僵尸契约效果 - 感染村民
 * 玩家契约僵尸后获得的能力：
 * 1. 攻击村民时将其转化为僵尸村民
 * 2. 转化过程有视觉效果和音效
 */
public class ZombieContract extends ContractEffect {
    private static final String EFFECT_ID = "zombie_infect_villager";
    private static final String DISPLAY_NAME = "black_souls_options.contracts.zombie.display_name";
    private static final String DESCRIPTION = "black_souls_options.contracts.zombie.description";
    
    // 转化概率（百分比）
    private static final float CONVERSION_CHANCE = 0.3f; // 30%概率转化
    
    // 僵尸契约玩家集合
    private static final Set<UUID> zombieContractPlayers = new HashSet<>();
    
    public ZombieContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            zombieContractPlayers.add(player.getUUID());
            
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
            zombieContractPlayers.remove(player.getUUID());
            
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
        
        // 检查玩家是否在太阳底下并着火
        if (hasZombieContract(player)) {
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
                // 检查玩家是否在水中或雨中没有遮挡（僵尸在水中不会着火）
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
     * 尝试将村民转化为僵尸村民
     */
    public static void tryConvertVillager(Entity target, Player attacker) {
        if (target != null && attacker != null && attacker.isAlive() && hasZombieContract(attacker)) {
            // 检查目标是否为村民
            if (target instanceof Villager villager) {
                // 检查转化概率
                if (attacker.getRandom().nextFloat() < CONVERSION_CHANCE) {
                    convertToZombieVillager(villager, attacker);
                }
            }
        }
    }
    
    /**
     * 将村民转化为僵尸村民
     */
    private static void convertToZombieVillager(Villager villager, Player attacker) {
        if (villager.level().isClientSide()) {
            return; // 只在服务端执行
        }
        
        // 创建僵尸村民
        ZombieVillager zombieVillager = new ZombieVillager(EntityType.ZOMBIE_VILLAGER, villager.level());
        
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
                SoundEvents.ZOMBIE_VILLAGER_CONVERTED, 
                SoundSource.HOSTILE, 1.0f, 1.0f);
        }
        
        // 给攻击者发送转化消息
        attacker.sendSystemMessage(Component.literal("§c你成功感染了一个村民！"));
    }
    
    /**
     * 检查玩家是否拥有僵尸契约效果
     */
    public static boolean hasZombieContract(Player player) {
        return player != null && zombieContractPlayers.contains(player.getUUID()) ;
    }
    
    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.translatable("black_souls_options.contracts.zombie.effect_title")
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FFFF"))));
        details.add(Component.translatable("black_souls_options.contracts.zombie.effect_subtitle")
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.zombie.effect1", (int)(CONVERSION_CHANCE * 100))
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.zombie.effect2")
                .withStyle(style -> style.withColor(TextColor.parseColor("#FF5555"))));
        details.add(Component.translatable("black_souls_options.contracts.zombie.effect3")
                .withStyle(style -> style.withColor(TextColor.parseColor("#FF5555"))));
        return details;
    }
}