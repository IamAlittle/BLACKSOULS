package com.iamalittle.black_souls_options.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import java.util.UUID;

import com.iamalittle.black_souls_options.config.BlackSoulsConfig;
import com.iamalittle.black_souls_options.contracts.ContractManager;
import com.iamalittle.black_souls_options.contracts.ContractManagerHelper;

/**
 * 杀害攻击网络包
 * 客户端通知服务端对目标实体执行攻击操作
 */
public class KillAttackPacket {
    
    private UUID playerId;
    private int targetEntityId;
    
    public KillAttackPacket(UUID playerId, int targetEntityId) {
        this.playerId = playerId;
        this.targetEntityId = targetEntityId;
    }
    
    public KillAttackPacket(FriendlyByteBuf buf) {
        this.playerId = buf.readUUID();
        this.targetEntityId = buf.readInt();
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(playerId);
        buf.writeInt(targetEntityId);
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public int getTargetEntityId() {
        return targetEntityId;
    }
    
    /**
     * 处理杀害攻击包
     */
    public static void handle(KillAttackPacket packet, ServerPlayer player) {
        if (player == null) {
            return;
        }
        
        // 验证玩家身份
        if (!player.getUUID().equals(packet.getPlayerId())) {
            BlackSoulsConfig.warn("Warning: Kill attack packet from wrong player");
            return;
        }
        
        // 获取目标实体
        Entity targetEntity = player.level().getEntity(packet.getTargetEntityId());
        if (targetEntity == null) {
            BlackSoulsConfig.debug("Target entity not found for kill attack: " + packet.getTargetEntityId());
            return;
        }
        
        // 检查配置是否启用秒杀模式
        if (BlackSoulsConfig.getInstance().isEnableInstantKill()) {
            // 直接秒杀模式：四重保险
            BlackSoulsConfig.debug("Instant kill mode enabled, performing instant kill on " + targetEntity.getName().getString());
            
            if (targetEntity instanceof LivingEntity livingTarget) {
                // 1. 造成虚空伤害
                livingTarget.hurt(player.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
                if (!livingTarget.isAlive()) {
                    BlackSoulsConfig.debug("Target died after void damage, skipping further steps");
                    return;
                }
                
                // 2. 设置目标血量为0
                livingTarget.setHealth(0.0F);
                if (!livingTarget.isAlive()) {
                    BlackSoulsConfig.debug("Target died after setting health to 0, skipping further steps");
                    return;
                }
                
                // 3. 确保实体被标记为死亡
                livingTarget.setRemoved(Entity.RemovalReason.KILLED);
                if (!livingTarget.isAlive()) {
                    BlackSoulsConfig.debug("Target died after marking as removed, skipping further steps");
                    return;
                }
            }
            
            // 4. 移除实体
            if (targetEntity.isAlive()) {
                targetEntity.remove(Entity.RemovalReason.KILLED);
                BlackSoulsConfig.debug("Target removed as final insurance");
            } else {
                BlackSoulsConfig.debug("Target already dead, skipping removal");
            }
            
            BlackSoulsConfig.debug("Instant kill completed on " + targetEntity.getName().getString());
        } else {
            // 普通攻击模式
            player.attack(targetEntity);
            BlackSoulsConfig.debug("Player " + player.getScoreboardName() + " performed kill attack on " + targetEntity.getName().getString());
        }
    }
    
    /**
     * 创建杀害攻击包
     */
    public static KillAttackPacket create(UUID playerId, int targetEntityId) {
        return new KillAttackPacket(playerId, targetEntityId);
    }
}