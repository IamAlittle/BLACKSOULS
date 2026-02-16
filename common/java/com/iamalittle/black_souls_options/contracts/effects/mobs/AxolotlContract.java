package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import com.iamalittle.black_souls_options.network.ContractNetworkHandler;
import com.iamalittle.black_souls_options.network.FeignDeathSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.food.FoodData;

import java.util.*;

import com.iamalittle.black_souls_options.config.BlackSoulsConfig;

/**
 * 美西螈契约效果 - 装死与生命恢复
 * 玩家契约美西螈后，停止移动时会躺地上装死，同时获得生命恢复效果
 */
public class AxolotlContract extends ContractEffect {
    private static final String EFFECT_ID = "axolotl_feign_death";
    private static final String DISPLAY_NAME = "black_souls_options.contracts.axolotl.display_name";
    private static final String DESCRIPTION = "black_souls_options.contracts.axolotl.description";
    
    // 移动检测相关变量
    private BlockPos lastPosition = null; // 玩家上次位置
    private long stillnessStartTime = 0; // 静止开始时间
    private static final long STILLNESS_THRESHOLD = 2000; // 静止阈值（毫秒）
    private boolean isDetectingStillness = false; // 是否正在检测静止
    
    // 装死状态管理
    private boolean isFeigningDeath = false; // 是否正在装死
    private static final Set<UUID> feigningDeathPlayers = new HashSet<>(); // 正在装死的玩家集合
    private MobEffectInstance feignDeathRegenerationEffect = null; // 装死系统给予的生命恢复效果实例
    
    // 饥饿值消耗相关
    private long lastHungerTickTime = 0; // 上次饥饿值消耗时间
    private static final long HUNGER_TICK_INTERVAL = 2000; // 饥饿值消耗间隔（毫秒）
    
    public AxolotlContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            // 初始化位置
            lastPosition = player.blockPosition();
            stillnessStartTime = System.currentTimeMillis();
            
