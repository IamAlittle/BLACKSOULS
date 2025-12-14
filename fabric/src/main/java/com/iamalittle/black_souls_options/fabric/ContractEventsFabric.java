package com.iamalittle.black_souls_options.fabric;

import com.iamalittle.black_souls_options.common.Events;
import com.iamalittle.black_souls_options.contracts.GlobalContractManager;
import com.iamalittle.black_souls_options.contracts.ContractSyncManager;
import com.iamalittle.black_souls_options.contracts.ContractManager;
import com.iamalittle.black_souls_options.contracts.Contract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.AxolotlContract;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

/**
 * Fabric版本的契约事件处理器
 */
public class ContractEventsFabric {
    
    public static void initialize() {
        // 服务器启动时设置服务器实例
        ServerLifecycleEvents.SERVER_STARTING.register(ContractEventsFabric::onServerStarting);
        
        // 服务器停止时保存所有数据
        ServerLifecycleEvents.SERVER_STOPPING.register(ContractEventsFabric::onServerStopping);
        
        // 玩家加入游戏时创建契约管理器
        ServerPlayConnectionEvents.JOIN.register((listener, sender, server) -> {
            // 获取玩家的契约管理器，这会自动加载对应的数据文件
            GlobalContractManager.getInstance().getContractManager(listener.player);
            
            // 同步契约效果状态
            if (listener.player instanceof ServerPlayer) {
                ContractSyncManager.onPlayerJoin((ServerPlayer) listener.player);
            }
            
            System.out.println("[BLACKSOULS] Contract manager created for player: " + listener.player.getScoreboardName());
        });
        
        // 玩家退出游戏时移除契约管理器并保存数据
        ServerPlayConnectionEvents.DISCONNECT.register((listener, server) -> {
            GlobalContractManager.getInstance().removeContractManager(listener.player.getUUID());
            System.out.println("[BLACKSOULS] Contract manager removed for player: " + listener.player.getScoreboardName());
        });
        
        // 玩家死亡事件处理 - 使用实体死亡事件
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            // 检查实体是否是玩家且已死亡
            if (entity instanceof ServerPlayer && !entity.isAlive()) {
                ServerPlayer player = (ServerPlayer) entity;
                onPlayerDeath(player, null);
            }
        });

        
        // 定期保存契约数据
        ServerTickEvents.END_SERVER_TICK.register(ContractEventsFabric::onServerTick);
        
        // 玩家复活事件处理
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof ServerPlayer) {
                ServerPlayer player = (ServerPlayer) entity;
                // 检查是否是玩家复活（新实体加载）
                if (player.isAlive() && player.getHealth() > 0) {
                    // 同步契约效果状态
                    ContractSyncManager.syncContractEffects(player);
                    
                    System.out.println("[BLACKSOULS] Player respawn detected: " + player.getScoreboardName());
                }
            }
        });
    }
    
    private static void onServerStarting(MinecraftServer server) {
        GlobalContractManager.getInstance().setServer(server);
        System.out.println("[BLACKSOULS] Contract system initialized for server");
    }
    
    private static void onServerStopping(MinecraftServer server) {
        GlobalContractManager.getInstance().onServerStopping();
        System.out.println("[BLACKSOULS] Contract system saved all data on server shutdown");
    }
    
    private static void onServerTick(MinecraftServer server) {
        GlobalContractManager.getInstance().tick();
    }
    
    /**
     * 处理玩家死亡事件，停用所有契约效果
     */
    private static void onPlayerDeath(Player player, DamageSource damageSource) {
        if (player instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            ContractManager manager = GlobalContractManager.getInstance().getContractManager(serverPlayer);
            if (manager != null) {
                // 停用所有契约效果
                for (Contract contract : manager.getAllContracts()) {
                    contract.deactivateEffects(serverPlayer);
                }
                // 保存状态变化到文件中
                manager.markForSave();
                System.out.println("[BLACKSOULS] All contract effects deactivated on player death: " + serverPlayer.getScoreboardName());
            }
        }
    }
}