package com.iamalittle.black_souls_options.contracts;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 契约效果同步管理器
 * 处理玩家死亡/重生时的契约效果状态同步
 */
public class ContractSyncManager {
    
    /**
     * 同步玩家的契约效果状态
     * 在玩家死亡/重生时调用
     */
    public static void syncContractEffects(ServerPlayer player) {
        if (player == null) return;
        
        ContractManager manager = GlobalContractManager.getInstance().getContractManager(player);
        if (manager != null) {
            // 检查是否需要重新加载数据（避免重复加载）
            if (manager.getContractCount() == 0) {
                // 如果契约数量为0，重新加载数据
                manager.forceReload();
                System.out.println("[BLACKSOULS] Contract data reloaded for player: " + player.getScoreboardName());
            }
            
            // 关键修复：重生时重新激活所有契约效果
            manager.activateAllEffects(player);
            System.out.println("[BLACKSOULS] Contract effects reactivated for player: " + player.getScoreboardName());
        }
    }
    
    /**
     * 处理玩家重生事件
     */
    public static void onPlayerRespawn(ServerPlayer player) {
        // 延迟一tick执行，确保玩家完全重生
        player.getServer().execute(() -> {
            syncContractEffects(player);
        });
    }
    
    /**
     * 处理玩家加入服务器事件
     */
    public static void onPlayerJoin(ServerPlayer player) {
        syncContractEffects(player);
    }
}