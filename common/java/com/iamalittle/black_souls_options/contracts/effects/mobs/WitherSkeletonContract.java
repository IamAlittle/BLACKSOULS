package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.*;

/**
 * 凋零骷髅契约效果 - 攻击造成凋零效果
 * 玩家攻击时有一定概率对目标施加凋零效果
 */
public class WitherSkeletonContract extends ContractEffect {
    private static final String EFFECT_ID = "wither_skeleton_wither";
    private static final String DISPLAY_NAME = "black_souls_options.contracts.wither_skeleton.display_name";
    private static final String DESCRIPTION = "black_souls_options.contracts.wither_skeleton.description";
    
    // 凋零效果参数
    private static final int WITHER_DURATION = 100; // 凋零持续时间（tick，约5秒）
    private static final int WITHER_AMPLIFIER = 0;  // 凋零效果等级
    private static final double WITHER_CHANCE = 0.3; // 触发概率（30%）
    
    // 凋零骷髅契约玩家集合
    private static final Set<UUID> witherSkeletonContractPlayers = new HashSet<>();
    
    public WitherSkeletonContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            witherSkeletonContractPlayers.add(player.getUUID());
            
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
            witherSkeletonContractPlayers.remove(player.getUUID());
            
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
        // 凋零骷髅契约不需要tick逻辑，攻击时触发效果
    }
    
    /**
     * 对目标施加凋零效果
     * @param target 目标实体
     * @param attacker 攻击者（玩家）
     */
    public static void applyWitherToTarget(Entity target, Player attacker) {
        if (target != null && attacker != null && attacker.isAlive() && hasWitherSkeletonContract(attacker)) {
            // 检查触发概率
            if (Math.random() < WITHER_CHANCE) {
                // 确保目标是生物实体
                if (target instanceof LivingEntity livingTarget) {
                    // 施加凋零效果
                    MobEffectInstance witherEffect = new MobEffectInstance(
                        MobEffects.WITHER,
                        WITHER_DURATION,
                        WITHER_AMPLIFIER,
                        false, // 不显示粒子
                        true   // 显示图标
                    );
                    
                    livingTarget.addEffect(witherEffect);
                    
                    // 显示凋零粒子效果
                    if (livingTarget.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                            livingTarget.getX(), livingTarget.getY() + 1, livingTarget.getZ(),
                            5, 0.5, 0.5, 0.5, 0.1);
                    }
                }
            }
        }
    }
    
    /**
     * 检查玩家是否拥有凋零骷髅契约效果
     */
    public static boolean hasWitherSkeletonContract(Player player) {
        return player != null && witherSkeletonContractPlayers.contains(player.getUUID());
    }
    
    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.translatable("black_souls_options.contracts.wither_skeleton.effect_title")
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FFFF"))));
        details.add(Component.translatable("black_souls_options.contracts.wither_skeleton.effect_subtitle")
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.wither_skeleton.effect1", (int)(WITHER_CHANCE * 100))
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.wither_skeleton.effect2")
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.wither_skeleton.effect3")
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        return details;
    }
}