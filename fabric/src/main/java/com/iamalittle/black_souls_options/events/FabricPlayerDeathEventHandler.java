package com.iamalittle.black_souls_options.events;

import com.iamalittle.black_souls_options.contracts.GlobalContractManager;
import com.iamalittle.black_souls_options.contracts.ContractManager;
import com.iamalittle.black_souls_options.contracts.Contract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.EvokerContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.SlimeContract;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;

/**
 * 玩家死亡事件处理器 - Fabric版本
 */
public class FabricPlayerDeathEventHandler {
    
    public static void initialize() {
        // 注册玩家死亡事件
        ServerPlayerEvents.ALLOW_DEATH.register((player, damageSource, damageAmount) -> {
            // 检查是否有激活的唤魔者契约效果
            boolean hasActiveEvokerContract = false;
            ContractManager manager = GlobalContractManager.getInstance().getServerContractManager(player);
            if (manager != null) {
                hasActiveEvokerContract = manager.getAllContracts().stream()
                    .anyMatch(contract -> "minecraft:evoker".equals(contract.getEntityType()) && 
                        contract.getEffects().stream().anyMatch(effect -> effect.isActive()));
            }
            
            // 检查是否可以触发不死图腾效果（通过唤魔者契约）
            if (hasActiveEvokerContract && EvokerContract.hasTotemInInventory(player)) {
                // 触发图腾效果
                EvokerContract.triggerTotemEffect(player);
                
                // 发送消息给玩家
                player.sendSystemMessage(Component.translatable("black_souls_options.messages.totem_of_undying_saved"));
                
                // 返回false取消死亡
                return false;
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
                    // 触发史莱姆分裂重生效果
                    SlimeContract.triggerSlimeRebirth(player);
                    
                    // 返回false取消死亡
                    return false;
                }
            }
            
            // 允许正常死亡
            return true;
        });
        
        // 注册玩家重生事件
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            // 玩家重生时重置史莱姆契约数据
            SlimeContract.resetPlayerData(newPlayer);
        });
    }
}