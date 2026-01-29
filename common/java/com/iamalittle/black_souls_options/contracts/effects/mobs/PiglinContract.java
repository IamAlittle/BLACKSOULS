package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import com.iamalittle.black_souls_options.render.PiglinLovedHighlighter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 猪灵契约效果 - 透视piglin_loved标签的方块和物品
 * 玩家与猪灵建立契约后获得的能力：
 * 1. 透视附近的带有piglin_loved标签的方块，显示金色边框
 * 2. 穿墙可见piglin_loved物品轮廓
 * 3. 支持多种piglin_loved物品类型（金块、金锭、金质工具等）
 */
public class PiglinContract extends ContractEffect {
    private static final String EFFECT_ID = "piglin_loved_vision";
    private static final String DISPLAY_NAME = "守财奴";
    private static final String DESCRIPTION = "透视附近的piglin_loved物品，显示金色边框，穿墙可见";
    
    // 猪灵契约玩家集合
    private static final Set<UUID> piglinContractPlayers = new HashSet<>();
    
    // 高亮渲染器实例
    private PiglinLovedHighlighter piglinLovedHighlighter;
    
    public PiglinContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            piglinContractPlayers.add(player.getUUID());
            
            // 初始化高亮渲染器
            if (piglinLovedHighlighter == null) {
                piglinLovedHighlighter = new PiglinLovedHighlighter();
            }
            
            // 启用piglin_loved物品高亮
            piglinLovedHighlighter.setEnabled(true);
            
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
            piglinContractPlayers.remove(player.getUUID());
            
            // 禁用piglin_loved物品高亮
            if (piglinLovedHighlighter != null) {
                piglinLovedHighlighter.setEnabled(false);
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
        // 每tick更新piglin_loved物品高亮渲染
        if (player == null || !player.isAlive() || player.level() == null) return;
        
        if (hasPiglinContract(player)) {
            if (piglinLovedHighlighter != null) {
                piglinLovedHighlighter.update(player);
            }
        }
    }
    
    /**
     * 检查玩家是否拥有猪灵契约效果
     */
    public static boolean hasPiglinContract(Player player) {
        return player != null && piglinContractPlayers.contains(player.getUUID());
    }
    
    /**
     * 获取猪灵契约玩家集合（用于渲染器）
     */
    public static Set<UUID> getPiglinContractPlayers() {
        return new HashSet<>(piglinContractPlayers);
    }
    
    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal("§6穿墙可见猪灵守护的东西（GUARDED_BY_PIGLINS、PIGLIN_LOVED标签）"));
        details.add(Component.literal("§6镶金黑黑石过于常见，不显示"));
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