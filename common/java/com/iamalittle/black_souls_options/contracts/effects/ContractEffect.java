package com.iamalittle.black_souls_options.contracts.effects;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
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
     * 设置激活状态（用于网络同步时设置状态而不激活效果）
     * @param active 是否激活
     */
    public void setActive(boolean active) {
        this.isActive = active;
    }
    
    /**
     * 激活效果
     * @param player 玩家
     */
    public void activate(Player player) {
        activate(player, true);
    }
    
    /**
     * 激活效果（可选择是否发送激活消息）
     * @param player 玩家
     * @param sendMessage 是否发送激活消息
     */
    public void activate(Player player, boolean sendMessage) {
        this.isActive = true;
        this.lastTickTime = System.currentTimeMillis();
        onActivate(player, sendMessage);
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
     * @param player 玩家
     * @param entityName 契约目标名称
     */
    protected void sendActivationMessage(Player player, String entityName) {
        // 关键修复：添加更严格的空指针检查
        if (player == null || !player.isAlive() || player.level() == null) {
            return;
        }
        
        Component message = buildMessageWithColoredEntityName(entityName, "§a契约效果激活！", "§a");
        player.sendSystemMessage(message);
    }
    
    /**
     * 发送停用提示消息
     * @param player 玩家
     * @param entityName 契约目标名称
     */
    protected void sendDeactivationMessage(Player player, String entityName) {
        // 关键修复：添加更严格的空指针检查
        if (player == null || !player.isAlive() || player.level() == null) {
            return;
        }
        
        Component message = buildMessageWithColoredEntityName(entityName, "§c契约效果停用", "§c");
        player.sendSystemMessage(message);
    }
    
    /**
     * 构建带颜色实体名称的消息
     * @param entityName 实体名称（可能是JSON格式或普通字符串）
     * @param suffix 消息后缀
     * @param messageColor 消息颜色代码
     * @return 构建好的消息组件
     */
    private Component buildMessageWithColoredEntityName(String entityName, String suffix, String messageColor) {
        // 尝试解析JSON格式的实体名称
        try {
            Component entityComponent = Component.Serializer.fromJson(entityName);
            if (entityComponent != null) {
                // 如果是JSON格式，构建完整消息：带颜色的实体名称 + 普通颜色的后缀
                return Component.literal("").append(entityComponent).append(Component.literal(suffix).withStyle(Component.literal("").getStyle().withColor(TextColor.parseColor(messageColor))));
            }
        } catch (Exception e) {
            // JSON解析失败，使用普通字符串
        }
        
        // 如果是普通字符串，直接构建消息
        return Component.literal(entityName + suffix).withStyle(Component.literal("").getStyle().withColor(TextColor.parseColor(messageColor)));
    }
    
    /**
     * 发送激活提示消息（向后兼容，使用效果名称）
     */
    protected void sendActivationMessage(Player player) {
        sendActivationMessage(player, displayName);
    }
    
    /**
     * 发送停用提示消息（向后兼容，使用效果名称）
     */
    protected void sendDeactivationMessage(Player player) {
        sendDeactivationMessage(player, displayName);
    }
    
    /**
     * 效果激活时调用
     * @param player 玩家
     * @param sendMessage 是否发送激活消息
     */
    protected abstract void onActivate(Player player, boolean sendMessage);
    
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
    
    /**
     * 客户端每tick更新效果
     * @param MC Minecraft实例
     * @param player 玩家
     */
    public void playerTick(Minecraft MC, Player player) {
        // 默认空实现，契约效果可以选择性地实现此方法
    }
}