            // 如果之前是装死状态，恢复装死状态
            if (isFeigningDeath) {
                feigningDeathPlayers.add(player.getUUID());
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
            // 结束装死状态
            if (isFeigningDeath) {
                isFeigningDeath = false;
                feigningDeathPlayers.remove(player.getUUID());
                
                // 只移除装死系统给予的生命恢复效果，保留其他来源的效果
                if (feignDeathRegenerationEffect != null) {
                    removeSpecificEffect(player, feignDeathRegenerationEffect);
                    feignDeathRegenerationEffect = null;
                }
                
                // 关键修复：在服务器端同步装死状态结束
                if (!player.level().isClientSide) {
                    // 同步装死状态到所有客户端
                    syncFeignDeathState(player, false);
                    BlackSoulsConfig.debug("[BLACKSOULS] Feign death state ended due to contract deactivation: " + player.getScoreboardName());
                }
            }
            
            // 使用契约目标名称发送消息
            String entityName = effectData.getString("contractEntityName");
            if (entityName.isEmpty()) {
                entityName = displayName; // 回退到效果名称
            }
            sendDeactivationMessage(player, entityName);
        }
        // 重置移动检测状态（关键修复）
        lastPosition = null;
        stillnessStartTime = 0;
        isDetectingStillness = false;
    }
    
    @Override
    protected void onTick(Player player) {
        if (player == null || !player.isAlive() || player.level() == null) return;
        
        detectPlayerStillness(player);
        
        // 如果玩家正在装死，每秒消耗1点饥饿值
        if (isFeigningDeath) {
            consumeHunger(player);
            
            // 检查玩家是否受伤，受伤时结束装死
            checkPlayerDamage(player);
        }
    }
    
    /**
     * 检测玩家是否静止不动
     */
    private void detectPlayerStillness(Player player) {
        // 关键修复：只在服务器端执行装死状态检测
        // 避免客户端和服务器端状态不一致导致的同步问题
        if (player.level().isClientSide) {
            // 客户端只检查是否正在装死，不进行状态切换
            // 装死状态由服务器端同步到客户端
            return;
        }
        
        // 获取玩家当前位置
        BlockPos currentPos = player.blockPosition();
        
        // 检查位置是否变化
        if (lastPosition != null && currentPos.equals(lastPosition)) {
            // 位置未变化，检查静止时间
            long currentTime = System.currentTimeMillis();
            long stillnessDuration = currentTime - stillnessStartTime;
            
            if (stillnessDuration >= STILLNESS_THRESHOLD && !isDetectingStillness) {
                // 玩家静止超过阈值，触发检测
                isDetectingStillness = true;
                onPlayerStill(player);
            }
        } else {
            // 位置变化，重置静止状态
            lastPosition = currentPos;
            stillnessStartTime = System.currentTimeMillis();
            onPlayerMoving(player);
            isDetectingStillness = false;
        }
    }
    
    /**
     * 玩家静止不动时触发 - 开始装死
     */
    private void onPlayerStill(Player player) {
        // 检查玩家是否在地面上（不在水中、空中或梯子上）
        if (!isPlayerOnGround(player)) {
            // 玩家不在陆地上，不能装死
            if (!player.level().isClientSide) {
                player.displayClientMessage(Component.literal("§c必须在地上才能装死！"), true);
            }
            return;
        }
        
        if (!isFeigningDeath) {
            // 开始装死
            isFeigningDeath = true;
            feigningDeathPlayers.add(player.getUUID());
            
            // 给予生命恢复效果并保存效果实例
            feignDeathRegenerationEffect = new MobEffectInstance(MobEffects.REGENERATION, 100, 0);
            player.addEffect(feignDeathRegenerationEffect);
            
            // 关键修复：只在服务器端发送同步消息
            if (!player.level().isClientSide) {
                player.displayClientMessage(Component.literal("§a开始装死！获得生命恢复效果。"), true);
                // 同步装死状态到所有客户端
                syncFeignDeathState(player, true);
            }
            //System.out.println("[BLACKSOULS] Player " + player.getScoreboardName() + " started feigning death");
        }
    }
    
    /**
     * 检查玩家是否在地面上（不在水中、空中或梯子上）
     */
    private boolean isPlayerOnGround(Player player) {
        // 检查玩家是否在陆地上（不在水中、空中或梯子上）
        return player.onGround() && !player.isInWater() && !player.isInLava() && !player.onClimbable();
    }
    
    /**
     * 玩家开始移动时触发 - 结束装死
     */
    private void onPlayerMoving(Player player) {
        if (isFeigningDeath) {
            // 结束装死
            isFeigningDeath = false;
            feigningDeathPlayers.remove(player.getUUID());
            
            // 只移除装死系统给予的生命恢复效果，保留其他来源的效果
            if (feignDeathRegenerationEffect != null) {
                // 使用安全的方法移除特定效果实例
                removeSpecificEffect(player, feignDeathRegenerationEffect);
                feignDeathRegenerationEffect = null;
            }
            
            // 关键修复：只在服务器端发送同步消息
            if (!player.level().isClientSide) {
                player.displayClientMessage(Component.literal("§c结束装死！"), true);
                // 同步装死状态到所有客户端
                syncFeignDeathState(player, false);
            }
            //System.out.println("[BLACKSOULS] Player " + player.getScoreboardName() + " stopped feigning death");
        }
        
        if (isDetectingStillness) {
            player.displayClientMessage(Component.literal("§c玩家开始移动！"), true);
        }
    }
    
    /**
     * 消耗饥饿值（每秒1点）
     */
    private void consumeHunger(Player player) {
        long currentTime = System.currentTimeMillis();
        
        // 检查是否达到饥饿值消耗间隔
        if (currentTime - lastHungerTickTime >= HUNGER_TICK_INTERVAL) {
            lastHungerTickTime = currentTime;
            
            FoodData foodData = player.getFoodData();
            int currentFoodLevel = foodData.getFoodLevel();
            
            // 如果饥饿值大于0，减少1点饥饿值
            if (currentFoodLevel > 0) {
                foodData.setFoodLevel(currentFoodLevel - 1);
            }
        }
    }
    
    /**
     * 安全地移除特定的效果实例
     */
    private void removeSpecificEffect(Player player, MobEffectInstance effectToRemove) {
        // 获取玩家当前的所有效果
        Collection<MobEffectInstance> activeEffects = player.getActiveEffects();
        
        // 遍历效果列表，只移除与指定效果匹配的实例
        for (MobEffectInstance effect : activeEffects) {
            if (effect.getEffect() == effectToRemove.getEffect() && 
                effect.getAmplifier() == effectToRemove.getAmplifier()) {
                // 找到匹配的效果，移除它
                player.removeEffect(effect.getEffect());
                break;
            }
        }
    }
    
    /**
     * 检查玩家是否受伤，受伤时结束装死
     */
    private void checkPlayerDamage(Player player) {
        // 检查玩家是否受伤（生命值减少）
        if (player.hurtTime > 0) {
            // 玩家受伤，结束装死
            if (isFeigningDeath) {
                isFeigningDeath = false;
                feigningDeathPlayers.remove(player.getUUID());
                
                // 只移除装死系统给予的生命恢复效果，保留其他来源的效果
                if (feignDeathRegenerationEffect != null) {
                    removeSpecificEffect(player, feignDeathRegenerationEffect);
                    feignDeathRegenerationEffect = null;
                }
                
                if (!player.level().isClientSide) {
                    player.displayClientMessage(Component.literal("§c受伤了！装死状态被强制结束。"), true);
                }
            }
        }
    }
    
    /**
     * 检查玩家是否正在装死（供Mixin使用）
     */
    public static boolean isPlayerFeigningDeath(Player player) {
        return feigningDeathPlayers.contains(player.getUUID());
    }
    
    /**
     * 同步装死状态到所有客户端
     */
    private void syncFeignDeathState(Player player, boolean isFeigningDeath) {
        if (player.level().isClientSide) {
            return; // 只在服务器端执行
        }
        
        try {
            // 创建装死状态同步数据包
            FeignDeathSyncPacket packet = new FeignDeathSyncPacket(player.getUUID(), isFeigningDeath);
            
            // 通过契约网络处理器发送同步数据包
            ContractNetworkHandler.broadcastFeignDeathState(packet);
            
            BlackSoulsConfig.debug("[BLACKSOULS] Feign death state synced: " + player.getScoreboardName() + " -> " + isFeigningDeath);
        } catch (Exception e) {
            BlackSoulsConfig.error("[BLACKSOULS] Failed to sync feign death state: " + e.getMessage());
        }
    }
    
    /**
     * 客户端：处理接收到的装死状态同步
     */
    public static void handleFeignDeathSync(com.iamalittle.black_souls_options.network.FeignDeathSyncPacket packet) {
        // 在客户端更新装死状态
        if (packet.isFeigningDeath()) {
            feigningDeathPlayers.add(packet.getPlayerUUID());
        } else {
            feigningDeathPlayers.remove(packet.getPlayerUUID());
        }
        
        BlackSoulsConfig.debug("[BLACKSOULS] Feign death state updated on client: " + packet.getPlayerUUID() + " -> " + packet.isFeigningDeath());
    }
    
    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.translatable("black_souls_options.contracts.axolotl.effect_title").withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.parseColor("#55FFFF"))));
        details.add(Component.translatable("black_souls_options.contracts.axolotl.feign_death_effect").withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.axolotl.hunger_consumption").withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.axolotl.regeneration_effect").withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.axolotl.damage_cancellation").withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.parseColor("#FF5555"))));
        return details;
    }
    

    
    @Override
    public CompoundTag saveToNBT() {
        CompoundTag nbt = super.saveToNBT();
        // 保存移动检测相关数据
        if (lastPosition != null) {
            nbt.putInt("lastPosX", lastPosition.getX());
            nbt.putInt("lastPosY", lastPosition.getY());
            nbt.putInt("lastPosZ", lastPosition.getZ());
        }
        nbt.putLong("stillnessStartTime", stillnessStartTime);
        nbt.putBoolean("isDetectingStillness", isDetectingStillness);
        nbt.putBoolean("isFeigningDeath", isFeigningDeath);
        nbt.putLong("lastHungerTickTime", lastHungerTickTime);
        return nbt;
    }

    @Override
    public void loadFromNBT(CompoundTag nbt) {
        super.loadFromNBT(nbt);
        // 加载移动检测相关数据
        if (nbt.contains("lastPosX")) {
            int x = nbt.getInt("lastPosX");
            int y = nbt.getInt("lastPosY");
            int z = nbt.getInt("lastPosZ");
            lastPosition = new BlockPos(x, y, z);
        }
        if (nbt.contains("stillnessStartTime")) {
            stillnessStartTime = nbt.getLong("stillnessStartTime");
        }
        if (nbt.contains("isDetectingStillness")) {
            isDetectingStillness = nbt.getBoolean("isDetectingStillness");
        }
        if (nbt.contains("isFeigningDeath")) {
            isFeigningDeath = nbt.getBoolean("isFeigningDeath");
            if (isFeigningDeath) {

            }
        }
        if (nbt.contains("lastHungerTickTime")) {
            lastHungerTickTime = nbt.getLong("lastHungerTickTime");
        }
    }
}