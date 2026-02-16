package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import java.util.*;

/**
 * 河豚契约效果 - 范围中毒
 * 玩家契约河豚后获得的能力：
 * 1. 靠近玩家1格内的生物目标会受到3秒的中毒效果
 * 2. 模仿河豚的防御机制：靠近时会释放毒素
 */
public class PufferfishContract extends ContractEffect {
    private static final String EFFECT_ID = "pufferfish_poison_aura";
    private static final String DISPLAY_NAME = "black_souls_options.contracts.pufferfish.display_name";
    private static final String DESCRIPTION = "black_souls_options.contracts.pufferfish.description";
    
    // 中毒持续时间（秒）
    private static final int POISON_DURATION = 3;
    
    // 中毒等级（0为基础等级）
    private static final int POISON_AMPLIFIER = 0;
    
    // 中毒范围（格数）
    private static final double POISON_RANGE = 1.8;
    
    // 河豚契约玩家集合
    private static final Set<UUID> pufferfishContractPlayers = new HashSet<>();
    
    public PufferfishContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }

    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            pufferfishContractPlayers.add(player.getUUID());

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
            pufferfishContractPlayers.remove(player.getUUID());

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
        
        // 只在服务端处理
        if (player.level().isClientSide()) {
            return;
        }
        
        // 检查玩家周围1格内的生物目标
        checkNearbyEntities(player);
    }
    
    /**
     * 检查玩家周围1格内的生物目标并施加中毒效果
     */
    private void checkNearbyEntities(Player player) {
        // 获取玩家周围1格内的所有实体
        List<Entity> nearbyEntities = player.level().getEntities(player, 
            player.getBoundingBox().inflate(POISON_RANGE));
        
        for (Entity entity : nearbyEntities) {
            // 排除玩家自己
            if (entity == player) {
                continue;
            }
            
            // 只对生物实体施加中毒效果
            if (entity instanceof LivingEntity livingEntity && !(entity instanceof Player)) {
                // 检查距离是否在1格内
                double distance = player.distanceTo(entity);
                if (distance <= POISON_RANGE) {
                    // 施加中毒效果
                    applyPoisonEffect(livingEntity, player);
                }
            }
        }
    }
    
    /**
     * 对目标施加中毒效果
     */
    private void applyPoisonEffect(LivingEntity target, Player player) {
        // 创建中毒效果实例
        MobEffectInstance poisonEffect = new MobEffectInstance(
            MobEffects.POISON, 
            POISON_DURATION * 20, // 转换为tick数
            POISON_AMPLIFIER,
            false, // 不显示粒子效果
            true   // 显示图标
        );
        
        // 施加中毒效果
        target.addEffect(poisonEffect);
        
        // 显示中毒粒子效果（可选）
        if (target.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.ENTITY_EFFECT,
                target.getX(), target.getY() + 1, target.getZ(),
                10, 0.5, 0.5, 0.5, 0.1);
        }
    }

    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.translatable("black_souls_options.contracts.pufferfish.effect_title")
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FFFF"))));
        details.add(Component.translatable("black_souls_options.contracts.pufferfish.effect_subtitle")
                .withStyle(style -> style.withColor(TextColor.parseColor("#00FF00"))));
        details.add(Component.translatable("black_souls_options.contracts.pufferfish.effect1")
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.pufferfish.effect2", POISON_DURATION)
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.pufferfish.effect3")
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.pufferfish.effect4")
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        return details;
    }
}