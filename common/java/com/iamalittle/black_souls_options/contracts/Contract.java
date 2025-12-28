package com.iamalittle.black_souls_options.contracts;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/**
 * 契约类，用于存储与实体的契约信息
 */
public class Contract {
    private final UUID entityId;         // 实体UUID
    private final String entityType;     // 实体类型
    private final String entityName;     // 实体名称
    private Vec3 entityPosition;         // 实体当前位置（精确坐标）
    private final String dimension;      // 维度信息
    private final long creationTime;     // 创建时间
    private long lastUpdateTime;         // 最后更新时间
    private boolean canUpdatePosition;   // 是否可以更新位置（基于区块加载状态）
    private boolean isTracking;          // 是否正在追踪该契约目标
    private long lastCrossDimensionCheckTime; // 最后跨维度检测时间
    private final List<ContractEffect> effects; // 契约效果列表
    
    public Contract(UUID entityId, String entityType, String entityName, Vec3 position, String dimension) {
        this.entityId = entityId;
        this.entityType = entityType;
        this.entityName = entityName;
        this.entityPosition = position;
        this.dimension = dimension;
        this.creationTime = System.currentTimeMillis();
        this.lastUpdateTime = creationTime;
        this.canUpdatePosition = false; // 默认不更新位置
        this.isTracking = false;        // 默认不追踪
        this.lastCrossDimensionCheckTime = 0; // 默认未检测过跨维度
        this.effects = new ArrayList<>();
    }
    
    public UUID getEntityId() {
        return entityId;
    }
    
    public String getEntityType() {
        return entityType;
    }
    
    public String getEntityName() {
        return entityName;
    }
    
    public Vec3 getEntityPosition() {
        return entityPosition;
    }
    
