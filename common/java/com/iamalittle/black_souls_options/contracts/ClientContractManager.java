package com.iamalittle.black_souls_options.contracts;

import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import java.util.*;

import com.iamalittle.black_souls_options.config.BlackSoulsConfig;

/**
 * 客户端专用契约管理器，用于管理玩家在客户端显示的契约数据
 * 防止单人游戏和服务器契约数据混乱
 */
public class ClientContractManager {
    private static ClientContractManager instance;
    private final Map<UUID, ContractManager> clientPlayerContractManagers;
    
    private ClientContractManager() {
        this.clientPlayerContractManagers = new HashMap<>();
        BlackSoulsConfig.debug("客户端契约管理器已初始化");
    }
    
    /**
     * 获取客户端契约管理器实例
     */
    public static ClientContractManager getInstance() {
        if (instance == null) {
            instance = new ClientContractManager();
        }
        return instance;
    }
    
    /**
     * 获取或创建客户端玩家的契约管理器（仅用于显示，不保存数据）
     */
    public ContractManager getClientContractManager(Player player) {
        if (player == null) {
            return null;
        }
        
        UUID playerUuid = player.getUUID();
        
        // 如果已有管理器，直接返回
        if (clientPlayerContractManagers.containsKey(playerUuid)) {
            return clientPlayerContractManagers.get(playerUuid);
        }
        
        // 创建只读的客户端契约管理器
        ContractManager clientManager = new ContractManager(player);
        clientPlayerContractManagers.put(playerUuid, clientManager);
        
        BlackSoulsConfig.debug("Client-only ContractManager created for player: " + player.getScoreboardName());
        return clientManager;
    }
    
    /**
     * 从服务器同步契约数据到客户端管理器
     */
    public void syncContractsFromServer(Player player, List<Contract> serverContracts) {
        if (player == null || serverContracts == null) {
            return;
        }
        
        ContractManager clientManager = getClientContractManager(player);
        
        // 关键修复：直接调用已修复的clearContracts方法，该方法已包含客户端判断逻辑
        // 避免玩家重生时收到"契约效果停用"消息
        if (clientManager != null) {
            clientManager.clearContracts();
            BlackSoulsConfig.debug("Client contracts cleared without deactivation messages");
        }
        
        // 添加服务器同步的契约数据
        for (Contract contract : serverContracts) {
            clientManager.addContractFromNetwork(contract);
        }
        
        BlackSoulsConfig.debug("Contract data synced from server to client for player: " + player.getScoreboardName());
    }
    
    /**
     * 移除客户端玩家的契约管理器（当客户端玩家退出时）
     */
    public void removeClientContractManager(UUID playerUuid) {
        ContractManager manager = clientPlayerContractManagers.get(playerUuid);
        if (manager != null) {
            // 停用所有契约效果（客户端效果）
            for (Contract contract : manager.getAllContracts()) {
                contract.deactivateEffects(manager.getOwner());
            }
            clientPlayerContractManagers.remove(playerUuid);
        }
        BlackSoulsConfig.debug("清理客户端契约数据");
    }
    
    /**
     * 检查是否为客户端管理器
     */
    public boolean isClientManager(ContractManager manager) {
        return clientPlayerContractManagers.containsValue(manager);
    }
    
    /**
     * 获取所有客户端契约管理器
     */
    public Collection<ContractManager> getAllClientContractManagers() {
        return Collections.unmodifiableCollection(clientPlayerContractManagers.values());
    }
    
    /**
     * 客户端tick更新（仅更新显示效果，不保存数据）
     */
    public void tick() {
        // 使用迭代器安全地遍历
        Iterator<ContractManager> iterator = clientPlayerContractManagers.values().iterator();
        while (iterator.hasNext()) {
            ContractManager manager = iterator.next();
            
            // 检查管理器中的玩家是否仍然有效
            if (manager.getOwner() == null || !manager.getOwner().isAlive()) {
                // 玩家已死亡或无效，移除管理器
                iterator.remove();
                continue;
            }
            
            // 客户端只更新效果，不保存数据
            manager.tick();
        }
        BlackSoulsConfig.debug("客户端契约管理器已关闭");
    }
}