package com.iamalittle.black_souls_options.network;

import com.iamalittle.black_souls_options.contracts.Contract;
import com.iamalittle.black_souls_options.contracts.ContractManager;
import com.iamalittle.black_souls_options.contracts.GlobalContractManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Collections;

/**
 * 契约数据同步数据包
 * 用于在服务器和客户端之间同步契约数据
 */
public class ContractSyncPacket {
    private final UUID playerUUID;
    private final List<ContractData> contracts;
    private final boolean fullSync;
    
    public ContractSyncPacket(UUID playerUUID, List<ContractData> contracts, boolean fullSync) {
        this.playerUUID = playerUUID;
        this.contracts = contracts;
        this.fullSync = fullSync;
    }
    
    public ContractSyncPacket(FriendlyByteBuf buf) {
        this.playerUUID = buf.readUUID();
        this.fullSync = buf.readBoolean();
        int contractCount = buf.readInt();
        this.contracts = new ArrayList<>();
        
        for (int i = 0; i < contractCount; i++) {
            ContractData contract = new ContractData(
                buf.readUUID(),
                buf.readUtf(),
                buf.readUtf(),
                new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                buf.readUtf(),
                buf.readLong(),
                buf.readBoolean(),
                buf.readBoolean() // 读取指令创建标识
            );
            
            // 读取效果信息
            int effectCount = buf.readInt();
            for (int j = 0; j < effectCount; j++) {
                String effectId = buf.readUtf();
                boolean isActive = buf.readBoolean();
                contract.addEffectInfo(effectId, isActive);
            }
            
            contracts.add(contract);
        }
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(playerUUID);
        buf.writeBoolean(fullSync);
        buf.writeInt(contracts.size());
        
        for (ContractData contract : contracts) {
            buf.writeUUID(contract.entityId);
            buf.writeUtf(contract.entityType);
            buf.writeUtf(contract.entityName);
            buf.writeDouble(contract.position.x);
            buf.writeDouble(contract.position.y);
            buf.writeDouble(contract.position.z);
            buf.writeUtf(contract.dimension);
            buf.writeLong(contract.creationTime);
            buf.writeBoolean(contract.isTracking);
            buf.writeBoolean(contract.isCommandCreated); // 写入指令创建标识
            
            // 写入效果信息
            buf.writeInt(contract.getEffectCount());
            for (ContractData.EffectInfo effect : contract.getEffects()) {
                buf.writeUtf(effect.effectId);
                buf.writeBoolean(effect.isActive);
            }
        }
    }
    
    public UUID getPlayerUUID() {
        return playerUUID;
    }
    
    public List<ContractData> getContracts() {
        return contracts;
    }
    
    public boolean isFullSync() {
        return fullSync;
    }
    
    /**
     * 契约数据传输对象
     */
    public static class ContractData {
        public final UUID entityId;
        public final String entityType;
        public final String entityName;
        public final Vec3 position;
        public final String dimension;
        public final long creationTime;
        public final boolean isTracking;
        public final boolean isCommandCreated; // 指令创建标识
        private final List<EffectInfo> effects;
        
        public ContractData(UUID entityId, String entityType, String entityName, 
                           Vec3 position, String dimension, long creationTime, boolean isTracking, boolean isCommandCreated) {
            this.entityId = entityId;
            this.entityType = entityType;
            this.entityName = entityName;
            this.position = position;
            this.dimension = dimension;
            this.creationTime = creationTime;
            this.isTracking = isTracking;
            this.isCommandCreated = isCommandCreated;
            this.effects = new ArrayList<>();
        }
        
        /**
         * 效果信息数据结构
         */
        public static class EffectInfo {
            public final String effectId;
            public final boolean isActive;
            
            public EffectInfo(String effectId, boolean isActive) {
                this.effectId = effectId;
                this.isActive = isActive;
            }
        }
        
        /**
         * 添加效果信息
         */
        public void addEffectInfo(String effectId, boolean isActive) {
            effects.add(new EffectInfo(effectId, isActive));
        }
        
        /**
         * 获取效果数量
         */
        public int getEffectCount() {
            return effects.size();
        }
        
        /**
         * 获取效果列表
         */
        public List<EffectInfo> getEffects() {
            return effects;
        }
        
        public static ContractData fromContract(Contract contract) {
            ContractData data = new ContractData(
                contract.getEntityId(),
                contract.getEntityType(),
                contract.getEntityName(),
                contract.getEntityPosition(),
                contract.getDimension(),
                contract.getCreationTime(),
                contract.isTracking(),
                contract.isCommandCreated() // 包含指令创建标识
            );
            
            // 添加效果信息
            for (com.iamalittle.black_souls_options.contracts.effects.ContractEffect effect : contract.getEffects()) {
                data.addEffectInfo(effect.getEffectId(), effect.isActive());
            }
            
            return data;
        }
        
        public Contract toContract() {
            Contract contract = new Contract(entityId, entityType, entityName, position, dimension);
            
            // 设置指令创建标识
            if (isCommandCreated) {
                contract.setCommandCreated(true);
            }
            
            // 设置效果状态
            for (EffectInfo effectInfo : effects) {
                com.iamalittle.black_souls_options.contracts.effects.ContractEffect effect = 
                    com.iamalittle.black_souls_options.contracts.effects.ContractEffectRegistry.getInstance().getEffect(effectInfo.effectId);
                
                if (effect != null) {
                    // 在添加效果前设置契约目标名称到效果数据中
                    effect.getEffectData().putString("contractEntityName", entityName);
                    contract.addEffect(effect);
                    
                    // 如果效果是激活状态，激活效果（不发送消息）
                    // 注意：这里不激活效果，因为客户端没有正确的玩家对象
                    // 效果激活将在客户端契约管理器处理时进行
                    if (effectInfo.isActive) {
                        effect.setActive(true); // 只设置激活状态，不实际激活
                    }
                }
            }
            
            return contract;
        }
    }
    
    /**
     * 创建契约数据包（用于服务器向客户端发送）
     */
    public static ContractSyncPacket createForPlayer(ServerPlayer player, boolean fullSync) {
        ContractManager manager = GlobalContractManager.getInstance().getServerContractManager(player);
        if (manager == null) {
            return new ContractSyncPacket(player.getUUID(), new ArrayList<>(), fullSync);
        }
        
        List<ContractData> contractData = new ArrayList<>();
        for (Contract contract : manager.getAllContracts()) {
            contractData.add(ContractData.fromContract(contract));
        }
        
        return new ContractSyncPacket(player.getUUID(), contractData, fullSync);
    }
}