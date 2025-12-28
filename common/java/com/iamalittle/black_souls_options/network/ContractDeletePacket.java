package com.iamalittle.black_souls_options.network;

import net.minecraft.network.FriendlyByteBuf;
import java.util.UUID;

/**
 * 契约删除数据包
 * 用于客户端向服务器发送契约删除请求
 */
public class ContractDeletePacket {
    private final UUID entityId;
    private final UUID playerId;
    
    public ContractDeletePacket(UUID entityId, UUID playerId) {
        this.entityId = entityId;
        this.playerId = playerId;
    }
    
    /**
     * 从字节缓冲区解码数据包
     */
    public ContractDeletePacket(FriendlyByteBuf buf) {
        this.entityId = buf.readUUID();
        this.playerId = buf.readUUID();
    }
    
    /**
     * 将数据包编码到字节缓冲区
     */
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(entityId);
        buf.writeUUID(playerId);
    }
    
    public UUID getEntityId() {
        return entityId;
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    @Override
    public String toString() {
        return "ContractDeletePacket{entityId=" + entityId + ", playerId=" + playerId + "}";
    }
}