package com.iamalittle.black_souls_options.contracts;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import com.iamalittle.black_souls_options.contracts.effects.ContractEffectRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;

import java.util.*;

public class ContractManager {
    
    private static final Map<UUID, List<Contract>> playerContracts = new HashMap<>();
    
    public static Contract createContract(Player player, Entity targetEntity) {
        Contract contract = new Contract(player, targetEntity);
        addEffectsToContract(contract, targetEntity);
        
        // 添加到玩家契约列表
        UUID playerId = player.getUUID();
        if (!playerContracts.containsKey(playerId)) {
            playerContracts.put(playerId, new ArrayList<>());
        }
        playerContracts.get(playerId).add(contract);
        
        System.out.println("Created contract for player " + player.getName().getString() + " with entity " + targetEntity.getName().getString());
        
        return contract;
    }
    
    public static void addEffectsToContract(Contract contract, Entity targetEntity) {
        EntityType<?> entityType = targetEntity.getType();
        List<ContractEffect> effects = ContractEffectRegistry.getEffectsForEntityType(entityType);
        
        for (ContractEffect effect : effects) {
            contract.addEffect(effect);
            System.out.println("Added effect " + effect.getName() + " to contract for entity type " + entityType);
        }
    }
    
    public static List<Contract> getPlayerContracts(Player player) {
        return playerContracts.getOrDefault(player.getUUID(), new ArrayList<>());
    }
    
    public static void removeContract(Player player, Contract contract) {
        UUID playerId = player.getUUID();
        if (playerContracts.containsKey(playerId)) {
            playerContracts.get(playerId).remove(contract);
            contract.deactivateEffects(player);
        }
    }
    
    public static void updateContracts(Player player) {
        List<Contract> contracts = getPlayerContracts(player);
        for (Contract contract : contracts) {
            contract.tickEffects(player);
        }
    }
    
    public static boolean hasContractsForEntityType(Player player, EntityType<?> entityType) {
        List<Contract> contracts = getPlayerContracts(player);
        for (Contract contract : contracts) {
            if (contract.getTargetEntityType() == entityType) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查并移除实体已消失的契约
     * 当契约对象被捕捉类模组变成物品或其他方式消失时调用
     */
    public static void checkAndRemoveVanishedEntityContracts(Player player) {
        List<Contract> contracts = getPlayerContracts(player);
        List<Contract> contractsToRemove = new ArrayList<>();
        
        for (Contract contract : contracts) {
            UUID entityId = contract.getTargetEntityId();
            
            // 检查实体是否仍然存在
            boolean entityExists = false;
            
            // 尝试在当前维度中查找实体
            if (player.level() != null) {
                Entity entity = player.level().getEntity(entityId);
                if (entity != null) {
                    entityExists = true;
                }
            }
            
            // 如果实体不存在，标记为需要移除
            if (!entityExists) {
                contractsToRemove.add(contract);
                System.out.println("检测到契约对象已消失，移除契约: " + contract.getTargetEntityName());
            }
        }
        
        // 移除已消失实体的契约
        for (Contract contract : contractsToRemove) {
            removeContract(player, contract);
            
            // 向玩家发送通知
            player.sendSystemMessage(Component.literal("§c契约对象 §e" + contract.getTargetEntityName() + " §c已消失，契约自动解除"));
        }
    }
    
    /**
     * 检查所有玩家的契约，移除实体已消失的契约
     */
    public static void checkAllVanishedEntityContracts() {
        for (Map.Entry<UUID, List<Contract>> entry : playerContracts.entrySet()) {
            // 获取玩家对象（简化实现，实际需要更复杂的玩家查找逻辑）
            // 这里假设可以通过某种方式获取玩家对象
            // 在实际实现中，可能需要通过服务器实例来查找玩家
            System.out.println("检查玩家 " + entry.getKey() + " 的契约状态");
        }
    }
}