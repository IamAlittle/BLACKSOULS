package com.iamalittle.black_souls_options.forge;

import com.iamalittle.black_souls_options.contracts.GlobalContractManager;
import com.iamalittle.black_souls_options.contracts.ContractSyncManager;
import com.iamalittle.black_souls_options.contracts.ContractManager;
import com.iamalittle.black_souls_options.contracts.Contract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.AxolotlContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.CreeperContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.GlowSquidContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.GuardianThornsContract;
import com.iamalittle.black_souls_options.contracts.effects.AttackEventHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge版本的契约事件处理器
 */
@Mod.EventBusSubscriber(modid = "black_souls_options", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ContractEventsForge {
    
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        GlobalContractManager.getInstance().setServer(event.getServer());
        System.out.println("[BLACKSOULS] Contract system initialized for server");
    }
    
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        GlobalContractManager.getInstance().onServerStopping();
        System.out.println("[BLACKSOULS] Contract system saved all data on server shutdown");
    }
    
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        // 获取玩家的契约管理器，这会自动加载对应的数据文件
        GlobalContractManager.getInstance().getContractManager(event.getEntity());
        
        // 同步契约数据到客户端
        if (event.getEntity() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            ContractSyncManager.onPlayerJoin(player);
        }
        
        System.out.println("[BLACKSOULS] Contract manager created for player: " + event.getEntity().getScoreboardName());
    }
    
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // 在移除契约管理器前，清理发光鱿鱼契约的光源方块
        cleanupGlowSquidLightBlocks(event.getEntity());
        
        GlobalContractManager.getInstance().removeContractManager(event.getEntity().getUUID());
        System.out.println("[BLACKSOULS] Contract manager removed for player: " + event.getEntity().getScoreboardName());
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
                
                System.out.println("[BLACKSOULS] Player respawn detected: " + player.getScoreboardName());
            }
        }
    }
    
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        // 玩家重生事件，这里不需要额外同步，因为onPlayerClone已经处理了
        if (event.getEntity() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            System.out.println("[BLACKSOULS] Player respawn event: " + player.getScoreboardName());
        }
    }
    
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        // 玩家死亡事件，停用所有契约效果
        if (event.getEntity() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            ContractManager manager = GlobalContractManager.getInstance().getContractManager(player);
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
            
            // 停用所有契约效果
            for (Contract contract : manager.getAllContracts()) {
                contract.deactivateEffects(player);
            }
            System.out.println("[BLACKSOULS] All contract effects deactivated on player death: " + player.getScoreboardName());
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
     * 清理玩家发光鱿鱼契约的光源方块
     * 在玩家离开服务器时调用，防止光源方块遗留
     */
    private static void cleanupGlowSquidLightBlocks(net.minecraft.world.entity.player.Player player) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        
        // 调用发光鱿鱼契约的清理方法
        GlowSquidContract.cleanupPlayerLightBlocks(player);
        System.out.println("[BLACKSOULS] Cleaned up glow squid light blocks for player: " + player.getScoreboardName());
    }
}