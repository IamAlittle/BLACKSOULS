package com.iamalittle.black_souls_options.contracts.effects;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import java.util.List;

/**
 * 契约效果基类，所有契约效果都应该继承此类
 * 契约效果是玩家与目标建立契约后获得的能力
 */
public abstract class ContractEffect {
    protected final String effectId;        // 效果唯一标识符
    protected final String displayName;     // 显示名称
    protected final String description;     // 效果描述
    protected boolean isActive;             // 是否激活
    protected long lastTickTime;            // 最后tick时间
    protected CompoundTag effectData;       // 效果数据
    
    /**
     * 构造函数
     * @param effectId 效果唯一标识符
     * @param displayName 显示名称
     * @param description 效果描述
     */
    public ContractEffect(String effectId, String displayName, String description) {
        this.effectId = effectId;
        this.displayName = displayName;
        this.description = description;
        this.isActive = false;
        this.lastTickTime = 0;
        this.effectData = new CompoundTag();
    }
    
    /**
     * 获取效果唯一标识符
     */
    public String getEffectId() {
        return effectId;
    }
    
    /**
     * 获取显示名称
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 获取效果描述
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 是否激活
     */
    public boolean isActive() {
        return isActive;
    }
    
    /**
     * 激活效果
     * @param player 玩家
     */
    public void activate(Player player) {
        this.isActive = true;
        this.lastTickTime = System.currentTimeMillis();
        onActivate(player);
    }
    
    /**
     * 停用效果
     * @param player 玩家
     */
    public void deactivate(Player player) {
        this.isActive = false;
        onDeactivate(player);
    }
    
    /**
     * 每tick更新效果
     * @param player 玩家
     */
    public void tick(Player player) {
        if (!isActive) return;
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTickTime >= getTickInterval()) {
            onTick(player);
            lastTickTime = currentTime;
        }
    }
    
    /**
     * 获取tick间隔（毫秒）
     */
    protected long getTickInterval() {
        return 1000; // 默认1秒
    }
    
    /**
     * 获取效果详细信息，用于在契约界面显示
     */
    public abstract List<Component> getEffectDetails();
    
    /**
     * 发送激活提示消息
     */
    protected void sendActivationMessage(Player player) {
        if (player != null) {
            String message = displayName + "契约效果激活！";
            player.sendSystemMessage(Component.literal("§a" + message));
        }
    }
    
    /**
     * 发送停用提示消息
     */
    protected void sendDeactivationMessage(Player player) {
        if (player != null) {
            String message = displayName + "契约效果停用";
            player.sendSystemMessage(Component.literal("§c" + message));
        }
    }
    
    /**
     * 效果激活时调用
     */
    protected abstract void onActivate(Player player);
    
    /**
     * 效果停用时调用
     */
    protected abstract void onDeactivate(Player player);
    
    /**
     * 效果tick时调用
     */
    protected abstract void onTick(Player player);
    
    /**
     * 保存效果数据到NBT
     */
    public CompoundTag saveToNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("effectId", effectId);
        tag.putBoolean("isActive", isActive);
        tag.putLong("lastTickTime", lastTickTime);
        tag.put("effectData", effectData.copy());
        return tag;
    }
    
    /**
     * 从NBT加载效果数据
     */
    public void loadFromNBT(CompoundTag tag) {
        if (tag.contains("isActive")) {
            this.isActive = tag.getBoolean("isActive");
        }
        if (tag.contains("lastTickTime")) {
            this.lastTickTime = tag.getLong("lastTickTime");
        }
        if (tag.contains("effectData")) {
            this.effectData = tag.getCompound("effectData");
        }
    }
    
    /**
     * 获取效果数据
     */
    public CompoundTag getEffectData() {
        return effectData;
    }
    
    /**
     * 设置效果数据
     */
    public void setEffectData(CompoundTag effectData) {
        this.effectData = effectData;
    }
}