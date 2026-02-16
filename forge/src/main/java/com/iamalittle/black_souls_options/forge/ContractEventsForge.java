package com.iamalittle.black_souls_options.forge;

import com.iamalittle.black_souls_options.contracts.GlobalContractManager;
import com.iamalittle.black_souls_options.contracts.ContractSyncManager;
import com.iamalittle.black_souls_options.contracts.ContractManager;
import com.iamalittle.black_souls_options.contracts.Contract;
import com.iamalittle.black_souls_options.contracts.ClientContractManager;
import com.iamalittle.black_souls_options.contracts.effects.mobs.AxolotlContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.CreeperContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.GlowSquidContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.GuardianThornsContract;
import com.iamalittle.black_souls_options.contracts.effects.AttackEventHandler;
import com.iamalittle.black_souls_options.contracts.effects.BlockBreakEventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.iamalittle.black_souls_options.config.BlackSoulsConfig;

/**
 * Forge版本的契约事件处理器
 */
@Mod.EventBusSubscriber(modid = "black_souls_options", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ContractEventsForge {
    
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        GlobalContractManager.getInstance().setServer(event.getServer());
        
        // 注册契约管理指令
        registerContractCommands(event.getServer());
        
        BlackSoulsConfig.debug("Contract system initialized for server");
    }
    
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        GlobalContractManager.getInstance().onServerStopping();
        BlackSoulsConfig.debug("Contract system saved all data on server shutdown");
    }
    
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        // 获取玩家的契约管理器，这会自动加载对应的数据文件
        GlobalContractManager.getInstance().getServerContractManager(event.getEntity());
        
        // 同步契约数据到客户端
        if (event.getEntity() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            ContractSyncManager.onPlayerJoin(player);
        }
        
        BlackSoulsConfig.debug("Contract manager created for player: " + event.getEntity().getScoreboardName());
    }
    
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // 在移除契约管理器前，清理发光鱿鱼契约的光源方块
        cleanupGlowSquidLightBlocks(event.getEntity());
        
        GlobalContractManager.getInstance().removeServerContractManager(event.getEntity().getUUID());
        BlackSoulsConfig.debug("Contract manager removed for player: " + event.getEntity().getScoreboardName());
    }
    
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            GlobalContractManager.getInstance().tick();
        }
    }
    
    /**
     * 处理玩家复活事件，同步契约效果状态
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            // 同步契约效果状态
            if (event.getEntity() instanceof ServerPlayer) {
                ServerPlayer player = (ServerPlayer) event.getEntity();
                ContractSyncManager.syncContractEffects(player);
                
                BlackSoulsConfig.debug("Player respawn detected: " + player.getScoreboardName());
            }
        }
    }
    
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        // 玩家重生事件，这里不需要额外同步，因为onPlayerClone已经处理了
        if (event.getEntity() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            BlackSoulsConfig.debug("Player respawn event: " + player.getScoreboardName());
        }
    }
    
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        // 玩家死亡事件，停用所有契约效果
        if (event.getEntity() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.getEntity();
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
            BlackSoulsConfig.debug("Contract effects maintained on player death: " + player.getScoreboardName());
        }
        }
    }

    /**
     * 处理玩家攻击事件，触发契约效果
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // 检查伤害来源是否为玩家
        if (event.getSource().getEntity() instanceof ServerPlayer) {
            ServerPlayer attacker = (ServerPlayer) event.getSource().getEntity();
            Entity target = event.getEntity();
            
            // 处理攻击事件
            AttackEventHandler.onPlayerAttack(attacker, target, event.getSource());
        }
        

    }
    
    /**
     * 处理玩家破坏方块事件，触发契约效果
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        // 检查玩家是否拥有蠹虫契约
        if (event.getPlayer() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.getPlayer();
            
            // 处理方块破坏事件
            BlockBreakEventHandler.onPlayerBreakBlock(player, event.getPos(), event.getState());
        }
    }
    
    /**
     * 处理玩家开始挖掘方块事件（玩家按左键时立即触发）
     */
    @SubscribeEvent
    public static void onPlayerLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        // 检查玩家是否拥有蠹虫契约
        if (event.getEntity() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            
            // 处理开始挖掘事件，如果返回true则阻止正常挖掘过程
            boolean shouldCancel = BlockBreakEventHandler.onPlayerStartBreakBlock(player, event.getPos(), event.getLevel().getBlockState(event.getPos()));
            if (shouldCancel) {
                event.setCanceled(true);
            }
        }
    }
    
    /**
     * 清理玩家发光鱿鱼契约的光源方块
     * 在玩家离开服务器时调用，防止光源方块遗留
     */
    private static void cleanupGlowSquidLightBlocks(net.minecraft.world.entity.player.Player player) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        
        // 调用发光鱿鱼契约的清理方法
        GlowSquidContract.cleanupPlayerLightBlocks(player);
        BlackSoulsConfig.debug("Cleaned up glow squid light blocks for player: " + player.getScoreboardName());
    }
    
    /**
     * 注册契约管理指令
     */
    private static void registerContractCommands(MinecraftServer server) {
        try {
            // 使用反射调用BSContractCommand的注册方法
            Class<?> commandClass = Class.forName("com.iamalittle.black_souls_options.commands.BSContractCommand");
            java.lang.reflect.Method registerMethod = commandClass.getMethod("register", MinecraftServer.class);
            registerMethod.invoke(null, server);
            BlackSoulsConfig.debug("Contract commands registered successfully");
        } catch (Exception e) {
            BlackSoulsConfig.error("Failed to register contract commands: " + e.getMessage());
        }
    }
    
    /**
     * 客户端tick事件处理
     */
    @Mod.EventBusSubscriber(modid = "black_souls_options", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientTickHandler {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            
            Minecraft minecraft = Minecraft.getInstance();
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
}