package com.iamalittle.black_souls_options.contracts;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import com.iamalittle.black_souls_options.network.ContractNetworkHandler;
import java.util.*;
import java.io.File;

/**
 * 全局契约管理器，负责管理服务器端玩家的契约数据
 * 客户端契约数据由ClientContractManager单独管理，防止数据混乱
 */
public class GlobalContractManager {
    private static GlobalContractManager instance;
    private final Map<UUID, ContractManager> serverPlayerContractManagers;
    private MinecraftServer server;
    
    private GlobalContractManager() {
        this.serverPlayerContractManagers = new HashMap<>();
    }
    
    /**
     * 获取全局契约管理器实例
     */
    public static GlobalContractManager getInstance() {
        if (instance == null) {
            instance = new GlobalContractManager();
        }
        return instance;
    }
    
    /**
     * 设置服务器实例
     */
    public void setServer(MinecraftServer server) {
        this.server = server;
    }
    
    /**
     * 获取服务器端玩家的契约管理器（仅服务器端使用）
     */
    public ContractManager getServerContractManager(Player player) {
        if (player == null) {
            return null;
        }
        
        UUID playerUuid = player.getUUID();
        
        // 如果已有管理器，直接返回
        if (serverPlayerContractManagers.containsKey(playerUuid)) {
            return serverPlayerContractManagers.get(playerUuid);
        }
        
        // 关键修复：检查是否在服务器端执行
        if (player.level() == null || player.level().isClientSide()) {
            // 客户端：返回null，客户端应使用ClientContractManager
            System.out.println("[BLACKSOULS] Warning: Attempted to get server ContractManager on client side");
            return null;
        }
        
        // 关键修复：检查玩家状态，避免为无效玩家创建管理器
        if (player.level().getServer() == null) {
            // 玩家无效，返回null或已存在的管理器（如果存在）
            if (serverPlayerContractManagers.containsKey(playerUuid)) {
                return serverPlayerContractManagers.get(playerUuid);
            }
            return null;
        }
        
        // 如果已有管理器，直接返回
        if (serverPlayerContractManagers.containsKey(playerUuid)) {
            return serverPlayerContractManagers.get(playerUuid);
        }
        
        // 创建新的契约管理器（仅在服务器端）
        ContractManager contractManager = new ContractManager(player);
        serverPlayerContractManagers.put(playerUuid, contractManager);
        
        // 向客户端发送契约数据同步
        if (player instanceof ServerPlayer) {
            ContractNetworkHandler.sendContractDataToPlayer((ServerPlayer) player, true);
        }
        
        System.out.println("[BLACKSOULS] Server ContractManager created for player: " + player.getScoreboardName());
        return contractManager;
    }
    
    /**
     * 获取玩家的契约管理器（兼容旧版本，推荐使用getServerContractManager）
     */
    public ContractManager getContractManager(Player player) {
        return getServerContractManager(player);
    }
    
    /**
     * 移除服务器端玩家的契约管理器（当玩家退出游戏时）
     */
    public void removeServerContractManager(UUID playerUuid) {
        ContractManager manager = serverPlayerContractManagers.get(playerUuid);
        if (manager != null) {
            // 强制保存数据
            manager.forceSave();
            serverPlayerContractManagers.remove(playerUuid);
        }
    }
    
    /**
     * 移除玩家的契约管理器（兼容旧版本）
     */
    public void removeContractManager(UUID playerUuid) {
        removeServerContractManager(playerUuid);
    }
    
    /**
     * 获取所有服务器端玩家的契约管理器
     */
    public Collection<ContractManager> getAllServerContractManagers() {
        return Collections.unmodifiableCollection(serverPlayerContractManagers.values());
    }
    
    /**
     * 获取所有玩家的契约管理器（兼容旧版本）
     */
    public Collection<ContractManager> getAllContractManagers() {
        return getAllServerContractManagers();
    }
    
    /**
     * 定期保存所有服务器端玩家的契约数据
     */
    public void tick() {
        // 使用迭代器安全地遍历，避免并发修改异常
        Iterator<ContractManager> iterator = serverPlayerContractManagers.values().iterator();
        while (iterator.hasNext()) {
            ContractManager manager = iterator.next();
            
            // 关键修复：检查管理器中的玩家是否仍然有效
            if (manager.getOwner() == null || !manager.getOwner().isAlive()) {
                // 玩家已死亡或无效，移除管理器并保存数据
                manager.forceSave();
                iterator.remove();
                continue;
            }
            
            manager.tick();
        }
    }
    
    /**
     * 服务器关闭时保存所有数据
     */
    public void onServerStopping() {
        for (ContractManager manager : serverPlayerContractManagers.values()) {
            manager.forceSave();
        }
        serverPlayerContractManagers.clear();
    }
    
    /**
     * 获取所有契约文件路径
     */
    public List<File> getAllContractFiles() {
        List<File> files = new ArrayList<>();
        
        if (server != null) {
            File worldDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR).toFile();
            File contractsDir = new File(worldDir, "contracts");
            
            if (contractsDir.exists() && contractsDir.isDirectory()) {
                File[] contractFiles = contractsDir.listFiles((dir, name) -> name.endsWith(".dat"));
                if (contractFiles != null) {
                    files.addAll(Arrays.asList(contractFiles));
                }
            }
        }
        
        return files;
    }
}