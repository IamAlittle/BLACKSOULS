package com.iamalittle.black_souls_options.forge;

import com.iamalittle.black_souls_options.effects.DeathTotemDataManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge版本的死亡图腾事件处理器
 */
@Mod.EventBusSubscriber(modid = "black_souls_options", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DeathTotemEventsForge {
    
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        DeathTotemDataManager.getInstance().setServer(event.getServer());
        System.out.println("[BLACKSOULS] Death totem system initialized for server");
    }
    
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        DeathTotemDataManager.getInstance().onServerStopping();
        System.out.println("[BLACKSOULS] Death totem system saved all data on server shutdown");
    }
    
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        // 获取玩家的死亡图腾数据，这会自动加载对应的数据文件
        DeathTotemDataManager.getInstance().getPlayerData(event.getEntity());
        System.out.println("[BLACKSOULS] Death totem data manager created for player: " + event.getEntity().getScoreboardName());
    }
    
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        DeathTotemDataManager.getInstance().removePlayerData(event.getEntity().getUUID());
        System.out.println("[BLACKSOULS] Death totem data manager removed for player: " + event.getEntity().getScoreboardName());
    }
    
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            DeathTotemDataManager.getInstance().tick();
        }
    }
    
    /**
     * 处理玩家重生事件，重置死亡图腾冷却状态
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            DeathTotemDataManager.getInstance().onPlayerRespawn(player);
            System.out.println("[BLACKSOULS] Death totem cooldown reset for player: " + player.getScoreboardName());
        }
    }
}