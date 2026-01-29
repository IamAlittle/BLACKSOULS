package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import java.util.*;

/**
 * 鸡契约效果 - 缓降
 * 玩家契约鸡后获得的能力：
 * 1. 距离地面超过3格时获得缓降效果
 * 2. 跳跃提升buff每等级增加1格计算高度
 * 3. 落地后缓降效果消失
 */
public class ChickenContract extends ContractEffect {
    private static final String EFFECT_ID = "chicken_slow_falling";
    private static final String DISPLAY_NAME = "鸡";
    private static final String DESCRIPTION = "高处下落时获得缓降效果";
    
    // 基础触发高度（格）
    private static final int BASE_TRIGGER_HEIGHT = 3;
    
    // 缓降效果持续时间（秒）
    private static final int SLOW_FALLING_DURATION = 10;
    
    // 鸡契约玩家集合
    private static final Set<UUID> chickenContractPlayers = new HashSet<>();
    
    // 记录玩家是否在空中（用于检测落地）
    private static final Map<UUID, Boolean> playerInAirMap = new HashMap<>();
    
    public ChickenContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            chickenContractPlayers.add(player.getUUID());
            playerInAirMap.put(player.getUUID(), false);
            
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
            chickenContractPlayers.remove(player.getUUID());
            playerInAirMap.remove(player.getUUID());
            
            // 移除缓降效果
            if (player.hasEffect(MobEffects.SLOW_FALLING)) {
                player.removeEffect(MobEffects.SLOW_FALLING);
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
        if (player == null || !player.isAlive() || player.level() == null) return;
        
        if (hasChickenContract(player)) {
            // 只在服务端执行
            if (!player.level().isClientSide()) {
                // 检查玩家是否在空中
                boolean isInAir = !player.onGround() && !player.isInWater() && !player.isPassenger();
                
                // 获取玩家当前的跳跃提升效果等级
                int jumpBoostLevel = getJumpBoostLevel(player);
                
                // 计算实际触发高度（基础高度 + 跳跃提升等级）
                int actualTriggerHeight = BASE_TRIGGER_HEIGHT + jumpBoostLevel;
                
                // 检查玩家是否高于触发高度
                boolean isAboveTriggerHeight = isAboveGround(player, actualTriggerHeight);
                
                // 获取玩家之前的状态
                boolean wasInAir = playerInAirMap.getOrDefault(player.getUUID(), false);
                
                if (isInAir && isAboveTriggerHeight) {
                    // 玩家在空中且高于触发高度，给予缓降效果
                    if (!player.hasEffect(MobEffects.SLOW_FALLING)) {
                        MobEffectInstance slowFalling = new MobEffectInstance(
                            MobEffects.SLOW_FALLING, 
                            SLOW_FALLING_DURATION * 20, // 转换为游戏刻（1秒=20刻）
                            0 // 基础等级
                        );
                        player.addEffect(slowFalling);
                    }
                    
                    // 更新状态为在空中
                    playerInAirMap.put(player.getUUID(), true);
                } else if (wasInAir && !isInAir) {
                    // 玩家从空中落地，移除缓降效果
                    if (player.hasEffect(MobEffects.SLOW_FALLING)) {
                        player.removeEffect(MobEffects.SLOW_FALLING);
                    }
                    
                    // 更新状态为在地面
                    playerInAirMap.put(player.getUUID(), false);
                } else if (!isInAir) {
                    // 玩家在地面，确保没有缓降效果
                    if (player.hasEffect(MobEffects.SLOW_FALLING)) {
                        player.removeEffect(MobEffects.SLOW_FALLING);
                    }
                    playerInAirMap.put(player.getUUID(), false);
                }
            }
        }
    }
    
    /**
     * 检查玩家是否高于地面指定高度
     */
    private static boolean isAboveGround(Player player, int height) {
        // 从玩家脚部位置向下检测
        double playerY = player.getY();
        
        // 检测玩家下方是否有方块
        for (int i = 1; i <= height; i++) {
            double checkY = playerY - i;
            if (player.level().getBlockState(player.blockPosition().atY((int)checkY)).isSolid()) {
                // 找到地面方块，计算当前高度
                double distanceToGround = playerY - checkY;
                return distanceToGround > height;
            }
        }
        
        // 如果向下检测height格都没有找到地面，说明玩家高于触发高度
        return true;
    }
    
    /**
     * 获取玩家的跳跃提升效果等级
     */
    private static int getJumpBoostLevel(Player player) {
        if (player.hasEffect(MobEffects.JUMP)) {
            MobEffectInstance jumpEffect = player.getEffect(MobEffects.JUMP);
            return jumpEffect != null ? jumpEffect.getAmplifier() + 1 : 0;
        }
        return 0;
    }
    
    /**
     * 检查玩家是否拥有鸡契约效果
     */
    public static boolean hasChickenContract(Player player) {
        return player != null && chickenContractPlayers.contains(player.getUUID());
    }
    @Override
    protected long getTickInterval() {
        return 100;
    }
    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal("§6鸡契约效果："));
        details.add(Component.literal("§7缓降"));
        details.add(Component.literal("§7- 距离地面超过" + BASE_TRIGGER_HEIGHT + "格时获得缓降效果"));
        details.add(Component.literal("§7- 落地后缓降效果消失"));
        return details;
    }
}