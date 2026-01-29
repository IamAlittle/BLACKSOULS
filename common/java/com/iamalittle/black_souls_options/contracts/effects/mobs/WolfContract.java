package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.*;

/**
 * 狼契约效果 - 自动驯服周围的狼
 * 玩家契约狼后获得的能力：
 * 1. 自动驯服玩家周围的野生狼
 * 2. 驯服的狼会跟随玩家并保护玩家
 */
public class WolfContract extends ContractEffect {
    private static final String EFFECT_ID = "wolf_tame_wolves";
    private static final String DISPLAY_NAME = "狼群领袖";
    private static final String DESCRIPTION = "自动驯服周围的野生狼";
    
    // 驯服范围（格数）
    private static final double VERTICAL_RANGE = 5.0;    // 垂直距离5格
    private static final double HORIZONTAL_RANGE = 10.0; // 水平距离10格
    
    // 检查间隔（tick数，20 tick = 1秒）
    private static final int CHECK_INTERVAL = 100; // 每5秒检查一次
    
    // 每次检查最多驯服的狼数量
    private static final int MAX_TAME_PER_CHECK = 2;
    
    // 狼契约玩家集合
    private static final Set<UUID> wolfContractPlayers = new HashSet<>();
    
    // 上次检查时间记录
    private final Map<UUID, Long> lastCheckTimeMap = new HashMap<>();
    
    // 记录玩家驯服的狼数量
    private final Map<UUID, Integer> playerTamedWolvesMap = new HashMap<>();

    public WolfContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }

    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            wolfContractPlayers.add(player.getUUID());
            playerTamedWolvesMap.put(player.getUUID(), 0);
            
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
            wolfContractPlayers.remove(player.getUUID());
            lastCheckTimeMap.remove(player.getUUID());
            playerTamedWolvesMap.remove(player.getUUID());
            
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
        
        // 检查是否应该进行驯服
        long currentTime = player.level().getGameTime();
        long lastCheckTime = lastCheckTimeMap.getOrDefault(player.getUUID(), 0L);
        
        if (currentTime - lastCheckTime < CHECK_INTERVAL) {
            return;
        }
        
        lastCheckTimeMap.put(player.getUUID(), currentTime);
        
        // 只有服务器端才处理驯服逻辑
        if (player.level().isClientSide()) {
            return;
        }
        
        // 驯服附近的野生狼
        tameNearbyWolves(player);
    }
    
    /**
     * 驯服玩家附近的野生狼
     */
    private void tameNearbyWolves(Player player) {
        // 获取玩家周围的狼（使用切比雪夫距离）
        AABB searchArea = new AABB(
            player.getX() - HORIZONTAL_RANGE, 
            player.getY() - VERTICAL_RANGE, 
            player.getZ() - HORIZONTAL_RANGE,
            player.getX() + HORIZONTAL_RANGE, 
            player.getY() + VERTICAL_RANGE, 
            player.getZ() + HORIZONTAL_RANGE
        );
        
        List<Wolf> nearbyWolves = player.level().getEntitiesOfClass(
            Wolf.class, searchArea, this::isWildWolf
        );
        
        // 限制每次检查驯服的数量
        int tamedCount = 0;
        for (Wolf wolf : nearbyWolves) {
            if (tamedCount >= MAX_TAME_PER_CHECK) {
                break;
            }
            
            if (tameWolf(player, wolf)) {
                tamedCount++;
                
                // 更新玩家驯服的狼数量
                int currentCount = playerTamedWolvesMap.getOrDefault(player.getUUID(), 0);
                playerTamedWolvesMap.put(player.getUUID(), currentCount + 1);
            }
        }
    }
    
    /**
     * 检查是否为野生狼（未驯服）
     */
    private boolean isWildWolf(Wolf wolf) {
        return wolf != null && wolf.isAlive() && !wolf.isTame();
    }
    
    /**
     * 驯服狼
     */
    private boolean tameWolf(Player player, Wolf wolf) {
        if (wolf == null || !wolf.isAlive() || wolf.isTame()) {
            return false;
        }
        
        try {
            // 驯服狼
            wolf.tame(player);
            
            // 设置狼的健康状态
            wolf.setHealth(wolf.getMaxHealth());
            
            // 设置狼的坐姿（站立）
            wolf.setOrderedToSit(false);
            
            // 播放驯服音效
            player.level().playSound(null, wolf.getX(), wolf.getY(), wolf.getZ(), 
                SoundEvents.WOLF_AMBIENT, wolf.getSoundSource(), 1.0f, 1.0f);
            
            // 显示驯服消息
            player.sendSystemMessage(Component.literal("§a你驯服了一只狼！"));
            
            return true;
        } catch (Exception e) {
            System.err.println("[Wolf Contract] Failed to tame wolf: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查玩家是否拥有狼契约效果
     */
    public static boolean hasWolfContract(Player player) {
        return player != null && wolfContractPlayers.contains(player.getUUID());
    }
    
    /**
     * 获取玩家驯服的狼数量
     */
    public static int getTamedWolvesCount(Player player) {
        if (player == null) return 0;
        return wolfContractPlayers.contains(player.getUUID()) ? 
            wolfContractPlayers.size() : 0;
    }

    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal("§6狼契约效果："));
        details.add(Component.literal("§a- 自动驯服周围10格内的野生狼"));
        return details;
    }

    @Override
    public CompoundTag saveToNBT() {
        CompoundTag nbt = super.saveToNBT();
        // 保存玩家驯服的狼数量
        if (effectData != null && effectData.contains("playerUUID")) {
            UUID playerUUID = effectData.getUUID("playerUUID");
            int tamedCount = playerTamedWolvesMap.getOrDefault(playerUUID, 0);
            nbt.putInt("tamedWolvesCount", tamedCount);
        }
        return nbt;
    }

    @Override
    public void loadFromNBT(CompoundTag nbt) {
        super.loadFromNBT(nbt);
        // 加载玩家驯服的狼数量
        if (effectData != null && effectData.contains("playerUUID")) {
            UUID playerUUID = effectData.getUUID("playerUUID");
            if (nbt.contains("tamedWolvesCount")) {
                int tamedCount = nbt.getInt("tamedWolvesCount");
                playerTamedWolvesMap.put(playerUUID, tamedCount);
            }
        }
    }
}