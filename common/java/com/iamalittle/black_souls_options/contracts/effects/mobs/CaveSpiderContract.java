package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 洞穴蜘蛛契约效果 - 中毒
 * 玩家契约洞穴蜘蛛后获得的能力：
 * 1. 攻击目标时使其中毒
 * 2. 中毒效果持续时间和强度与洞穴蜘蛛相同
 */
public class CaveSpiderContract extends ContractEffect {
    private static final String EFFECT_ID = "cave_spider_poison";
    private static final String DISPLAY_NAME = "black_souls_options.contracts.cave_spider.display_name";
    private static final String DESCRIPTION = "black_souls_options.contracts.cave_spider.description";
    
    // 中毒持续时间（秒）
    private static final int POISON_DURATION = 10;
    
    // 中毒等级（0为基础等级）
    private static final int POISON_AMPLIFIER = 0;
    
    public CaveSpiderContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            caveSpiderContractPlayers.add(player.getUUID());
            
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
            caveSpiderContractPlayers.remove(player.getUUID());
            
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
        // 洞穴蜘蛛契约不需要每tick更新，中毒逻辑在攻击时处理
    }
    
    @Override
    public void playerTick(Minecraft minecraft, Player player) {
        if (player == null || !isActive()) return;
        
        // 检查玩家是否接触墙壁（水平方向有碰撞）
        if (!player.horizontalCollision) return;
        
        // 检查玩家是否向下看（视线向量的Y轴值小于0.2）
        Vec3 lookVec = player.getViewVector(0);
        if (lookVec.y >= 0.2) return;
        
        // 设置玩家垂直移动速度，实现爬墙效果
        player.setDeltaMovement(player.getDeltaMovement().x, 0.2, player.getDeltaMovement().z);
    }
    
    // 契约玩家集合（用于快速检查）
    private static final Set<UUID> caveSpiderContractPlayers = new HashSet<>();
    
    /**
     * 使目标中毒
     */
    public static void poisonTarget(Entity target, Player attacker) {
        if (target != null && attacker != null && attacker.isAlive() && hasCaveSpiderContract(attacker)) {
            // 检查目标是否可以被施加效果
            if (target instanceof net.minecraft.world.entity.LivingEntity livingTarget) {
                // 施加中毒效果
                MobEffectInstance poisonEffect = new MobEffectInstance(
                    MobEffects.POISON, 
                    POISON_DURATION * 20, // 转换为tick数
                    POISON_AMPLIFIER,
                    false, // 不显示粒子效果
                    true   // 显示图标
                );
                
                livingTarget.addEffect(poisonEffect);
            }
        }
    }
    
    /**
     * 检查玩家是否拥有洞穴蜘蛛契约效果
     */
    public static boolean hasCaveSpiderContract(Player player) {
        return player != null && caveSpiderContractPlayers.contains(player.getUUID());
    }
    
    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.translatable("black_souls_options.contracts.cave_spider.effect_title").withStyle(style -> style.withColor(TextColor.parseColor("#55FFFF"))));
        details.add(Component.translatable("black_souls_options.contracts.cave_spider.poison_attack").withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.cave_spider.attack_poison").withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.cave_spider.poison_duration", POISON_DURATION).withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.cave_spider.wall_climb").withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.cave_spider.climb_effect").withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        return details;
    }
}