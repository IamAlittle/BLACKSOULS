package com.iamalittle.black_souls_options.effects;

import net.minecraft.nbt.CompoundTag;
import java.util.UUID;

/**
 * 玩家死亡图腾数据，存储冷却状态和触发信息
 */
public class PlayerDeathTotemData {
    private final UUID playerUuid;
    private int remainingCooldownTicks;
    private boolean triggeredThisLife;
    private long lastSaveTime;
    private boolean needsSave;
    
    public PlayerDeathTotemData(UUID playerUuid) {
        this.playerUuid = playerUuid;
        this.remainingCooldownTicks = 0;
        this.triggeredThisLife = false;
        this.lastSaveTime = System.currentTimeMillis();
        this.needsSave = false;
    }
    
    public UUID getPlayerUuid() {
        return playerUuid;
    }
    
    public int getRemainingCooldownTicks() {
        return remainingCooldownTicks;
    }
    
    public void setRemainingCooldownTicks(int ticks) {
        this.remainingCooldownTicks = ticks;
        this.needsSave = true;
    }
    
    public boolean isTriggeredThisLife() {
        return triggeredThisLife;
    }
    
    public void setTriggeredThisLife(boolean triggered) {
        this.triggeredThisLife = triggered;
        this.needsSave = true;
    }
    
    public long getLastSaveTime() {
        return lastSaveTime;
    }
    
    public void setLastSaveTime(long time) {
        this.lastSaveTime = time;
    }
    
    public boolean needsSave() {
        return needsSave;
    }
    
    public void setNeedsSave(boolean needsSave) {
        this.needsSave = needsSave;
    }
    
    /**
     * 每刻更新冷却时间
     */
    public void tick() {
        if (remainingCooldownTicks > 0) {
            remainingCooldownTicks--;
            // 冷却时间变化时标记需要保存
            this.needsSave = true;
        }
    }
    
    /**
     * 更新冷却时间但不标记需要保存（用于离线玩家更新）
     */
    public void tickWithoutSave() {
        if (remainingCooldownTicks > 0) {
            remainingCooldownTicks--;
            // 离线玩家更新时不标记需要保存，由调用方决定是否保存
        }
    }
    
    /**
     * 检查是否在冷却中
     */
    public boolean isOnCooldown() {
        return remainingCooldownTicks > 0;
    }
    
    /**
     * 重置冷却状态（重生时调用）
     * 重置冷却时间
     */
    public void resetCooldownState() {
        this.remainingCooldownTicks = 0; // 死亡时重置冷却时间
        this.needsSave = true;
    }
    
    // triggerTotem方法已移除，直接使用setRemainingCooldownTicks方法设置冷却时间
    
    /**
     * 保存数据到NBT
     */
    public CompoundTag saveToNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putUUID("playerUuid", playerUuid);
        nbt.putInt("remainingCooldownTicks", remainingCooldownTicks);
        nbt.putBoolean("triggeredThisLife", triggeredThisLife);
        nbt.putLong("lastSaveTime", lastSaveTime);
        return nbt;
    }
    
    /**
     * 从NBT加载数据
     */
    public void loadFromNBT(CompoundTag nbt) {
        if (nbt.contains("remainingCooldownTicks")) {
            this.remainingCooldownTicks = nbt.getInt("remainingCooldownTicks");
        }
        if (nbt.contains("triggeredThisLife")) {
            this.triggeredThisLife = nbt.getBoolean("triggeredThisLife");
        }
        if (nbt.contains("lastSaveTime")) {
            this.lastSaveTime = nbt.getLong("lastSaveTime");
        }
        
        // 加载后标记为不需要立即保存
        this.needsSave = false;
    }
}