package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.ContractDetector;
import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 赤足兽契约效果 - 岩浆行走
 * 玩家契约赤足兽后获得的能力：
 * 1. 可以在岩浆上行走
 * 2. 岩浆对玩家变为固体表面
 */
public class StriderContract extends ContractEffect {
    private static final String EFFECT_ID = "strider_lava_walking";
    private static final String DISPLAY_NAME = "岩浆行走";
    private static final String DESCRIPTION = "可以在岩浆上行走";
    
    // 移除静态Set存储，改为通过ContractManager检查契约状态

    public StriderContract() {
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
        // 不需要每tick执行操作，Mixin会处理碰撞形状修改
    }
    
    /**
     * 检查玩家是否拥有赤足兽契约效果
     */
    public static boolean hasStriderContract(Player player) {
        return player != null && ContractDetector.hasContract(player, "minecraft:strider");
    }
    
    /**
     * 检查玩家是否可以在岩浆上行走
     */
    public static boolean canWalkOnLava(Player player) {
        return hasStriderContract(player);
    }

    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal("§6赤足兽岩浆行走效果："));
        details.add(Component.literal("§a可以在岩浆上行走"));
        details.add(Component.literal("§a岩浆对玩家变为固体表面"));
        details.add(Component.literal("§7像赤足兽一样在岩浆中自由行走"));
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