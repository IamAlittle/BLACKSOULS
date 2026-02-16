package com.iamalittle.black_souls_options.contracts;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
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
    private boolean isCommandCreated = false; // 是否为指令创建的契约
    
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
        this.isCommandCreated = false; // 默认不是指令创建的
    }
    
    /**
     * 设置是否为指令创建的契约
     */
    public void setCommandCreated(boolean isCommandCreated) {
        this.isCommandCreated = isCommandCreated;
    }
    
    /**
     * 检查是否为指令创建的契约
     */
    public boolean isCommandCreated() {
        return isCommandCreated;
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
    public void activateEffects(Player player) {
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
    public void deactivateEffects(Player player) {
        for (ContractEffect effect : effects) {
            effect.deactivate(player);
        }
    }
    
    /**
     * 静默停用所有契约效果（不发送停用消息）
     */
    public void deactivateEffectsSilently(Player player) {
        for (ContractEffect effect : effects) {
            // 直接设置效果为未激活状态，不调用deactivate方法
            effect.setActive(false);
        }
    }
    
    /**
     * 重新激活之前已激活的契约效果（不激活未激活的契约）
     * 关键修复：只重新激活之前已激活的契约效果，避免玩家手动关闭后又被重新激活的问题
     */
    public void reactivateActiveEffects(Player player) {
        // 关键修复：检查玩家状态，避免在玩家死亡或无效状态下执行
        if (player == null || !player.isAlive() || player.level() == null) {
            return;
        }
        
        for (ContractEffect effect : effects) {
            // 只重新激活之前已激活的契约效果
            if (effect.isActive()) {
                // 在激活前设置效果的目标名称信息
                effect.getEffectData().putString("contractEntityName", entityName);
                effect.activate(player, false); // 重新激活时不发送消息
            }
        }
    }
    
    /**
     * 更新所有契约效果
     */
    public void tickEffects(Player player) {
        for (ContractEffect effect : effects) {
            effect.tick(player);
        }
    }
    
    /**
     * 客户端playerTick效果更新
     */
    public void playerTickEffects(Minecraft minecraft, Player player) {
        for (ContractEffect effect : effects) {
            effect.playerTick(minecraft, player);
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
        tag.putBoolean("isCommandCreated", isCommandCreated); // 保存指令创建标识
        
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
        if (tag.contains("isCommandCreated")) {
            contract.isCommandCreated = tag.getBoolean("isCommandCreated");
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
                }
            }
        }
        
        return contract;
    }
}