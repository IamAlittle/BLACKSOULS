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
 * 猪契约效果 - 基础契约
 * 玩家契约猪后获得的能力：
 * 暂无特殊效果
 */
public class PigContract extends ContractEffect {
    private static final String EFFECT_ID = "pig_basic";
    private static final String DISPLAY_NAME = "我是猪";
    private static final String DESCRIPTION = "我只是一只猪，你觉得我能干嘛";
    
    // 猪契约玩家集合
    private static final Set<UUID> pigContractPlayers = new HashSet<>();
    
    public PigContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            pigContractPlayers.add(player.getUUID());
            
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
            pigContractPlayers.remove(player.getUUID());
            
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
        // 猪契约不需要每tick更新
    }
    
    /**
     * 检查玩家是否拥有猪契约效果
     */
    public static boolean hasPigContract(Player player) {
        return player != null && pigContractPlayers.contains(player.getUUID());
    }
    
    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal("§6猪契约效果："));
        details.add(Component.literal("§7- 我是猪"));

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
}