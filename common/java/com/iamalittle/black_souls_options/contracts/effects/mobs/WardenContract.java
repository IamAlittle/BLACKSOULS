package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import com.iamalittle.black_souls_options.render.WardenBlockHighlighter;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * 监守者契约效果 - 振动感知发光
 * 使用Minecraft官方振动系统检测玩家周围的振动事件
 */
public class WardenContract extends ContractEffect {
    private static final String EFFECT_ID = "warden_vibration_glow";
    private static final String DISPLAY_NAME = "感知";
    private static final String DESCRIPTION = "使用振动系统感知周围环境，使振动源获得发光效果";
    
    // 监守者契约玩家集合
    private static final Map<UUID, WardenVibrationListener> wardenContractListeners = new HashMap<>();
    
    // 方块高亮渲染器 - 改为实例变量，每个玩家单独管理
    private WardenBlockHighlighter wardenBlockHighlighter;
    
    // 音效相关
    private static final int HEARTBEAT_INTERVAL = 2; // 心跳音效间隔（2秒）
    private static final int HEARTBEAT_INTERVAL_FAST = 1; // 快速心跳音效间隔（1秒）
    private static final int VIBRATION_THRESHOLD = 3; // 触发快速心跳的振动目标数量阈值
    private static final Map<UUID, Integer> heartbeatTimers = new HashMap<>(); // 心跳计时器
    private static final Map<UUID, Integer> vibrationCounters = new HashMap<>(); // 振动目标计数器
    
    // 发光效果持续时间（tick，约5秒）
    private static final int GLOWING_DURATION = 100;
    
    // 振动检测范围（格）
    private static final int VIBRATION_DETECTION_RANGE = 16;
    
