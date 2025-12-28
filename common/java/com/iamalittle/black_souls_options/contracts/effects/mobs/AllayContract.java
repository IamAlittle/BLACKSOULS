package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 悦灵契约效果 - 生命恢复与拾取范围
 * 玩家契约悦灵后可以获得每秒恢复生命值和增加拾取物品范围的效果
 */
public class AllayContract extends ContractEffect {
    private static final String EFFECT_ID = "allay_life_boost";
    private static final String DISPLAY_NAME = "悦灵祝福";
    private static final String DESCRIPTION = "每秒恢复1点生命值，吸取两格外的掉落物";
    
    // 拾取范围增加量
    private int pickupRangeBoost = 3;
    
    // 用于跟踪时间的计数器
    private long lastHealTime = 0;

    public AllayContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            // 激活时应用拾取范围提升效果
            applyPickupRangeBoost(player);
            // 使用契约目标名称发送消息（仅在需要时发送）
            if (sendMessage) {
                String entityName = effectData.getString("contractEntityName");
                if (entityName.isEmpty()) {
                    entityName = displayName; // 回退到效果名称
                }
                sendActivationMessage(player, entityName);
            }
            // 初始化最后治疗时间
            lastHealTime = System.currentTimeMillis();
        }
    }
    
    @Override
    protected void onDeactivate(Player player) {
        if (player != null) {
            // 停用效果时移除所有效果
            removePickupRangeBoost(player);
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
        if (player != null) {
            // 检查是否需要恢复生命值（每秒一次）
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastHealTime >= 1000) {
                healPlayer(player);
                lastHealTime = currentTime;
            }
            
            // 吸引周围的掉落物
            attractNearbyItems(player);
        }
    }
    
    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal("§b悦灵祝福效果："));
        details.add(Component.literal("§7每秒恢复1点生命值"));
        details.add(Component.literal("§7拾取物品范围增加" + pickupRangeBoost + "格"));
        return details;
    }
    
    /**
     * 应用拾取范围提升效果
     */
    private void applyPickupRangeBoost(Player player) {
        // 不需要额外的效果，掉落物吸引通过onTick实时检测
    }
    
    /**
     * 移除拾取范围提升效果
     */
    private void removePickupRangeBoost(Player player) {
        // 不需要移除效果，效果通过onTick实时检测
    }
    
    /**
     * 吸引周围的掉落物
     */
    private void attractNearbyItems(Player player) {
        if (player == null || !player.isAlive()) return;
        
        Level level = player.level();
        BlockPos playerPos = player.blockPosition();
        Vec3 playerCenter = player.getEyePosition();
        
        // 计算拾取范围（基础范围 + 提升量）
        double baseRange = 1.5; // Minecraft默认拾取范围
        double totalRange = baseRange + pickupRangeBoost;
        double totalRangeSquared = totalRange * totalRange;
        
        // 获取玩家周围的掉落物
        List<ItemEntity> nearbyItems = level.getEntitiesOfClass(ItemEntity.class, 
            player.getBoundingBox().inflate(totalRange), 
            itemEntity -> itemEntity != null && !itemEntity.hasPickUpDelay() && itemEntity.isAlive()
        );
        
        // 对每个掉落物施加吸引力
            for (ItemEntity itemEntity : nearbyItems) {
                Vec3 itemPos = itemEntity.position();
                
                // 计算距离
                double distanceSquared = playerCenter.distanceToSqr(itemPos);
                
                // 如果在吸引范围内
                if (distanceSquared <= totalRangeSquared && distanceSquared > 0.1) {
                    // 计算吸引力方向和强度
                    Vec3 direction = playerCenter.subtract(itemPos).normalize();
                    // 添加向上的分量，使掉落物有轻微上浮效果
                    direction = new Vec3(direction.x, Math.max(direction.y, 0.3), direction.z).normalize();
                    double strength = (1.0 - distanceSquared / totalRangeSquared) * 0.5; // 吸引力强度保持0.5
                    
                    // 施加吸引力
                    Vec3 velocity = itemEntity.getDeltaMovement();
                    Vec3 newVelocity = velocity.add(direction.scale(strength));
                    
                    // 提高最大速度限制
                    double maxSpeed = 2.0;
                    if (newVelocity.length() > maxSpeed) {
                        newVelocity = newVelocity.normalize().scale(maxSpeed);
                    }
                    
                    itemEntity.setDeltaMovement(newVelocity);
                    itemEntity.hasImpulse = true;
                }
            }
    }
    
    /**
     * 治疗玩家
     */
    private void healPlayer(Player player) {
        // 恢复1点生命值
        player.heal(1.0F);
    }
    
    /**
     * 获取拾取范围增加量
     */
    public int getPickupRangeBoost() {
        return pickupRangeBoost;
    }
    
    /**
     * 设置拾取范围增加量
     */
    public void setPickupRangeBoost(int boost) {
        this.pickupRangeBoost = Math.max(1, Math.min(10, boost)); // 限制在1-10格之间
    }
    
    @Override
    public CompoundTag saveToNBT() {
        CompoundTag nbt = super.saveToNBT();
        nbt.putInt("pickupRangeBoost", pickupRangeBoost);
        nbt.putLong("lastHealTime", lastHealTime);
        return nbt;
    }
    
    @Override
    public void loadFromNBT(CompoundTag nbt) {
        super.loadFromNBT(nbt);
        if (nbt.contains("pickupRangeBoost")) {
            pickupRangeBoost = nbt.getInt("pickupRangeBoost");
        }
        if (nbt.contains("lastHealTime")) {
            lastHealTime = nbt.getLong("lastHealTime");
        }
    }
}