package com.iamalittle.black_souls_options.contracts;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import java.util.*;
import java.io.File;

/**
 * 全局契约管理器，负责管理所有玩家的契约数据
 */
public class GlobalContractManager {
    private static GlobalContractManager instance;
    private final Map<UUID, ContractManager> playerContractManagers;
    private MinecraftServer server;
    
    private GlobalContractManager() {
        this.playerContractManagers = new HashMap<>();
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
     * 获取玩家的契约管理器
     */
    public ContractManager getContractManager(Player player) {
        UUID playerUuid = player.getUUID();
        
        // 如果已有管理器，直接返回
        if (playerContractManagers.containsKey(playerUuid)) {
            return playerContractManagers.get(playerUuid);
        }
        
        // 创建新的契约管理器
        ContractManager contractManager = new ContractManager(player);
        playerContractManagers.put(playerUuid, contractManager);
        
        return contractManager;
    }
    
    /**
     * 移除玩家的契约管理器（当玩家退出游戏时）
     */
    public void removeContractManager(UUID playerUuid) {
        ContractManager manager = playerContractManagers.get(playerUuid);
        if (manager != null) {
            // 强制保存数据
            manager.forceSave();
            playerContractManagers.remove(playerUuid);
        }
    }
    
    /**
     * 获取所有玩家的契约管理器
     */
    public Collection<ContractManager> getAllContractManagers() {
        return Collections.unmodifiableCollection(playerContractManagers.values());
    }
    
    /**
     * 定期保存所有玩家的契约数据
     */
    public void tick() {
        for (ContractManager manager : playerContractManagers.values()) {
            manager.tick();
        }
    }
    
    /**
     * 服务器关闭时保存所有数据
     */
    public void onServerStopping() {
        for (ContractManager manager : playerContractManagers.values()) {
            manager.forceSave();
        }
        playerContractManagers.clear();
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