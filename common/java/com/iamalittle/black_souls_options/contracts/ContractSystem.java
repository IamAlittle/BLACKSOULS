package com.iamalittle.black_souls_options.contracts;

import com.iamalittle.black_souls_options.common.Events;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import java.util.UUID;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 契约系统主类，负责管理所有玩家的契约管理器
 */
public class ContractSystem {
    private static ContractSystem instance;
    private Timer updateTimer; // 定时更新器
    private static final long UPDATE_INTERVAL_MS = 200; // 更新间隔：200毫秒（更频繁的更新）
    
    // 静态初始化块，注册事件监听器
    static {
        // 注册区块加载事件监听器
        Events.ChunkLoaded.add(event -> {
            // 当区块加载时，更新所有契约位置
            getInstance().updateAllContracts();
        });
        
        // 注册区块卸载事件监听器
        Events.ChunkUnloaded.add(event -> {
            // 当区块卸载时，更新所有契约位置状态
            getInstance().updateAllContracts();
        });
    }
    
    private ContractSystem() {
        startUpdateTimer();
    }
    
    /**
     * 启动定时更新器
     */
    private void startUpdateTimer() {
        if (updateTimer != null) {
            updateTimer.cancel();
        }
        updateTimer = new Timer("ContractSystemUpdateTimer", true);
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // 定期更新所有契约位置
                updateAllContracts();
            }
        }, UPDATE_INTERVAL_MS, UPDATE_INTERVAL_MS);
    }
    
    /**
     * 获取契约系统单例
     */
    public static synchronized ContractSystem getInstance() {
        if (instance == null) {
            instance = new ContractSystem();
        }
        return instance;
    }
    
    /**
     * 获取当前玩家的契约管理器
     */
    public static ContractManager getContractManager() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            return ContractManagerHelper.getAppropriateContractManager(minecraft.player);
        }
        return null;
    }
    
    /**
     * 更新所有在线玩家的契约管理器中的实体位置
     */
    public void updateAllContracts() {
        // 获取全局契约管理器并更新所有玩家的实体位置
        GlobalContractManager globalManager = GlobalContractManager.getInstance();
        
        // 遍历所有玩家的契约管理器并更新实体位置
        for (ContractManager manager : globalManager.getAllServerContractManagers()) {
            manager.updateAllEntityPositions();
        }
        
        // 检查并移除实体已消失的契约（每5秒检查一次）
        if (System.currentTimeMillis() % 5000 < UPDATE_INTERVAL_MS) {
            checkVanishedEntityContracts();
        }
    }
    
    /**
     * 检查并移除实体已消失的契约
     */
    private void checkVanishedEntityContracts() {
        // 获取全局契约管理器
        GlobalContractManager globalManager = GlobalContractManager.getInstance();
        
        // 遍历所有玩家的契约管理器并检查消失的实体
        for (ContractManager manager : globalManager.getAllServerContractManagers()) {
            manager.checkAndRemoveVanishedEntityContracts();
        }
    }
}