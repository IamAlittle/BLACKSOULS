package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import com.iamalittle.black_souls_options.render.ChestHighlighter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 海豚契约效果 - 透视箱子
 * 玩家与海豚建立契约后获得的能力：
 * 1. 透视附近的箱子，显示红色边框
 * 2. 穿墙可见箱子轮廓
 * 3. 支持多种容器类型（箱子、木桶、潜影盒等）
 */
public class DolphinContract extends ContractEffect {
    private static final String EFFECT_ID = "dolphin_chest_vision";
    private static final String DISPLAY_NAME = "通透了";
    private static final String DESCRIPTION = "透视附近的容器，显示红色边框，穿墙可见";
    
    // 海豚契约玩家集合
    private static final Set<UUID> dolphinContractPlayers = new HashSet<>();
    
    // 高亮渲染器实例
    private ChestHighlighter chestHighlighter;
    
    public DolphinContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            dolphinContractPlayers.add(player.getUUID());
            
            // 初始化高亮渲染器
            if (chestHighlighter == null) {
                chestHighlighter = new ChestHighlighter();
            }
            
            // 启用箱子高亮
            chestHighlighter.setEnabled(true);
            
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
            dolphinContractPlayers.remove(player.getUUID());
            
            // 禁用箱子高亮
            if (chestHighlighter != null) {
                chestHighlighter.setEnabled(false);
            }
            
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
        // 每tick更新箱子高亮渲染
        if (player == null || !player.isAlive() || player.level() == null) return;
        
        if (hasDolphinContract(player)) {
            if (chestHighlighter != null) {
                chestHighlighter.update(player);
            }
        }
    }
    
    /**
     * 检查玩家是否拥有海豚契约效果
     */
    public static boolean hasDolphinContract(Player player) {
        return player != null && dolphinContractPlayers.contains(player.getUUID());
    }
    
    /**
     * 获取海豚契约玩家集合（用于渲染器）
     */
    public static Set<UUID> getDolphinContractPlayers() {
        return new HashSet<>(dolphinContractPlayers);
    }
    
    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal("§a透视附近的箱子"));
        details.add(Component.literal("§a穿墙可见容器"));
        details.add(Component.literal("§a支持箱子、木桶、潜影盒等"));
        return details;
    }
    
    @Override
    public CompoundTag saveToNBT() {
        CompoundTag nbt = super.saveToNBT();
        // 不需要保存额外的数据
        return nbt;
    }
    
    @Override
    public void loadFromNBT(CompoundTag nbt) {
        super.loadFromNBT(nbt);
        // 不需要加载额外的数据
    }
}