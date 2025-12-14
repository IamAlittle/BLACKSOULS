package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

/**
 * 美西螈契约效果 - 装死与生命恢复
 * 玩家契约美西螈后，停止移动时会躺地上装死，同时获得生命恢复效果
 */
public class AxolotlContract extends ContractEffect {
    private static final String EFFECT_ID = "axolotl_feign_death";
    private static final String DISPLAY_NAME = "美西螈装死";
    private static final String DESCRIPTION = "消耗饥饿值装死,期间消除敌对生物仇恨";
    
    // 移动检测相关变量
    private BlockPos lastPosition = null; // 玩家上次位置
    private long stillnessStartTime = 0; // 静止开始时间
    private static final long STILLNESS_THRESHOLD = 2000; // 静止阈值（毫秒）
    private boolean isDetectingStillness = false; // 是否正在检测静止
    
    public AxolotlContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player) {
        if (player != null) {
            // 初始化位置
            lastPosition = player.blockPosition();
            stillnessStartTime = System.currentTimeMillis();
            sendActivationMessage(player);
        }
    }
    
    @Override
    protected void onDeactivate(Player player) {
        if (player != null) {
            sendDeactivationMessage(player);
        }
        // 重置移动检测状态（关键修复）
        lastPosition = null;
        stillnessStartTime = 0;
        isDetectingStillness = false;
    }
    
    @Override
    protected void onTick(Player player) {
        if (player != null) {
            detectPlayerStillness(player);
        }
    }
    
    /**
     * 检测玩家是否静止不动
     */
    private void detectPlayerStillness(Player player) {
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
     * 玩家静止不动时触发
     */
    private void onPlayerStill(Player player) {
        // 这里可以添加玩家静止时的逻辑
        player.displayClientMessage(Component.literal("§a检测到玩家静止不动！"), true);
    }
    
    /**
     * 玩家开始移动时触发
     */
    private void onPlayerMoving(Player player) {
        // 这里可以添加玩家移动时的逻辑
        if (isDetectingStillness) {
            player.displayClientMessage(Component.literal("§c玩家开始移动！"), true);
        }
    }
    
    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal("§b美西螈契约效果："));
        details.add(Component.literal("§7基础契约效果"));
        details.add(Component.literal("§7- 检测玩家静止不动功能已启用"));
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
    }
}