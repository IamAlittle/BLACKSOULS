package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.ContractDetector;
import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * 恼鬼契约效果 - 使恼鬼无法攻击玩家
 * 玩家契约恼鬼后获得的能力：
 * 1. 附近的恼鬼不会攻击玩家
 * 2. 恼鬼会将玩家从攻击目标中移除
 */
public class VexContract extends ContractEffect {
    private static final String EFFECT_ID = "vex_immune";
    private static final String DISPLAY_NAME = "幽灵的庇护";
    private static final String DESCRIPTION = "使恼鬼无法攻击玩家";
    
    // 保护范围（格数）
    private static final double VERTICAL_RANGE = 8.0;    // 垂直距离8格
    private static final double HORIZONTAL_RANGE = 16.0; // 水平距离16格
    
    // 检查间隔（tick数，20 tick = 1秒）
    private static final int CHECK_INTERVAL = 40; // 每2秒检查一次
    
    // 上次检查时间记录
    private long lastCheckTime = 0;

    public VexContract() {
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
        
        // 检查是否应该进行保护检查
        long currentTime = player.level().getGameTime();
        if (currentTime - lastCheckTime < CHECK_INTERVAL) {
            return;
        }
        
        lastCheckTime = currentTime;
        
        // 只有服务器端才处理保护逻辑
        if (player.level().isClientSide()) {
            return;
        }
        
        // 保护玩家免受恼鬼攻击
        protectFromVex(player);
    }
    
    /**
     * 保护玩家免受恼鬼攻击
     */
    private void protectFromVex(Player player) {
        // 获取玩家周围的恼鬼
        AABB searchArea = new AABB(
            player.getX() - HORIZONTAL_RANGE, 
            player.getY() - VERTICAL_RANGE, 
            player.getZ() - HORIZONTAL_RANGE,
            player.getX() + HORIZONTAL_RANGE, 
            player.getY() + VERTICAL_RANGE, 
            player.getZ() + HORIZONTAL_RANGE
        );
        
        List<Vex> nearbyVexes = player.level().getEntitiesOfClass(
            Vex.class, searchArea, vex -> true
        );
        
        for (Vex vex : nearbyVexes) {
            // 如果恼鬼的目标是玩家，清除其目标
            if (vex.getTarget() == player) {
                vex.setTarget(null);
            }
            
            // 防止恼鬼将玩家设为目标
            if (vex.getLastHurtByMob() == player) {
                vex.setLastHurtByMob(null);
            }
            
            // 防止恼鬼将玩家设为攻击者
            if (vex.getLastHurtMob() == player) {
                vex.setLastHurtMob(null);
            }
        }
    }
    
    /**
     * 检查玩家是否拥有恼鬼契约效果
     */
    public static boolean hasVexContract(Player player) {
        return player != null && ContractDetector.hasContract(player, "minecraft:vex");
    }
    
    /**
     * 处理玩家受到伤害时的免疫检查
     * 如果伤害来源是恼鬼且玩家有恼鬼契约，则免疫伤害
     */
    public static boolean onPlayerHurt(Player player, DamageSource source) {
        if (player == null || source == null) return false;
        
        // 检查伤害来源是否是恼鬼
        Entity attacker = source.getEntity();
        if (attacker instanceof Vex && hasVexContract(player)) {
            // 免疫恼鬼的伤害
            return true;
        }
        
        return false;
    }

    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal("§6恼鬼契约效果："));
        details.add(Component.literal("§a- 水平范围：" + HORIZONTAL_RANGE + "格"));
        details.add(Component.literal("§a- 垂直范围：" + VERTICAL_RANGE + "格"));
        details.add(Component.literal("§a- 使范围内的恼鬼无法攻击玩家"));
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