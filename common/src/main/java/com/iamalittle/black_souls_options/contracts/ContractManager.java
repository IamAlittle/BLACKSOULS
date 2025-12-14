package com.iamalittle.black_souls_options.contracts;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import com.iamalittle.black_souls_options.contracts.effects.ContractEffectRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

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
}