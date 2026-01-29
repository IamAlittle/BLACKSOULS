package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import java.util.*;

/**
 * 恶魂契约效果 - 玩家周围有火球靠近时自动让火球静止在原地
 * 玩家契约恶魂后获得的能力：
 * 1. 自动检测玩家周围靠近的火球
 * 2. 将靠近的火球静止在原地2秒钟
 */
public class GhastContract extends ContractEffect {
    private static final String EFFECT_ID = "ghast_fireball_deflection";
    private static final String DISPLAY_NAME = "那是气球吗？";
    private static final String DESCRIPTION = "火球无法碰到玩家";
    
    // 恶魂契约玩家集合
    private static final Set<UUID> ghastContractPlayers = new HashSet<>();
    
    // 检测范围（格）
    private static final double DETECTION_RANGE = 10.0;

    
    public GhastContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            ghastContractPlayers.add(player.getUUID());
            
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
            ghastContractPlayers.remove(player.getUUID());
            
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
        // 关键修复：检查玩家状态，避免在玩家死亡或无效状态下执行
        if (player == null || !player.isAlive() || player.level() == null) {
            return;
        }
        
        // 客户端不执行逻辑
        if (player.level().isClientSide()) {
            return;
        }
        
        // 检测玩家周围10格内的火球
        List<Entity> fireballs = player.level().getEntitiesOfClass(Entity.class, 
                new AABB(player.getX() - DETECTION_RANGE, player.getY() - DETECTION_RANGE, player.getZ() - DETECTION_RANGE,
                         player.getX() + DETECTION_RANGE, player.getY() + DETECTION_RANGE, player.getZ() + DETECTION_RANGE));
        
        for (Entity fireball : fireballs) {
            // 检查是否为火球类型
            if (isFireball(fireball)) {
                // 计算火球到玩家的距离
                double distance = player.distanceTo(fireball);
                
                // 计算火球到玩家的方向向量
                Vec3 toPlayer = new Vec3(player.getX() - fireball.getX(), 
                                        player.getY() - fireball.getY(), 
                                        player.getZ() - fireball.getZ()).normalize();
                
                // 计算火球当前的速度方向
                Vec3 fireballVelocity = fireball.getDeltaMovement();
                
                // 如果火球正在靠近玩家（速度方向与到玩家方向夹角小于90度）
                if (fireballVelocity.dot(toPlayer) > 0) {
                    // 让火球静止在原地（设置速度为0）
                    fireball.setDeltaMovement(Vec3.ZERO);
                }
            }
        }
    }
    
    /**
     * 检查实体是否为火球
     */
    private boolean isFireball(Entity entity) {
        return entity instanceof LargeFireball || 
               entity instanceof Fireball ||
               entity instanceof SmallFireball ||
               entity instanceof DragonFireball ||
               (entity instanceof Projectile && entity.getType().toString().contains("fireball"));
    }
    

    
    /**
     * 检查玩家是否拥有恶魂契约效果
     */
    public static boolean hasGhastContract(Player player) {
        return player != null && ghastContractPlayers.contains(player.getUUID());
    }
    
    @Override
    protected long getTickInterval() {
        return 5; // 每5tick检测一次（0.25秒）
    }

    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal("§6恶魂契约效果："));
        details.add(Component.literal("§7- 自动静止周围" + (int)DETECTION_RANGE + "格内的火球"));
        details.add(Component.literal("§7- 烈焰人的火球不会命中"));
        return details;
    }
}