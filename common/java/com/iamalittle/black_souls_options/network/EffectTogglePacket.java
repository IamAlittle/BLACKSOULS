package com.iamalittle.black_souls_options.network;

import net.minecraft.network.FriendlyByteBuf;
import java.util.UUID;

/**
 * 效果开关状态同步数据包
 * 用于客户端和服务器之间同步契约效果的开关状态
 */
public class EffectTogglePacket {
    private final UUID entityId;
    private final UUID playerId;
    private final boolean isActive;
    
    public EffectTogglePacket(UUID entityId, UUID playerId, boolean isActive) {
        this.entityId = entityId;
        this.playerId = playerId;
        this.isActive = isActive;
    }
    
    /**
     * 从字节缓冲区解码数据包
     */
    public EffectTogglePacket(FriendlyByteBuf buf) {
        this.entityId = buf.readUUID();
        this.playerId = buf.readUUID();
        this.isActive = buf.readBoolean();
    }
    
    /**
     * 将数据包编码到字节缓冲区
     */
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(entityId);
        buf.writeUUID(playerId);
        buf.writeBoolean(isActive);
    }
    
    public UUID getEntityId() {
        return entityId;
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    @Override
    public String toString() {
        return "EffectTogglePacket{entityId=" + entityId + ", playerId=" + playerId + ", isActive=" + isActive + "}";
    }
}