    public void setEntityPosition(Vec3 position) {
        this.entityPosition = position;
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    public String getDimension() {
        return dimension;
    }
    
    public long getCreationTime() {
        return creationTime;
    }
    
    public long getLastUpdateTime() {
        return lastUpdateTime;
    }
    
    /**
     * 获取是否可以更新位置
     */
    public boolean canUpdatePosition() {
        return canUpdatePosition;
    }
    
    /**
     * 设置是否可以更新位置
     * @param canUpdate true表示可以更新（区块加载状态），false表示不更新（区块卸载状态）
     */
    public void setCanUpdatePosition(boolean canUpdate) {
        this.canUpdatePosition = canUpdate;
    }
    
    /**
     * 获取是否正在追踪该契约目标
     */
    public boolean isTracking() {
        return isTracking;
    }
    
    /**
     * 设置是否追踪该契约目标
     */
    public void setTracking(boolean tracking) {
        this.isTracking = tracking;
    }
    
    /**
     * 获取最后跨维度检测时间
     */
    public long getLastCrossDimensionCheckTime() {
        return lastCrossDimensionCheckTime;
    }
    
    /**
     * 设置最后跨维度检测时间
     */
    public void setLastCrossDimensionCheckTime(long time) {
        this.lastCrossDimensionCheckTime = time;
    }
    

    
    /**
     * 获取契约效果列表
     */
    public List<ContractEffect> getEffects() {
        return Collections.unmodifiableList(effects);
    }
    
    /**
     * 添加契约效果
     */
    public void addEffect(ContractEffect effect) {
        if (effect != null && !hasEffect(effect.getEffectId())) {
            effects.add(effect);
        }
    }
    
    /**
     * 移除契约效果
     */
    public void removeEffect(String effectId) {
        effects.removeIf(effect -> effect.getEffectId().equals(effectId));
    }
    
    /**
     * 检查是否有指定效果
     */
    public boolean hasEffect(String effectId) {
        return effects.stream().anyMatch(effect -> effect.getEffectId().equals(effectId));
    }
    
    /**
     * 获取指定效果
     */
    public ContractEffect getEffect(String effectId) {
        return effects.stream()
            .filter(effect -> effect.getEffectId().equals(effectId))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 获取契约的所有效果详细信息
     */
    public List<Component> getAllEffectDetails() {
        List<Component> allDetails = new ArrayList<>();
        for (ContractEffect effect : effects) {
            allDetails.addAll(effect.getEffectDetails());
        }
        return allDetails;
    }
    
    /**
     * 激活所有契约效果
     */
    public void activateEffects(net.minecraft.world.entity.player.Player player) {
        activateEffects(player, true);
    }
    
    /**
     * 激活所有契约效果（可选择是否发送激活消息）
     */
    public void activateEffects(net.minecraft.world.entity.player.Player player, boolean sendMessage) {
        for (ContractEffect effect : effects) {
            // 在激活前设置效果的目标名称信息
            effect.getEffectData().putString("contractEntityName", entityName);
            effect.activate(player, sendMessage);
        }
    }
    
    /**
     * 停用所有契约效果
     */
    public void deactivateEffects(net.minecraft.world.entity.player.Player player) {
        for (ContractEffect effect : effects) {
            effect.deactivate(player);
        }
    }
    
    /**
     * 重新激活之前已激活的契约效果（不激活未激活的契约）
     * 关键修复：不再重新激活任何效果，避免玩家手动关闭后又被重新激活的问题
     */
    public void reactivateActiveEffects(net.minecraft.world.entity.player.Player player) {
        // 关键修复：玩家重生时不再重新激活任何契约效果
        // 避免玩家手动关闭效果后又被重新激活的问题
        System.out.println("[BLACKSOULS] Contract effects reactivation skipped to prevent manual toggle issues");
        
        // 注释掉原有的重新激活逻辑
        /*
        for (ContractEffect effect : effects) {
            // 只重新激活之前已激活的契约效果
            if (effect.isActive()) {
                // 在激活前设置效果的目标名称信息
                effect.getEffectData().putString("contractEntityName", entityName);
                effect.activate(player, false); // 重新激活时不发送消息
            }
        }
        */
    }
    
    /**
     * 更新所有契约效果
     */
    public void tickEffects(net.minecraft.world.entity.player.Player player) {
        for (ContractEffect effect : effects) {
            effect.tick(player);
        }
    }
    
    /**
     * 将契约转换为NBT数据
     */
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("entityId", entityId);
        tag.putString("entityType", entityType);
        tag.putString("entityName", entityName);
        tag.putDouble("posX", entityPosition.x);
        tag.putDouble("posY", entityPosition.y);
        tag.putDouble("posZ", entityPosition.z);
        tag.putString("dimension", dimension);
        tag.putLong("creationTime", creationTime);
        tag.putLong("lastUpdateTime", lastUpdateTime);
        tag.putBoolean("canUpdatePosition", canUpdatePosition);
        tag.putBoolean("isTracking", isTracking);
        tag.putLong("lastCrossDimensionCheckTime", lastCrossDimensionCheckTime);
        
        // 保存契约效果
        ListTag effectsList = new ListTag();
        for (ContractEffect effect : effects) {
            CompoundTag effectTag = effect.saveToNBT();
            effectsList.add(effectTag);
        }
        tag.put("effects", effectsList);
        
        return tag;
    }
    
    /**
     * 从NBT数据创建契约
     */
    public static Contract fromNBT(CompoundTag tag) {
        UUID entityId = tag.getUUID("entityId");
        String entityType = tag.getString("entityType");
        String entityName = tag.getString("entityName");
        
        // 读取精确坐标
        double posX = tag.contains("posX", 6) ? tag.getDouble("posX") : tag.getLong("posX");
        double posY = tag.contains("posY", 6) ? tag.getDouble("posY") : tag.getLong("posY");
        double posZ = tag.contains("posZ", 6) ? tag.getDouble("posZ") : tag.getLong("posZ");
        
        Vec3 position = new Vec3(posX, posY, posZ);
        String dimension = tag.getString("dimension");
        
        Contract contract = new Contract(entityId, entityType, entityName, position, dimension);
        
        // 设置额外的时间信息
        if (tag.contains("lastUpdateTime")) {
            contract.lastUpdateTime = tag.getLong("lastUpdateTime");
        }
        if (tag.contains("canUpdatePosition")) {
            contract.canUpdatePosition = tag.getBoolean("canUpdatePosition");
        }
        if (tag.contains("isTracking")) {
            contract.isTracking = tag.getBoolean("isTracking");
        }
        if (tag.contains("lastCrossDimensionCheckTime")) {
            contract.lastCrossDimensionCheckTime = tag.getLong("lastCrossDimensionCheckTime");
        }
        
        // 加载契约效果
        if (tag.contains("effects")) {
            ListTag effectsList = tag.getList("effects", 10); // 10 = CompoundTag
            for (int i = 0; i < effectsList.size(); i++) {
                CompoundTag effectTag = effectsList.getCompound(i);
                String effectId = effectTag.getString("effectId");
                
                // 从注册表获取效果实例
                com.iamalittle.black_souls_options.contracts.effects.ContractEffect effect = 
                    com.iamalittle.black_souls_options.contracts.effects.ContractEffectRegistry.getInstance().getEffect(effectId);
                
                if (effect != null) {
                    effect.loadFromNBT(effectTag);
                    // 在添加效果前设置契约目标名称
                    effect.getEffectData().putString("contractEntityName", entityName);
                    contract.addEffect(effect);
                    // 关键修复：不要在NBT加载时自动重新激活效果
                    // 激活状态应该由玩家手动控制，而不是在数据加载时自动激活
                    // 这样可以确保玩家手动关闭的效果不会被错误地重新激活
                }
            }
        }
        
        return contract;
    }
}