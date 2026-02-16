package com.iamalittle.black_souls_options.contracts;

import net.minecraft.world.entity.player.Player;

import com.iamalittle.black_souls_options.config.BlackSoulsConfig;

/**
 * 契约管理器帮助类，用于正确区分客户端和服务器端的契约管理器使用
 */
public class ContractManagerHelper {
    
    /**
     * 获取适合当前环境的契约管理器
     * - 服务器端：使用GlobalContractManager的服务器端管理器
     * - 客户端：使用ClientContractManager的客户端管理器
     */
    public static ContractManager getAppropriateContractManager(Player player) {
        if (player == null) {
            return null;
        }
        
        // 检查是否在客户端
        if (player.level() != null && player.level().isClientSide()) {
            // 客户端：使用ClientContractManager
            return ClientContractManager.getInstance().getClientContractManager(player);
        } else {
            // 服务器端：使用GlobalContractManager
            return GlobalContractManager.getInstance().getServerContractManager(player);
        }
    }
    
    /**
     * 检查是否为客户端管理器
     */
    public static boolean isClientManager(ContractManager manager) {
        if (manager == null) {
            return false;
        }
        
        // 检查是否在ClientContractManager中
        return ClientContractManager.getInstance().isClientManager(manager);
    }
    
    /**
     * 检查是否为服务器端管理器
     */
    public static boolean isServerManager(ContractManager manager) {
        if (manager == null) {
            return false;
        }
        
        // 检查是否在GlobalContractManager中
        for (ContractManager serverManager : GlobalContractManager.getInstance().getAllServerContractManagers()) {
            if (serverManager == manager) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 同步服务器契约数据到客户端
     */
    public static void syncContractsToClient(Player player) {
        if (player == null || player.level() == null || !player.level().isClientSide()) {
            // 只能在客户端执行
            return;
        }
        
        // 获取服务器端的契约数据（通过网络同步）
        // 这里需要网络同步逻辑，暂时留空
        BlackSoulsConfig.debug("Contract sync requested for client player: " + player.getScoreboardName());
    }
    
    /**
     * 清理客户端契约数据（当客户端玩家退出时）
     */
    public static void cleanupClientContracts(Player player) {
        if (player == null) {
            return;
        }
        
        ClientContractManager.getInstance().removeClientContractManager(player.getUUID());
        BlackSoulsConfig.debug("Client contracts cleaned up for player: " + player.getScoreboardName());
    }
}