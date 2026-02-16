package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.ContractDetector;
import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * 海龟契约效果 - 吸引敌对生物的仇恨
 * 玩家契约海龟后获得的能力：
 * 1. 附近的僵尸猪灵会主动攻击玩家
 * 2. 附近的豹猫会主动攻击玩家
 * 3. 附近的野生狼会主动攻击玩家
 */
public class TurtleContract extends ContractEffect {
    private static final String EFFECT_ID = "turtle_attract_hostile";
    private static final String DISPLAY_NAME = "black_souls_options.contracts.turtle.display_name";
    private static final String DESCRIPTION = "black_souls_options.contracts.turtle.description";
    
    // 吸引仇恨的范围（格数）
    private static final double VERTICAL_RANGE = 3.0;    // 垂直距离3格
    private static final double HORIZONTAL_RANGE = 24.0; // 水平切比雪夫距离24格
    
    // 检查间隔（tick数，20 tick = 1秒）
    private static final int CHECK_INTERVAL = 40; // 每2秒检查一次
    
    // 上次检查时间记录
    private long lastCheckTime = 0;

    public TurtleContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }

    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
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
            // 停用效果时发送消息
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
        
        // 检查是否应该进行仇恨吸引
        long currentTime = player.level().getGameTime();
        if (currentTime - lastCheckTime < CHECK_INTERVAL) {
            return;
        }
        
        lastCheckTime = currentTime;
        
        // 只有服务器端才处理仇恨逻辑
        if (player.level().isClientSide()) {
            return;
        }
        
        // 吸引附近生物的仇恨
        attractNearbyHostileMobs(player);
    }
    
    /**
     * 吸引附近敌对生物的仇恨
     */
    private void attractNearbyHostileMobs(Player player) {
        // 获取玩家周围的生物（使用切比雪夫距离）
        AABB searchArea = new AABB(
            player.getX() - HORIZONTAL_RANGE, 
            player.getY() - VERTICAL_RANGE, 
            player.getZ() - HORIZONTAL_RANGE,
            player.getX() + HORIZONTAL_RANGE, 
            player.getY() + VERTICAL_RANGE, 
            player.getZ() + HORIZONTAL_RANGE
        );
        
        List<LivingEntity> nearbyEntities = player.level().getEntitiesOfClass(
            LivingEntity.class, searchArea, entity -> isTargetEntity(entity)
        );
        
        for (LivingEntity entity : nearbyEntities) {
            if (entity instanceof Mob mob) {
                // 设置生物的目标为玩家
                mob.setTarget(player);
            }
        }
    }
    
    /**
     * 检查是否为需要吸引仇恨的生物
     */
    private boolean isTargetEntity(LivingEntity entity) {
        // 排除玩家自身
        if (entity instanceof Player) {
            return false;
        }
        
        // 僵尸猪灵
        if (entity instanceof ZombifiedPiglin) {
            return true;
        }

        // 豹猫
        if (entity instanceof Ocelot) {
            return true;
        }
        // 猫
        if (entity instanceof Cat) {
            return true;
        }
        
        // 野生狼（未驯服）
        if (entity instanceof Wolf wolf && !wolf.isTame()) {
            return true;
        }
        
        return false;
    }

    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.translatable("black_souls_options.contracts.turtle.effect_title").withStyle(style -> style.withColor(TextColor.parseColor("#55FFFF"))));
        details.add(Component.translatable("black_souls_options.contracts.turtle.effect1").withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        return details;
    }

    @Override
    public CompoundTag saveToNBT() {
        CompoundTag nbt = super.saveToNBT();
        // 不需要保存额外数据
        return nbt;
    }

    @Override
    public void loadFromNBT(CompoundTag nbt) {
        super.loadFromNBT(nbt);
        // 不需要加载额外数据
    }
}