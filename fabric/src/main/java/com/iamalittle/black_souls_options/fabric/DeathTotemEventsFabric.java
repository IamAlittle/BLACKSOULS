package com.iamalittle.black_souls_options.fabric;

import com.iamalittle.black_souls_options.effects.DeathTotemDataManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric版本的死亡图腾事件处理器
 */
public class DeathTotemEventsFabric {
    
    public static void initialize() {
        // 服务器启动时设置服务器实例
        ServerLifecycleEvents.SERVER_STARTING.register(DeathTotemEventsFabric::onServerStarting);
        
        // 服务器停止时保存所有数据
        ServerLifecycleEvents.SERVER_STOPPING.register(DeathTotemEventsFabric::onServerStopping);
        
        // 玩家加入游戏时创建数据管理器
        ServerPlayConnectionEvents.JOIN.register((listener, sender, server) -> {
            // 获取玩家的死亡图腾数据，这会自动加载对应的数据文件
            DeathTotemDataManager.getInstance().getPlayerData(listener.player);
            System.out.println("[BLACKSOULS] Death totem data manager created for player: " + listener.player.getScoreboardName());
        });
        
        // 玩家退出游戏时移除数据管理器并保存数据
        ServerPlayConnectionEvents.DISCONNECT.register((listener, server) -> {
            DeathTotemDataManager.getInstance().removePlayerData(listener.player.getUUID());
            System.out.println("[BLACKSOULS] Death totem data manager removed for player: " + listener.player.getScoreboardName());
        });
        
        // 定期更新死亡图腾数据
        ServerTickEvents.END_SERVER_TICK.register(DeathTotemEventsFabric::onServerTick);
        
        // 玩家重生事件处理
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof ServerPlayer) {
                ServerPlayer player = (ServerPlayer) entity;
                // 检查是否是玩家重生（新实体加载）
                if (player.isAlive() && player.getHealth() > 0) {
                    // 重置死亡图腾冷却状态
                    DeathTotemDataManager.getInstance().onPlayerRespawn(player);
                    System.out.println("[BLACKSOULS] Death totem cooldown reset for player: " + player.getScoreboardName());
                }
            }
        });
        
        System.out.println("[BLACKSOULS] Death totem events initialized for Fabric");
    }
    
    private static void onServerStarting(MinecraftServer server) {
        DeathTotemDataManager.getInstance().setServer(server);
        System.out.println("[BLACKSOULS] Death totem system initialized for server");
    }
    
    private static void onServerStopping(MinecraftServer server) {
        DeathTotemDataManager.getInstance().onServerStopping();
        System.out.println("[BLACKSOULS] Death totem system saved all data on server shutdown");
    }
    
    private static void onServerTick(MinecraftServer server) {
        DeathTotemDataManager.getInstance().tick();
    }
}