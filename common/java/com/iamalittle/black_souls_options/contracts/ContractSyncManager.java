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
     * 在玩家死亡/重生时调用（仅在服务器端执行）
     */
    public static void syncContractEffects(ServerPlayer player) {
        if (player == null || player.level() == null || player.level().isClientSide()) {
            System.out.println("[BLACKSOULS] Warning: Attempted to sync contract effects on client side");
            return;
        }
        
        ContractManager manager = GlobalContractManager.getInstance().getContractManager(player);
        if (manager != null) {
            // 检查是否需要重新加载数据（避免重复加载）
            if (manager.getContractCount() == 0) {
                // 如果契约数量为0，重新加载数据
                manager.forceReload();
                System.out.println("[BLACKSOULS] Contract data reloaded for player: " + player.getScoreboardName());
            }
            
            // 关键修复：重生时只重新激活之前已激活的契约效果
            // 避免激活所有契约，包括那些玩家手动关闭的契约
            manager.reactivateActiveEffects(player);
            System.out.println("[BLACKSOULS] Active contract effects reactivated for player: " + player.getScoreboardName());
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
        
        // 同步契约数据到客户端
        syncContractDataToClient(player);
    }
    
    /**
     * 同步契约数据到客户端
     */
    public static void syncContractDataToClient(ServerPlayer player) {
        if (player == null || player.level() == null || player.level().isClientSide()) {
            System.out.println("[BLACKSOULS] Warning: Attempted to sync contract data on client side");
            return;
        }
        
        // 延迟一tick执行，确保玩家完全加入游戏
        player.getServer().execute(() -> {
            try {
                // 导入网络处理器类
                Class<?> networkHandlerClass = Class.forName("com.iamalittle.black_souls_options.network.ContractNetworkHandler");
                java.lang.reflect.Method method = networkHandlerClass.getMethod("sendContractDataToPlayer", ServerPlayer.class, boolean.class);
                method.invoke(null, player, true); // true表示全量同步
                System.out.println("[BLACKSOULS] Contract data synced to client for player: " + player.getScoreboardName());
            } catch (Exception e) {
                System.err.println("[BLACKSOULS] Failed to sync contract data to client: " + e.getMessage());
            }
        });
    }
}