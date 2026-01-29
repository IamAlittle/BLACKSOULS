package com.iamalittle.black_souls_options.fabric;

import com.iamalittle.black_souls_options.common.Events;
import com.iamalittle.black_souls_options.contracts.GlobalContractManager;
import com.iamalittle.black_souls_options.contracts.ContractSyncManager;
import com.iamalittle.black_souls_options.contracts.ContractManager;
import com.iamalittle.black_souls_options.contracts.Contract;
import com.iamalittle.black_souls_options.contracts.ClientContractManager;
import com.iamalittle.black_souls_options.contracts.effects.mobs.AxolotlContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.CreeperContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.GlowSquidContract;
import com.iamalittle.black_souls_options.contracts.effects.AttackEventHandler;
import com.iamalittle.black_souls_options.contracts.effects.BlockBreakEventHandler;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

/**
 * Fabric版本的契约事件处理器
 */
public class ContractEventsFabric {
    
    public static void initialize() {
        // 客户端tick事件处理
        ClientTickEvents.END_CLIENT_TICK.register(ContractEventsFabric::onClientTick);
        
        // 服务器启动时设置服务器实例
        ServerLifecycleEvents.SERVER_STARTING.register(ContractEventsFabric::onServerStarting);
        
        // 服务器停止时保存所有数据
        ServerLifecycleEvents.SERVER_STOPPING.register(ContractEventsFabric::onServerStopping);
        
        // 玩家加入游戏时创建契约管理器
        ServerPlayConnectionEvents.JOIN.register((listener, sender, server) -> {
            // 获取玩家的契约管理器，这会自动加载对应的数据文件
            GlobalContractManager.getInstance().getServerContractManager(listener.player);
            
            // 同步契约效果状态
            if (listener.player instanceof ServerPlayer) {
                ContractSyncManager.onPlayerJoin((ServerPlayer) listener.player);
            }
            
            System.out.println("[BLACKSOULS] Contract manager created for player: " + listener.player.getScoreboardName());
        });
        
        // 玩家退出游戏时移除契约管理器并保存数据
        ServerPlayConnectionEvents.DISCONNECT.register((listener, server) -> {
            // 在移除契约管理器前，清理发光鱿鱼契约的光源方块
            cleanupGlowSquidLightBlocks(listener.player);
            
            GlobalContractManager.getInstance().removeServerContractManager(listener.player.getUUID());
            System.out.println("[BLACKSOULS] Contract manager removed for player: " + listener.player.getScoreboardName());
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
        
        // 玩家攻击事件处理
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClientSide() && player instanceof ServerPlayer) {
                // 处理攻击事件
                AttackEventHandler.onPlayerAttack((ServerPlayer) player, entity);
            }
            return InteractionResult.PASS;
        });
        
        // 玩家方块破坏完成事件处理
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!world.isClientSide() && player instanceof ServerPlayer) {
                // 处理方块破坏完成事件
                BlockBreakEventHandler.onPlayerBreakBlock((ServerPlayer) player, pos, state);
            }
        });
        
        // 玩家开始挖掘方块事件处理（玩家按左键时立即触发）
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!world.isClientSide() && player instanceof ServerPlayer) {
                // 处理开始挖掘事件，如果返回true则阻止正常挖掘过程
                boolean shouldCancel = BlockBreakEventHandler.onPlayerStartBreakBlock((ServerPlayer) player, pos, state);
                return !shouldCancel; // 如果shouldCancel为true，则返回false阻止挖掘
            }
            return true; // 默认允许挖掘
        });
        
        // 玩家死亡事件处理
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity instanceof ServerPlayer) {
                ServerPlayer player = (ServerPlayer) entity;
                // 检查玩家是否死亡（实体卸载且不是活着状态）
                if (!player.isAlive()) {
                    ContractManager manager = GlobalContractManager.getInstance().getServerContractManager(player);
                    if (manager != null) {
                            // 检查是否有激活的苦力怕契约效果，如果有则触发自爆效果
                            boolean hasActiveCreeperContract = manager.getAllContracts().stream()
                                .anyMatch(contract -> "minecraft:creeper".equals(contract.getEntityType()) && 
                                    contract.getEffects().stream().anyMatch(effect -> effect.isActive()));
                            
                            if (hasActiveCreeperContract) {
                                // 触发苦力怕契约的自爆效果
                                CreeperContract creeperContract = new CreeperContract();
                                creeperContract.onPlayerDeath(player);
                            }
                            
                            // 移除停用所有契约效果的逻辑，让契约在玩家死亡后保持状态
                            // 不再调用 contract.deactivateEffects(player)
                            System.out.println("[BLACKSOULS] Contract effects maintained on player death: " + player.getScoreboardName());
                        }
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
     * 清理玩家发光鱿鱼契约的光源方块
     * 在玩家离开服务器时调用，防止光源方块遗留
     */
    private static void cleanupGlowSquidLightBlocks(Player player) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        
        // 调用发光鱿鱼契约的清理方法
        GlowSquidContract.cleanupPlayerLightBlocks(player);
        System.out.println("[BLACKSOULS] Cleaned up glow squid light blocks for player: " + player.getScoreboardName());
    }
    
    /**
     * 客户端tick事件处理
     */
    private static void onClientTick(Minecraft minecraft) {
        if (minecraft.player == null) return;
        
        // 获取客户端契约管理器
        ClientContractManager clientContractManager = ClientContractManager.getInstance();
        ContractManager manager = clientContractManager.getClientContractManager(minecraft.player);
        
        // 调用客户端管理器的playerTick方法
        if (manager != null) {
            manager.playerTick(minecraft);
        }
    }
}