    public WardenContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    // 属性修改相关
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("1e9b8c7d-6f5a-4b3c-8d2e-1f0a9b8c7d6e");
    private static final String SPEED_MODIFIER_NAME = "warden_contract_speed_boost";
    private static final double SPEED_BOOST_PERCENTAGE = 0.35; // 35%速度提升
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null && player.level() instanceof ServerLevel serverLevel) {
            // 创建振动监听器
            WardenVibrationListener listener = new WardenVibrationListener(player);
            wardenContractListeners.put(player.getUUID(), listener);
            
            // 初始化方块高亮渲染器 - 每个玩家单独实例化
            wardenBlockHighlighter = new WardenBlockHighlighter();
            
            // 启用方块高亮渲染
            wardenBlockHighlighter.setEnabled(true);
            wardenBlockHighlighter.update(player);
            
            // 给玩家施加黑暗和失明效果（不显示图标）
            applyDarknessAndBlindness(player);
            
            // 应用速度提升效果
            applySpeedBoost(player);
            
            // 开始心跳音效循环
            heartbeatTimers.put(player.getUUID(), 0);
            vibrationCounters.put(player.getUUID(), 0);
            
            // 立即播放一次心跳音效
            playHeartbeatSound(player);
            
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
        if (player != null && player.level() instanceof ServerLevel serverLevel) {
            WardenVibrationListener listener = wardenContractListeners.remove(player.getUUID());
            
            if (listener != null) {
                listener.cleanup();
            }
            
            // 禁用方块高亮渲染
            if (wardenBlockHighlighter != null) {
                wardenBlockHighlighter.setEnabled(false);
                wardenBlockHighlighter = null; // 释放引用
            }
            
            // 移除黑暗和失明效果
            removeDarknessAndBlindness(player);
            
            // 移除速度提升效果
            removeSpeedBoost(player);
            
            // 停止心跳音效循环
            heartbeatTimers.remove(player.getUUID());
            vibrationCounters.remove(player.getUUID());
            
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
        // 检查玩家是否存活且不在客户端
        if (player == null || !player.isAlive() || player.level().isClientSide()) {
            return;
        }
        
        // 检查并重新创建振动监听器
        WardenVibrationListener listener = wardenContractListeners.get(player.getUUID());
        if (listener == null) {
            listener = new WardenVibrationListener(player);
            wardenContractListeners.put(player.getUUID(), listener);
        } else {
            // 检查振动
            listener.checkVibrations();
        }
        
        // 防止黑暗和失明效果被移除 - 定期重新应用
        ensureDarknessAndBlindnessEffects(player);
        
        // 确保速度提升效果不会被移除
        ensureSpeedBoostEffect(player);
        
        // 处理心跳音效循环
        handleHeartbeatSound(player);
    }
    
    /**
     * 为触发振动的实体应用发光效果
     */
    private void applyGlowingToVibrationSources(List<LivingEntity> vibrationSources) {
        for (LivingEntity entity : vibrationSources) {
            // 应用发光效果
            MobEffectInstance glowEffect = new MobEffectInstance(
                MobEffects.GLOWING,
                GLOWING_DURATION,
                0, // 等级0
                false, // 不显示粒子
                false // 显示图标
            );
            
            entity.addEffect(glowEffect);
        }
    }
    
    /**
     * 播放振动检测效果 - 粒子从目标飞向玩家
     */
    private void playVibrationDetectionEffects(ServerLevel level, Vec3 sourcePos, Player player) {
        if (player == null) return;
        
        // 计算从振动源指向玩家的方向向量
        Vec3 playerPos = player.position();
        Vec3 direction = playerPos.subtract(sourcePos).normalize();
        
        // 在振动源位置播放振动粒子效果，粒子飞向玩家
        // 创建VibrationParticleOption，指定目标位置和持续时间
        VibrationParticleOption vibrationParticle = new VibrationParticleOption(
            new EntityPositionSource(player, player.getEyeHeight() * 0.6f), // 玩家躯干高度作为目标（眼睛高度的60%）
            20 // 粒子持续时间（tick）
        );
        
        level.sendParticles(
            vibrationParticle,
            sourcePos.x, sourcePos.y + 1, sourcePos.z,
            1, // 振动粒子通常只发射1个
            0.0, 0.0, 0.0, // 振动粒子不需要方向参数
            0.0 // 振动粒子不需要速度参数
        );
    }
    
    @Override
    protected long getTickInterval() {
        return 1000; // 每秒检查一次监听器状态
    }
    
    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal("§6监守者契约效果："));
        details.add(Component.literal("§7 感知"));
        details.add(Component.literal("§7- 获得黑暗和失明效果"));
        details.add(Component.literal("§7- 当检测到振动时，使振动源获得发光效果"));
        details.add(Component.literal("§7- 检测范围：" + VIBRATION_DETECTION_RANGE + "格"));
        details.add(Component.literal("§7- 激活时循环播放心跳声"));
        details.add(Component.literal("§7- 探测到目标时播放触手点击声"));
        details.add(Component.literal("§7- 被探测目标听到近距离音效"));
        details.add(Component.literal("§7- 获得35%移动速度提升"));
        details.add(Component.literal("§7- 失明状态下不可疾跑，看不见路不敢走很正常"));
        return details;
    }
    
    /**
     * 给玩家施加黑暗和失明效果（不显示图标）
     */
    private void applyDarknessAndBlindness(Player player) {
        if (player == null) return;
        
        // 施加黑暗效果（DARKNESS）- 无限持续时间，不显示粒子，不显示图标
        MobEffectInstance darknessEffect = new MobEffectInstance(
            MobEffects.DARKNESS,
            -1, // 无限持续时间
            0,  // 等级0
            false, // 不显示粒子
            false  // 不显示图标
        );
        
        // 施加失明效果（BLINDNESS）- 无限持续时间，不显示粒子，不显示图标
        MobEffectInstance blindnessEffect = new MobEffectInstance(
            MobEffects.BLINDNESS,
            -1, // 无限持续时间
            0,  // 等级0
            false, // 不显示粒子
            false  // 不显示图标
        );
        
        player.addEffect(darknessEffect);
        player.addEffect(blindnessEffect);
    }
    
    /**
     * 移除玩家的黑暗和失明效果
     */
    private void removeDarknessAndBlindness(Player player) {
        if (player == null) return;
        
        // 移除黑暗效果
        player.removeEffect(MobEffects.DARKNESS);
        
        // 移除失明效果
        player.removeEffect(MobEffects.BLINDNESS);
    }
    
    /**
     * 确保黑暗和失明效果不会被移除 - 定期检查并重新应用
     */
    private void ensureDarknessAndBlindnessEffects(Player player) {
        if (player == null) return;
        
        // 检查黑暗效果是否存在
        MobEffectInstance darknessEffect = player.getEffect(MobEffects.DARKNESS);
        if (darknessEffect == null || darknessEffect.getDuration() < 100) {
            // 重新应用黑暗效果
            MobEffectInstance newDarknessEffect = new MobEffectInstance(
                MobEffects.DARKNESS,
                -1, // 无限持续时间
                0,  // 等级0
                false, // 不显示粒子
                false  // 不显示图标
            );
            player.addEffect(newDarknessEffect);
        }
        
        // 检查失明效果是否存在
        MobEffectInstance blindnessEffect = player.getEffect(MobEffects.BLINDNESS);
        if (blindnessEffect == null || blindnessEffect.getDuration() < 100) {
            // 重新应用失明效果
            MobEffectInstance newBlindnessEffect = new MobEffectInstance(
                MobEffects.BLINDNESS,
                -1, // 无限持续时间
                0,  // 等级0
                false, // 不显示粒子
                false  // 不显示图标
            );
            player.addEffect(newBlindnessEffect);
        }
    }
    
    /**
     * 检查玩家是否拥有监守者契约效果
     */
    public static boolean hasWardenContract(Player player) {
        return player != null && wardenContractListeners.containsKey(player.getUUID());
    }
    
    /**
     * 简化的振动监听器
     * 使用定时检测替代复杂的GameEventListener实现
     */
    private class WardenVibrationListener {
        private final Player player;
        private long lastVibrationCheckTime = 0;
        
        public WardenVibrationListener(Player player) {
            this.player = player;
        }
        
        /**
         * 检查振动事件
         */
        public void checkVibrations() {
            if (player == null || !player.isAlive() || !(player.level() instanceof ServerLevel serverLevel)) {
                return;
            }
            
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastVibrationCheckTime >= 500) { // 每500ms检查一次
                detectAndProcessVibrations(serverLevel);
                lastVibrationCheckTime = currentTime;
            }
        }
        
        /**
         * 检测并处理玩家周围的振动
         */
        private void detectAndProcessVibrations(ServerLevel level) {
            // 检测玩家周围的移动实体
            AABB searchArea = new AABB(
                player.getX() - VIBRATION_DETECTION_RANGE,
                player.getY() - VIBRATION_DETECTION_RANGE,
                player.getZ() - VIBRATION_DETECTION_RANGE,
                player.getX() + VIBRATION_DETECTION_RANGE,
                player.getY() + VIBRATION_DETECTION_RANGE,
                player.getZ() + VIBRATION_DETECTION_RANGE
            );
            
            List<LivingEntity> movingEntities = level.getEntitiesOfClass(
                LivingEntity.class, searchArea,
                entity -> entity != player && 
                         entity.isAlive() && 
                         entity.getDeltaMovement().lengthSqr() > 0.01 // 检测移动
            );
            
            // 检测攻击动作的实体
            List<LivingEntity> attackingEntities = level.getEntitiesOfClass(
                LivingEntity.class, searchArea,
                entity -> entity != player && 
                         entity.isAlive() && 
                         entity.getLastHurtMobTimestamp() > level.getGameTime() - 20 // 最近20tick内攻击过
            );
            
            // 合并所有触发振动的实体
            List<LivingEntity> vibrationSources = new ArrayList<>();
            vibrationSources.addAll(movingEntities);
            vibrationSources.addAll(attackingEntities);
            
            // 如果检测到移动或攻击的实体，触发振动效果
            if (!vibrationSources.isEmpty()) {
                processVibration(level, vibrationSources);
            }
        }
        
        /**
         * 处理振动事件
         */
        private void processVibration(ServerLevel level, List<LivingEntity> vibrationSources) {
            // 只为触发振动的实体应用发光效果
            applyGlowingToVibrationSources(vibrationSources);
            
            // 播放振动检测效果
            for (LivingEntity source : vibrationSources) {
                playVibrationDetectionEffects(level, source.position(), player);
            }
            
            // 更新振动目标计数器
            vibrationCounters.put(player.getUUID(), vibrationSources.size());
            
            // 播放音效
            if (!vibrationSources.isEmpty()) {
                // 玩家听到触手点击音效（音量较小）
                playTendrilClicksSound(player);
                
                // 被探测到的目标听到近距离音效
                for (LivingEntity target : vibrationSources) {
                    playNearbyCloseSound(target);
                }
            }
        }
        
        public void cleanup() {
            // 清理资源
        }
    }
    
    /**
     * 处理心跳音效循环
     */
    private void handleHeartbeatSound(Player player) {
        if (player == null || !heartbeatTimers.containsKey(player.getUUID())) {
            return;
        }
        
        // 获取当前计时器值
        int currentTimer = heartbeatTimers.get(player.getUUID());
        currentTimer++;
        
        // 根据振动目标数量决定心跳间隔
        int currentVibrationCount = vibrationCounters.getOrDefault(player.getUUID(), 0);
        int heartbeatInterval = currentVibrationCount >= VIBRATION_THRESHOLD ? 
            HEARTBEAT_INTERVAL_FAST : HEARTBEAT_INTERVAL;
        
        // 检查是否到达播放间隔
        if (currentTimer >= heartbeatInterval) {
            playHeartbeatSound(player);
            currentTimer = 0; // 重置计时器
        }
        
        // 更新计时器
        heartbeatTimers.put(player.getUUID(), currentTimer);
    }
    
    /**
     * 播放心跳音效
     */
    private void playHeartbeatSound(Player player) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        
        // 生成随机音调（0.95-1.05之间轻微变化）
        float pitch = 0.95f + (float) Math.random() * 0.1f;
        
        // 播放entity.warden.heartbeat音效，带随机音调
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), 
            SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.0F, pitch);
    }
    
    /**
     * 播放触手点击音效（玩家探测到目标时）
     */
    private void playTendrilClicksSound(Player player) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        
        // 播放entity.warden.tendril_clicks音效，音量较小（0.3F）
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), 
            SoundEvents.WARDEN_TENDRIL_CLICKS, SoundSource.HOSTILE, 0.3F, 1.0F);
    }
    
    /**
     * 播放近距离音效（被探测到的目标听到）
     */
    private void playNearbyCloseSound(LivingEntity target) {
        if (target == null || target.level().isClientSide()) {
            return;
        }
        
        // 播放entity.warden.nearby_close音效
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(), 
            SoundEvents.WARDEN_NEARBY_CLOSE, SoundSource.HOSTILE, 1.0F, 1.0F);
    }
    
    /**
     * 应用速度提升效果
     */
    private void applySpeedBoost(Player player) {
        if (player == null) return;
        
        var speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute == null) return;
        
        // 检查修改器是否已经存在
        if (speedAttribute.getModifier(SPEED_MODIFIER_UUID) != null) {
            return; // 修改器已存在，不需要重复添加
        }
        
        // 创建速度属性修改器
        AttributeModifier speedModifier = new AttributeModifier(
            SPEED_MODIFIER_UUID,
            SPEED_MODIFIER_NAME,
            SPEED_BOOST_PERCENTAGE,
            AttributeModifier.Operation.MULTIPLY_TOTAL
        );
        
        // 应用速度提升
        speedAttribute.addPermanentModifier(speedModifier);
    }
    
    /**
     * 移除速度提升效果
     */
    private void removeSpeedBoost(Player player) {
        if (player == null) return;
        
        // 移除速度属性修改器
        player.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(SPEED_MODIFIER_UUID);
    }
    
    /**
     * 确保速度提升效果不会被移除 - 定期检查并重新应用
     */
    private void ensureSpeedBoostEffect(Player player) {
        if (player == null) return;
        
        // 检查速度提升效果是否存在
        var speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute != null && speedAttribute.getModifier(SPEED_MODIFIER_UUID) == null) {
            // 重新应用速度提升效果
            applySpeedBoost(player);
        }
    }
}