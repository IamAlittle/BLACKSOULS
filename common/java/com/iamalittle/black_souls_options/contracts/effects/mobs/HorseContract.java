package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 马契约效果 - 自动上坡
 * 玩家与马、驴、骡建立契约后获得的能力：
 * 1. 可以像半砖和楼梯那样自动上坡
 */
public class HorseContract extends ContractEffect {
    private static final String EFFECT_ID = "horse_auto_step";
    private static final String DISPLAY_NAME = "black_souls_options.contracts.horse.display_name";
    private static final String DESCRIPTION = "black_souls_options.contracts.horse.description";
    
    // 马契约玩家集合
    private static final Set<UUID> horseContractPlayers = new HashSet<>();
    
    public HorseContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            horseContractPlayers.add(player.getUUID());
            
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
            horseContractPlayers.remove(player.getUUID());
            
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
        // 自动上坡逻辑由Mixin处理
    }
    
    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.translatable("black_souls_options.contracts.horse.effect_title")
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FFFF"))));
        details.add(Component.translatable("black_souls_options.contracts.horse.effect1")
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
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
     * 检查玩家是否拥有马契约效果
     */
    public static boolean hasHorseContract(Player player) {
        return player != null && horseContractPlayers.contains(player.getUUID());
    }
}