package com.iamalittle.black_souls_options.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import java.util.UUID;

import com.iamalittle.black_souls_options.config.BlackSoulsConfig;

/**
 * 随机音效网络包
 * 客户端通知服务端执行随机音效播放
 */
public class RandomSoundPacket {
    
    private UUID playerId;
    
    public RandomSoundPacket(UUID playerId) {
        this.playerId = playerId;
    }
    
    public RandomSoundPacket(FriendlyByteBuf buf) {
        this.playerId = buf.readUUID();
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(playerId);
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    /**
     * 处理随机音效包
     */
    public static void handle(RandomSoundPacket packet, ServerPlayer player) {
        if (player == null) {
            return;
        }
        
        // 验证玩家身份
        if (!player.getUUID().equals(packet.getPlayerId())) {
            BlackSoulsConfig.warn("Warning: Random sound packet from wrong player");
            return;
        }
        
        // 在服务端执行随机音效播放
        com.iamalittle.black_souls_options.contracts.effects.mobs.ParrotContract.performRandomSound(player);
    }
    
    /**
     * 创建随机音效包
     */
    public static RandomSoundPacket create(UUID playerId) {
        return new RandomSoundPacket(playerId);
    }
}