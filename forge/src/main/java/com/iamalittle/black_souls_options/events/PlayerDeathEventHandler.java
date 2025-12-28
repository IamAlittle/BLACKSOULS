package com.iamalittle.black_souls_options.events;

import com.iamalittle.black_souls_options.effects.DeathTotemEffect;
import com.iamalittle.black_souls_options.contracts.GlobalContractManager;
import com.iamalittle.black_souls_options.contracts.ContractManager;
import com.iamalittle.black_souls_options.contracts.Contract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.EvokerContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.SlimeContract;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 玩家死亡事件处理器 - Forge版本
 */
@Mod.EventBusSubscriber
public class PlayerDeathEventHandler {
    
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            // 只在服务端处理
            if (!player.level().isClientSide()) {
                // 检查是否有激活的唤魔者契约效果
                boolean hasActiveEvokerContract = false;
                ContractManager manager = GlobalContractManager.getInstance().getContractManager(player);
                if (manager != null) {
                    hasActiveEvokerContract = manager.getAllContracts().stream()
                        .anyMatch(contract -> "minecraft:evoker".equals(contract.getEntityType()) && 
                            contract.getEffects().stream().anyMatch(effect -> effect.isActive()));
                }
                
                // 检查是否可以触发不死图腾效果（通过唤魔者契约）
                if (hasActiveEvokerContract && EvokerContract.canTriggerTotem(player)) {
                    // 取消死亡事件，触发图腾效果
                    event.setCanceled(true);
                    EvokerContract.triggerTotemEffect(player);
                    
                    // 发送消息给玩家
                    if (player instanceof ServerPlayer serverPlayer) {
                        serverPlayer.sendSystemMessage(Component.literal("§6你被唤魔者的不死图腾效果拯救了！"));
                    }
                }
                // 如果唤魔者契约没有触发，检查史莱姆契约
                else {
                    // 检查是否有激活的史莱姆契约效果
                    boolean hasActiveSlimeContract = false;
                    if (manager != null) {
                        hasActiveSlimeContract = manager.getAllContracts().stream()
                            .anyMatch(contract -> ("minecraft:slime".equals(contract.getEntityType()) || 
                                                   "minecraft:magma_cube".equals(contract.getEntityType())) && 
                                contract.getEffects().stream().anyMatch(effect -> effect.isActive()));
                    }
                    
                    // 检查是否可以触发史莱姆分裂重生效果
                    if (hasActiveSlimeContract && SlimeContract.canTriggerSlimeRebirth(player)) {
                        // 取消死亡事件，触发史莱姆分裂重生效果
                        event.setCanceled(true);
                        SlimeContract.triggerSlimeRebirth(player);
                    }
                }
            }
        }
    }
    
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        // 玩家重生时重置冷却状态
        DeathTotemEffect.onPlayerRespawn(player);
        
        // 玩家重生时重置史莱姆契约数据
        SlimeContract.resetPlayerData(player);
    }
    
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // 玩家重生时（从死亡状态恢复）
        if (event.isWasDeath()) {
            Player newPlayer = event.getEntity();
            DeathTotemEffect.onPlayerRespawn(newPlayer);
        }
    }
    
    // 注意：死亡图腾效果的冷却时间更新已移至DeathTotemEventsForge.java中处理
    // 避免重复调用导致冷却时间更新过快
    // @SubscribeEvent
    // public static void onServerTick(TickEvent.ServerTickEvent event) {
    //     if (event.phase == TickEvent.Phase.END) {
    //         // 每刻更新死亡图腾效果
    //         DeathTotemEffect.tick();
    //     }
    // }
}