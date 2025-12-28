package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 狐狸契约效果 - 浆果丛免疫
 * 玩家契约狐狸后获得的能力：
 * 1. 在浆果丛中不会减速
 * 2. 在浆果丛中不会受到伤害
 */
public class FoxContract extends ContractEffect {
    private static final String EFFECT_ID = "fox_berry_immunity";
    private static final String DISPLAY_NAME = "狐狸";
    private static final String DESCRIPTION = "浆果丛中不会减速和受伤";
    
    // 狐狸契约玩家集合
    private static final Set<UUID> foxContractPlayers = new HashSet<>();
    
    public FoxContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            foxContractPlayers.add(player.getUUID());
            
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
            foxContractPlayers.remove(player.getUUID());
            
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
        // 浆果丛免疫逻辑由Mixin处理
    }
    
    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal("§6狐狸契约效果："));
        details.add(Component.literal("§7- 在浆果丛中不会减速"));
        details.add(Component.literal("§7- 在浆果丛中不会受到伤害"));
        return details;
    }
    
    @Override
    public CompoundTag saveToNBT() {
        return super.saveToNBT();
    }
    
    @Override
    public void loadFromNBT(CompoundTag nbt) {
        super.loadFromNBT(nbt);
    }
    
    /**
     * 检查玩家是否拥有狐狸契约效果
     */
    public static boolean hasFoxContract(Player player) {
        return player != null && foxContractPlayers.contains(player.getUUID());
    }
}