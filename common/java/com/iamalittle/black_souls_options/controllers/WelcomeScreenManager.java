package com.iamalittle.black_souls_options.controllers;

import com.iamalittle.black_souls_options.config.BlackSoulsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 欢迎界面管理器，负责管理存档和服务器流程记录
 * 当玩家在存档或服务器中完成欢迎界面流程后，记录该存档或服务器IP
 * 下次进入时检测到已记录则不再显示欢迎界面
 */
public class WelcomeScreenManager {
    private static WelcomeScreenManager instance;
    private static final String WELCOME_DATA_FILE = "welcome_screen_data.dat";
    private static final String TAG_COMPLETED_WORLDS = "completedWorlds";
    private static final String TAG_COMPLETED_SERVERS = "completedServers";
    
    private final Set<String> completedWorlds;
    private final Set<String> completedServers;
    private File dataFile;
    
    private WelcomeScreenManager() {
        this.completedWorlds = new HashSet<>();
        this.completedServers = new HashSet<>();
        initializeDataFile();
        loadData();
    }
    
    /**
     * 获取欢迎界面管理器实例
     */
    public static WelcomeScreenManager getInstance() {
        if (instance == null) {
            instance = new WelcomeScreenManager();
        }
        return instance;
    }
    
    /**
     * 初始化数据文件路径
     */
    private void initializeDataFile() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            // 使用Minecraft游戏目录下的配置文件夹
            File gameDir = minecraft.gameDirectory;
            File configDir = new File(gameDir, "config");
            File modConfigDir = new File(configDir, "black_souls_options");
            
            if (!modConfigDir.exists()) {
                modConfigDir.mkdirs();
            }
            
