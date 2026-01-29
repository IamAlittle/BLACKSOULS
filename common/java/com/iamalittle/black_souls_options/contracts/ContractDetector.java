package com.iamalittle.black_souls_options.contracts;

import net.minecraft.world.entity.player.Player;

/**
 * 契约检测器 - 用于检测玩家是否拥有特定契约
 * 契约只作为检测，不包含激活逻辑
 * 
 * 注意：各个契约类已实现自己的检测方法，此类仅保留通用检测功能
 */
public class ContractDetector {
    
    /**
     * 检测玩家是否拥有特定实体类型的契约
     * @param player 玩家
     * @param entityType 实体类型（如"minecraft:llama"）
     * @return 是否拥有该契约
     */
    public static boolean hasContract(Player player, String entityType) {
        if (player == null || entityType == null) {
            return false;
        }
        
        ContractManager manager = ContractManagerHelper.getAppropriateContractManager(player);
        if (manager != null) {
            return manager.getAllContracts().stream()
                .anyMatch(contract -> entityType.equals(contract.getEntityType()) && 
                    contract.getEffects().stream().anyMatch(effect -> effect.isActive()));
        }
        
        return false;
    }
}