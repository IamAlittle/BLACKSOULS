package com.iamalittle.black_souls_options.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import java.util.UUID;

/**
 * 雪球攻击网络包
 * 客户端通知服务端执行雪球攻击
 */
public class SnowballAttackPacket {
    
    private UUID playerId;
    
    public SnowballAttackPacket(UUID playerId) {
        this.playerId = playerId;
    }
    
    public SnowballAttackPacket(FriendlyByteBuf buf) {
        this.playerId = buf.readUUID();
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(playerId);
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    /**
     * 处理雪球攻击包
     */
    public static void handle(SnowballAttackPacket packet, ServerPlayer player) {
        if (player == null) {
            return;
        }
        
        // 验证玩家身份
        if (!player.getUUID().equals(packet.getPlayerId())) {
            System.err.println("[BLACKSOULS] Warning: Snowball attack packet from wrong player");
            return;
        }
        
        // 在服务端执行雪球攻击
        com.iamalittle.black_souls_options.contracts.effects.mobs.SnowGolemContract.performSnowballAttack(player);
    }
    
    /**
     * 创建雪球攻击包
     */
    public static SnowballAttackPacket create(UUID playerId) {
        return new SnowballAttackPacket(playerId);
    }
}