            this.dataFile = new File(modConfigDir, WELCOME_DATA_FILE);
        } else {
            // 备用路径，如果Minecraft实例不可用
            this.dataFile = new File("./config/black_souls_options/" + WELCOME_DATA_FILE);
        }
    }
    
    /**
     * 加载数据
     */
    private void loadData() {
        if (dataFile.exists()) {
            try {
                CompoundTag rootTag = NbtIo.read(dataFile);
                if (rootTag != null) {
                    // 加载已完成的存档列表
                    if (rootTag.contains(TAG_COMPLETED_WORLDS)) {
                        CompoundTag worldsTag = rootTag.getCompound(TAG_COMPLETED_WORLDS);
                        for (String worldId : worldsTag.getAllKeys()) {
                            completedWorlds.add(worldId);
                        }
                    }
                    
                    // 加载已完成的服务器列表
                    if (rootTag.contains(TAG_COMPLETED_SERVERS)) {
                        CompoundTag serversTag = rootTag.getCompound(TAG_COMPLETED_SERVERS);
                        for (String serverIp : serversTag.getAllKeys()) {
                            completedServers.add(serverIp);
                        }
                    }
                    
                    BlackSoulsConfig.debug("[WelcomeScreenManager] Loaded welcome screen data: " + 
                        completedWorlds.size() + " worlds, " + completedServers.size() + " servers");
                }
            } catch (IOException e) {
                BlackSoulsConfig.error("[WelcomeScreenManager] Failed to load welcome screen data: " + e.getMessage());
            }
        }
    }
    
    /**
     * 保存数据
     */
    private void saveData() {
        try {
            CompoundTag rootTag = new CompoundTag();
            
            // 保存已完成的存档列表
            CompoundTag worldsTag = new CompoundTag();
            for (String worldId : completedWorlds) {
                worldsTag.putBoolean(worldId, true);
            }
            rootTag.put(TAG_COMPLETED_WORLDS, worldsTag);
            
            // 保存已完成的服务器列表
            CompoundTag serversTag = new CompoundTag();
            for (String serverIp : completedServers) {
                serversTag.putBoolean(serverIp, true);
            }
            rootTag.put(TAG_COMPLETED_SERVERS, serversTag);
            
            // 确保目录存在
            File parentDir = dataFile.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            NbtIo.write(rootTag, dataFile);
            BlackSoulsConfig.debug("[WelcomeScreenManager] Saved welcome screen data");
        } catch (IOException e) {
            BlackSoulsConfig.error("[WelcomeScreenManager] Failed to save welcome screen data: " + e.getMessage());
        }
    }
    
    /**
     * 检查当前存档或服务器是否需要显示欢迎界面
     */
    public boolean shouldShowWelcomeScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.level == null) {
            return false;
        }
        
        // 检查是否是服务器连接
        if (minecraft.getCurrentServer() != null) {
            ServerData serverData = minecraft.getCurrentServer();
            String serverKey = generateServerKey(serverData);
            
            // 如果服务器已记录，不显示欢迎界面
            if (completedServers.contains(serverKey)) {
                BlackSoulsConfig.debug("[WelcomeScreenManager] Server already completed welcome screen: " + serverKey);
                return false;
            }
            
            BlackSoulsConfig.debug("[WelcomeScreenManager] First time on server: " + serverKey);
            return true;
        } else {
            // 单人游戏存档
            String worldKey = generateWorldKey();
            
            // 如果存档已记录，不显示欢迎界面
            if (completedWorlds.contains(worldKey)) {
                BlackSoulsConfig.debug("[WelcomeScreenManager] World already completed welcome screen: " + worldKey);
                return false;
            }
            
            BlackSoulsConfig.debug("[WelcomeScreenManager] First time in world: " + worldKey);
            return true;
        }
    }
    
    /**
     * 标记当前存档或服务器已完成欢迎界面流程
     */
    public void markWelcomeScreenCompleted() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.level == null) {
            return;
        }
        
        boolean changed = false;
        
        // 检查是否是服务器连接
        if (minecraft.getCurrentServer() != null) {
            ServerData serverData = minecraft.getCurrentServer();
            String serverKey = generateServerKey(serverData);
            
            if (!completedServers.contains(serverKey)) {
                completedServers.add(serverKey);
                changed = true;
                BlackSoulsConfig.debug("[WelcomeScreenManager] Marked server as completed: " + serverKey);
            }
        } else {
            // 单人游戏存档
            String worldKey = generateWorldKey();
            
            if (!completedWorlds.contains(worldKey)) {
                completedWorlds.add(worldKey);
                changed = true;
                BlackSoulsConfig.debug("[WelcomeScreenManager] Marked world as completed: " + worldKey);
            }
        }
        
        if (changed) {
            saveData();
        }
    }
    
    /**
     * 生成存档的唯一标识符
     */
    private String generateWorldKey() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.level == null) {
            return "unknown_world";
        }
        
        // 在客户端环境中，使用当前加载的存档文件夹名称作为唯一标识
        try {
            // 获取游戏目录
            File gameDir = minecraft.gameDirectory;
            if (gameDir == null) {
                return "unknown_world";
            }
            
            // 获取存档文件夹路径（在客户端，存档位于saves目录下）
            File savesDir = new File(gameDir, "saves");
            if (!savesDir.exists()) {
                return "unknown_world";
            }
            
            // 获取当前世界的显示名称
            String levelName = "singleplayer";
            if (minecraft.level.getServer() != null) {
                levelName = minecraft.level.getServer().getWorldData().getLevelName();
            }

            File currentSaveFolder = null;
            long latestModified = 0;
            
            File[] saveFolders = savesDir.listFiles(File::isDirectory);
            if (saveFolders != null) {
                for (File saveFolder : saveFolders) {
                    File levelDat = new File(saveFolder, "level.dat");
                    if (levelDat.exists()) {
                        long modifiedTime = levelDat.lastModified();
                        if (modifiedTime > latestModified) {
                            latestModified = modifiedTime;
                            currentSaveFolder = saveFolder;
                        }
                    }
                }
            }
            
            if (currentSaveFolder != null) {
                // 使用简化的路径格式：\saves\存档名称
                String relativePath = "\\saves\\" + currentSaveFolder.getName();
                return relativePath;
            }
            
            // 如果找不到当前存档文件夹，使用存档名称作为key
            return levelName;
        } catch (Exception e) {
            BlackSoulsConfig.error("[WelcomeScreenManager] Error generating world key: " + e.getMessage());
            return "singleplayer";
        }
    }
    
    /**
     * 生成服务器的唯一标识符
     */
    private String generateServerKey(ServerData serverData) {
        if (serverData == null) {
            return "unknown_server";
        }
        
        // 使用服务器IP和端口生成唯一标识
        String ip = serverData.ip != null ? serverData.ip : "unknown";
        String name = serverData.name != null ? serverData.name : "unknown";
        
        return ip + "_" + name;
    }
    
    /**
     * 清除所有记录的数据（用于调试或重置）
     */
    public void clearAllData() {
        completedWorlds.clear();
        completedServers.clear();
        
        if (dataFile.exists()) {
            dataFile.delete();
        }
        
        BlackSoulsConfig.debug("[WelcomeScreenManager] Cleared all welcome screen data");
    }
    
    /**
     * 获取统计信息
     */
    public String getStatistics() {
        return "Completed worlds: " + completedWorlds.size() + 
               ", Completed servers: " + completedServers.size();
    }
}