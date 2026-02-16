package com.iamalittle.black_souls_options.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import java.util.UUID;

import com.iamalittle.black_souls_options.config.BlackSoulsConfig;

/**
 * 吐口水攻击网络包
 * 客户端通知服务端执行吐口水攻击
 */
public class SpitAttackPacket {
    
    private UUID playerId;
    
    public SpitAttackPacket(UUID playerId) {
        this.playerId = playerId;
    }
    
    public SpitAttackPacket(FriendlyByteBuf buf) {
        this.playerId = buf.readUUID();
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(playerId);
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    /**
     * 处理吐口水攻击包
     */
    public static void handle(SpitAttackPacket packet, ServerPlayer player) {
        if (player == null) {
            return;
        }
        
        // 验证玩家身份
        if (!player.getUUID().equals(packet.getPlayerId())) {
            BlackSoulsConfig.warn("Warning: Spit attack packet from wrong player");
            return;
        }
        
        // 在服务端执行吐口水攻击
        com.iamalittle.black_souls_options.contracts.effects.mobs.LlamaContract.performSpitAttack(player);
    }
    
    /**
     * 创建吐口水攻击包
     */
    public static SpitAttackPacket create(UUID playerId) {
        return new SpitAttackPacket(playerId);
    }
}