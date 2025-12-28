package com.iamalittle.black_souls_options.effects;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 死亡图腾数据管理器，负责保存和加载玩家的冷却数据
 */
public class DeathTotemDataManager {
    private static DeathTotemDataManager instance;
    private final Map<UUID, PlayerDeathTotemData> playerDataMap;
    private MinecraftServer server;
    
    private DeathTotemDataManager() {
        this.playerDataMap = new HashMap<>();
    }
    
    /**
     * 获取全局数据管理器实例
     */
    public static DeathTotemDataManager getInstance() {
        if (instance == null) {
            instance = new DeathTotemDataManager();
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
     * 获取玩家的死亡图腾数据
     */
    public PlayerDeathTotemData getPlayerData(Player player) {
        if (player == null || player.level() == null || player.level().getServer() == null) {
            // 玩家无效，返回null或已存在的数据（如果存在）
            if (player != null) {
                UUID playerUuid = player.getUUID();
                if (playerDataMap.containsKey(playerUuid)) {
                    return playerDataMap.get(playerUuid);
                }
            }
            return null;
        }
        
        UUID playerUuid = player.getUUID();
        
        // 如果已有数据，直接返回
        if (playerDataMap.containsKey(playerUuid)) {
            return playerDataMap.get(playerUuid);
        }
        
        // 创建新的数据对象并尝试从文件加载
        PlayerDeathTotemData playerData = new PlayerDeathTotemData(playerUuid);
        playerDataMap.put(playerUuid, playerData);
        
        // 从文件加载数据
        loadPlayerData(playerData);
        
        return playerData;
    }
    
    /**
     * 移除玩家的数据（当玩家退出游戏时）
     */
    public void removePlayerData(UUID playerUuid) {
        PlayerDeathTotemData playerData = playerDataMap.get(playerUuid);
        if (playerData != null) {
            // 强制保存数据
            savePlayerData(playerData);
            playerDataMap.remove(playerUuid);
        }
    }
    
    /**
     * 从文件加载玩家数据
     */
    private void loadPlayerData(PlayerDeathTotemData playerData) {
        File saveFile = getSaveFile(playerData.getPlayerUuid());
        if (!saveFile.exists()) {
            return;
        }
        
        try {
            CompoundTag rootTag = NbtIo.read(saveFile);
            if (rootTag != null) {
                playerData.loadFromNBT(rootTag);
            }
        } catch (IOException e) {
            System.err.println("Failed to load death totem data from file: " + saveFile.getAbsolutePath());
            e.printStackTrace();
        }
    }
    
    /**
     * 保存玩家数据到文件
     */
    public void savePlayerData(PlayerDeathTotemData playerData) {
        File saveFile = getSaveFile(playerData.getPlayerUuid());
        
        try {
            // 确保目录存在
            File parentDir = saveFile.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            CompoundTag rootTag = playerData.saveToNBT();
            NbtIo.write(rootTag, saveFile);
        } catch (IOException e) {
            System.err.println("Failed to save death totem data to file: " + saveFile.getAbsolutePath());
            e.printStackTrace();
        }
    }
    
    /**
     * 获取保存文件路径
     */
    private File getSaveFile(UUID playerUuid) {
        if (server != null) {
            File worldDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR).toFile();
            File deathTotemDir = new File(worldDir, "death_totem");
            return new File(deathTotemDir, playerUuid.toString() + ".dat");
        } else {
            // 服务器未设置时使用临时路径
            return new File("temp_death_totem", playerUuid.toString() + ".dat");
        }
    }
    
    /**
     * 定期保存所有玩家的数据
     */
    public void tick() {
        // 使用迭代器安全地遍历，避免并发修改异常
        Iterator<PlayerDeathTotemData> iterator = playerDataMap.values().iterator();
        while (iterator.hasNext()) {
            PlayerDeathTotemData playerData = iterator.next();
            
            // 更新冷却时间
            playerData.tick();
            
            // 检查是否需要保存
            if (playerData.needsSave() && System.currentTimeMillis() - playerData.getLastSaveTime() > 30000) {
                savePlayerData(playerData);
                playerData.setLastSaveTime(System.currentTimeMillis());
                playerData.setNeedsSave(false);
            }
        }
        
        // 处理离线玩家的冷却时间更新
        updateOfflinePlayersCooldown();
    }
    
    /**
     * 更新离线玩家的冷却时间
     */
    private void updateOfflinePlayersCooldown() {
        // 获取所有保存的玩家数据文件
        File deathTotemDir = getDeathTotemDirectory();
        if (!deathTotemDir.exists() || !deathTotemDir.isDirectory()) {
            return;
        }
        
        File[] playerFiles = deathTotemDir.listFiles((dir, name) -> name.endsWith(".dat"));
        if (playerFiles == null) {
            return;
        }
        
        for (File playerFile : playerFiles) {
            try {
                String fileName = playerFile.getName();
                UUID playerUuid = UUID.fromString(fileName.substring(0, fileName.length() - 4));
                
                // 如果玩家在线，跳过（已经在内存中处理）
                if (playerDataMap.containsKey(playerUuid)) {
                    continue;
                }
                
                // 加载离线玩家数据
                PlayerDeathTotemData offlinePlayerData = loadOfflinePlayerData(playerUuid);
                if (offlinePlayerData != null && offlinePlayerData.isOnCooldown()) {
                    // 保存原始冷却时间用于比较
                    int originalTicks = offlinePlayerData.getRemainingCooldownTicks();
                    
                    // 更新冷却时间（每tick减少1）
                    offlinePlayerData.tickWithoutSave();
                    
                    // 如果冷却时间有变化，保存数据
                    if (offlinePlayerData.getRemainingCooldownTicks() != originalTicks) {
                        savePlayerData(offlinePlayerData);
                    }
                }
            } catch (Exception e) {
                // 忽略无效的文件名
            }
        }
    }
    
    /**
     * 加载离线玩家数据
     */
    private PlayerDeathTotemData loadOfflinePlayerData(UUID playerUuid) {
        File saveFile = getSaveFile(playerUuid);
        if (!saveFile.exists()) {
            return null;
        }
        
        try {
            CompoundTag rootTag = NbtIo.read(saveFile);
            if (rootTag != null) {
                PlayerDeathTotemData playerData = new PlayerDeathTotemData(playerUuid);
                playerData.loadFromNBT(rootTag);
                return playerData;
            }
        } catch (IOException e) {
            System.err.println("Failed to load offline player death totem data from file: " + saveFile.getAbsolutePath());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * 获取死亡图腾数据目录
     */
    private File getDeathTotemDirectory() {
        if (server != null) {
            File worldDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR).toFile();
            return new File(worldDir, "death_totem");
        } else {
            // 服务器未设置时使用临时路径
            return new File("temp_death_totem");
        }
    }
    
    /**
     * 服务器关闭时保存所有数据
     */
    public void onServerStopping() {
        for (PlayerDeathTotemData playerData : playerDataMap.values()) {
            savePlayerData(playerData);
        }
        playerDataMap.clear();
    }
    
    /**
     * 强制保存所有玩家的数据
     */
    public void forceSaveAll() {
        for (PlayerDeathTotemData playerData : playerDataMap.values()) {
            savePlayerData(playerData);
        }
    }
    
    /**
     * 处理玩家重生事件，重置死亡图腾冷却状态
     */
    public void onPlayerRespawn(Player player) {
        if (player == null) return;
        
        PlayerDeathTotemData playerData = getPlayerData(player);
        if (playerData != null) {
            playerData.resetCooldownState();
        }
    }
}