package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import java.util.*;

/**
 * 山羊契约效果 - 视角朝地板时向前冲撞
 * 玩家视角朝地板时向前移动会对正前方的目标造成伤害并击飞击退
 * 必须包含向前动量，排除向后、向左、向右的向量
 */
public class GoatContract extends ContractEffect {
    private static final String EFFECT_ID = "goat_floor_charge";
    private static final String DISPLAY_NAME = "山羊冲撞";
    private static final String DESCRIPTION = "视角朝地板时向前移动对正前方目标造成伤害并击飞击退";
    
    // 存储玩家最后位置用于检测移动
    private static final Map<UUID, Vec3> playerLastPositions = new HashMap<>();
    
    // 冲撞参数
    private static final float CHARGE_DAMAGE = 2.0f; // 冲撞伤害
    private static final float KNOCKBACK_STRENGTH = 1.5f; // 击退强度
    private static final double CHARGE_RANGE = 2.0; // 冲撞检测范围
    private static final double MIN_VIEW_ANGLE = 0.0; // 最小视角角度（朝地板的角度阈值）
    private static final double MAX_VIEW_ANGLE = 20.0; // 最大视角角度（朝地板的角度阈值）
    private static final double MIN_MOVEMENT_SPEED = 0.1; // 最小移动速度阈值
    
    public GoatContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            // 记录玩家当前位置
            playerLastPositions.put(player.getUUID(), player.position());
            
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
            // 清理玩家位置记录
            playerLastPositions.remove(player.getUUID());
            
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
        
        // 客户端不执行逻辑
        if (player.level().isClientSide()) {
            return;
        }
        
        // 检查玩家是否视角朝地板
        if (isLookingAtFloor(player)) {
            // 检查玩家是否向前移动
            if (isMovingForward(player)) {
                // 执行冲撞攻击
                performChargeAttack(player);
            }
        }
        
