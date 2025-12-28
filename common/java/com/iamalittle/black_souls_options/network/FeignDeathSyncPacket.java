package com.iamalittle.black_souls_options.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import java.util.UUID;

/**
 * 装死状态同步数据包
 * 用于在服务器和客户端之间同步玩家的装死状态
 */
public class FeignDeathSyncPacket {
    private final UUID playerUUID;
    private final boolean isFeigningDeath;
    
    public FeignDeathSyncPacket(UUID playerUUID, boolean isFeigningDeath) {
        this.playerUUID = playerUUID;
        this.isFeigningDeath = isFeigningDeath;
    }
    
    public FeignDeathSyncPacket(FriendlyByteBuf buf) {
        this.playerUUID = buf.readUUID();
        this.isFeigningDeath = buf.readBoolean();
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(playerUUID);
        buf.writeBoolean(isFeigningDeath);
    }
    
    public UUID getPlayerUUID() {
        return playerUUID;
    }
    
    public boolean isFeigningDeath() {
        return isFeigningDeath;
    }
    
    /**
     * 创建装死状态同步数据包
     */
    public static FeignDeathSyncPacket create(Player player, boolean isFeigningDeath) {
        return new FeignDeathSyncPacket(player.getUUID(), isFeigningDeath);
    }
}