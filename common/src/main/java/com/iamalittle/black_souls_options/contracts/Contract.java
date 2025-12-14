package com.iamalittle.black_souls_options.contracts;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

import java.util.*;

public class Contract {
    
    private UUID playerId;
    private UUID targetEntityId;
    private EntityType<?> targetEntityType;
    private String targetEntityName;
    private List<ContractEffect> effects;
    private boolean active;
    
    public Contract(Player player, Entity targetEntity) {
        this.playerId = player.getUUID();
        this.targetEntityId = targetEntity.getUUID();
        this.targetEntityType = targetEntity.getType();
        this.targetEntityName = targetEntity.getName().getString();
        this.effects = new ArrayList<>();
        this.active = true;
    }
    
    public Contract(CompoundTag nbt) {
        this.playerId = nbt.getUUID("playerId");
        this.targetEntityId = nbt.getUUID("targetEntityId");
        this.targetEntityName = nbt.getString("targetEntityName");
        this.effects = new ArrayList<>();
        this.active = nbt.getBoolean("active");
        
        // 从NBT加载效果（简化实现）
        // 实际实现可能需要更复杂的序列化逻辑
    }
    
    public void addEffect(ContractEffect effect) {
        effects.add(effect);
    }
    
    public void removeEffect(ContractEffect effect) {
        effects.remove(effect);
    }
    
    public void activateEffects(Player player) {
        for (ContractEffect effect : effects) {
            effect.onActivate(player);
        }
        active = true;
    }
    
    public void deactivateEffects(Player player) {
        for (ContractEffect effect : effects) {
            effect.onDeactivate(player);
        }
        active = false;
    }
    
    public void tickEffects(Player player) {
        if (active) {
            for (ContractEffect effect : effects) {
                effect.onTick(player);
            }
        }
    }
    
    public CompoundTag save() {
        CompoundTag nbt = new CompoundTag();
        nbt.putUUID("playerId", playerId);
        nbt.putUUID("targetEntityId", targetEntityId);
        nbt.putString("targetEntityName", targetEntityName);
        nbt.putBoolean("active", active);
        
        // 保存效果数据（简化实现）
        // 实际实现可能需要更复杂的序列化逻辑
        
        return nbt;
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public UUID getTargetEntityId() {
        return targetEntityId;
    }
    
    public EntityType<?> getTargetEntityType() {
        return targetEntityType;
    }
    
    public String getTargetEntityName() {
        return targetEntityName;
    }
    
    public List<ContractEffect> getEffects() {
        return new ArrayList<>(effects);
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    @Override
    public String toString() {
        return "Contract{playerId=" + playerId + 
               ", targetEntityId=" + targetEntityId + 
               ", targetEntityType=" + targetEntityType + 
               ", targetEntityName='" + targetEntityName + "'" + 
               ", effects=" + effects.size() + 
               ", active=" + active + "}";
    }
}