        // 更新玩家最后位置
        playerLastPositions.put(player.getUUID(), player.position());
    }
    
    /**
     * 检查玩家是否视角朝地板
     * 通过计算玩家视角与水平面的夹角来判断
     */
    private boolean isLookingAtFloor(Player player) {
        Vec3 lookVec = player.getLookAngle();
        
        // 计算视角向量与水平面的夹角
        double angle = Math.toDegrees(Math.acos(Math.abs(lookVec.y) / lookVec.length()));
        
        // 如果夹角在70度到90度之间，说明视角朝地板
        return angle >= MIN_VIEW_ANGLE && angle <= MAX_VIEW_ANGLE;
    }
    
    /**
     * 检查玩家是否向前移动
     * 通过比较当前位置和最后位置来计算移动方向和速度
     */
    private boolean isMovingForward(Player player) {
        Vec3 currentPos = player.position();
        Vec3 lastPos = playerLastPositions.get(player.getUUID());
        
        if (lastPos == null) {
            return false;
        }
        
        // 计算移动向量
        Vec3 movement = currentPos.subtract(lastPos);
        
        // 检查移动速度是否达到阈值
        if (movement.length() < MIN_MOVEMENT_SPEED) {
            return false;
        }
        
        // 获取玩家朝向向量（水平方向）
        Vec3 lookVec = player.getLookAngle();
        Vec3 forwardDirection = new Vec3(lookVec.x, 0, lookVec.z).normalize();
        
        // 计算移动方向（水平方向）
        Vec3 movementDirection = new Vec3(movement.x, 0, movement.z).normalize();
        
        // 检查移动方向是否与玩家朝向一致（向前移动）
        double dotProduct = movementDirection.dot(forwardDirection);
        
        // 如果点积大于0.7，说明是向前移动（排除向后、向左、向右的向量）
        return dotProduct > 0.7;
    }
    
    /**
     * 执行冲撞攻击
     */
    private void performChargeAttack(Player player) {
        // 播放冲撞音效
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), 
            SoundEvents.GOAT_HORN_PLAY, SoundSource.PLAYERS, 0.8f, 1.2f);
        
        // 获取玩家朝向（水平方向）
        Vec3 lookVec = player.getLookAngle();
        Vec3 forwardDirection = new Vec3(lookVec.x, 0, lookVec.z).normalize();
        
        // 检测前方敌人
        boolean hitEnemy = detectAndAttackEnemies(player, forwardDirection);
        
        if (hitEnemy) {
            // 如果击中敌人，给玩家一个小的反冲效果
            Vec3 knockback = forwardDirection.scale(-0.3).add(0, 0.2, 0);
            player.setDeltaMovement(knockback);

        }
    }
    
    /**
     * 检测并攻击前方敌人
     * @return 是否击中敌人
     */
    private boolean detectAndAttackEnemies(Player player, Vec3 direction) {
        boolean hitEnemy = false;
        
        // 计算检测范围
        Vec3 startPos = player.position();
        Vec3 endPos = startPos.add(direction.scale(CHARGE_RANGE));
        
        // 创建检测区域（前方扇形区域）
        AABB detectionBox = new AABB(
            Math.min(startPos.x, endPos.x) - 1.5,
            Math.min(startPos.y, endPos.y) - 1.5,
            Math.min(startPos.z, endPos.z) - 1.5,
            Math.max(startPos.x, endPos.x) + 1.5,
            Math.max(startPos.y, endPos.y) + 1.5,
            Math.max(startPos.z, endPos.z) + 1.5
        );
        
        // 获取范围内的所有实体
        List<LivingEntity> entities = player.level().getEntitiesOfClass(
            LivingEntity.class, detectionBox, entity -> 
                entity != player && 
                entity.isAlive() && 
                !entity.isInvulnerable() &&
                entity.isAttackable()
        );
        
        for (LivingEntity entity : entities) {
            // 检查实体是否在冲撞路径上
            if (isEntityInChargePath(player, entity, direction)) {
                // 对实体造成伤害
                DamageSource damageSource = player.damageSources().playerAttack(player);
                boolean wasHurt = entity.hurt(damageSource, CHARGE_DAMAGE);
                
                if (wasHurt) {
                    hitEnemy = true;
                    
                    // 计算击退方向（向前方击退）
                    Vec3 knockbackDirection = direction.normalize();
                    
                    // 应用击飞和击退效果
                    entity.setDeltaMovement(
                        knockbackDirection.x * KNOCKBACK_STRENGTH,
                        0.5 + (Math.random() * 0.3), // 向上击飞
                        knockbackDirection.z * KNOCKBACK_STRENGTH
                    );
                    
                    // 播放击中音效
                    player.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), 
                        SoundEvents.GOAT_RAM_IMPACT, SoundSource.NEUTRAL, 1.0f, 0.8f);
                }
            }
        }
        
        return hitEnemy;
    }
    
    /**
     * 检查实体是否在冲撞路径上
     */
    private boolean isEntityInChargePath(Player player, LivingEntity entity, Vec3 direction) {
        Vec3 playerPos = player.position();
        Vec3 entityPos = entity.position();
        
        // 计算实体到玩家冲撞路径的距离
        Vec3 entityToPlayer = entityPos.subtract(playerPos);
        Vec3 projected = direction.scale(entityToPlayer.dot(direction));
        
        // 计算垂直距离
        double perpendicularDistance = entityToPlayer.subtract(projected).length();
        
        // 检查实体是否在路径范围内（1.5格宽）
        return perpendicularDistance <= 1.5 && 
               projected.dot(direction) > 0 && // 在玩家前方
               projected.length() <= CHARGE_RANGE; // 在冲撞范围内
    }
    
    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal("§6山羊契约效果："));
        details.add(Component.literal("§7- 视角朝地板时向前移动触发"));
        details.add(Component.literal("§7- 对正前方目标造成" + CHARGE_DAMAGE + "点伤害"));
        details.add(Component.literal("§7- 造成伤害后击飞击退"));
        return details;
    }
    
    /**
     * 重写tick间隔，设置为100毫秒确保响应及时
     */
    @Override
    protected long getTickInterval() {
        return 100;
    }
    
    /**
     * 清理指定玩家的数据
     */
    public static void cleanupPlayerData(Player player) {
        if (player != null) {
            playerLastPositions.remove(player.getUUID());
        }
    }
}